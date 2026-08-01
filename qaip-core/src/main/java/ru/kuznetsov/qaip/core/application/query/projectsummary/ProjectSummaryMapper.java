package ru.kuznetsov.qaip.core.application.query.projectsummary;

import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Project;

import java.util.Objects;

public final class ProjectSummaryMapper {
    public ProjectSummaryResult map(Project project) {
        Objects.requireNonNull(project, "project");
        EvidenceManifest evidence = project.evidenceManifest();
        int evidenceCount = Math.addExact(
                Math.addExact(evidence.identityAssertions().size(), evidence.relationships().size()),
                evidence.provenance().size());
        return new ProjectSummaryResult(
                project.metadata().id(),
                project.projectContractVersion(),
                project.schemaVersion(),
                project.sources().size(),
                project.nodes().size(),
                project.relationships().size(),
                evidenceCount,
                project.declaredChanges().size());
    }
}
