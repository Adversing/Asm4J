package me.adversing.asm.instruction.impl.load;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Li implements InstructionHandler {
    @Override
    public String getName() {
        return "li";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        Operand register = operands.getFirst();
        if (!checkDestinationRegister(register.value(), evaluator)) {
            return;
        }

        if (!checkOperands(operands, evaluator)) {
            return;
        }

        evaluator.setRegisterValue(register, Integer.parseInt(operands.get(1).value()));
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
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Li instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.get(1).value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid value for Li instruction.");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Source register (." + operands.getFirst().value() + ") not found.");
            return false;
        }
        return true;
    }
}
