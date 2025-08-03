package me.adversing.asm.diagnostic.exception;

public class InvalidProgramStructureException extends Throwable {
    public InvalidProgramStructureException(String message) {
        super(message);
    }

    public InvalidProgramStructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
