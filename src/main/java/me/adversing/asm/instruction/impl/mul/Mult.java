package me.adversing.asm.instruction.impl.mul;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;

import java.util.List;

public class Mult extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "mult";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        long value1 = evaluator.getRegisterValue(operands.getFirst());
        long value2 = evaluator.getRegisterValue(operands.get(1));
        long result = value1 * value2;

        evaluator.setRegisterValue(OperandConstants.LO_REGISTER, (int)(result & 0xFFFFFFFFL));
        evaluator.setRegisterValue(OperandConstants.HI_REGISTER, (int)(result >> 32));
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Mult uses special registers HI and LO
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (!validateOperandCount(operands, 2, evaluator)) {
            return false;
        }

        return validateIntRegister(operands.getFirst().value(), evaluator) &&
               validateIntRegister(operands.get(1).value(), evaluator);
    }
}
