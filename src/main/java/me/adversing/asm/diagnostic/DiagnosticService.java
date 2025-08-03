package me.adversing.asm.diagnostic;

import lombok.Getter;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DiagnosticService {
    private Logger logger = null;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void report() {
        logger.info("=== Execution Report ===");
        if (errors.isEmpty() && warnings.isEmpty()) {
            logger.info("Status: Success - No issues detected");
        } else {
            if (!errors.isEmpty()) {
                logger.error("Errors found:");
                errors.forEach(error -> logger.error("- {}", error));
            }
            if (!warnings.isEmpty()) {
                logger.warn("Warnings found:");
                warnings.forEach(warning -> logger.warn("- {}", warning));
            }
        }
        logger.info("=====================\n");
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public DiagnosticService withLogger(Logger diagnosticLogger) {
        if (this.logger != null) {
            throw new IllegalStateException("Logger already set.");
        }

        this.logger = diagnosticLogger;
        return this;
    }
}
