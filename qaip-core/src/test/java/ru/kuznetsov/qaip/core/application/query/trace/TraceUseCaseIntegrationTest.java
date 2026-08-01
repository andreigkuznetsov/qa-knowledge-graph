package ru.kuznetsov.qaip.core.application.query.trace;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.core.application.query.trace.DefaultTraceUseCaseTest.*;

class TraceUseCaseIntegrationTest {
    @Test
    void real_in_memory_flow_handles_connected_cycle_isolated_and_absence_deterministically() {
        Project project = project("P", List.of(
                        node("A", "story"), node("B", "rule"), node("C", "test"), node("ISO", "story")),
                List.of(
                        relationship("AB", "A", "B", "derives"),
                        relationship("BC", "B", "C", "verifies"),
                        relationship("CA", "C", "A", "covers")));
        Project before = project;
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project);
        TraceUseCase useCase = new DefaultTraceUseCase(new InMemoryProjectReader(repository),
                new ProjectNodeLookup(), new TraceGraphBuilder(), new TraceMapper());

        TraceFound connected = assertInstanceOf(TraceFound.class, useCase.execute("P", "B"));
        assertEquals(List.of("B", "A", "C"), connected.trace().nodes().stream().map(TraceNode::nodeId).toList());
        assertEquals(List.of("AB", "BC", "CA"), connected.trace().relationships().stream()
                .map(TraceRelationship::relationshipId).toList());
        assertEquals(connected, useCase.execute("P", "B"));

        TraceFound isolated = assertInstanceOf(TraceFound.class, useCase.execute("P", "ISO"));
        assertEquals(List.of(new TraceNode("ISO", "story")), isolated.trace().nodes());
        assertTrue(isolated.trace().relationships().isEmpty());
        assertEquals(new TraceProjectNotFound("missing"), useCase.execute("missing", "A"));
        assertEquals(new TraceNodeNotFound("P", "missing"), useCase.execute("P", "missing"));
        assertEquals(before, project);
    }
}
