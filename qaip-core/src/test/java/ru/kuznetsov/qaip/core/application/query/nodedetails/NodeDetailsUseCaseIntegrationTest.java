package ru.kuznetsov.qaip.core.application.query.nodedetails;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeDetailsUseCaseIntegrationTest {
    @Test
    void real_in_memory_flow_handles_found_and_both_absence_levels_without_mutation() {
        var node = DefaultNodeDetailsUseCaseTest.node("N-1", "BUSINESS_RULE", "Rule");
        var project = DefaultNodeDetailsUseCaseTest.project("P-1", List.of(node));
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project);
        assertInstanceOf(ProjectAlreadyExists.class, repository.insertIfAbsent(project));
        NodeDetailsUseCase useCase = new DefaultNodeDetailsUseCase(new InMemoryProjectReader(repository),
                new ProjectNodeLookup(), new NodeDetailsMapper());

        NodeDetailsFound found = assertInstanceOf(NodeDetailsFound.class, useCase.execute("P-1", "N-1"));
        assertEquals(new NodeDetailsMapper().map(node), found.details());
        assertEquals(found, useCase.execute("P-1", "N-1"));
        assertEquals(new NodeDetailsNodeNotFound("P-1", "missing"), useCase.execute("P-1", "missing"));
        assertEquals(new NodeDetailsProjectNotFound("missing"), useCase.execute("missing", "N-1"));
        assertSame(project, new InMemoryProjectReader(repository).findById("P-1").orElseThrow());
    }
}
