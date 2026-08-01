package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DefaultProjectSummaryUseCaseTest {
    @Test
    void existing_project_is_read_once_and_mapped_to_success() {
        Project project = project("P-1");
        ReaderSpy reader = new ReaderSpy(Optional.of(project));
        var useCase = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());

        ProjectSummaryFound found = assertInstanceOf(ProjectSummaryFound.class, useCase.execute("P-1"));
        assertEquals("P-1", reader.id.get());
        assertEquals(1, reader.calls.get());
        assertEquals(new ProjectSummaryResult("P-1", "contract", "schema", 0, 0, 0, 0, 0), found.summary());
        assertEquals(found, useCase.execute("P-1"));
        assertEquals(2, reader.calls.get());
    }

    @Test
    void absence_preserves_exact_id_and_is_distinct_from_empty_project() {
        ReaderSpy absent = new ReaderSpy(Optional.empty());
        var missingUseCase = new DefaultProjectSummaryUseCase(absent, new ProjectSummaryMapper());
        assertEquals(new ProjectSummaryNotFound(" P-1 "), missingUseCase.execute(" P-1 "));
        assertEquals(1, absent.calls.get());
        assertEquals(" P-1 ", absent.id.get());

        var emptyUseCase = new DefaultProjectSummaryUseCase(new ReaderSpy(Optional.of(project("P-1"))),
                new ProjectSummaryMapper());
        assertInstanceOf(ProjectSummaryFound.class, emptyUseCase.execute("P-1"));
    }

    @Test
    void dependencies_and_input_are_validated_before_interaction() {
        ReaderSpy reader = new ReaderSpy(Optional.empty());
        assertThrows(NullPointerException.class,
                () -> new DefaultProjectSummaryUseCase(null, new ProjectSummaryMapper()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectSummaryUseCase(reader, null));
        var useCase = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(" \t"));
        assertEquals(0, reader.calls.get());
    }

    @Test
    void reader_failures_and_dependency_contract_defects_propagate() {
        ProjectPersistenceException persistence = new ProjectPersistenceException("offline");
        ProjectReader failed = id -> { throw persistence; };
        var failedUseCase = new DefaultProjectSummaryUseCase(failed, new ProjectSummaryMapper());
        assertSame(persistence, assertThrows(ProjectPersistenceException.class, () -> failedUseCase.execute("P")));

        IllegalStateException defect = new IllegalStateException("defect");
        ProjectReader defective = id -> { throw defect; };
        assertSame(defect, assertThrows(IllegalStateException.class,
                () -> new DefaultProjectSummaryUseCase(defective, new ProjectSummaryMapper()).execute("P")));

        ProjectReader nullResult = id -> null;
        assertThrows(NullPointerException.class,
                () -> new DefaultProjectSummaryUseCase(nullResult, new ProjectSummaryMapper()).execute("P"));

        Project invalidForMapper = new Project("contract", "schema", new Metadata("P", "name", null, null, Map.of()),
                List.of(), new Subject("local"), List.of(), List.of(), null, List.of(), Map.of());
        assertThrows(NullPointerException.class, () -> new DefaultProjectSummaryUseCase(
                new ReaderSpy(Optional.of(invalidForMapper)), new ProjectSummaryMapper()).execute("P"));
    }

    static Project project(String id) {
        return new Project("contract", "schema", new Metadata(id, "name", null, null, Map.of()), List.of(),
                new Subject("local"), List.of(), List.of(), new EvidenceManifest("evidence", "source", Map.of(),
                "normalization", "canonicalization", "fingerprint", List.of(), List.of(), List.of()),
                List.of(), Map.of());
    }

    private static final class ReaderSpy implements ProjectReader {
        final Optional<Project> result;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<String> id = new AtomicReference<>();
        ReaderSpy(Optional<Project> result) { this.result = result; }
        public Optional<Project> findById(String projectId) {
            calls.incrementAndGet();
            id.set(projectId);
            return result;
        }
    }
}
