package me.adversing.asm.instruction.impl.syscall;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class Syscall extends BaseInstructionHandler {

    private static final BufferedReader STDIN_READER = new BufferedReader(new InputStreamReader(System.in));
    private static final int MAX_SBRK_SIZE = 16 * 1024 * 1024;
    private static int heapPointer = 0x10000000; // start at 256MB

    @Override
    public String getName() {
        return "syscall";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        // Get syscall value from $v0 register
        int syscallValue = evaluator.getRegisterValue(OperandConstants.V0_REGISTER);

        switch (syscallValue) {
            case -1:
                evaluator.getDiagnosticService().addError("No valid value stored in $v0 register for syscall instruction.");
                break;
            case 1: // print integer
                printInteger(evaluator);
                break;
            case 2: // print float
                printFloat(evaluator);
                break;
            case 3: // print double
                printDouble(evaluator);
                break;
            case 4: // print string
                printString(evaluator);
                break;
            case 10: // exit program
                CompletableFuture<Void> shutdownTask = evaluator.shutdown(this);
                shutdownTask.exceptionally(ex -> {
                    evaluator.getDiagnosticService().addError("Error while shutting down the program: " + ex.getMessage());
                    return null;
                });
                break;
            case 5: // read integer
                readInteger(evaluator);
                break;
            case 6: // read float
                readFloat(evaluator);
                break;
            case 7: // read double
                readDouble(evaluator);
                break;
            case 8: // read string
                readString(evaluator);
                break;
            case 9: // sbrk (memory allocation)
                sbrk(evaluator);
                break;
            case 11: // print character
                printCharacter(evaluator);
                break;
            case 12: // read character
                readCharacter(evaluator);
                break;
            case 17: // exit2 (exit with code)
                exit2(evaluator);
                break;
            default:
                evaluator.getDiagnosticService().addError("Unsupported syscall value: " + syscallValue);
        }
    }

    /**
     * Syscall 1: Print integer from $a0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void printInteger(ASMEvaluator evaluator) {
        int intToPrint = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);
        evaluator.getProgramLogger().info(intToPrint);
    }

    /**
     * Syscall 2: Print float from $f12 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void printFloat(ASMEvaluator evaluator) {
        try {
            double doubleValue = evaluator.getFpRegisterValue(OperandConstants.F12_REGISTER);
            float floatValue = (float) doubleValue;
            evaluator.getProgramLogger().info(floatValue);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error accessing floating-point register $f12: " + e.getMessage());
        }
    }

    /**
     * Syscall 3: Print double from $f12 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void printDouble(ASMEvaluator evaluator) {
        try {
            double doubleValue = evaluator.getFpRegisterValue(OperandConstants.F12_REGISTER);
            evaluator.getProgramLogger().info(doubleValue);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error accessing floating-point register $f12: " + e.getMessage());
        }
    }

    /**
     * Syscall 4: Print null-terminated string from address in $a0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void printString(ASMEvaluator evaluator) {
        int address = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);
        if (address < 0) {
            evaluator.getDiagnosticService().addError("Invalid memory address for string: " + address);
            return;
        }

        StringBuilder strBuilder = new StringBuilder();
        try {
            byte b;
            int currentAddress = address;
            while ((b = evaluator.loadByteFromMemory(currentAddress++)) != 0) {
                strBuilder.append((char) b);
                if (strBuilder.length() > 65536) {
                    evaluator.getDiagnosticService().addError("String too long (>64KB) or not null-terminated");
                    return;
                }
            }
            String rawString = strBuilder.toString();
            String processedString = parseEscapeSequences(rawString);
            evaluator.getProgramLogger().info(processedString);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error reading string from memory at address " + address + ": " + e.getMessage());
        }
    }

    /**
     * Syscall 11: Print character from $a0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void printCharacter(ASMEvaluator evaluator) {
        int charToPrint = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);
        char character = (char) (charToPrint & 0xFF); // Ensure it's a valid ASCII character
        evaluator.getProgramLogger().info(character);
    }

    /**
     * Syscall 5: Read integer from standard input and store in $v0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void readInteger(ASMEvaluator evaluator) {
        try {
            String line = STDIN_READER.readLine();
            if (line == null) {
                evaluator.getDiagnosticService().addError("End of input reached while reading integer");
                return;
            }

            line = line.trim();
            if (line.isEmpty()) {
                evaluator.setRegisterValue(OperandConstants.V0_REGISTER, 0);
                return;
            }

            String[] parts = line.split("\\s+");
            int value = Integer.parseInt(parts[0]);
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, value);

        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid integer format in input: " + e.getMessage());
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, 0);
        } catch (IOException e) {
            evaluator.getDiagnosticService().addError("I/O error while reading integer: " + e.getMessage());
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, 0);
        }
    }

    /**
     * Syscall 6: Read float from standard input and store in $f0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void readFloat(ASMEvaluator evaluator) {
        try {
            String line = STDIN_READER.readLine();
            if (line == null) {
                evaluator.getDiagnosticService().addError("End of input reached while reading float");
                return;
            }

            line = line.trim();
            if (line.isEmpty()) {
                evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0f);
                return;
            }

            String[] parts = line.split("\\s+");
            float value = Float.parseFloat(parts[0]);
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, value);

        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid float format in input: " + e.getMessage());
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0f);
        } catch (IOException e) {
            evaluator.getDiagnosticService().addError("I/O error while reading float: " + e.getMessage());
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0f);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error accessing floating-point register $f0: " + e.getMessage());
        }
    }

    /**
     * Syscall 7: Read double from standard input and store in $f0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void readDouble(ASMEvaluator evaluator) {
        try {
            String line = STDIN_READER.readLine();
            if (line == null) {
                evaluator.getDiagnosticService().addError("End of input reached while reading double");
                return;
            }

            line = line.trim();
            if (line.isEmpty()) {
                evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0);
                return;
            }

            String[] parts = line.split("\\s+");
            double value = Double.parseDouble(parts[0]);
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, value);

        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid double format in input: " + e.getMessage());
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0);
        } catch (IOException e) {
            evaluator.getDiagnosticService().addError("I/O error while reading double: " + e.getMessage());
            evaluator.setFpRegisterValue(OperandConstants.F0_REGISTER, 0.0);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error accessing floating-point register $f0: " + e.getMessage());
        }
    }

    /**
     * Syscall 8: Read string from standard input into buffer.
     * $a0 = buffer address, $a1 = maximum length (including null terminator)
     *
     * @param evaluator the ASM evaluator instance
     */
    private void readString(ASMEvaluator evaluator) {
        int bufferAddress = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);
        int maxLength = evaluator.getRegisterValue(OperandConstants.A1_REGISTER);

        if (bufferAddress < 0) {
            evaluator.getDiagnosticService().addError("Invalid buffer address for read_string: " + bufferAddress);
            return;
        }

        if (maxLength <= 0) {
            evaluator.getDiagnosticService().addError("Invalid buffer length for read_string: " + maxLength);
            return;
        }

        try {
            String line = STDIN_READER.readLine();
            if (line == null) {
                // EOF - store empty string
                evaluator.storeByteToMemory(bufferAddress, (byte) 0);
                return;
            }

            // limit string to maxLength - 1 characters (reserve space for null terminator)
            int lengthToStore = Math.min(line.length(), maxLength - 1);

            // store characters to memory
            for (int i = 0; i < lengthToStore; i++) {
                char c = line.charAt(i);
                evaluator.storeByteToMemory(bufferAddress + i, (byte) c);
            }

            // add null terminator
            evaluator.storeByteToMemory(bufferAddress + lengthToStore, (byte) 0);

        } catch (IOException e) {
            evaluator.getDiagnosticService().addError("I/O error while reading string: " + e.getMessage());
            // store empty string on error
            evaluator.storeByteToMemory(bufferAddress, (byte) 0);
        } catch (Exception e) {
            evaluator.getDiagnosticService().addError("Error storing string to memory: " + e.getMessage());
        }
    }

    /**
     * Syscall 12: Read character from standard input and store in $v0 register.
     *
     * @param evaluator the ASM evaluator instance
     */
    private void readCharacter(ASMEvaluator evaluator) {
        try {
            int character = System.in.read();
            if (character == -1) {
                // EOF
                evaluator.setRegisterValue(OperandConstants.V0_REGISTER, 0);
            } else {
                evaluator.setRegisterValue(OperandConstants.V0_REGISTER, character & 0xFF);
            }
        } catch (IOException e) {
            evaluator.getDiagnosticService().addError("I/O error while reading character: " + e.getMessage());
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, 0);
        }
    }

    /**
     * Syscall 9: sbrk - Allocate memory on heap.
     * $a0 = number of bytes to allocate
     * Returns address in $v0 register
     *
     * @param evaluator the ASM evaluator instance
     */
    private void sbrk(ASMEvaluator evaluator) {
        int bytesToAllocate = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);

        if (bytesToAllocate < 0) {
            evaluator.getDiagnosticService().addError("Invalid allocation size for sbrk: " + bytesToAllocate);
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, -1);
            return;
        }

        if (bytesToAllocate > MAX_SBRK_SIZE) {
            evaluator.getDiagnosticService().addError("Allocation size too large for sbrk: " + bytesToAllocate + " (max: " + MAX_SBRK_SIZE + ")");
            evaluator.setRegisterValue(OperandConstants.V0_REGISTER, -1);
            return;
        }

        // return current heap pointer and advance it
        int currentHeap = heapPointer;
        heapPointer += bytesToAllocate;

        // word-align the heap pointer for next allocation
        if (heapPointer % 4 != 0) {
            heapPointer += 4 - (heapPointer % 4);
        }

        evaluator.setRegisterValue(OperandConstants.V0_REGISTER, currentHeap);
    }

    /**
     * Syscall 17: exit2 - Exit program with specified exit code.
     * $a0 = exit code
     *
     * @param evaluator the ASM evaluator instance
     */
    private void exit2(ASMEvaluator evaluator) {
        int exitCode = evaluator.getRegisterValue(OperandConstants.A0_REGISTER);
        CompletableFuture<Void> shutdownTask = evaluator.shutdown(this);
        shutdownTask.exceptionally(ex -> {
            evaluator.getDiagnosticService().addError("Error while shutting down the program: " + ex.getMessage());
            return null;
        });

        evaluator.handleShutdown(exitCode);
    }

    /**
     * Parse escape sequences in strings (e.g., \n, \t, \\, \").
     *
     * @param input the raw string with escape sequences
     * @return the processed string with escape sequences converted
     */
    private String parseEscapeSequences(String input) {
        StringBuilder result = new StringBuilder();
        boolean escape = false;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (escape) {
                switch (current) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    case '\\':
                        result.append('\\');
                        break;
                    case '\"':
                        result.append('\"');
                        break;
                    case '\'':
                        result.append('\'');
                        break;
                    case '0':
                        result.append('\0');
                        break;
                    default: // unk
                        result.append('\\').append(current);
                        break;
                }
                escape = false;
            } else {
                if (current == '\\') {
                    escape = true;
                } else {
                    result.append(current);
                }
            }
        }

        if (escape) {
            result.append('\\');
        }

        return result.toString();
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Syscall doesn't use a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (!operands.isEmpty()) {
            evaluator.getDiagnosticService().addError("Syscall instruction must have no operands.");
            return false;
        }
        return true;
    }
}
