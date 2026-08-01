package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsFound;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsNodeNotFound;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsUseCase;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.io.PrintStream;
import java.util.Objects;

final class RelationshipsCliCommand {
    private final RelationshipsUseCase useCase;
    private final RelationshipsTextRenderer renderer;

    RelationshipsCliCommand(RelationshipsUseCase useCase, RelationshipsTextRenderer renderer) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    int execute(String projectId, String nodeId, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            var result = Objects.requireNonNull(useCase.execute(projectId, nodeId), "relationships result");
            if (result instanceof RelationshipsFound found) {
                out.println(renderer.renderFound(found.projectId(), found.nodeId(), found.relationships()));
                return CliExitCode.SUCCESS.value();
            }
            if (result instanceof RelationshipsProjectNotFound notFound) {
                out.println("Project not found: " + notFound.projectId());
                return CliExitCode.PROJECT_NOT_FOUND.value();
            }
            RelationshipsNodeNotFound notFound = (RelationshipsNodeNotFound) result;
            out.println("Node not found: " + notFound.nodeId() + " in project " + notFound.projectId());
            return CliExitCode.NODE_NOT_FOUND.value();
        } catch (IllegalArgumentException exception) {
            err.println("Invalid relationships request: " + safeMessage(exception));
            return CliExitCode.INVALID_USAGE.value();
        } catch (ProjectPersistenceException exception) {
            err.println("Relationships failed: " + safeMessage(exception));
            return CliExitCode.APPLICATION_FAILURE.value();
        } catch (RuntimeException exception) {
            err.println("Relationships failed.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No details available."
                : exception.getMessage();
    }
}
