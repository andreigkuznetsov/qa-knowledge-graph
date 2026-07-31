package ru.kuznetsov.qaip.core.application.persistence;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultPersistProjectTest {
    private final PersistenceProofFixture fixture = new PersistenceProofFixture();

    @Test
    void accepted_insert_delegates_once_with_exact_project_instance() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Project> received = new AtomicReference<>();
        ProjectRepository repository = project -> {
            calls.incrementAndGet();
            received.set(project);
            return new ProjectInserted(project.metadata().id());
        };
        PersistProjectAccepted accepted = assertInstanceOf(PersistProjectAccepted.class,
                new DefaultPersistProject(repository).execute(fixture.document));
        assertEquals("P-1", accepted.projectId());
        assertEquals(1, calls.get());
        assertSame(fixture.document.project(), received.get());
    }

    @Test
    void duplicate_is_mapped_to_deterministic_expected_rejection() {
        ProjectRepository repository = project -> new ProjectAlreadyExists(project.metadata().id());
        PersistProjectRejected rejected = assertInstanceOf(PersistProjectRejected.class,
                new DefaultPersistProject(repository).execute(fixture.document));
        assertEquals(PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, rejected.finding().code());
        assertEquals("P-1", rejected.finding().projectId());
        assertEquals("Project 'P-1' already exists.", rejected.finding().message());
    }

    @Test
    void persistence_exception_propagates_unchanged() {
        ProjectPersistenceException failure = new ProjectPersistenceException("storage unavailable");
        ProjectRepository repository = project -> { throw failure; };
        assertSame(failure, assertThrows(ProjectPersistenceException.class,
                () -> new DefaultPersistProject(repository).execute(fixture.document)));
    }

    @Test
    void null_dependency_and_document_are_rejected() {
        assertThrows(NullPointerException.class, () -> new DefaultPersistProject(null));
        assertThrows(NullPointerException.class,
                () -> new DefaultPersistProject(project -> new ProjectInserted("P-1")).execute(null));
    }
}
