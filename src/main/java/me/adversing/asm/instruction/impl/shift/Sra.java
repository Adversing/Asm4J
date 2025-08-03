package me.adversing.asm.instruction.impl.shift;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Sra implements InstructionHandler {
    @Override
    public String getName() {
        return "sra";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.get(1));
        int shiftAmount = Integer.parseInt(operands.get(2).value());
        evaluator.setRegisterValue(operands.getFirst(), value >> shiftAmount);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getIntRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Sra instruction must have exactly 3 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source Register not found.");
            return false;
        }

        try {
            int shiftAmount = Integer.parseInt(operands.get(2).value());
            if (shiftAmount < 0 || shiftAmount > 31) {
                evaluator.getDiagnosticService().addError("Shift amount must be between 0 and 31.");
                return false;
            }
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid shift amount.");
            return false;
        }
        return true;
    }
}
