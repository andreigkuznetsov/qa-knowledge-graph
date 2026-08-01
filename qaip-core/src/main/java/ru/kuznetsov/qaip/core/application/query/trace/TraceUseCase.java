package ru.kuznetsov.qaip.core.application.query.trace;

public interface TraceUseCase {
    TraceQueryResult execute(String projectId, String startNodeId);
}
