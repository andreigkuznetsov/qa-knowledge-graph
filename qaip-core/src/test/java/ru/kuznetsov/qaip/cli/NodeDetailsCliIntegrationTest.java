package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.node.ProjectNodeLookup;
import ru.kuznetsov.qaip.core.application.query.nodedetails.DefaultNodeDetailsUseCase;
import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsMapper;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeDetailsCliIntegrationTest {
    @Test
    void same_process_dispatch_delivers_found_project_missing_and_node_missing() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project());
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        var summary = new DefaultProjectSummaryUseCase(reader, new ProjectSummaryMapper());
        var details = new DefaultNodeDetailsUseCase(reader, new ProjectNodeLookup(), new NodeDetailsMapper());

        QaipCliApplicationTest.Streams found = new QaipCliApplicationTest.Streams();
        assertEquals(0, QaipCliApplication.run(new String[]{"show", "node", "P-1", "N-1"},
                found.out, found.err, summary, details));
        assertEquals(String.join(System.lineSeparator(), "Node Details", "Project ID: P-1", "Node ID: N-1",
                "Type: BUSINESS_RULE", "Name: Rule", "Description: not specified", "Status: CONFIRMED")
                + System.lineSeparator(), found.stdout());
        assertEquals("", found.stderr());

        QaipCliApplicationTest.Streams missingNode = new QaipCliApplicationTest.Streams();
        assertEquals(5, QaipCliApplication.run(new String[]{"show", "node", "P-1", "missing"},
                missingNode.out, missingNode.err, summary, details));
        QaipCliApplicationTest.Streams missingProject = new QaipCliApplicationTest.Streams();
        assertEquals(3, QaipCliApplication.run(new String[]{"show", "node", "missing", "N-1"},
                missingProject.out, missingProject.err, summary, details));
    }

    private static Project project() {
        Node node = new Node("N-1", "BUSINESS_RULE", "Rule", null, "CONFIRMED",
                List.of(), List.of(), Map.of(), Map.of());
        return new Project("contract", "schema", new Metadata("P-1", "Project", null, null, Map.of()),
                List.of(), new Subject("local"), List.of(node), List.of(), new EvidenceManifest("evidence",
                "source", Map.of(), "normalization", "canonicalization", "fingerprint",
                List.of(), List.of(), List.of()), List.of(), Map.of());
    }
}
