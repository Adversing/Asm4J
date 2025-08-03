package me.adversing.asm.instruction.impl.load;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Lw implements InstructionHandler {
    @Override
    public String getName() {
        return "lw";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        String targetRegister = operands.getFirst().value();
        String sourceLabel = operands.get(1).value();

        Integer address = evaluator.getVariableAddress(sourceLabel);
        if (address == null) {
            evaluator.getDiagnosticService().addError("Label not found: ." + sourceLabel);
            return;
        }

        int value = evaluator.loadWordFromMemory(address);

        evaluator.setRegisterValue(new Operand(targetRegister), value);
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
            evaluator.getDiagnosticService().addError("Lw instruction must have exactly 2 operand(s).");
            return false;
        }

        String targetRegister = operands.getFirst().value();
        String source = operands.get(1).value();

        if (!evaluator.getIntRegisterOffsets().containsKey(targetRegister)) {
            evaluator.getDiagnosticService().addError("Destination register (." + targetRegister + ") not found.");
            return false;
        }

        if (evaluator.getVariableAddress(source) == null) {
            evaluator.getDiagnosticService().addError("Source label not found: ." + source);
            return false;
        }
        return true;
    }
}
