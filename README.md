# Asm4J

A comprehensive Java-based MIPS Assembly interpreter and simulator that provides a complete environment for executing MIPS assembly programs with full instruction set support, memory management, and debugging capabilities.

## Features

- **Complete MIPS Instruction Set**: Supports arithmetic, logical, memory, branch, jump, and floating-point instructions
- **Memory Management**: Safe memory allocation using Java's Unsafe API with proper cleanup
- **Register Support**: Full support for integer, floating-point, and CP0 registers
- **Error Handling**: Comprehensive error detection and reporting system
- **Debugging**: Optional debug mode with detailed execution tracing
- **Extensible Architecture**: Modular instruction handler system for easy extension

## Architecture

### Core Components

- **ASMEvaluator**: Main execution engine that manages registers, memory, and instruction execution
- **Parser**: Parses assembly files and converts them into executable instruction objects
- **InstructionFactory**: Dynamically loads and manages all instruction handlers
- **DiagnosticService**: Centralized error reporting and diagnostic system
- **BaseInstructionHandler**: Base class providing common validation and utility methods

### Supported Instructions

#### Arithmetic Instructions
- `add`, `addi`, `addiu`, `addu` - Addition operations
- `sub`, `subu` - Subtraction operations
- `mul`, `mult`, `multu` - Multiplication operations
- `div`, `divu` - Division operations

#### Logical Instructions
- `and`, `andi` - Bitwise AND
- `or`, `ori` - Bitwise OR
- `xor`, `xori` - Bitwise XOR
- `nor` - Bitwise NOR
- `sll`, `sllv`, `sra`, `srav`, `srl`, `srlv` - Shift operations

#### Memory Instructions
- `lw`, `lh`, `lhu`, `lb`, `lbu` - Load operations
- `sw`, `sh`, `sb` - Store operations
- `lwc1`, `swc1`, `ldc1`, `sdc1` - Floating-point memory operations

#### Branch and Jump Instructions
- `beq`, `bne` - Conditional branches
- `bgez`, `bgtz`, `blez`, `bltz` - Zero comparison branches
- `j`, `jal`, `jr`, `jalr` - Jump operations

#### Floating-Point Instructions
- `add.d`, `add.s`, `sub.d`, `sub.s` - FP arithmetic
- `mul.d`, `mul.s`, `div.d`, `div.s` - FP multiplication/division
- `mov.d`, `mov.s` - FP move operations
- `cvt.*` - Format conversion instructions

## Building and Running

### Prerequisites

- Java 22 or higher
- Gradle 8.0 or higher

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew run
```

### Run with Debug Mode

```bash
./gradlew run --args="--debug"
```

## Usage

### Assembly Program Format

The program should follow standard MIPS assembly syntax:

```assembly
.data
    message: .asciiz "Hello, World!"

.text
    main:
        li $v0, 4          # System call for print string
        la $a0, message    # Load address of message
        syscall            # Print the string

        li $v0, 10         # System call for exit
        syscall            # Exit program
```

### Register Naming

- **Integer Registers**: `$zero`, `$at`, `$v0-$v1`, `$a0-$a3`, `$t0-$t9`, `$s0-$s7`, `$k0-$k1`, `$gp`, `$sp`, `$fp`, `$ra`
- **Floating-Point Registers**: `$f0-$f31`
- **Special Registers**: `$hi`, `$lo` (for multiplication/division results)
- **CP0 Registers**: `$cause`, `$epc` (for exception handling)

## Error Handling

The interpreter provides comprehensive error detection:

- **Syntax Errors**: Invalid instruction formats or operands
- **Runtime Errors**: Division by zero, arithmetic overflow, invalid memory access
- **Register Errors**: References to non-existent registers
- **Memory Errors**: Out-of-bounds memory access, invalid addresses

## Development

### Adding New Instructions

1. Create a new instruction class extending `BaseInstructionHandler`
2. Implement required methods: `getName()`, `execute()`, `checkOperands()`
3. Place the class in the appropriate package under `src/main/java/me/adversing/asm/instruction/impl/`
4. The instruction will be automatically discovered and loaded by the `InstructionFactory`

### Example Instruction Implementation

```java
public class MyInstruction extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "myinst";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }
        // Implementation here
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        return validateOperandCount(operands, 2, evaluator) &&
               validateIntRegister(operands.getFirst().value(), evaluator) &&
               validateIntRegister(operands.get(1).value(), evaluator);
    }
}
```

## License

This project is released under the [MIT License](https://choosealicense.com/licenses/mit/).

