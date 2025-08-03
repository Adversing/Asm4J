package me.adversing.asm.instruction.impl.jump;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Jalr extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "jalr";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        evaluator.setRegisterValue(OperandConstants.RA_REGISTER, evaluator.getProgramCounter() + 1);
        evaluator.jumpToRegister(operands.getFirst().value());
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Jump and link register uses $ra register automatically
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 1) {
            evaluator.getDiagnosticService().addError("Jalr instruction must have exactly 1 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Source Register not found.");
            return false;
        }
        return true;
    }
}
