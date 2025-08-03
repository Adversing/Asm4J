package me.adversing.asm.instruction.impl.add;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class AddS implements InstructionHandler {
    @Override
    public String getName() {
        return "add.s";
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

        float value1 = (float) evaluator.getFpRegisterValue(operands.get(1));
        float value2 = (float) evaluator.getFpRegisterValue(operands.get(2));
        evaluator.setFpRegisterValue(register, value1 + value2);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        } else if (evaluator.getFpRegisterOffsets().get(register) != 0.0) {
            evaluator.getDiagnosticService().addError("Register ." + register + " is already in use.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Add.s instruction must have exactly 3 operand(s).");
            return false;
        }

        

        float value1 = (float) evaluator.getFpRegisterValue(operands.get(1));
        float value2 = (float) evaluator.getFpRegisterValue(operands.get(2));

        if (value1 == Float.MIN_VALUE || value2 == Float.MIN_VALUE ||
                value1 == Float.MAX_VALUE || value2 == Float.MAX_VALUE) {
            evaluator.getDiagnosticService().addError("Add.s instruction arithmetic arithmetic overflow detected.");
            return false;
        }
        return true;
    }
}
