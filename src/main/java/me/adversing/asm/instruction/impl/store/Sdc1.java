package me.adversing.asm.instruction.impl.store;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Sdc1 implements InstructionHandler {
    @Override
    public String getName() {
        return "sdc1";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        double value = evaluator.getFpRegisterValue(operands.getFirst());
        int address = evaluator.getRegisterValue(operands.get(1));
        long bits = Double.doubleToLongBits(value);
        evaluator.storeDoubleWordToMemory(address, bits);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getFpRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 2) {
            evaluator.getDiagnosticService().addError("Sdc1 instruction must have exactly 2 operand(s).");
            return false;
        }

        if (!evaluator.getFpRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Invalid register combination.");
            return false;
        }
        return true;
    }
}
