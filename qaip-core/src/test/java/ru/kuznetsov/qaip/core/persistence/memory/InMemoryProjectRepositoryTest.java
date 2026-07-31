package ru.kuznetsov.qaip.core.persistence.memory;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.ProjectInserted;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProjectRepositoryTest {
    @Test
    void first_insert_succeeds_and_duplicate_never_replaces_stored_project() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Project first = project("P-1", "First");
        Project differentDuplicate = project("P-1", "Second");

        ProjectInserted inserted = assertInstanceOf(ProjectInserted.class, repository.insertIfAbsent(first));
        ProjectAlreadyExists identical = assertInstanceOf(
                ProjectAlreadyExists.class, repository.insertIfAbsent(first));
        ProjectAlreadyExists different = assertInstanceOf(
                ProjectAlreadyExists.class, repository.insertIfAbsent(differentDuplicate));

        assertEquals("P-1", inserted.projectId());
        assertEquals("P-1", identical.projectId());
        assertEquals("P-1", different.projectId());
        assertSame(first, repository.storedProject("P-1"));
        assertEquals("First", first.metadata().name());
    }

    @Test
    void different_identifiers_are_inserted_independently() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        Project first = project("P-1", "First");
        Project second = project("P-2", "Second");
        assertInstanceOf(ProjectInserted.class, repository.insertIfAbsent(first));
        assertInstanceOf(ProjectInserted.class, repository.insertIfAbsent(second));
        assertSame(first, repository.storedProject("P-1"));
        assertSame(second, repository.storedProject("P-2"));
    }

    @Test
    void null_or_unusable_identity_is_a_contract_failure() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        assertThrows(NullPointerException.class, () -> repository.insertIfAbsent(null));
        Project withoutMetadata = new Project("qaip-project-v1", "0.1", null, List.of(),
                new Subject("local"), List.of(), List.of(), evidence(), changes(), Map.of());
        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(withoutMetadata));
        assertThrows(ProjectPersistenceException.class,
                () -> repository.insertIfAbsent(project(" ", "Invalid")));
    }

    @Test
    void concurrent_same_identifier_has_exactly_one_winner_and_stores_winning_instance() throws Exception {
        int attempts = 16;
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<Attempt>> futures = new ArrayList<>();
            for (int index = 0; index < attempts; index++) {
                Project candidate = project("P-RACE", "Candidate-" + index);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return new Attempt(candidate, repository.insertIfAbsent(candidate));
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "workers did not become ready");
            start.countDown();
            List<Attempt> results = new ArrayList<>();
            for (Future<Attempt> future : futures) results.add(future.get(5, TimeUnit.SECONDS));
            List<Attempt> winners = results.stream()
                    .filter(attempt -> attempt.result() instanceof ProjectInserted).toList();
            assertEquals(1, winners.size());
            assertEquals(attempts - 1, results.stream()
                    .filter(attempt -> attempt.result() instanceof ProjectAlreadyExists).count());
            assertEquals("P-RACE", ((ProjectInserted) winners.getFirst().result()).projectId());
            assertTrue(results.stream().allMatch(attempt ->
                    resultProjectId(attempt).equals(attempt.project().metadata().id())));
            assertSame(winners.getFirst().project(), repository.storedProject("P-RACE"));
        }
    }

    @Test
    void concurrent_distinct_identifiers_do_not_interfere() throws Exception {
        int attempts = 12;
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < attempts; index++) {
                Project candidate = project("P-" + index, "Project-" + index);
                futures.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return repository.insertIfAbsent(candidate);
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                assertInstanceOf(ProjectInserted.class, future.get(5, TimeUnit.SECONDS));
            }
            for (int index = 0; index < attempts; index++) {
                assertEquals("Project-" + index, repository.storedProject("P-" + index).metadata().name());
            }
        }
    }

    private record Attempt(Project project, ru.kuznetsov.qaip.core.persistence.ProjectInsertResult result) { }

    private static String resultProjectId(Attempt attempt) {
        if (attempt.result() instanceof ProjectInserted inserted) return inserted.projectId();
        return ((ProjectAlreadyExists) attempt.result()).projectId();
    }

    static Project project(String id, String name) {
        return new Project("qaip-project-v1", "0.1", new Metadata(id, name, null, null, Map.of()),
                List.of(), new Subject("local"), List.of(), List.of(), evidence(), changes(), Map.of());
    }

    private static EvidenceManifest evidence() {
        return new EvidenceManifest("impact-evidence-manifest-v1", "source", Map.of(),
                "impact-evidence-normalization-v1", "impact-evidence-canonical-v1", "fingerprint",
                List.of(), List.of(), List.of());
    }

    private static List<DeclaredChange> changes() {
        return List.of(new DeclaredChange("NODE", "N-1", "ADDED", "0.1", null, Map.of()));
    }
}
