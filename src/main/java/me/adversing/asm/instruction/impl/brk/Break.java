package me.adversing.asm.instruction.impl.brk;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.BaseInstructionHandler;
import me.adversing.asm.instruction.handler.OperandConstants;

import java.util.List;

public class Break extends BaseInstructionHandler {
    @Override
    public String getName() {
        return "break";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        evaluator.setCp0RegisterValue(OperandConstants.CAUSE_REGISTER, 9); // Break exception code
        evaluator.setCp0RegisterValue(OperandConstants.EPC_REGISTER, evaluator.getProgramCounter());

        if (!operands.isEmpty()) {
            try {
                int errorCode = Integer.parseInt(operands.getFirst().value());
                evaluator.shutdown(this);
                evaluator.handleShutdown(errorCode);
            } catch (NumberFormatException e) {
                evaluator.getDiagnosticService().addError("Break instruction error code must be an integer.");
            }
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Break instruction doesn't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() > 1) {
            evaluator.getDiagnosticService().addError("Break instruction must have zero or one operand.");
            return false;
        }
        return true;
    }
}
