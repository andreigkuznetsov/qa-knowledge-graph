package ru.kuznetsov.qaip.core.application.query.trace;

import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.Objects;

public final class DefaultTraceUseCase implements TraceUseCase {
    private final ProjectReader projectReader;
    private final ProjectNodeLookup projectNodeLookup;
    private final TraceGraphBuilder traceGraphBuilder;
    private final TraceMapper traceMapper;

    public DefaultTraceUseCase(ProjectReader projectReader, ProjectNodeLookup projectNodeLookup,
                               TraceGraphBuilder traceGraphBuilder, TraceMapper traceMapper) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.projectNodeLookup = Objects.requireNonNull(projectNodeLookup, "projectNodeLookup");
        this.traceGraphBuilder = Objects.requireNonNull(traceGraphBuilder, "traceGraphBuilder");
        this.traceMapper = Objects.requireNonNull(traceMapper, "traceMapper");
    }

    @Override
    public TraceQueryResult execute(String projectId, String startNodeId) {
        requireId(projectId, "projectId");
        requireId(startNodeId, "startNodeId");

        var projectResult = Objects.requireNonNull(projectReader.findById(projectId), "project reader result");
        if (projectResult.isEmpty()) return new TraceProjectNotFound(projectId);

        var project = projectResult.orElseThrow();
        var nodeResult = Objects.requireNonNull(
                projectNodeLookup.findById(project, startNodeId), "node lookup result");
        if (nodeResult.isEmpty()) return new TraceNodeNotFound(projectId, startNodeId);

        var traceGraph = Objects.requireNonNull(
                traceGraphBuilder.build(project, startNodeId), "trace graph builder result");
        var trace = Objects.requireNonNull(traceMapper.map(traceGraph), "trace mapper result");
        return new TraceFound(projectId, startNodeId, trace);
    }

    private static void requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
