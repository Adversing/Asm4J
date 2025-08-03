package me.adversing.asm.instruction.impl.and;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Andi implements InstructionHandler {
    @Override
    public String getName() {
        return "andi";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        Operand register = operands.getFirst();
        if (!checkDestinationRegister(register.value().replace("$", ""), evaluator)) {
            return;
        }

        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value = evaluator.getRegisterValue(operands.get(1));
        int immediate = Integer.parseInt(operands.get(2).value());
        evaluator.setRegisterValue(register, value & immediate);
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        if (!evaluator.getIntRegisterOffsets().containsKey(register)) {
            evaluator.getDiagnosticService().addError("Register ." + register + " not found.");
            return false;
        } else if (evaluator.getIntRegisterOffsets().get(register) != 0) {
            evaluator.getDiagnosticService().addError("Register ." + register + " is already in use.");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Andi instruction must have exactly 3 operand(s).");
            return false;
        }

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Source Register not found.");
            return false;
        }

        try {
            int immediate = Integer.parseInt(operands.get(2).value());
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
