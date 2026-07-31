package ru.kuznetsov.qaip.core.persistence;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reusable behavioral contract for every {@link ProjectRepository} adapter. */
public abstract class ProjectRepositoryContractTest {
    protected abstract ProjectRepository repository();

    protected abstract Project storedProject(ProjectRepository repository, String projectId);

    @Test
    void first_insert_returns_input_identity_and_stores_submitted_project() {
        ProjectRepository repository = repository();
        Project submitted = project("P-1", "First");

        ProjectInserted result = assertInstanceOf(ProjectInserted.class,
                repository.insertIfAbsent(submitted));

        assertEquals(submitted.metadata().id(), result.projectId());
        assertEquals(submitted, storedProject(repository, "P-1"));
    }

    @Test
    void identical_duplicate_is_a_conflict_and_leaves_original_unchanged() {
        ProjectRepository repository = repository();
        Project original = project("P-1", "First");
        Project identicalDuplicate = project("P-1", "First");
        repository.insertIfAbsent(original);

        ProjectAlreadyExists result = assertInstanceOf(ProjectAlreadyExists.class,
                repository.insertIfAbsent(identicalDuplicate));

        assertEquals(identicalDuplicate.metadata().id(), result.projectId());
        assertEquals(original, storedProject(repository, "P-1"));
    }

    @Test
    void different_duplicate_is_a_conflict_and_never_replaces_original() {
        ProjectRepository repository = repository();
        Project original = project("P-1", "First");
        Project differentDuplicate = project("P-1", "Second");
        repository.insertIfAbsent(original);

        ProjectAlreadyExists result = assertInstanceOf(ProjectAlreadyExists.class,
                repository.insertIfAbsent(differentDuplicate));

        assertEquals(differentDuplicate.metadata().id(), result.projectId());
        assertEquals(original, storedProject(repository, "P-1"));
    }

    @Test
    void identity_comparison_is_exact_case_sensitive_and_not_trimmed() {
        ProjectRepository repository = repository();
        for (String id : List.of("Project", "project", " Project", "Project ")) {
            Project submitted = project(id, id);
            ProjectInserted result = assertInstanceOf(ProjectInserted.class,
                    repository.insertIfAbsent(submitted));
            assertEquals(id, result.projectId());
            assertEquals(submitted, storedProject(repository, id));
        }
    }

    @Test
    void invalid_canonical_identity_is_a_persistence_contract_failure() {
        ProjectRepository repository = repository();
        Project nullMetadata = project(null, "Invalid", true);
        Project nullId = project(null, "Invalid");

        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(nullMetadata));
        assertThrows(ProjectPersistenceException.class, () -> repository.insertIfAbsent(nullId));
        for (String blank : List.of("", " ", "\t")) {
            assertThrows(ProjectPersistenceException.class,
                    () -> repository.insertIfAbsent(project(blank, "Invalid")));
        }
    }

    protected static Project project(String id, String name) {
        return project(id, name, false);
    }

    private static Project project(String id, String name, boolean omitMetadata) {
        Metadata metadata = omitMetadata ? null : new Metadata(id, name, null, null, Map.of());
        return new Project("qaip-project-v1", "0.1", metadata, List.of(), new Subject("local"),
                List.of(), List.of(), evidence(), changes(), Map.of());
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
