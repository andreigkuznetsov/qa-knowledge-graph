package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DefaultAnalyzeRepositoryService implements AnalyzeRepositoryService {
    private final RepositoryAnalysisService repositoryAnalysisService;
    private final ImportProjectUseCase importProjectUseCase;

    public DefaultAnalyzeRepositoryService(
            RepositoryAnalysisService repositoryAnalysisService,
            ImportProjectUseCase importProjectUseCase
    ) {
        this.repositoryAnalysisService = Objects.requireNonNull(
                repositoryAnalysisService, "repositoryAnalysisService");
        this.importProjectUseCase = Objects.requireNonNull(importProjectUseCase, "importProjectUseCase");
    }

    @Override
    public AnalyzeRepositoryOutcome analyze(AnalyzeRepositoryCommand command) {
        Objects.requireNonNull(command, "command");
        Path repositoryRoot = repositoryRoot(command.repositoryPath());
        var analysis = repositoryAnalysisService.analyze(
                new ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisRequest(
                        repositoryRoot, command.projectName()));

        if (analysis.status() == RepositoryAnalysisStatus.FAILED) {
            throw new RepositoryAnalysisException(
                    RepositoryAnalysisErrorCode.REPOSITORY_ANALYSIS_FAILED,
                    analysis.failureMessage().orElse("Repository analysis failed"));
        }

        var imported = importProjectUseCase.execute(
                new RawProjectJson(analysis.canonicalProjectJson().toString()));
        if (!(imported instanceof ImportProjectCompleted completed)) {
            throw new RepositoryAnalysisException(
                    RepositoryAnalysisErrorCode.RUNTIME_IMPORT_FAILED,
                    "Canonical project could not be imported into Runtime");
        }

        return new AnalyzeRepositoryOutcome(
                completed.persisted().projectId(),
                analysis.projectIdentity(),
                analysis.status(),
                analysis.discoveredOperationCount(),
                analysis.warnings());
    }

    private static Path repositoryRoot(String value) {
        Path path;
        try {
            path = Path.of(value);
        } catch (InvalidPathException exception) {
            throw invalidInput("repositoryPath is invalid");
        }
        if (!Files.exists(path)) throw invalidInput("repositoryPath does not exist");
        if (!Files.isDirectory(path)) throw invalidInput("repositoryPath must be a directory");
        return path;
    }

    private static RepositoryAnalysisException invalidInput(String message) {
        return new RepositoryAnalysisException(RepositoryAnalysisErrorCode.INVALID_REPOSITORY_INPUT, message);
    }
}
