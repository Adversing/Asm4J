package me.adversing.asm.instruction.impl.abs;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class AbsD implements InstructionHandler {
    @Override
    public String getName() {
        return "abs.d";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        Operand register = operands.getFirst();
        if (!checkDestinationRegister(register.value().replace("$", ""), evaluator)) {
            return;
        }

        double value = evaluator.getFpRegisterValue(operands.get(1));
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        evaluator.setFpRegisterValue(register, Math.abs(value));
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        } else {
            // check if register is already in use
            if (evaluator.getFpRegisterOffsets().get(register) != 0.0) {
                evaluator.getDiagnosticService().addError("Register ." + register + " is already in use.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Abs instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Double.parseDouble(operands.get(1).value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Abs instruction operand must be a double.");
            return false;
        }

        double value = evaluator.getFpRegisterValue(operands.get(1));

        if (value == Double.MIN_VALUE || value == Double.MAX_VALUE) {
            evaluator.getDiagnosticService().addError("Abs instruction arithmetic arithmetic overflow detected.");
            return false;
        }
        return true;
    }
}
