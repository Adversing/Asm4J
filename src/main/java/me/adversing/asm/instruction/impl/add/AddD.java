package me.adversing.asm.instruction.impl.add;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class AddD implements InstructionHandler {
    @Override
    public String getName() {
        return "add.d";
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

        double value1 = evaluator.getFpRegisterValue(operands.get(1));
        double value2 = evaluator.getFpRegisterValue(operands.get(2));
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
            evaluator.getDiagnosticService().addError("Add.d instruction must have exactly 3 operand(s).");
            return false;
        }

        

        double value1 = evaluator.getFpRegisterValue(operands.get(1));
        double value2 = evaluator.getFpRegisterValue(operands.get(2));

        if (value1 == Double.MIN_VALUE || value2 == Double.MIN_VALUE ||
                value1 == Double.MAX_VALUE || value2 == Double.MAX_VALUE) {
            evaluator.getDiagnosticService().addError("Add.d instruction arithmetic arithmetic overflow detected.");
            return false;
        }
        return true;
    }
}
