package me.adversing.asm.instruction.impl.div;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Divu extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "divu";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        long value1 = evaluator.getRegisterValue(operands.getFirst()) & 0xFFFFFFFFL;
        long value2 = evaluator.getRegisterValue(operands.get(1)) & 0xFFFFFFFFL;

        if (value2 == 0) {
            evaluator.getDiagnosticService().addError("Division by zero in divu instruction.");
            return;
        }

        evaluator.setRegisterValue(OperandConstants.LO_REGISTER, (int)(value1 / value2));
        evaluator.setRegisterValue(OperandConstants.HI_REGISTER, (int)(value1 % value2));
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Divu uses special registers LO and HI
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Divu instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }

        long value2 = evaluator.getRegisterValue(operands.get(1)) & 0xFFFFFFFFL;
        if (value2 == 0L) {
            evaluator.getDiagnosticService().addError("Division by zero detected.");
            return false;
        }
        return true;
    }
}
