package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.io.PrintStream;
import java.util.Objects;

public final class QaipCliApplication {
    private static final String USAGE = "Usage: qaip summary <project-id>";

    private QaipCliApplication() { }

    public static void main(String[] args) {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        ProjectSummaryUseCase useCase = new DefaultProjectSummaryUseCase(
                new InMemoryProjectReader(repository), new ProjectSummaryMapper());
        System.exit(run(args, System.out, System.err, useCase));
    }

    static int run(String[] args, PrintStream out, PrintStream err, ProjectSummaryUseCase useCase) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(useCase, "useCase");
        if (args.length != 2 || !"summary".equals(args[0])) {
            err.println(USAGE);
            return CliExitCode.INVALID_USAGE.value();
        }
        return new ProjectSummaryCliCommand(useCase, new ProjectSummaryTextRenderer())
                .execute(args[1], out, err);
    }
}
