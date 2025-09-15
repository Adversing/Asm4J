package me.adversing.asm.variable;

import lombok.Getter;
import java.util.Arrays;

public record Variable(String name, Type type, String value) {
    public static boolean isValidType(String type) {
        return Arrays.stream(Type.values())
                .anyMatch(t -> t.type.equalsIgnoreCase(type));
    }

    public boolean is(Type type) {
        return type == this.type;
    }

    @Getter
    public enum Type {
        WORD(".word"),
        BYTE(".byte"),
        HALF(".half"),
        FLOAT(".float"),
        DOUBLE(".double"),
        ASCII(".ascii"),
        ASCIIZ(".asciiz"),
        SPACE(".space");

        private final String type;

        Type(String type) {
            this.type = type;
        }

        public static Type fromString(String type) {
            return Arrays.stream(Type.values())
                    .filter(t -> t.type.equalsIgnoreCase(type))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + type));
        }
    }
}
