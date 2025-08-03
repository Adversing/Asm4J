package me.adversing.asm.instruction.impl.branch;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;

import java.util.List;

public class Bltzal extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "bltzal";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.getFirst());
        if (value < 0) {
            evaluator.setRegisterValue(OperandConstants.RA_REGISTER, evaluator.getProgramCounter() + 1);
            evaluator.branchToLabel(operands.get(1).value());
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Branch instructions don't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Bltzal instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.getFirst().value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Bltzal instruction operand must be an integer.");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Bltzal instruction Register not found.");
            return false;
        }
        return true;
    }
}
