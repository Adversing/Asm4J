package me.adversing.asm.instruction.impl.load;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class La implements InstructionHandler {

    @Override
    public String getName() {
        return "la";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        String targetRegister = operands.getFirst().value();
        String label = operands.get(1).value();

        if (!checkDestinationRegister(targetRegister, evaluator)) {
            return;
        }

        Integer address = evaluator.getVariableAddress(label);
        if (address == null) {
            evaluator.getDiagnosticService().addError("Label not found: ." + label);
            return;
        }

        evaluator.loadAddress(targetRegister, address);
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
            evaluator.getDiagnosticService().addError("La instruction must have exactly 2 operand(s).");
            return false;
        }
        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value())) {
            evaluator.getDiagnosticService().addError("Destination register (." + operands.getFirst().value() + ") not found.");
            return false;
        }
        return true;
    }
}
