package ru.kuznetsov.qaip.explorer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisResult;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;
import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAnalyzeRepositoryServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path repository;

    @Test
    void complete_analysis_is_imported_once() {
        RepositoryAnalysisService analysis = mock(RepositoryAnalysisService.class);
        ImportProjectUseCase importer = mock(ImportProjectUseCase.class);
        when(analysis.analyze(any())).thenReturn(success(RepositoryAnalysisStatus.COMPLETE, List.of()));
        when(importer.execute(any())).thenReturn(importCompleted());

        var outcome = new DefaultAnalyzeRepositoryService(analysis, importer)
                .analyze(command());

        assertEquals("PROJECT-1", outcome.repositoryId());
        assertEquals(RepositoryAnalysisStatus.COMPLETE, outcome.analysisStatus());
        assertEquals(2, outcome.discoveredOperationCount());
        verify(importer).execute(any());
    }

    @Test
    void partial_analysis_is_imported_with_immutable_warnings() {
        RepositoryAnalysisService analysis = mock(RepositoryAnalysisService.class);
        ImportProjectUseCase importer = mock(ImportProjectUseCase.class);
        when(analysis.analyze(any())).thenReturn(success(
                RepositoryAnalysisStatus.PARTIAL, List.of("Unsupported construct omitted")));
        when(importer.execute(any())).thenReturn(importCompleted());

        var outcome = new DefaultAnalyzeRepositoryService(analysis, importer).analyze(command());

        assertEquals(RepositoryAnalysisStatus.PARTIAL, outcome.analysisStatus());
        assertEquals(List.of("Unsupported construct omitted"), outcome.warnings());
        assertThrows(UnsupportedOperationException.class, () -> outcome.warnings().add("mutation"));
        verify(importer).execute(any());
    }

    @Test
    void failed_analysis_is_not_imported() {
        RepositoryAnalysisService analysis = mock(RepositoryAnalysisService.class);
        ImportProjectUseCase importer = mock(ImportProjectUseCase.class);
        when(analysis.analyze(any())).thenReturn(new RepositoryAnalysisResult(
                RepositoryAnalysisStatus.FAILED, null, null, 0, List.of(), Optional.of("No operations")));

        var exception = assertThrows(RepositoryAnalysisException.class,
                () -> new DefaultAnalyzeRepositoryService(analysis, importer).analyze(command()));

        assertEquals(RepositoryAnalysisErrorCode.REPOSITORY_ANALYSIS_FAILED, exception.code());
        verify(importer, never()).execute(any());
    }

    @Test
    void invalid_repository_path_is_rejected_before_analysis() {
        RepositoryAnalysisService analysis = mock(RepositoryAnalysisService.class);
        ImportProjectUseCase importer = mock(ImportProjectUseCase.class);
        var command = new AnalyzeRepositoryCommand(
                repository.resolve("missing").toString(), "Example project");

        var exception = assertThrows(RepositoryAnalysisException.class,
                () -> new DefaultAnalyzeRepositoryService(analysis, importer).analyze(command));

        assertEquals(RepositoryAnalysisErrorCode.INVALID_REPOSITORY_INPUT, exception.code());
        verify(analysis, never()).analyze(any());
        verify(importer, never()).execute(any());
    }

    private AnalyzeRepositoryCommand command() {
        return new AnalyzeRepositoryCommand(repository.toString(), "Example project");
    }

    private static RepositoryAnalysisResult success(
            RepositoryAnalysisStatus status, List<String> warnings) {
        return new RepositoryAnalysisResult(
                status,
                "PROJECT-1",
                JSON.createObjectNode().put("projectContractVersion", "qaip-project-v1"),
                2,
                warnings,
                Optional.empty());
    }

    private static ImportProjectCompleted importCompleted() {
        return new ImportProjectCompleted(
                mock(ProjectImportSuccess.class), new PersistProjectAccepted("PROJECT-1"));
    }
}
