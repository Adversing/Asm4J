package me.adversing.asm.instruction.impl.sqrt;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class SqrtS implements InstructionHandler {
    @Override
    public String getName() {
        return "sqrt.s";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        float value = (float) evaluator.getFpRegisterValue(operands.get(1));
        if (value < 0) {
            evaluator.getDiagnosticService().addError("Cannot compute square root of negative number.");
            return;
        }
        evaluator.setFpRegisterValue(operands.getFirst(), (float) Math.sqrt(value));
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Sqrt.s instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Float.parseFloat(operands.get(1).value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid immediate value.");
            return false;
        }

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
