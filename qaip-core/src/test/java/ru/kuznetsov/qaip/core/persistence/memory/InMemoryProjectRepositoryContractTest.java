package ru.kuznetsov.qaip.core.persistence.memory;

import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.ProjectRepositoryContractTest;

final class InMemoryProjectRepositoryContractTest extends ProjectRepositoryContractTest {
    @Override
    protected ProjectRepository repository() {
        return new InMemoryProjectRepository();
    }

    @Override
    protected Project storedProject(ProjectRepository repository, String projectId) {
        return ((InMemoryProjectRepository) repository).storedProject(projectId);
    }
}
