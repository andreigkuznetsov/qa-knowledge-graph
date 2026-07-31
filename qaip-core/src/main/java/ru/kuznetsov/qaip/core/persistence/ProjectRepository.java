package ru.kuznetsov.qaip.core.persistence;

import ru.kuznetsov.qaip.core.domain.Project;

public interface ProjectRepository {
    /**
     * Atomically stores {@code project} if its canonical identifier is absent.
     * The canonical identifier is exactly {@code project.metadata().id()}: comparison is exact and
     * case-sensitive, with no trimming, normalization, generated identifier, or version participation.
     * The project must contain non-null metadata with a non-null, non-blank identifier; otherwise a
     * {@link ProjectPersistenceException} is thrown.
     *
     * <p>Duplicate detection and insertion are one indivisible repository operation, not a public
     * check-then-act sequence. If the identifier is absent, this invocation stores the submitted project
     * and returns {@link ProjectInserted} carrying that canonical identifier. If it is present, this
     * invocation does not store the submitted project, does not modify or replace the existing project,
     * and returns {@link ProjectAlreadyExists} carrying the submitted project's canonical identifier.
     * Both structurally identical and structurally different projects with the same identifier are
     * duplicates.</p>
     *
     * <p>Infrastructure failures are reported as {@link ProjectPersistenceException}, never as duplicate
     * results.</p>
     *
     * @param project immutable project to insert
     * @return a non-null fact describing the outcome of this invocation
     * @throws NullPointerException if {@code project} is null
     * @throws ProjectPersistenceException if the canonical identifier is unusable or persistence fails
     */
    ProjectInsertResult insertIfAbsent(Project project);
}
