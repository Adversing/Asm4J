package me.adversing.asm.instruction.impl.branch;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Bgez implements InstructionHandler {
    @Override
    public String getName() {
        return "bgez";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {

        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.getFirst());
        if (value >= 0) {
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
            evaluator.getDiagnosticService().addError("Bgez instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.getFirst().value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Bgez instruction operand must be an integer.");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Bgez instruction Register not found.");
            return false;
        }
        return true;
    }
}
