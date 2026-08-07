package ru.kuznetsov.qaip.core.application.query.operationtests;

import ru.kuznetsov.qaip.core.domain.Project;

public interface OperationTestsResolver {
    OperationTestsQueryResult resolve(Project project, String projectId, String operationId);
}
