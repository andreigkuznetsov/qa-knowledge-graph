package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.importproject.result.ImportCompletedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportFindingResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceFailedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;

import java.util.Objects;

public final class ImportResultMapper {
    public ImportResult map(ImportProjectUseCaseResult result) {
        Objects.requireNonNull(result, "result");
        if (result instanceof ImportProjectCompleted completed) {
            var project = completed.imported().document().project();
            return new ImportCompletedResult(
                    completed.persisted().projectId(),
                    project.nodes().size(),
                    project.relationships().size());
        }
        if (result instanceof ImportProjectRejected rejected) {
            var failure = rejected.failure();
            return new ImportRejectedResult(
                    failure.failedStage().name(),
                    failure.findings().stream()
                            .map(finding -> new ImportFindingResult(
                                    finding.code(),
                                    finding.severity().name(),
                                    finding.message(),
                                    finding.location()))
                            .toList());
        }
        if (result instanceof ImportProjectPersistenceRejected rejected) {
            var finding = rejected.rejection().finding();
            return new ImportPersistenceRejectedResult(
                    finding.code().name(), finding.message());
        }
        ImportProjectPersistenceFailed failed = (ImportProjectPersistenceFailed) result;
        return new ImportPersistenceFailedResult(failed.message());
    }
}
