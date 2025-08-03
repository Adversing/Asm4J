package me.adversing.asm.instruction.impl.load;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Lui implements InstructionHandler {
    @Override
    public String getName() {
        return "lui";
    }

    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int immediate = Integer.parseInt(operands.get(1).value());
        evaluator.setRegisterValue(operands.getFirst(), immediate << 16);
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
            evaluator.getDiagnosticService().addError("Lui instruction must have exactly 2 operand(s).");
            return false;
        }

        try {
            int immediate = Integer.parseInt(operands.get(1).value());
            if (immediate < 0 || immediate > 0xFFFF) {
                evaluator.getDiagnosticService().addError("Immediate value out of range.");
                return false;
            }
        } catch (NumberFormatException e) {
            evaluator.getDiagnosticService().addError("Invalid immediate value.");
            return false;
        }
        return true;
    }
}
