package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceFailed;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceRejected;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectRejected;
import ru.kuznetsov.qaip.core.application.importproject.ImportResultMapper;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportCompletedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportFindingResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceFailedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportRejectedResult;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFinding;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFindingCode;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportResultMapperTest {
    private final ImporterTestFixture fixture = new ImporterTestFixture();
    private final ImportResultMapper mapper = new ImportResultMapper();

    @Test
    void completed_result_maps_persisted_id_and_imported_project_counts_exactly() {
        var source = new ImportProjectCompleted(
                new ProjectImportSuccess(fixture.applicationValid, List.of()),
                new PersistProjectAccepted(" P-1 "));

        assertEquals(new ImportCompletedResult(" P-1 ", 2, 1), mapper.map(source));
    }

    @Test
    void every_import_rejection_stage_maps_its_canonical_name_and_exact_finding() {
        for (ProjectImportStage stage : ProjectImportStage.values()) {
            ProjectImportFinding finding = new ProjectImportFinding(
                    stage, " CODE ", ProjectImportSeverity.ERROR, " message ", " path ");
            ImportRejectedResult mapped = assertInstanceOf(ImportRejectedResult.class,
                    mapper.map(new ImportProjectRejected(new ProjectImportFailure(stage, List.of(finding)))));

            assertEquals(stage.name(), mapped.stage());
            assertEquals(List.of(new ImportFindingResult(
                    " CODE ", "ERROR", " message ", " path ")), mapped.findings());
        }
    }

    @Test
    void import_rejection_preserves_finding_order_and_duplicates() {
        ProjectImportFinding first = finding("FIRST", ProjectImportSeverity.ERROR, "first", "one");
        ProjectImportFinding duplicate = finding("DUPLICATE", ProjectImportSeverity.WARNING, "duplicate", "two");
        var source = new ImportProjectRejected(new ProjectImportFailure(
                ProjectImportStage.APPLICATION_VALIDATION, List.of(first, duplicate, duplicate)));

        ImportRejectedResult mapped = assertInstanceOf(ImportRejectedResult.class, mapper.map(source));

        assertEquals(List.of(
                new ImportFindingResult("FIRST", "ERROR", "first", "one"),
                new ImportFindingResult("DUPLICATE", "WARNING", "duplicate", "two"),
                new ImportFindingResult("DUPLICATE", "WARNING", "duplicate", "two")), mapped.findings());
    }

    @Test
    void persistence_rejection_maps_exact_code_and_message() {
        var source = new ImportProjectPersistenceRejected(new PersistProjectRejected(
                new PersistProjectFinding(PersistProjectFindingCode.PROJECT_ALREADY_EXISTS,
                        " Project already exists. ", "P-1")));

        assertEquals(new ImportPersistenceRejectedResult(
                "PROJECT_ALREADY_EXISTS", " Project already exists. "), mapper.map(source));
    }

    @Test
    void persistence_failure_maps_only_the_safe_application_message() {
        var source = new ImportProjectPersistenceFailed("Project persistence failed.");

        ImportPersistenceFailedResult mapped = assertInstanceOf(
                ImportPersistenceFailedResult.class, mapper.map(source));

        assertEquals("Project persistence failed.", mapped.message());
        org.junit.jupiter.api.Assertions.assertFalse(mapped.message().contains("database"));
        org.junit.jupiter.api.Assertions.assertFalse(mapped.message().contains("exception"));
    }

    @Test
    void null_is_rejected_and_repeated_mapping_is_deterministic() {
        assertThrows(NullPointerException.class, () -> mapper.map(null));
        var source = new ImportProjectPersistenceFailed("Project persistence failed.");
        assertEquals(mapper.map(source), mapper.map(source));
    }

    private static ProjectImportFinding finding(
            String code, ProjectImportSeverity severity, String message, String location) {
        return new ProjectImportFinding(
                ProjectImportStage.APPLICATION_VALIDATION, code, severity, message, location);
    }
}
