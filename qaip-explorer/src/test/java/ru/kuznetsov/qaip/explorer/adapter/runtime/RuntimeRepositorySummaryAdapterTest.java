package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryNotFound;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryResult;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.explorer.application.InMemoryRepositoryAnalysisCatalog;
import ru.kuznetsov.qaip.explorer.application.RepositoryAnalysisRecord;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryErrorCode;
import ru.kuznetsov.qaip.explorer.application.RepositorySummaryException;
import ru.kuznetsov.qaip.runtime.QaipRuntime;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeRepositorySummaryAdapterTest {

    @Test
    void maps_runtime_summary_and_imported_project_to_user_view() {
        QaipRuntime runtime = mock(QaipRuntime.class);
        var summaryQuery = mock(ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase.class);
        var reader = mock(ru.kuznetsov.qaip.core.persistence.read.ProjectReader.class);
        Project project = mock(Project.class);
        when(runtime.projectSummaryUseCase()).thenReturn(summaryQuery);
        when(runtime.projectReader()).thenReturn(reader);
        when(summaryQuery.execute("PROJECT-1")).thenReturn(new ProjectSummaryFound(runtimeSummary()));
        when(reader.findById("PROJECT-1")).thenReturn(Optional.of(project));
        when(project.nodes()).thenReturn(List.of(
                node("BUSINESS_OPERATION"), node("BUSINESS_RULE"),
                node("TECHNICAL_IMPLEMENTATION"), node("TECHNICAL_IMPLEMENTATION"),
                node("TEST_IMPLEMENTATION"), node("CHECK")));
        var catalog = new InMemoryRepositoryAnalysisCatalog();
        catalog.save(new RepositoryAnalysisRecord(
                "PROJECT-1", "PROJECT-1", RepositoryAnalysisStatus.PARTIAL, 1, List.of("warning")));

        var view = new RuntimeRepositorySummaryAdapter(runtime, catalog).getSummary("PROJECT-1");

        assertEquals(1, view.operationCount());
        assertEquals(1, view.businessRuleCount());
        assertEquals(2, view.implementationNodeCount());
        assertEquals(1, view.testCount());
        assertEquals(1, view.checkCount());
        assertEquals(1, view.warningCount());
        verify(summaryQuery).execute("PROJECT-1");
    }

    @Test
    void reports_repository_not_found_from_runtime_summary_query() {
        QaipRuntime runtime = mock(QaipRuntime.class);
        var summaryQuery = mock(ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase.class);
        when(runtime.projectSummaryUseCase()).thenReturn(summaryQuery);
        when(summaryQuery.execute("missing")).thenReturn(new ProjectSummaryNotFound("missing"));

        var exception = assertThrows(RepositorySummaryException.class,
                () -> new RuntimeRepositorySummaryAdapter(
                        runtime, new InMemoryRepositoryAnalysisCatalog()).getSummary("missing"));

        assertEquals(RepositorySummaryErrorCode.REPOSITORY_NOT_FOUND, exception.code());
        verify(summaryQuery).execute("missing");
    }

    @Test
    void reports_unavailable_analysis_when_runtime_project_has_no_explorer_record() {
        QaipRuntime runtime = mock(QaipRuntime.class);
        var summaryQuery = mock(ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase.class);
        when(runtime.projectSummaryUseCase()).thenReturn(summaryQuery);
        when(summaryQuery.execute("PROJECT-1")).thenReturn(new ProjectSummaryFound(runtimeSummary()));

        var exception = assertThrows(RepositorySummaryException.class,
                () -> new RuntimeRepositorySummaryAdapter(
                        runtime, new InMemoryRepositoryAnalysisCatalog()).getSummary("PROJECT-1"));

        assertEquals(RepositorySummaryErrorCode.ANALYSIS_UNAVAILABLE, exception.code());
    }

    @Test
    void hides_runtime_failures_behind_typed_summary_error() {
        QaipRuntime runtime = mock(QaipRuntime.class);
        var summaryQuery = mock(ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryUseCase.class);
        when(runtime.projectSummaryUseCase()).thenReturn(summaryQuery);
        when(summaryQuery.execute("PROJECT-1")).thenThrow(new IllegalStateException("internal detail"));

        var exception = assertThrows(RepositorySummaryException.class,
                () -> new RuntimeRepositorySummaryAdapter(
                        runtime, new InMemoryRepositoryAnalysisCatalog()).getSummary("PROJECT-1"));

        assertEquals(RepositorySummaryErrorCode.RUNTIME_FAILURE, exception.code());
        assertEquals("Repository summary is unavailable", exception.getMessage());
    }

    private static ProjectSummaryResult runtimeSummary() {
        return new ProjectSummaryResult("PROJECT-1", "qaip-project-v1", "0.1", 1, 6, 5, 0, 1);
    }

    private static Node node(String type) {
        return new Node(type, type, type, null, null, List.of(), List.of(), null, null);
    }
}
