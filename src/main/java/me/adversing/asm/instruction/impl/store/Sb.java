package me.adversing.asm.instruction.impl.store;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Sb implements InstructionHandler {
    @Override
    public String getName() {
        return "sb";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.getFirst());
        int address = evaluator.getRegisterValue(operands.get(1));
        evaluator.storeByteToMemory(address, (byte)(value & 0xFF));
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
            evaluator.getDiagnosticService().addError("Sb instruction must have exactly 2 operand(s).");
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
