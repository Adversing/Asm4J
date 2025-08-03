package me.adversing.asm.instruction.impl.ceil;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class CeilWS implements InstructionHandler {
    @Override
    public String getName() {
        return "ceil.w.s";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        float value = (float) evaluator.getFpRegisterValue(operands.get(1));
        evaluator.setRegisterValue(operands.getFirst(), (int)Math.ceil(value));
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
            evaluator.getDiagnosticService().addError("Ceil.w.s instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source Register not found.");
            return false;
        }

        float value = (float) evaluator.getFpRegisterValue(operands.get(1));
        if (Float.isInfinite(value) || Float.isNaN(value)) {
            evaluator.getDiagnosticService().addError("Invalid floating point value.");
            return false;
        }
        return true;
    }
}
