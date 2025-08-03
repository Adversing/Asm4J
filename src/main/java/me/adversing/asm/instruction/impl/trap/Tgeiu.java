package me.adversing.asm.instruction.impl.trap;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Tgeiu implements InstructionHandler {
    @Override
    public String getName() {
        return "tgeiu";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        long value = evaluator.getRegisterValue(operands.getFirst()) & 0xFFFFFFFFL;
        long immediate = Integer.parseInt(operands.get(1).value()) & 0xFFFFFFFFL;

        if (value >= immediate) {
            evaluator.getDiagnosticService().addError("Trap exception: unsigned value is greater than or equal to immediate.");
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // No destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Tgeiu instruction must have exactly 2 operand(s).");
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
