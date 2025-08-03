package me.adversing.asm.instruction.impl.trap;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Tnei implements InstructionHandler {
    @Override
    public String getName() {
        return "tnei";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.getFirst());
        int immediate = Integer.parseInt(operands.get(1).value());

        if (value != immediate) {
            evaluator.getDiagnosticService().addError("Trap exception: value is not equal to immediate.");
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Tnei doesn't use a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Tnei instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Source Register not found.");
            return false;
        }

        try {
            Integer.parseInt(operands.get(1).value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid immediate value.");
            return false;
        }
        return true;
    }
}
