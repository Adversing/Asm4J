package me.adversing.asm.instruction.impl.eret;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Eret implements InstructionHandler {
    @Override
    public String getName() {
        return "eret";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int returnAddress = evaluator.getCp0RegisterValue(new Operand("$epc"));
        evaluator.setProgramCounter(returnAddress);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Eret doesn't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (!operands.isEmpty()) {
            evaluator.getDiagnosticService().addError("Eret instruction must have no operands.");
            return false;
        }
        return true;
    }
}
