package me.adversing.asm.instruction.impl.trap;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Tne implements InstructionHandler {
    @Override
    public String getName() {
        return "tne";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value1 = evaluator.getRegisterValue(operands.getFirst());
        int value2 = evaluator.getRegisterValue(operands.get(1));

        if (value1 != value2) {
            evaluator.getDiagnosticService().addError("Trap exception: values are not equal.");
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Tne doesn't use a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Tne instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
