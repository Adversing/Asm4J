package me.adversing.asm.instruction.impl.branch;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.handler.InstructionHandler;

import java.util.List;

public class Beq implements InstructionHandler {
    @Override
    public String getName() {
        return "beq";
    }
    @Override
    public void execute(List<Operand> operands, ASMEvaluator evaluator) {
        if (!checkOperands(operands, evaluator)) {
            return;
        }

        int value1 = evaluator.getRegisterValue(operands.getFirst());
        int value2 = evaluator.getRegisterValue(operands.get(1));

        if (value1 == value2) {
            evaluator.branchToLabel(operands.get(2).value());
        }
    }

    @Override
    public boolean checkDestinationRegister(String register, ASMEvaluator evaluator) {
        return true; // Branch instructions don't have a destination register
    }

    @Override
    public boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator) {
        if (operands.size() != 3) {
            evaluator.getDiagnosticService().addError("Beq instruction must have exactly 3 operand(s).");
            return false;
        }

        

        if (!evaluator.getIntRegisterOffsets().containsKey(operands.getFirst().value()) ||
                !evaluator.getIntRegisterOffsets().containsKey(operands.get(1).value())) {
            evaluator.getDiagnosticService().addError("Beq instruction registers not found.");
            return false;
        }
        return true;
    }
}
