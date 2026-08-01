package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.DefaultNodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.DefaultRelationshipsUseCase;
import ru.kuznetsov.qaip.core.application.query.relationship.ProjectRelationshipLookup;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.relationship.RelationshipsUseCase;
import ru.kuznetsov.qaip.core.application.query.trace.DefaultTraceUseCase;
import ru.kuznetsov.qaip.core.application.query.trace.TraceGraphBuilder;
import ru.kuznetsov.qaip.core.application.query.trace.TraceMapper;
import ru.kuznetsov.qaip.core.application.query.trace.TraceUseCase;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.io.PrintStream;
import java.util.Objects;

public final class QaipCliApplication {
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage:",
            "  qaip summary <project-id>",
            "  qaip show node <project-id> <node-id>",
            "  qaip show relationships <project-id> <node-id>",
            "  qaip trace <project-id> <start-node-id>");

    private QaipCliApplication() { }

    public static void main(String[] args) {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        ProjectSummaryUseCase useCase = new DefaultProjectSummaryUseCase(
                reader, new ProjectSummaryMapper());
        NodeDetailsUseCase nodeDetailsUseCase = new DefaultNodeDetailsUseCase(
                reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        RelationshipsUseCase relationshipsUseCase = new DefaultRelationshipsUseCase(
                reader, new ProjectNodeLookup(), new ProjectRelationshipLookup(), new RelationshipDetailsMapper());
        TraceUseCase traceUseCase = new DefaultTraceUseCase(
                reader, new ProjectNodeLookup(), new TraceGraphBuilder(), new TraceMapper());
        System.exit(run(args, System.out, System.err, useCase, nodeDetailsUseCase, relationshipsUseCase,
                traceUseCase));
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase) {
        return run(args, out, err, useCase, (projectId, nodeId) -> {
            throw new IllegalStateException("Node Details use case is not configured");
        });
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase,
                   NodeDetailsUseCase nodeDetailsUseCase) {
        return run(args, out, err, useCase, nodeDetailsUseCase, (projectId, nodeId) -> {
            throw new IllegalStateException("Relationships use case is not configured");
        });
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase,
                   NodeDetailsUseCase nodeDetailsUseCase, RelationshipsUseCase relationshipsUseCase) {
        return run(args, out, err, useCase, nodeDetailsUseCase, relationshipsUseCase,
                (projectId, startNodeId) -> {
                    throw new IllegalStateException("Trace use case is not configured");
                });
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase,
                   NodeDetailsUseCase nodeDetailsUseCase, RelationshipsUseCase relationshipsUseCase,
                   TraceUseCase traceUseCase) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(nodeDetailsUseCase, "nodeDetailsUseCase");
        Objects.requireNonNull(relationshipsUseCase, "relationshipsUseCase");
        Objects.requireNonNull(traceUseCase, "traceUseCase");
        if (args.length == 2 && "summary".equals(args[0])) {
            return new ProjectSummaryCliCommand(useCase, new ProjectSummaryTextRenderer())
                    .execute(args[1], out, err);
        }
        if (args.length == 4 && "show".equals(args[0]) && "node".equals(args[1])) {
            return new NodeDetailsCliCommand(nodeDetailsUseCase, new NodeDetailsTextRenderer())
                    .execute(args[2], args[3], out, err);
        }
        if (args.length == 4 && "show".equals(args[0]) && "relationships".equals(args[1])) {
            return new RelationshipsCliCommand(relationshipsUseCase, new RelationshipsTextRenderer())
                    .execute(args[2], args[3], out, err);
        }
        if (args.length == 3 && "trace".equals(args[0])) {
            return new TraceCliCommand(traceUseCase, new TraceTextRenderer())
                    .execute(args[1], args[2], out, err);
        }
        err.println(USAGE);
        return CliExitCode.INVALID_USAGE.value();
    }
}
