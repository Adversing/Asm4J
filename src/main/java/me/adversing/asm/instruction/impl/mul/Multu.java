package me.adversing.asm.instruction.impl.mul;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Multu extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "multu";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        long value1 = evaluator.getRegisterValue(operands.getFirst()) & 0xFFFFFFFFL;
        long value2 = evaluator.getRegisterValue(operands.get(1)) & 0xFFFFFFFFL;
        long result = value1 * value2;

        evaluator.setRegisterValue(OperandConstants.LO_REGISTER, (int)(result & 0xFFFFFFFFL));
        evaluator.setRegisterValue(OperandConstants.HI_REGISTER, (int)(result >> 32));
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Multu uses special registers HI and LO
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Multu instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            Integer.parseInt(operands.getFirst().value().substring(1));
            Integer.parseInt(operands.get(1).value().substring(1));
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Multu instruction operands must be registers.");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source registers not found.");
            return false;
        }
        return true;
    }
}
