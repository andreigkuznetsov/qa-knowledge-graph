package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.DefaultNodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.application.query.relationship.*;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipsCliIntegrationTest {
    @Test
    void same_process_dispatch_handles_relationships_empty_lists_and_both_absence_levels() {
        Project project = project();
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project);
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        var summary = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());
        var node = new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), new NodeDetailsMapper());
        var relationships = new DefaultRelationshipsUseCase(reader, new ProjectNodeLookup(),
                new ProjectRelationshipLookup(), new RelationshipDetailsMapper());

        QaipCliApplicationTest.Streams connected = new QaipCliApplicationTest.Streams();
        assertEquals(0, QaipCliApplication.run(new String[]{"show", "relationships", "P", "N"},
                connected.out, connected.err, summary, node, relationships));
        assertEquals(String.join(System.lineSeparator(), "Relationships", "Project ID: P", "Node ID: N", "",
                "Incoming:", "IN | DIRECT | A -> N", "", "Outgoing:", "OUT | DIRECT | N -> B")
                + System.lineSeparator(), connected.stdout());

        QaipCliApplicationTest.Streams isolated = new QaipCliApplicationTest.Streams();
        assertEquals(0, QaipCliApplication.run(new String[]{"show", "relationships", "P", "I"},
                isolated.out, isolated.err, summary, node, relationships));
        assertEquals(2, occurrences(isolated.stdout(), "(none)"));

        QaipCliApplicationTest.Streams missingNode = new QaipCliApplicationTest.Streams();
        assertEquals(5, QaipCliApplication.run(new String[]{"show", "relationships", "P", "missing"},
                missingNode.out, missingNode.err, summary, node, relationships));
        assertEquals("Node not found: missing in project P" + System.lineSeparator(), missingNode.stdout());

        QaipCliApplicationTest.Streams missingProject = new QaipCliApplicationTest.Streams();
        assertEquals(3, QaipCliApplication.run(new String[]{"show", "relationships", "missing", "N"},
                missingProject.out, missingProject.err, summary, node, relationships));
        assertEquals("Project not found: missing" + System.lineSeparator(), missingProject.stdout());
        assertSame(project, reader.findById("P").orElseThrow());
    }

    private static Project project() {
        Node connected = new Node("N", "RULE", "connected", null, null,
                List.of(), List.of(), Map.of(), Map.of());
        Node isolated = new Node("I", "RULE", "isolated", null, null,
                List.of(), List.of(), Map.of(), Map.of());
        Relationship incoming = new Relationship("IN", "A", "DIRECT", "N", Map.of(), List.of());
        Relationship outgoing = new Relationship("OUT", "N", "DIRECT", "B", Map.of(), List.of());
        return new Project("contract", "schema", new Metadata("P", "project", null, null, Map.of()), List.of(),
                new Subject("local"), List.of(connected, isolated), List.of(incoming, outgoing),
                new EvidenceManifest("evidence", "source", Map.of(), "normalization", "canonicalization",
                        "fingerprint", List.of(), List.of(), List.of()), List.of(), Map.of());
    }

    private static int occurrences(String source, String token) {
        return (source.length() - source.replace(token, "").length()) / token.length();
    }
}
