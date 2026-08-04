package ru.kuznetsov.qaip.runtime;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectRejected;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryFound;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaipRuntimeFactoryTest {

    @Test
    void creates_complete_in_memory_runtime_without_external_configuration() {
        QaipRuntime runtime = QaipRuntimeFactory.inMemory();

        assertNotNull(runtime.importProjectUseCase());
        assertNotNull(runtime.projectReader());
        assertNotNull(runtime.operationListQuery());
        assertNotNull(runtime.projectSummaryUseCase());
        assertNotNull(runtime.nodeDetailsUseCase());
        assertNotNull(runtime.relationshipsUseCase());
        assertNotNull(runtime.traceUseCase());
        assertNotNull(runtime.validationUseCase());
    }

    @Test
    void imports_and_reads_canonical_project() throws Exception {
        QaipRuntime runtime = QaipRuntimeFactory.inMemory();

        var result = runtime.importProjectUseCase().execute(new RawProjectJson(canonicalProject()));

        assertInstanceOf(ImportProjectCompleted.class, result);
        var project = runtime.projectReader().findById("P-1");
        assertTrue(project.isPresent());
        assertEquals("P-1", project.orElseThrow().metadata().id());
    }

    @Test
    void obtains_existing_project_summary_after_import() throws Exception {
        QaipRuntime runtime = QaipRuntimeFactory.inMemory();
        runtime.importProjectUseCase().execute(new RawProjectJson(canonicalProject()));

        var found = assertInstanceOf(
                ProjectSummaryFound.class, runtime.projectSummaryUseCase().execute("P-1"));

        assertEquals("P-1", found.summary().projectId());
        assertEquals(0, found.summary().nodeCount());
        assertEquals(0, found.summary().relationshipCount());
    }

    @Test
    void separately_created_runtimes_are_isolated() throws Exception {
        QaipRuntime first = QaipRuntimeFactory.inMemory();
        QaipRuntime second = QaipRuntimeFactory.inMemory();
        first.importProjectUseCase().execute(new RawProjectJson(canonicalProject()));

        assertNotSame(first, second);
        assertTrue(first.projectReader().findById("P-1").isPresent());
        assertFalse(second.projectReader().findById("P-1").isPresent());
    }

    @Test
    void invalid_canonical_input_preserves_import_rejection_behavior() {
        QaipRuntime runtime = QaipRuntimeFactory.inMemory();

        var result = runtime.importProjectUseCase().execute(new RawProjectJson("{ invalid"));

        assertInstanceOf(ImportProjectRejected.class, result);
        assertFalse(runtime.projectReader().findById("P-1").isPresent());
    }

    private static String canonicalProject() throws IOException {
        try (var stream = QaipRuntimeFactoryTest.class.getResourceAsStream(
                "/schema/valid/minimal-project.json")) {
            if (stream == null) throw new IOException("Canonical project fixture is missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
