package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.io.PrintStream;
import java.util.Objects;

final class ProjectSummaryCliCommand {
    private final ProjectSummaryUseCase useCase;
    private final ProjectSummaryTextRenderer renderer;

    ProjectSummaryCliCommand(ProjectSummaryUseCase useCase, ProjectSummaryTextRenderer renderer) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    int execute(String projectId, PrintStream out, PrintStream err) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try {
            var result = Objects.requireNonNull(useCase.execute(projectId), "project summary result");
            if (result instanceof ProjectSummaryFound found) {
                out.println(renderer.renderFound(found.summary()));
                return CliExitCode.SUCCESS.value();
            }
            ProjectSummaryNotFound notFound = (ProjectSummaryNotFound) result;
            out.println(renderer.renderNotFound(notFound.projectId()));
            return CliExitCode.PROJECT_NOT_FOUND.value();
        } catch (IllegalArgumentException exception) {
            err.println("Invalid project ID: " + safeMessage(exception));
            return CliExitCode.INVALID_USAGE.value();
        } catch (ProjectPersistenceException exception) {
            err.println("Project summary failed: " + safeMessage(exception));
            return CliExitCode.APPLICATION_FAILURE.value();
        } catch (RuntimeException exception) {
            err.println("Project summary failed.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No details available."
                : exception.getMessage();
    }
}
