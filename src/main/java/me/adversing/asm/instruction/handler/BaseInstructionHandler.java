package me.adversing.asm.instruction.handler;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;

import java.util.List;

public abstract class BaseInstructionHandler implements InstructionHandler {

    public abstract String getName();
    public abstract void execute(List<Operand> operands, ASMEvaluator evaluator);
    public abstract boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator);
    
    protected boolean validateOperandCount(List<Operand> operands, int expectedCount, ASMEvaluator evaluator) {
        if (operands.size() != expectedCount) {
            evaluator.getDiagnosticService().addError(
                getName() + " instruction must have " + expectedCount + " operands, got " + operands.size()
            );
            return false;
        }
        return true;
    }
    
    /**
     * Validates that a register exists in the integer register map.
     * 
     * @param register the register name to validate
     * @param evaluator the evaluator containing register mappings
     * @return true if register exists, false otherwise
     */
    protected boolean validateIntRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getIntRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Integer register not found: " + register);
            return false;
        }
        return true;
    }
    
    /**
     * Validates that a register exists in the floating-point register map.
     * 
     * @param register the register name to validate
     * @param evaluator the evaluator containing register mappings
     * @return true if register exists, false otherwise
     */
    protected boolean validateFpRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Floating-point register not found: " + register);
            return false;
        }
        return true;
    }
    
    /**
     * Validates that a string can be parsed as an integer.
     * 
     * @param value the string to validate
     * @param evaluator the evaluator for error reporting
     * @return true if value is a valid integer, false otherwise
     */
    protected boolean validateInteger(String value, ASMEvaluator evaluator) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid integer value: " + value);
            return false;
        }
    }
    
    /**
     * Validates that a string can be parsed as a float.
     * 
     * @param value the string to validate
     * @param evaluator the evaluator for error reporting
     * @return true if value is a valid float, false otherwise
     */
    protected boolean validateFloat(String value, ASMEvaluator evaluator) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid float value: " + value);
            return false;
        }
    }
    
    /**
     * Validates that a string can be parsed as a double.
     * 
     * @param value the string to validate
     * @param evaluator the evaluator for error reporting
     * @return true if value is a valid double, false otherwise
     */
    protected boolean validateDouble(String value, ASMEvaluator evaluator) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid double value: " + value);
            return false;
        }
    }
    
    /**
     * Validates that an immediate value is within the specified range.
     * 
     * @param value the immediate value
     * @param min minimum allowed value (inclusive)
     * @param max maximum allowed value (inclusive)
     * @param evaluator the evaluator for error reporting
     * @return true if value is in range, false otherwise
     */
    protected boolean validateImmediateRange(int value, int min, int max, ASMEvaluator evaluator) {
        if (value < min || value > max) {
            evaluator.getDiagnosticService().addError(
                "Immediate value " + value + " must be between " + min + " and " + max
            );
            return false;
        }
        return true;
    }
    
    /**
     * Default implementation for instructions that don't use a destination register.
     * 
     * @param register the register name (ignored)
     * @param evaluator the evaluator (ignored)
     * @return always true
     */
    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true;
    }
}
