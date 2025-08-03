package me.adversing.asm.instruction.handler;

import me.adversing.asm.Operand;
import me.adversing.asm.engine.ASMEvaluator;

import java.util.List;

public interface InstructionHandler {
    String getName();
    void execute(List<Operand> operands, ASMEvaluator evaluator);
    boolean checkDestinationRegister(String register, ASMEvaluator evaluator);
    boolean checkOperands(List<Operand> operands, ASMEvaluator evaluator);
}
