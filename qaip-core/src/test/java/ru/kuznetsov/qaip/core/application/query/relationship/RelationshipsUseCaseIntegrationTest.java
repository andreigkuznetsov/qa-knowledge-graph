package ru.kuznetsov.qaip.core.application.query.relationship;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipsUseCaseIntegrationTest {
    @Test
    void real_in_memory_flow_handles_relationships_empty_lists_and_both_absence_levels_without_mutation() {
        var target = DefaultRelationshipsUseCaseTest.node("N");
        var isolated = DefaultRelationshipsUseCaseTest.node("I");
        var incoming = DefaultRelationshipsUseCaseTest.relationship("IN", "A", "N");
        var outgoing = DefaultRelationshipsUseCaseTest.relationship("OUT", "N", "B");
        Project project = DefaultRelationshipsUseCaseTest.project(
                "P", List.of(target, isolated), List.of(incoming, outgoing));
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project);
        RelationshipsUseCase useCase = new DefaultRelationshipsUseCase(new InMemoryProjectReader(repository),
                new ProjectNodeLookup(), new ProjectRelationshipLookup(), new RelationshipDetailsMapper());

        RelationshipsFound found = assertInstanceOf(RelationshipsFound.class, useCase.execute("P", "N"));
        assertEquals(List.of(DefaultRelationshipsUseCaseTest.details(incoming)), found.relationships().incoming());
        assertEquals(List.of(DefaultRelationshipsUseCaseTest.details(outgoing)), found.relationships().outgoing());
        RelationshipsFound empty = assertInstanceOf(RelationshipsFound.class, useCase.execute("P", "I"));
        assertEquals(new RelationshipDetailsResult(List.of(), List.of()), empty.relationships());
        assertEquals(new RelationshipsNodeNotFound("P", "missing"), useCase.execute("P", "missing"));
        assertEquals(new RelationshipsProjectNotFound("missing"), useCase.execute("missing", "N"));
        assertEquals(found, useCase.execute("P", "N"));
        assertSame(project, new InMemoryProjectReader(repository).findById("P").orElseThrow());
    }
}
