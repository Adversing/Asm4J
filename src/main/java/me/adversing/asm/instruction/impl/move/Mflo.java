package me.adversing.asm.instruction.impl.move;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;

import java.util.List;

public class Mflo extends BaseInstructionHandler {
    
    @Override
    public String getName() {
        return "mflo";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int loValue = evaluator.getRegisterValue(OperandConstants.LO_REGISTER);
        
        evaluator.setRegisterValue(operands.getFirst(), loValue);
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (!validateOperandCount(operands, 1, evaluator)) {
            return false;
        }
        
        return validateIntRegister(operands.getFirst().value(), evaluator);
    }
}
