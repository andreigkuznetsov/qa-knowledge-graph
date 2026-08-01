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
import ru.kuznetsov.qaip.core.application.query.validation.DefaultValidationUseCase;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationReportMapper;
import ru.kuznetsov.qaip.core.application.query.validation.ValidationUseCase;
import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.application.validation.rule.IsolatedNodeValidationRule;
import ru.kuznetsov.qaip.core.application.validation.rule.ScenarioWithoutTestValidationRule;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

public final class QaipCliApplication {
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage:",
            "  qaip summary <project-id>",
            "  qaip show node <project-id> <node-id>",
            "  qaip show relationships <project-id> <node-id>",
            "  qaip trace <project-id> <start-node-id>",
            "  qaip validate project <project-id>");

    private QaipCliApplication() { }

    public static void main(String[] args) {
        System.exit(runWithRuntime(args, System.out, System.err, RuntimeComposition::create));
    }

    static int runWithRuntime(String[] args, PrintStream out, PrintStream err,
                              Supplier<RuntimeComposition> compositionFactory) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(compositionFactory, "compositionFactory");
        if (!isValidCommand(args)) {
            err.println(USAGE);
            return CliExitCode.INVALID_USAGE.value();
        }

        RuntimeComposition composition;
        try {
            composition = Objects.requireNonNull(compositionFactory.get(), "runtime composition");
        } catch (IllegalStateException exception) {
            err.println("Database configuration is missing or invalid.");
            return CliExitCode.APPLICATION_FAILURE.value();
        }

        ProjectReader reader = composition.reader();
        ProjectSummaryUseCase useCase = new DefaultProjectSummaryUseCase(
                reader, new ProjectSummaryMapper());
        NodeDetailsUseCase nodeDetailsUseCase = new DefaultNodeDetailsUseCase(
                reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        RelationshipsUseCase relationshipsUseCase = new DefaultRelationshipsUseCase(
                reader, new ProjectNodeLookup(), new ProjectRelationshipLookup(), new RelationshipDetailsMapper());
        TraceUseCase traceUseCase = new DefaultTraceUseCase(
                reader, new ProjectNodeLookup(), new TraceGraphBuilder(), new TraceMapper());
        ValidationEngine validationEngine = new ValidationEngine(java.util.List.of(
                new IsolatedNodeValidationRule(), new ScenarioWithoutTestValidationRule()));
        ValidationUseCase validationUseCase = new DefaultValidationUseCase(
                reader, validationEngine, new ValidationReportMapper());
        return run(args, out, err, useCase, nodeDetailsUseCase, relationshipsUseCase,
                traceUseCase, validationUseCase);
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
        return run(args, out, err, useCase, nodeDetailsUseCase, relationshipsUseCase, traceUseCase,
                projectId -> {
                    throw new IllegalStateException("Validation use case is not configured");
                });
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase,
                   NodeDetailsUseCase nodeDetailsUseCase, RelationshipsUseCase relationshipsUseCase,
                   TraceUseCase traceUseCase, ValidationUseCase validationUseCase) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(nodeDetailsUseCase, "nodeDetailsUseCase");
        Objects.requireNonNull(relationshipsUseCase, "relationshipsUseCase");
        Objects.requireNonNull(traceUseCase, "traceUseCase");
        Objects.requireNonNull(validationUseCase, "validationUseCase");
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
        if (args.length == 3 && "validate".equals(args[0]) && "project".equals(args[1])) {
            return new ValidationCliCommand(validationUseCase, new ValidationTextRenderer())
                    .execute(args[2], out, err);
        }
        err.println(USAGE);
        return CliExitCode.INVALID_USAGE.value();
    }

    private static boolean isValidCommand(String[] args) {
        return args.length == 2 && "summary".equals(args[0])
                || args.length == 4 && "show".equals(args[0]) && "node".equals(args[1])
                || args.length == 4 && "show".equals(args[0]) && "relationships".equals(args[1])
                || args.length == 3 && "trace".equals(args[0])
                || args.length == 3 && "validate".equals(args[0]) && "project".equals(args[1]);
    }
}
