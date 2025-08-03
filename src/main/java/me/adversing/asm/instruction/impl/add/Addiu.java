package me.adversing.asm.instruction.impl.add;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Addiu implements InstructionHandler {
    @Override
    public String getName() {
        return "addiu";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        Operand register = operands.getFirst();
        if (!checkDestinationRegister(register.value().replace("$", ""), evaluator)) {
            return;
        }

        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value1 = evaluator.getRegisterValue(operands.get(1));
        int immediate = Integer.parseInt(operands.get(2).value());
        evaluator.setRegisterValue(register, value1 + immediate);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getIntRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        } else if (evaluator.getIntRegisterOffsets().get(register) != 0) {
            evaluator.getDiagnosticService().addError("Register ." + register + " is already in use.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Addiu instruction must have exactly 3 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.get(2).value());
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Addiu instruction immediate must be an integer.");
            return false;
        }
        return true;
    }
}
