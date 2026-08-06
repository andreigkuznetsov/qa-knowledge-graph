package ru.kuznetsov.qaip.runtime;

import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQuery;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsQuery;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListQuery;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsQuery;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsUseCase;
import ru.kuznetsov.qaip.core.application.query.trace.TraceUseCase;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationUseCase;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public record QaipRuntime(
        ImportProjectUseCase importProjectUseCase,
        ProjectReader projectReader,
        EventPathQuery eventPathQuery,
        OperationDetailsQuery operationDetailsQuery,
        OperationListQuery operationListQuery,
        OperationTestsQuery operationTestsQuery,
        ProjectSummaryUseCase projectSummaryUseCase,
        NodeDetailsUseCase nodeDetailsUseCase,
        RelationshipsUseCase relationshipsUseCase,
        TraceUseCase traceUseCase,
        ValidationUseCase validationUseCase
) {
    public QaipRuntime {
        Objects.requireNonNull(importProjectUseCase, "importProjectUseCase");
        Objects.requireNonNull(projectReader, "projectReader");
        Objects.requireNonNull(eventPathQuery, "eventPathQuery");
        Objects.requireNonNull(operationDetailsQuery, "operationDetailsQuery");
        Objects.requireNonNull(operationListQuery, "operationListQuery");
        Objects.requireNonNull(operationTestsQuery, "operationTestsQuery");
        Objects.requireNonNull(projectSummaryUseCase, "projectSummaryUseCase");
        Objects.requireNonNull(nodeDetailsUseCase, "nodeDetailsUseCase");
        Objects.requireNonNull(relationshipsUseCase, "relationshipsUseCase");
        Objects.requireNonNull(traceUseCase, "traceUseCase");
        Objects.requireNonNull(validationUseCase, "validationUseCase");
    }
}
