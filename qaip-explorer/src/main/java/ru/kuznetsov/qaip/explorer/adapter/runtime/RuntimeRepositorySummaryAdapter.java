package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryErrorCode;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryException;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryGateway;
import ru.kuznetsov.qaip.explorer.view.RepositorySummaryView;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.Objects;

public final class RuntimeRepositorySummaryAdapter implements RepositorySummaryGateway {
    private final QaipRuntime runtime;
    private final RepositoryAnalysisCatalog catalog;

    public RuntimeRepositorySummaryAdapter(QaipRuntime runtime, RepositoryAnalysisCatalog catalog) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public RepositorySummaryView getSummary(String repositoryId) {
        requireRepositoryId(repositoryId);
        try {
            var result = runtime.projectSummaryUseCase().execute(repositoryId);
            if (result instanceof ProjectSummaryNotFound) {
                throw new RepositorySummaryException(
                        RepositorySummaryErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
            }
            var runtimeSummary = ((ProjectSummaryFound) result).summary();
            var analysis = catalog.findByRepositoryId(repositoryId).orElseThrow(() ->
                    new RepositorySummaryException(
                            RepositorySummaryErrorCode.ANALYSIS_UNAVAILABLE,
                            "Repository analysis is unavailable"));
            var project = runtime.projectReader().findById(runtimeSummary.projectId()).orElseThrow(() ->
                    new IllegalStateException("Runtime project disappeared after summary query"));

            return new RepositorySummaryView(
                    analysis.repositoryId(),
                    analysis.projectIdentity(),
                    analysis.analysisStatus(),
                    analysis.operationCount(),
                    count(project, "BUSINESS_RULE"),
                    count(project, "TECHNICAL_IMPLEMENTATION"),
                    count(project, "TEST_IMPLEMENTATION"),
                    count(project, "CHECK"),
                    analysis.warnings().size());
        } catch (RepositorySummaryException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RepositorySummaryException(
                    RepositorySummaryErrorCode.RUNTIME_FAILURE, "Repository summary is unavailable", exception);
        }
    }

    private static int count(ru.kuznetsov.qaip.core.domain.Project project, String type) {
        return Math.toIntExact(project.nodes().stream().filter(node -> type.equals(node.type())).count());
    }

    private static void requireRepositoryId(String repositoryId) {
        if (repositoryId == null || repositoryId.isBlank()) {
            throw new RepositorySummaryException(
                    RepositorySummaryErrorCode.REPOSITORY_NOT_FOUND, "Repository was not found");
        }
    }
}
