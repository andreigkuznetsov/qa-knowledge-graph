package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryResult;

import java.util.Objects;

final class ProjectSummaryTextRenderer {
    String renderFound(ProjectSummaryResult summary) {
        Objects.requireNonNull(summary, "summary");
        return String.join(System.lineSeparator(),
                "Project Summary",
                "Project ID: " + summary.projectId(),
                "Contract Version: " + summary.projectContractVersion(),
                "Schema Version: " + summary.schemaVersion(),
                "Sources: " + summary.sourceCount(),
                "Nodes: " + summary.nodeCount(),
                "Relationships: " + summary.relationshipCount(),
                "Evidence: " + summary.evidenceCount(),
                "Declared Changes: " + summary.declaredChangeCount());
    }

    String renderNotFound(String projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return "Project not found: " + projectId;
    }
}
