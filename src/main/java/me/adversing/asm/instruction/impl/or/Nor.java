package me.adversing.asm.instruction.impl.or;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Nor implements InstructionHandler {
    @Override
    public String getName() {
        return "nor";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value1 = evaluator.getRegisterValue(operands.get(1));
        int value2 = evaluator.getRegisterValue(operands.get(2));
        evaluator.setRegisterValue(operands.getFirst(), ~(value1 | value2));
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
            evaluator.getDiagnosticService().addError("Nor instruction must have exactly 3 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.get(1).value());
            evaluator.getDiagnosticService().addError("Cannot write to a constant value.");
            return false;
        } catch (NumberFormatException ignored) {
        }

        try {
            Integer.parseInt(operands.get(2).value());
            evaluator.getDiagnosticService().addError("Cannot write to a constant value.");
            return false;
        } catch (NumberFormatException ignored) {
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(2).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
