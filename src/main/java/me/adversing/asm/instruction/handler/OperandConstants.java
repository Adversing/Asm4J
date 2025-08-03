package me.adversing.asm.instruction.handler;

import me.adversing.asm.Operand;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class OperandConstants {   
    
    public static final Operand LO_REGISTER = new Operand("$lo");
    public static final Operand HI_REGISTER = new Operand("$hi");
    public static final Operand RA_REGISTER = new Operand("$ra");
    
    public static final Operand V0_REGISTER = new Operand("$v0");
    public static final Operand V1_REGISTER = new Operand("$v1");
    
    public static final Operand A0_REGISTER = new Operand("$a0");
    public static final Operand A1_REGISTER = new Operand("$a1");
    public static final Operand A2_REGISTER = new Operand("$a2");
    public static final Operand A3_REGISTER = new Operand("$a3");
    
    public static final Operand T0_REGISTER = new Operand("$t0");
    public static final Operand T1_REGISTER = new Operand("$t1");
    public static final Operand T2_REGISTER = new Operand("$t2");
    public static final Operand T3_REGISTER = new Operand("$t3");
    
    // CP0 registers
    public static final Operand CAUSE_REGISTER = new Operand("$cause");
    public static final Operand EPC_REGISTER = new Operand("$epc");

    // Floating-point registers
    public static final Operand F0_REGISTER = new Operand("$f0");
    public static final Operand F12_REGISTER = new Operand("$f12");
}
