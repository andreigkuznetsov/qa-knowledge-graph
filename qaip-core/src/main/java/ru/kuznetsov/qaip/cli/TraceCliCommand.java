package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.trace.TraceFound;
import ru.kuznetsov.qaip.core.application.query.trace.TraceNodeNotFound;
import ru.kuznetsov.qaip.core.application.query.trace.TraceProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.trace.TraceUseCase;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.io.PrintStream;
import java.util.Objects;

final class TraceCliCommand {
    private final TraceUseCase useCase;
    private final TraceTextRenderer renderer;

    TraceCliCommand(TraceUseCase useCase, TraceTextRenderer renderer) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    int execute(String projectId, String startNodeId, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            var result = Objects.requireNonNull(useCase.execute(projectId, startNodeId), "trace result");
            if (result instanceof TraceFound found) {
                out.println(renderer.renderFound(found.projectId(), found.startNodeId(), found.trace()));
                return CliExitCode.SUCCESS.value();
            }
            if (result instanceof TraceProjectNotFound notFound) {
                out.println("Project not found: " + notFound.projectId());
                return CliExitCode.PROJECT_NOT_FOUND.value();
            }
            TraceNodeNotFound notFound = (TraceNodeNotFound) result;
            out.println("Node not found: " + notFound.startNodeId() + " in project " + notFound.projectId());
            return CliExitCode.NODE_NOT_FOUND.value();
        } catch (IllegalArgumentException exception) {
            err.println("Invalid trace request: " + safeMessage(exception));
            return CliExitCode.INVALID_USAGE.value();
        } catch (ProjectPersistenceException exception) {
            err.println("Trace failed: " + safeMessage(exception));
            return CliExitCode.APPLICATION_FAILURE.value();
        } catch (RuntimeException exception) {
            err.println("Trace failed.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No details available."
                : exception.getMessage();
    }
}
