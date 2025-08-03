package me.adversing.asm.instruction.impl.nop;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Nop implements InstructionHandler {
    @Override
    public String getName() {
        return "nop";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        checkOperands(operands, evaluator);
        // NOP instruction performs no operation
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Nop doesn't use any registers
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (!operands.isEmpty()) {
            evaluator.getDiagnosticService().addError("Nop instruction must have no operands.");
            return false;
        }
        return true;
    }
}
