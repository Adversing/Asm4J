package me.adversing.asm.instruction.impl.move;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;

import java.util.List;

public class Move extends BaseInstructionHandler {
    
    @Override
    public String getName() {
        return "move";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int sourceValue = evaluator.getRegisterValue(operands.get(1));
        
        evaluator.setRegisterValue(operands.getFirst(), sourceValue);
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
