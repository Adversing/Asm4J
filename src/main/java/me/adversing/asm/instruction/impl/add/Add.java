package me.adversing.asm.instruction.impl.add;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Add implements InstructionHandler {
    @Override
    public String getName() {
        return "add";
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

        int value1 = evaluator.getRegisterValue(operands.get(1));
        int value2 = evaluator.getRegisterValue(operands.get(2));
        evaluator.setRegisterValue(register, value1 + value2);
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
            evaluator.getDiagnosticService().addError("Add instruction must have exactly 3 operand(s).");
            return false;
        }

        

        int value1 = evaluator.getRegisterValue(operands.get(1));
        int value2 = evaluator.getRegisterValue(operands.get(2));

        if ((value1 > 0 && value2 > 0 && value1 > Integer.MAX_VALUE - value2) ||
            (value1 < 0 && value2 < 0 && value1 < Integer.MIN_VALUE - value2)) {
            evaluator.getDiagnosticService().addError("Add instruction arithmetic overflow detected.");
            return false;
        }
        return true;
    }
}
