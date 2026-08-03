package ru.kuznetsov.qaip.runtime;

import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsUseCase;
import ru.kuznetsov.qaip.core.application.query.trace.TraceUseCase;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationUseCase;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public record QaipRuntime(
        ImportProjectUseCase importProjectUseCase,
        ProjectReader projectReader,
        ProjectSummaryUseCase projectSummaryUseCase,
        NodeDetailsUseCase nodeDetailsUseCase,
        RelationshipsUseCase relationshipsUseCase,
        TraceUseCase traceUseCase,
        ValidationUseCase validationUseCase
) {
    public QaipRuntime {
        Objects.requireNonNull(importProjectUseCase, "importProjectUseCase");
        Objects.requireNonNull(projectReader, "projectReader");
        Objects.requireNonNull(projectSummaryUseCase, "projectSummaryUseCase");
        Objects.requireNonNull(nodeDetailsUseCase, "nodeDetailsUseCase");
        Objects.requireNonNull(relationshipsUseCase, "relationshipsUseCase");
        Objects.requireNonNull(traceUseCase, "traceUseCase");
        Objects.requireNonNull(validationUseCase, "validationUseCase");
    }
}
