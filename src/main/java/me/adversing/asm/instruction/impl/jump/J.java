package me.adversing.asm.instruction.impl.jump;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class J implements InstructionHandler {
    @Override
    public String getName() {
        return "j";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        evaluator.jumpToLabel(operands.getFirst().value());
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Jump instructions don't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 1) {
            evaluator.getDiagnosticService().addError("J instruction must have exactly 1 operand(s).");
            return false;
        }
        return true;
    }
}
