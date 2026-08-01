package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsFound;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsNodeNotFound;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.io.PrintStream;
import java.util.Objects;

final class NodeDetailsCliCommand {
    private final NodeDetailsUseCase useCase;
    private final NodeDetailsTextRenderer renderer;

    NodeDetailsCliCommand(NodeDetailsUseCase useCase, NodeDetailsTextRenderer renderer) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    int execute(String projectId, String nodeId, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            var result = Objects.requireNonNull(useCase.execute(projectId, nodeId), "node details result");
            if (result instanceof NodeDetailsFound found) {
                out.println(renderer.renderFound(projectId, found.details()));
                return CliExitCode.SUCCESS.value();
            }
            if (result instanceof NodeDetailsProjectNotFound notFound) {
                out.println("Project not found: " + notFound.projectId());
                return CliExitCode.PROJECT_NOT_FOUND.value();
            }
            NodeDetailsNodeNotFound notFound = (NodeDetailsNodeNotFound) result;
            out.println("Node not found: " + notFound.nodeId() + " in project " + notFound.projectId());
            return CliExitCode.NODE_NOT_FOUND.value();
        } catch (IllegalArgumentException exception) {
            err.println("Invalid node details request: " + safeMessage(exception));
            return CliExitCode.INVALID_USAGE.value();
        } catch (ProjectPersistenceException exception) {
            err.println("Node details failed: " + safeMessage(exception));
            return CliExitCode.APPLICATION_FAILURE.value();
        } catch (RuntimeException exception) {
            err.println("Node details failed.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No details available."
                : exception.getMessage();
    }
}
