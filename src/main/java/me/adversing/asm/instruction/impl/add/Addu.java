package me.adversing.asm.instruction.impl.add;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Addu implements InstructionHandler {
    @Override
    public String getName() {
        return "addu";
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

        long value1 = evaluator.getRegisterValue(operands.get(1)) & 0xFFFFFFFFL;
        long value2 = evaluator.getRegisterValue(operands.get(2)) & 0xFFFFFFFFL;
        evaluator.setRegisterValue(register, (int)(value1 + value2));
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
            evaluator.getDiagnosticService().addError("Addu instruction must have exactly 3 operand(s).");
            return false;
        }

        
        return true;
    }

}
