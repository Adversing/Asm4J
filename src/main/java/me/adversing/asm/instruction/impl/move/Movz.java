package me.adversing.asm.instruction.impl.move;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Movz implements InstructionHandler {
    @Override
    public String getName() {
        return "movz";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int condition = evaluator.getRegisterValue(operands.get(2));
        if (condition == 0) {
            int value = evaluator.getRegisterValue(operands.get(1));
            evaluator.setRegisterValue(operands.getFirst(), value);
        }
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
            evaluator.getDiagnosticService().addError("Movz instruction must have exactly 3 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(2).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
