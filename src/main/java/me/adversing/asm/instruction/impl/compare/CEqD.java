package me.adversing.asm.instruction.impl.compare;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class CEqD implements InstructionHandler {
    @Override
    public String getName() {
        return "c.eq.d";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        double value1 = evaluator.getFpRegisterValue(operands.getFirst());
        double value2 = evaluator.getFpRegisterValue(operands.get(1));
        evaluator.setFpConditionFlag(value1 == value2);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Compare instructions don't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("C.eq.d instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getFpRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("C.eq.d instruction registers not found.");
            return false;
        }
        return true;
    }
}
