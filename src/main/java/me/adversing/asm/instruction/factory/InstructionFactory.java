package me.adversing.asm.instruction.factory;

import me.adversing.asm.instruction.handler.InstructionHandler;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

public class InstructionFactory {
    private static final InstructionFactory INSTANCE = new InstructionFactory();
    private final Map<String, InstructionHandler> instructionMap;

    private InstructionFactory() {
        instructionMap = new HashMap<>();
        loadInstructions();
    }

    private void loadInstructions() {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder().forPackages("me.adversing.asm.instruction.impl")
        );

        Set<Class<? extends InstructionHandler>> classes = reflections.getSubTypesOf(InstructionHandler.class);
        if (classes.isEmpty()) {
            throw new RuntimeException("No instruction handlers found.");
        }

        for (Class<? extends InstructionHandler> clazz : classes) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            try {
                InstructionHandler handler = clazz.getDeclaredConstructor().newInstance();
                String instructionName = handler.getName();
                instructionMap.put(instructionName, handler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize instruction: " + clazz.getName());
            }
        }

    }

    public static InstructionFactory getInstance() {
        return INSTANCE;
    }

    public InstructionHandler getInstructionHandler(String instructionName) {
        return instructionMap.get(instructionName);
    }
}
