package me.adversing.asm.instruction.impl.mul;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class MulD implements InstructionHandler {
    @Override
    public String getName() {
        return "mul.d";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        double value1 = evaluator.getFpRegisterValue(operands.get(1));
        double value2 = evaluator.getFpRegisterValue(operands.get(2));
        evaluator.setFpRegisterValue(operands.getFirst(), value1 * value2);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Mul.d instruction must have exactly 3 operand(s).");
            return false;
        }

        

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.get(1).value()) ||
                !evaluator.getFpRegisterOffsets().containsKey(operands.get(2).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
