package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.validation.ValidationCompleted;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationUseCase;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.io.PrintStream;
import java.util.Objects;

final class ValidationCliCommand {
    private final ValidationUseCase useCase;
    private final ValidationTextRenderer renderer;

    ValidationCliCommand(ValidationUseCase useCase, ValidationTextRenderer renderer) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    int execute(String projectId, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            var result = Objects.requireNonNull(useCase.execute(projectId), "validation result");
            if (result instanceof ValidationCompleted completed) {
                out.println(renderer.renderCompleted(completed.projectId(), completed.report()));
                return CliExitCode.SUCCESS.value();
            }
            ValidationProjectNotFound notFound = (ValidationProjectNotFound) result;
            out.println("Project not found: " + notFound.projectId());
            return CliExitCode.PROJECT_NOT_FOUND.value();
        } catch (IllegalArgumentException exception) {
            err.println("Invalid validation request: " + safeMessage(exception));
            return CliExitCode.INVALID_USAGE.value();
        } catch (ProjectPersistenceException exception) {
            err.println("Validation failed: " + safeMessage(exception));
            return CliExitCode.APPLICATION_FAILURE.value();
        } catch (RuntimeException exception) {
            err.println("Validation failed.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No details available."
                : exception.getMessage();
    }
}
