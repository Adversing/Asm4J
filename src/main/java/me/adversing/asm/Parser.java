package me.adversing.asm;

import lombok.Getter;
import me.adversing.asm.diagnostic.DiagnosticService;
import me.adversing.asm.diagnostic.exception.InvalidProgramStructureException;
import me.adversing.asm.instruction.Instruction;
import me.adversing.asm.variable.Variable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Getter
public class Parser {
    private final DiagnosticService diagnosticService;
    private final Set<String> definedLabels = new HashSet<>();
    private final Set<String> usedLabels = new HashSet<>();
    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final Set<String> definedVariables = new HashSet<>();
    private final Set<String> usedVariables = new HashSet<>();

    public Parser(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    public List<Instruction> parse(List<String> lines) {
        List<Instruction> instructions = new ArrayList<>();

        if (!validateSections(lines)) {
            return instructions;
        }

        processInstructions(lines, instructions);
        validateLabels(diagnosticService);

        return instructions;
    }

    private boolean validateSections(List<String> lines) {
        boolean hasData = false;
        boolean hasText = false;

        for (String line : lines) {
            line = line.trim();
            if (line.equals(".data")) {
                if (hasData) {
                    diagnosticService.addError("Duplicate .data section found.");
                    return false;
                }
                hasData = true;
            } else if (line.equals(".text")) {
                if (hasText) {
                    diagnosticService.addError("Duplicate .text section found.");
                    return false;
                }
                hasText = true;
            }
        }

        if (!(hasData && hasText)) {
            diagnosticService.addError("Program must contain both .data and .text sections.");
            return false;
        }

        return true;
    }

    private void processInstructions(List<String> lines, List<Instruction> instructions) {
        boolean inTextSection = false;

        for (String line : lines) {
            line = line.trim();
            if (shouldSkipLine(line)) continue;

            line = removeInlineComments(line);
            switch (line) {
                case "" -> {
                    continue;
                }
                case ".data" -> {
                    inTextSection = false;
                    continue;
                }
                case ".text" -> {
                    inTextSection = true;
                    continue;
                }
            }

            if (!inTextSection) {
                processDataDeclaration(line);
            } else {
                processInstruction(line, inTextSection, instructions);
            }
        }
    }

    private void processDataDeclaration(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 2) return;

        String name = parts[0];
        if (name.endsWith(":")) {
            name = name.substring(0, name.length() - 1);
        }

        String type = parts[1];
        String value = parts.length > 2 ? parts[2] : "";

        if (!Variable.isValidType(type)) {
            diagnosticService.addError("Invalid data type: " + type + " for variable: " + name);
            return;
        }

        if (variables.containsKey(name)) {
            diagnosticService.addError("Duplicate variable declaration: " + name);
            return;
        }

        Variable.Type varType = Variable.Type.fromString(type);
        if (varType == Variable.Type.WORD && !value.matches("-?\\d+")) {
            diagnosticService.addError("Invalid value for .word: " + value);
            return;
        } else if (varType == Variable.Type.FLOAT && !value.matches("-?\\d+(\\.\\d+)?")) {
            diagnosticService.addError("Invalid value for .float: " + value);
            return;
        } else if ((varType == Variable.Type.ASCII || varType == Variable.Type.ASCIIZ) && !value.matches("\".*\"")) {
            diagnosticService.addError("Invalid value for .ascii/.asciiz: " + value);
            return;
        }

        variables.put(name, new Variable(name, varType, value));
        definedVariables.add(name);
    }

    private boolean shouldSkipLine(String line) {
        return line.isEmpty() || line.startsWith("#");
    }
    
    private String removeInlineComments(String line) {
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '#') {
                    return line.substring(0, i).trim();
                }
            }
        }

        return line;
    }
    
    private List<String> parseInstructionLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inParentheses = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '(') {
                inParentheses = true;
                current.append(c);
            } else if (c == ')') {
                inParentheses = false;
                current.append(c);
            } else if (c == ',' && !inParentheses) {
                if (!current.isEmpty()) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                }
            } else if (Character.isWhitespace(c) && !inParentheses) {
                if (!current.isEmpty()) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    private void processInstruction(String line, boolean inTextSection, List<Instruction> instructions) {
        if (line.contains(":")) {
            String[] labelParts = line.split(":", 2);
            String labelName = labelParts[0].trim();
            if (!labelName.isEmpty()) {
                processLabel(labelName + ":");
            }
            if (labelParts.length > 1) {
                line = labelParts[1].trim();
                if (line.isEmpty()) {
                    return; // just a label, no instruction
                }
            } else {
                return; // just a label again
            }
        }

        if (!inTextSection) {
            diagnosticService.addError("Instructions must be in .text section.");
            return;
        }

        List<String> parts = parseInstructionLine(line);
        if (parts.isEmpty()) return;

        String name = parts.getFirst();
        List<Operand> operands = new ArrayList<>();

        for (int i = 1; i < parts.size(); i++) {
            String operand = parts.get(i).trim();
            if (!operand.isEmpty()) {
                operands.add(new Operand(operand));
                trackOperandUsage(operand);
            }
        }

        instructions.add(new Instruction(name, operands));
    }

    private void processLabel(String name) {
        String label = name.substring(0, name.length() - 1);
        if (!definedLabels.add(label)) {
            diagnosticService.addError("Duplicate label defined: " + label);
        }
    }



    private void trackOperandUsage(String operand) {
        if (operand.contains("(") && operand.contains(")")) {
            int openParen = operand.indexOf('(');
            int closeParen = operand.indexOf(')');
            if (openParen < closeParen) {
                String register = operand.substring(openParen + 1, closeParen);
                if (register.startsWith("$")) {
                    validateRegister(register);
                }

                if (openParen > 0) {
                    String offset = operand.substring(0, openParen);
                    try {
                        Integer.parseInt(offset);
                    } catch (NumberFormatException e) {
                        if (definedVariables.contains(offset)) {
                            usedVariables.add(offset);
                        } else {
                            usedLabels.add(offset);
                        }
                    }
                }
            }
        } else if (operand.startsWith("$")) {
            validateRegister(operand);
        } else {
            try {
                Integer.parseInt(operand);
            } catch (NumberFormatException e) {
                if (definedVariables.contains(operand)) {
                    usedVariables.add(operand);
                } else {
                    usedLabels.add(operand);
                }
            }
        }
    }

    private void validateRegister(String register) {
        if (!register.matches("\\$([tsvak]\\d+|zero|ra|sp|fp|gp|hi|lo|f\\d+|cp0_\\d+)")) {
            diagnosticService.addError("Invalid register: " + register);
        }
    }

    private void validateLabels(DiagnosticService diagnosticService) {
        for (String label : usedLabels) {
            if (!definedLabels.contains(label)) {
                diagnosticService.addError("Undefined label: " + label);
            }
        }
    }

    public List<Instruction> parseFile(File file)
            throws InvalidProgramStructureException, IOException {
        if (!file.getName().endsWith(".asm")) {
            throw new InvalidProgramStructureException("File must have .asm extension.");
        }

        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.isEmpty()) {
            throw new InvalidProgramStructureException("File is empty or unreadable.");
        }

        return parse(lines);
    }
}
