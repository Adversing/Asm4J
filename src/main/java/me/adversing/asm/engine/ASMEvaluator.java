package me.adversing.asm.engine;

import jdk.internal.misc.Unsafe;
import lombok.Getter;
import lombok.Setter;
import me.adversing.asm.Operand;
import me.adversing.asm.diagnostic.DiagnosticService;
import me.adversing.asm.instruction.Instruction;
import me.adversing.asm.instruction.factory.InstructionFactory;
import me.adversing.asm.instruction.handler.InstructionHandler;
import me.adversing.asm.variable.Variable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ASMEvaluator implements AutoCloseable {
    // ------------------------------------------------
    // Unsafe singleton accessor
    // ------------------------------------------------
    private static final Unsafe UNSAFE;

    // ------------------------------------------------
    // MIPS Endianness Configuration
    // ------------------------------------------------
    private static final ByteOrder MIPS_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private static final ByteOrder HOST_BYTE_ORDER = ByteOrder.nativeOrder();
    private static final boolean NEEDS_BYTE_SWAP = !MIPS_BYTE_ORDER.equals(HOST_BYTE_ORDER);

    static {
        try {
            UNSAFE = (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
                    .findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Unsafe instance", e);
        }
    }

    // ------------------------------------------------
    // Configuration constants
    // ------------------------------------------------
    private static final int NUM_INT_REGISTERS   = 64;    // $t0..$t31 + $v0..$v31, etc.
    private static final int NUM_FP_REGISTERS    = 32;
    private static final int NUM_CP0_REGISTERS   = 32;

    private static final int SIZEOF_INT    = Integer.BYTES;     // 4 bytes
    private static final int SIZEOF_DOUBLE = Double.BYTES;      // 8 bytes
    private static final int SIZEOF_LONG   = Long.BYTES;        // 8 bytes
    private static final int SIZEOF_FLOAT  = Float.BYTES;       // 4 bytes
    private static final int SIZEOF_SHORT  = Short.BYTES;       // 2 bytes
    private static final int SIZEOF_BYTE   = Byte.BYTES;        // 1 byte

    private static final long INT_REGISTER_REGION_SIZE  = (long) NUM_INT_REGISTERS * SIZEOF_INT;
    private static final long FP_REGISTER_REGION_SIZE   = (long) NUM_FP_REGISTERS * SIZEOF_DOUBLE;
    private static final long CP0_REGISTER_REGION_SIZE  = (long) NUM_CP0_REGISTERS * SIZEOF_INT;
    private static final long MAIN_MEMORY_SIZE          = 4096 * 4096; // 4 MB

    // ------------------------------------------------
    // Off-heap memory addresses
    // ------------------------------------------------
    private final long intRegisterBase;   // base address of integer registers
    private final long fpRegisterBase;    // base address of floating-point registers
    private final long cp0RegisterBase;   // base address of CP0 registers
    private final long memoryBase;        // base address of main memory

    // ------------------------------------------------
    // Metadata for register name -> offset
    // ------------------------------------------------
    private final Map<String, Long> intRegisterOffsets = new HashMap<>();
    private final Map<String, Long> fpRegisterOffsets  = new HashMap<>();
    private final Map<String, Long> cp0RegisterOffsets = new HashMap<>();

    // Program state
    @Setter private boolean fpConditionFlag;
    @Setter private int programCounter;

    // Labels and diagnostic
    private final Map<String, Integer> labels = new HashMap<>();
    private final DiagnosticService diagnosticService;

    // "Load linked" bit
    private boolean llBit = false;

    // ------------------------------------------------

    @Getter private final Logger programLogger = LogManager.getLogger("programOutput");
    @Getter private final Logger debugLogger = LogManager.getLogger("debugOutput");
    @Getter private final Logger diagnosticLogger = LogManager.getLogger("diagnosticOutput");
    @Getter private final Map<String, Integer> variableAddresses;
    @Getter private final boolean debug;

    // ------------------------------------------------

    // Shutdown management
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private CompletableFuture<Void> shutdownFuture;

    public ASMEvaluator(DiagnosticService diagnosticService, boolean debug) {
        this.debug = debug;
        this.diagnosticService = diagnosticService.withLogger(diagnosticLogger);
        // off-heap memory allocation
        intRegisterBase = UNSAFE.allocateMemory(INT_REGISTER_REGION_SIZE);
        fpRegisterBase  = UNSAFE.allocateMemory(FP_REGISTER_REGION_SIZE);
        cp0RegisterBase = UNSAFE.allocateMemory(CP0_REGISTER_REGION_SIZE);
        memoryBase      = UNSAFE.allocateMemory(MAIN_MEMORY_SIZE);

        UNSAFE.setMemory(intRegisterBase, INT_REGISTER_REGION_SIZE, (byte) 0);
        UNSAFE.setMemory(fpRegisterBase,  FP_REGISTER_REGION_SIZE,  (byte) 0);
        UNSAFE.setMemory(cp0RegisterBase, CP0_REGISTER_REGION_SIZE, (byte) 0);
        UNSAFE.setMemory(memoryBase,      MAIN_MEMORY_SIZE,         (byte) 0);

        this.variableAddresses = new ConcurrentHashMap<>();
        initializeRegisters();
    }

    private void freeMemory() {
        try {
            UNSAFE.freeMemory(intRegisterBase);
            UNSAFE.freeMemory(fpRegisterBase);
            UNSAFE.freeMemory(cp0RegisterBase);
            UNSAFE.freeMemory(memoryBase);
        } catch (Exception e) {
            if (debug) debugLogger.error("Error freeing memory: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        if (!shutdownRequested.get()) {
            freeMemory();
        }
    }

    public synchronized CompletableFuture<Void> shutdown(@Nullable InstructionHandler callerInstance) {
        if (shutdownRequested.compareAndSet(false, true)) {
            shutdownFuture = new CompletableFuture<>();
            if (debug) {
                debugLogger.debug(
                        callerInstance != null ?
                                ("Shutdown requested from " + callerInstance.getName() +  " instruction.")
                                : ("Shutdown requested")
                );
            }
        }
        return shutdownFuture;
    }

    private void initializeRegisters() {
        initializeArgumentRegisters();
        initializeTemporaryAndValueRegisters();
        initializeSpecialRegisters();
        initializeFPRegisters();
        initializeCP0Registers();
    }

    private void initializeArgumentRegisters() {
        long currentOffset = 0;
        for (int i = 0; i < 8; i++) {
            intRegisterOffsets.put("$a" + i, currentOffset);
            currentOffset += SIZEOF_INT;
            if (debug) debugLogger.debug("Register $a{} at offset {}", i, currentOffset);
        }
    }

    private void initializeTemporaryAndValueRegisters() {
        long currentOffset = 8 * SIZEOF_INT;
        for (int i = 0; i < 32; i++) {
            intRegisterOffsets.put("$t" + i, currentOffset);
            currentOffset += SIZEOF_INT;
            if (debug) debugLogger.debug("Register $t{} at offset {}", i, currentOffset);

            intRegisterOffsets.put("$v" + i, currentOffset);
            currentOffset += SIZEOF_INT;
            if (debug) debugLogger.debug("Register $v{} at offset {}", i, currentOffset);
        }
    }

    private void initializeSpecialRegisters() {
        long currentOffset = (8 + 64) * SIZEOF_INT;
        String[] specialRegs = {"$ra", "$hi", "$lo"};
        for (String reg : specialRegs) {
            intRegisterOffsets.put(reg, currentOffset);
            currentOffset += SIZEOF_INT;
            if (debug) debugLogger.debug("Register {} at offset {}", reg, currentOffset);
        }
    }

    private void initializeFPRegisters() {
        long currentOffset = 0;
        for (int i = 0; i < 32; i++) {
            fpRegisterOffsets.put("$f" + i, currentOffset);
            currentOffset += SIZEOF_DOUBLE;
            if (debug) debugLogger.debug("Register $f{} at offset {}", i, currentOffset);
        }
    }

    private void initializeCP0Registers() {
        long currentOffset = 0;
        for (int i = 0; i < NUM_CP0_REGISTERS; i++) {
            cp0RegisterOffsets.put("cp0_" + i, currentOffset);
            currentOffset += SIZEOF_INT;
            if (debug) debugLogger.debug("Register cp0_{} at offset {}", i, currentOffset);
        }
    }

    public void evaluate(List<Instruction> instructions) {
        processLabels(instructions);
        executeInstructions(instructions);
    }

    private void processLabels(List<Instruction> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction.isLabel()) {
                String labelName = instruction.getLabel();
                labels.put(labelName, i);
                if (debug) debugLogger.debug("Label {} at {}", labelName, i);
            }
            if (debug) debugLogger.debug("Instruction {} at {}", instruction.name(), i);
        }
    }

    public void initializeVariables(Map<String, Variable> variables) {
        long currentAddress = memoryBase;

        for (Variable var : variables.values()) {
            String varName = var.name();
            int relativeAddress = (int)(currentAddress - memoryBase);
            variableAddresses.put(varName, relativeAddress);

            switch (var.type()) {
                case WORD -> {
                    int value = Integer.parseInt(var.value());
                    storeWordToMemory((int)(currentAddress - memoryBase), value);
                    currentAddress += SIZEOF_INT;
                }
                case BYTE -> {
                    byte value = Byte.parseByte(var.value());
                    storeByteToMemory((int)(currentAddress - memoryBase), value);
                    currentAddress += SIZEOF_BYTE;
                }
                case HALF -> {
                    short value = Short.parseShort(var.value());
                    storeHalfWordToMemory((int)(currentAddress - memoryBase), value);
                    currentAddress += SIZEOF_SHORT;
                }
                case FLOAT -> {
                    float value = Float.parseFloat(var.value());
                    storeFloatToMemory((int)(currentAddress - memoryBase), value);
                    currentAddress += SIZEOF_FLOAT;
                }
                case DOUBLE -> {
                    double value = Double.parseDouble(var.value());
                    storeDoubleWordToMemory((int)(currentAddress - memoryBase), Double.doubleToLongBits(value));
                    currentAddress += SIZEOF_DOUBLE;
                }
                case ASCII, ASCIIZ -> {
                    String value = var.value().replace("\"", "");
                    byte[] bytes = value.getBytes();
                    for (byte b : bytes) {
                        storeByteToMemory((int)(currentAddress - memoryBase), b);
                        currentAddress += SIZEOF_BYTE;
                    }
                    if (var.is(Variable.Type.ASCIIZ)) {
                        storeByteToMemory((int)(currentAddress - memoryBase), (byte) 0);
                        currentAddress += SIZEOF_BYTE;
                    }
                }
                case SPACE -> {
                    int size = Integer.parseInt(var.value());
                    for (int i = 0; i < size; i++) {
                        storeByteToMemory((int)(currentAddress - memoryBase), (byte) 0);
                        currentAddress += SIZEOF_BYTE;
                    }
                }
            }

            if (debug) {
                debugLogger.debug("Initialized variable \"{}\" of type \"{}\" at address \"{}\"",
                        var.name(), var.type(), currentAddress - memoryBase);
            }
        }
    }

    private void executeInstructions(List<Instruction> instructions) {
        try {
            for (programCounter = 0; programCounter < instructions.size(); programCounter++) {
                if (shouldShutdown()) {
                    logShutdownDetected("before executing instruction");
                    break;
                }

                Instruction instruction = instructions.get(programCounter);
                if (!instruction.isLabel()) {
                    executeInstruction(instruction);
                }

                if (shouldShutdown()) {
                    logShutdownDetected("after executing instruction");
                    break;
                }
            }
        } finally {
            handleShutdown();
        }
    }

    private boolean shouldShutdown() {
        return shutdownRequested.get();
    }

    private void logShutdownDetected(String context) {
        if (debug) debugLogger.debug("Shutdown detected {}. Stopping execution.", context);
    }

    public void handleShutdown(int errorCode) {
        if (shutdownRequested.get()) {
            if (debug) debugLogger.debug("Performing shutdown steps with error code: {}.", errorCode);
            freeMemory();
            if (shutdownFuture != null && !shutdownFuture.isDone()) {
                shutdownFuture.complete(null);
            }
            if (debug) debugLogger.debug("Memory freed. Shutdown complete with code: {}.", errorCode);

            if (errorCode != 0) {
                throw new RuntimeException("Program terminated with error code: " + errorCode);
            }

            System.exit(errorCode);
        }
    }

    private void handleShutdown() {
        handleShutdown(0);
    }

    private void executeInstruction(Instruction instruction) {
        InstructionHandler handler = InstructionFactory.getInstance()
                .getInstructionHandler(instruction.name());

        if (handler != null) {
            handler.execute(instruction.operands(), this);
            if (debug) debugLogger.debug("Register $v0 = {}", getRegisterValue(new Operand("$v0")));
        } else {
            diagnosticService.addError("Instruction not supported: " + instruction.name());
        }
    }

    public int getRegisterValue(Operand operand) {
        Long offset = intRegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown register: " + operand.value());
            return 0;
        }
        if (debug) debugLogger.debug("Value of " + operand.value() + " is " + UNSAFE.getInt(intRegisterBase + offset));
        return UNSAFE.getInt(intRegisterBase + offset);
    }

    public void setRegisterValue(Operand operand, int value) {
        Long offset = intRegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown register: " + operand.value());
            return;
        }
        if (debug) debugLogger.debug("Setting " + operand.value() + " to " + value);
        UNSAFE.putInt(intRegisterBase + offset, value);
    }

    public double getFpRegisterValue(Operand operand) {
        Long offset = fpRegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown FP register: " + operand.value());
            return 0.0;
        }
        if (debug) debugLogger.debug("Value of " + operand.value() + " is " + UNSAFE.getDouble(fpRegisterBase + offset));
        return UNSAFE.getDouble(fpRegisterBase + offset);
    }

    public void setFpRegisterValue(Operand operand, double value) {
        Long offset = fpRegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown FP register: " + operand.value());
            return;
        }
        if (debug) debugLogger.debug("Setting " + operand.value() + " to " + value);
        UNSAFE.putDouble(fpRegisterBase + offset, value);
    }

    public int getCp0RegisterValue(Operand operand) {
        Long offset = cp0RegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown CP0 register: " + operand.value());
            return 0;
        }
        if (debug) debugLogger.debug("Value of " + operand.value() + " is " + UNSAFE.getInt(cp0RegisterBase + offset));
        return UNSAFE.getInt(cp0RegisterBase + offset);
    }

    public void setCp0RegisterValue(Operand operand, int value) {
        Long offset = cp0RegisterOffsets.get(operand.value());
        if (offset == null) {
            diagnosticService.addError("Unknown CP0 register: " + operand.value());
            return;
        }
        if (debug) debugLogger.debug("Setting {} to {}", operand.value(), value);
        UNSAFE.putInt(cp0RegisterBase + offset, value);
    }

    public void branchToLabel(String label) {
        Integer target = labels.get(label);
        if (target != null) {
            programCounter = target - 1; // -1 because the for-loop increments it
            if (debug) debugLogger.debug("Branching to label {} at {}", label, target);
        } else {
            diagnosticService.addError("Label not found: " + label);
        }
    }

    public void jumpToLabel(String label) {
        Integer target = labels.get(label);
        if (target != null) {
            programCounter = target;
            if (debug) debugLogger.debug("Jumping to label {} at {}", label, target);
        } else {
            diagnosticService.addError("Label not found: " + label);
        }
    }

    public void jumpToRegister(String register) {
        programCounter = getRegisterValue(new Operand(register));
        if (debug) debugLogger.debug("Jumping to register {} at {}", register, programCounter);
    }

    public void storeByteToMemory(int address, byte value) {
        checkMemoryBounds(address, SIZEOF_BYTE);
        if (debug) debugLogger.debug("Storing byte {} at {}", value, address);
        UNSAFE.putByte(memoryBase + address, value);
    }

    public void storeConditional(int address, int value) {
        if (llBit) {
            storeWordToMemory(address, value);
            llBit = false;
            setRegisterValue(new Operand("$t1"), 1);
            if (debug) debugLogger.debug("LL bit set to 1");
        } else {
            setRegisterValue(new Operand("$t1"), 0);
        }
    }

    public void loadLinked(int address) {
        llBit = true;
        setRegisterValue(new Operand("$t1"), loadWordFromMemory(address));
        if (debug) debugLogger.debug("LL bit set to 1");
    }

    public void storeDoubleWordToMemory(int address, long value) {
        checkMemoryBounds(address, SIZEOF_LONG);
        long mipsValue = NEEDS_BYTE_SWAP ? Long.reverseBytes(value) : value;
        UNSAFE.putLong(memoryBase + address, mipsValue);
        if (debug) debugLogger.debug("Storing double word {} (MIPS: {}) at {}", value, mipsValue, address);
    }

    public void storeHalfWordToMemory(int address, short value) {
        checkMemoryBounds(address, SIZEOF_SHORT);
        short mipsValue = NEEDS_BYTE_SWAP ? Short.reverseBytes(value) : value;
        UNSAFE.putShort(memoryBase + address, mipsValue);
        if (debug) debugLogger.debug("Storing half word {} (MIPS: {}) at {}", value, mipsValue, address);
    }

    public void storeWordToMemory(int address, int value) {
        checkMemoryBounds(address, SIZEOF_INT);
        int mipsValue = NEEDS_BYTE_SWAP ? Integer.reverseBytes(value) : value;
        UNSAFE.putInt(memoryBase + address, mipsValue);
        if (debug) debugLogger.debug("Storing word {} (MIPS: {}) at {}", value, mipsValue, address);
    }

    public void storeFloatToMemory(int address, float value) {
        checkMemoryBounds(address, SIZEOF_FLOAT);
        if (NEEDS_BYTE_SWAP) {
            int intBits = Float.floatToIntBits(value);
            int mipsIntBits = Integer.reverseBytes(intBits);
            UNSAFE.putInt(memoryBase + address, mipsIntBits);
            if (debug) debugLogger.debug("Storing float {} (MIPS bits: {}) at {}", value, mipsIntBits, address);
        } else {
            UNSAFE.putFloat(memoryBase + address, value);
            if (debug) debugLogger.debug("Storing float {} at {}", value, address);
        }
    }

    public void storeWordLeftToMemory(int address, int value) {
        // Align address to lower multiple of 4
        int alignedAddress = address & ~3;
        if (debug) debugLogger.debug("[SWLTM] Storing word left {} at {}", value, alignedAddress);
        checkMemoryBounds(alignedAddress, SIZEOF_INT);

        int shift = (address & 3) * 8;
        int mask = 0xFFFFFFFF >>> shift;
        if (debug) debugLogger.debug("[SWLTM] Shift: {}, Mask: {}", shift, mask);

        int existingValue = UNSAFE.getInt(memoryBase + alignedAddress);
        if (debug) debugLogger.debug("[SWLTM] Existing value: {}", existingValue);
        int newValue = (value << shift) | (existingValue & mask);
        if (debug) debugLogger.debug("[SWLTM] New value: {}", newValue);

        UNSAFE.putInt(memoryBase + alignedAddress, newValue);
        if (debug) debugLogger.debug("[SWLTM] Stored word left {} at {}", value, alignedAddress);
    }

    public void storeWordRightToMemory(int address, int value) {
        // Align address to lower multiple of 4
        int alignedAddress = address & ~3;
        if (debug) debugLogger.debug("[SWRTM] Storing word right {} at {}", value, alignedAddress);
        checkMemoryBounds(alignedAddress, SIZEOF_INT);

        int shift = (3 - (address & 3)) * 8;
        int mask = 0xFFFFFFFF << shift;
        if (debug) debugLogger.debug("[SWRTM] Shift: {}, Mask: {}", shift, mask);

        int existingValue = UNSAFE.getInt(memoryBase + alignedAddress);
        if (debug) debugLogger.debug("[SWRTM] Existing value: {}", existingValue);
        int newValue = (value >>> shift) | (existingValue & mask);
        if (debug) debugLogger.debug("[SWRTM] New value: {}", newValue);

        UNSAFE.putInt(memoryBase + alignedAddress, newValue);
        if (debug) debugLogger.debug("[SWRTM] Stored word right {} at {}", value, alignedAddress);
    }

    public byte loadByteFromMemory(int address) {
        checkMemoryBounds(address, SIZEOF_BYTE);
        if (debug) debugLogger.debug("Loading byte at {} with value {}", address, UNSAFE.getByte(memoryBase + address));
        return UNSAFE.getByte(memoryBase + address);
    }

    public short loadHalfWordFromMemory(int address) {
        checkMemoryBounds(address, SIZEOF_SHORT);
        short rawValue = UNSAFE.getShort(memoryBase + address);
        short hostValue = NEEDS_BYTE_SWAP ? Short.reverseBytes(rawValue) : rawValue;
        if (debug) debugLogger.debug("Loading half word at {} with MIPS value {} (host: {})", address, rawValue, hostValue);
        return hostValue;
    }

    public int loadWordFromMemory(int address) {
        checkMemoryBounds(address, SIZEOF_INT);
        int rawValue = UNSAFE.getInt(memoryBase + address);
        int hostValue = NEEDS_BYTE_SWAP ? Integer.reverseBytes(rawValue) : rawValue;
        if (debug) debugLogger.debug("Loading word at {} with MIPS value {} (host: {})", address, rawValue, hostValue);
        return hostValue;
    }

    public float loadFloatFromMemory(int address) {
        checkMemoryBounds(address, SIZEOF_FLOAT);
        if (NEEDS_BYTE_SWAP) {
            int rawIntBits = UNSAFE.getInt(memoryBase + address);
            int hostIntBits = Integer.reverseBytes(rawIntBits);
            float hostValue = Float.intBitsToFloat(hostIntBits);
            if (debug) debugLogger.debug("Loading float at {} with MIPS bits {} (host: {})", address, rawIntBits, hostValue);
            return hostValue;
        } else {
            float value = UNSAFE.getFloat(memoryBase + address);
            if (debug) debugLogger.debug("Loading float at {} with value {}", address, value);
            return value;
        }
    }

    public long loadDoubleWordFromMemory(int address) {
        checkMemoryBounds(address, SIZEOF_LONG);
        long rawValue = UNSAFE.getLong(memoryBase + address);
        long hostValue = NEEDS_BYTE_SWAP ? Long.reverseBytes(rawValue) : rawValue;
        if (debug) debugLogger.debug("Loading double word at {} with MIPS value {} (host: {})", address, rawValue, hostValue);
        return hostValue;
    }

    /**
     * LWL (Load Word Left) – loads high-order bytes from memory into the register.
     */
    public int loadWordLeftFromMemory(int address) {
        // Align address
        int alignedAddress = address & ~3;
        checkMemoryBounds(alignedAddress, SIZEOF_INT);

        if (debug)
            debugLogger.debug("Loading word left at {} with value {}", address, UNSAFE.getInt(memoryBase + alignedAddress));

        int shift = (address & 3) * 8;
        int value = UNSAFE.getInt(memoryBase + alignedAddress);
        if (debug) debugLogger.debug("Shift: {}, Value: {}", shift, value);
        if (debug) debugLogger.debug("Shifted value: {}", value >>> shift);
        return value >>> shift;
    }

    /**
     * LWR (Load Word Right) – loads low-order bytes from memory into the register.
     */
    public int loadWordRightFromMemory(int address) {
        // Align address
        int alignedAddress = address & ~3;
        checkMemoryBounds(alignedAddress, SIZEOF_INT);
        if (debug)
            debugLogger.debug("Loading word right at {} with value {}", address, UNSAFE.getInt(memoryBase + alignedAddress));

        int shift = (3 - (address & 3)) * 8;
        int value = UNSAFE.getInt(memoryBase + alignedAddress);
        if (debug) debugLogger.debug("Shift: {}, Value: {}", shift, value);
        if (debug) debugLogger.debug("Shifted value: {}", value << shift);
        return value << shift;
    }

    public Integer getVariableAddress(String varName) {
        Integer address = variableAddresses.get(varName);
        if (address == null) {
            diagnosticService.addError("Variable not found: " + varName);
        }
        if (debug) debugLogger.debug("Variable address: {}", address);
        return address;
    }

    public void loadAddress(String targetRegister, int address) {
        if (debug) debugLogger.debug("Loading address {} into {}", address, targetRegister);
        setRegisterValue(new Operand(targetRegister), address);
    }

    // ------------------------------------------------
    // Utility: Check memory bounds (maybe this should be moved to a separate class?)
    // ------------------------------------------------
    private void checkMemoryBounds(int address, int size) {
        if (address < 0 || (long) address + size > MAIN_MEMORY_SIZE) {
            diagnosticService.addError(
                    String.format("Memory access out of bounds: address=%d size=%d", address, size)
            );
        }
    }
}