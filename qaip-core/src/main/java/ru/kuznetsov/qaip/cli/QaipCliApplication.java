package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.DefaultNodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.io.PrintStream;
import java.util.Objects;

public final class QaipCliApplication {
    private static final String USAGE = String.join(System.lineSeparator(),
            "Usage:",
            "  qaip summary <project-id>",
            "  qaip show node <project-id> <node-id>");

    private QaipCliApplication() { }

    public static void main(String[] args) {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        ProjectSummaryUseCase useCase = new DefaultProjectSummaryUseCase(
                reader, new ProjectSummaryMapper());
        NodeDetailsUseCase nodeDetailsUseCase = new DefaultNodeDetailsUseCase(
                reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        System.exit(run(args, System.out, System.err, useCase, nodeDetailsUseCase));
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase) {
        return run(args, out, err, useCase, (projectId, nodeId) -> {
            throw new IllegalStateException("Node Details use case is not configured");
        });
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase,
                   NodeDetailsUseCase nodeDetailsUseCase) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(nodeDetailsUseCase, "nodeDetailsUseCase");
        if (args.length == 2 && "summary".equals(args[0])) {
            return new ProjectSummaryCliCommand(useCase, new ProjectSummaryTextRenderer())
                    .execute(args[1], out, err);
        }
        if (args.length == 4 && "show".equals(args[0]) && "node".equals(args[1])) {
            return new NodeDetailsCliCommand(nodeDetailsUseCase, new NodeDetailsTextRenderer())
                    .execute(args[2], args[3], out, err);
        }
        err.println(USAGE);
        return CliExitCode.INVALID_USAGE.value();
    }
}
