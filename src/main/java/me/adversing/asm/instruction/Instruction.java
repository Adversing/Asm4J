package me.adversing.asm.instruction;

import me.adversing.asm.Operand;

import java.util.List;

public record Instruction(String name, List<Operand> operands) {

    public boolean isLabel() {
        return name.endsWith(":");
    }

    public String getLabel() {
        if (isLabel()) {
            return name.substring(0, name.length() - 1);
        }
        return null;
    }
}
