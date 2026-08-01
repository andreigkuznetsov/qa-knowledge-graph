package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.DefaultImportProjectUseCase;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceFailed;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectPersistenceRejected;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectRejected;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFinding;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFindingCode;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultImportProjectUseCaseTest {
    private final ImporterTestFixture fixture = new ImporterTestFixture();
    private final RawProjectJson source = new RawProjectJson("{}");

    @Test
    void invokes_importer_once_and_stops_without_persistence_after_import_failure() {
        ProjectImportFailure failure = new ProjectImportFailure(ProjectImportStage.PARSING, List.of(
                new ProjectImportFinding(ProjectImportStage.PARSING, "MALFORMED_JSON",
                        ProjectImportSeverity.ERROR, "Malformed JSON.", "project")));
        AtomicInteger imports = new AtomicInteger();
        AtomicInteger persists = new AtomicInteger();
        ProjectImporter importer = actual -> {
            imports.incrementAndGet();
            assertSame(source, actual);
            return failure;
        };
        PersistProject persistence = document -> {
            persists.incrementAndGet();
            return new PersistProjectAccepted("P-1");
        };

        ImportProjectRejected result = assertInstanceOf(ImportProjectRejected.class,
                new DefaultImportProjectUseCase(importer, persistence).execute(source));

        assertSame(failure, result.failure());
        assertEquals(1, imports.get());
        assertEquals(0, persists.get());
    }

    @Test
    void persists_success_document_once_and_returns_both_typed_successes() {
        ProjectImportSuccess imported = new ProjectImportSuccess(fixture.applicationValid, List.of());
        PersistProjectAccepted persisted = new PersistProjectAccepted("P-1");
        AtomicInteger imports = new AtomicInteger();
        AtomicInteger persists = new AtomicInteger();

        ImportProjectCompleted result = assertInstanceOf(ImportProjectCompleted.class,
                new DefaultImportProjectUseCase(actual -> {
                    imports.incrementAndGet();
                    return imported;
                }, document -> {
                    persists.incrementAndGet();
                    assertSame(imported.document(), document);
                    return persisted;
                }).execute(source));

        assertSame(imported, result.imported());
        assertSame(persisted, result.persisted());
        assertEquals(1, imports.get());
        assertEquals(1, persists.get());
    }

    @Test
    void returns_typed_duplicate_rejection_unchanged() {
        ProjectImportSuccess imported = new ProjectImportSuccess(fixture.applicationValid, List.of());
        PersistProjectRejected duplicate = new PersistProjectRejected(new PersistProjectFinding(
                PersistProjectFindingCode.PROJECT_ALREADY_EXISTS,
                "Project 'P-1' already exists.", "P-1"));

        ImportProjectPersistenceRejected result = assertInstanceOf(ImportProjectPersistenceRejected.class,
                new DefaultImportProjectUseCase(ignored -> imported, ignored -> duplicate).execute(source));

        assertSame(duplicate, result.rejection());
    }

    @Test
    void translates_persistence_exception_to_stable_application_failure() {
        ProjectImportSuccess imported = new ProjectImportSuccess(fixture.applicationValid, List.of());

        ImportProjectPersistenceFailed result = assertInstanceOf(ImportProjectPersistenceFailed.class,
                new DefaultImportProjectUseCase(ignored -> imported, ignored -> {
                    throw new ProjectPersistenceException("database details");
                }).execute(source));

        assertEquals("Project persistence failed.", result.message());
    }
}
