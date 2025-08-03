package me.adversing;

import me.adversing.asm.Parser;
import me.adversing.asm.diagnostic.DiagnosticService;
import me.adversing.asm.diagnostic.exception.InvalidProgramStructureException;
import me.adversing.asm.engine.ASMEvaluator;
import me.adversing.asm.instruction.Instruction;
import me.adversing.asm.variable.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        boolean debug = hasDebugFlag(args);

        DiagnosticService diagnosticService = new DiagnosticService();
        Parser parser = new Parser(diagnosticService);
        String file = validateFile(args[0]);
        File asmFile = new File(file);

        try (ASMEvaluator evaluator = new ASMEvaluator(diagnosticService, debug)) {
            List<Instruction> instructions = parser.parseFile(asmFile);
            Map<String, Variable> variables = parser.getVariables();

            if (!diagnosticService.hasErrors()) {
                CompletableFuture<Void> evaluationFuture = CompletableFuture.runAsync(() -> {
                    evaluator.initializeVariables(variables);
                    evaluator.evaluate(instructions);
                });

                try {
                    evaluationFuture.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    diagnosticService.addError("Evaluation was interrupted.");
                } catch (ExecutionException e) {
                    diagnosticService.addError("An error occurred during evaluation: " + e.getCause());
                }
            } else {
                diagnosticService.report();
            }
        } catch (InvalidProgramStructureException | IOException e) {
            diagnosticService.addError("Failed to process file: " + e.getMessage());
            diagnosticService.report();
        }
    }

    private static boolean hasDebugFlag(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> "--debug".equals(arg) || "-d".equals(arg));
    }

    private static String validateFile(String file) {
        if (file == null) {
            throw new IllegalArgumentException("No file provided.");
        }
        return file;
    }
}
