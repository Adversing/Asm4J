package me.adversing.asm.instruction.impl.div;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class DivD implements InstructionHandler {
    @Override
    public String getName() {
        return "div.d";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        double value1 = evaluator.getFpRegisterValue(operands.get(1));
        double value2 = evaluator.getFpRegisterValue(operands.get(2));

        if (value2 == 0.0) {
            evaluator.getDiagnosticService().addError("Division by zero in div.d instruction.");
            return;
        }

        evaluator.setFpRegisterValue(operands.getFirst(), value1 / value2);
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
            evaluator.getDiagnosticService().addError("Div.d instruction must have exactly 3 operand(s).");
            return false;
        }

        // Check destination register
        if (!evaluator.getFpRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Destination Register not found.");
            return false;
        }

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.get(1).value()) ||
                !evaluator.getFpRegisterOffsets().containsKey(operands.get(2).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }

        double value2 = evaluator.getFpRegisterValue(operands.get(2));
        if (value2 == 0.0) {
            evaluator.getDiagnosticService().addError("Division by zero detected.");
            return false;
        }
        return true;
    }
}
