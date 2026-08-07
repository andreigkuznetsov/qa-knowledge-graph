package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewQuery;
import ru.kuznetsov.qaip.explorer.application.GetOperationOverviewService;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionResult;

import java.util.Objects;

public final class RuntimeOperationOverviewProjectionService implements GetOperationOverviewService {
    private final OperationOverviewQuery query;
    private final OperationOverviewViewMapper mapper;

    public RuntimeOperationOverviewProjectionService(
            OperationOverviewQuery query,
            OperationOverviewViewMapper mapper
    ) {
        this.query = Objects.requireNonNull(query, "query");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public OperationOverviewProjectionResult getOperationOverview(String repositoryId, String operationId) {
        return mapper.map(query.execute(repositoryId, operationId));
    }
}
