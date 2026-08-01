package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.projectsummary.DefaultProjectSummaryUseCase;
import ru.kuznetsov.qaip.core.application.query.projectsummary.ProjectSummaryMapper;
import ru.kuznetsov.qaip.core.domain.*;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectSummaryCliIntegrationTest {
    @Test
    void same_process_in_memory_flow_delivers_found_and_missing_results() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        repository.insertIfAbsent(project());
        var useCase = new DefaultProjectSummaryUseCase(
                new InMemoryProjectReader(repository), new ProjectSummaryMapper());

        QaipCliApplicationTest.Streams found = new QaipCliApplicationTest.Streams();
        assertEquals(0, QaipCliApplication.run(new String[]{"summary", "P-1"}, found.out, found.err, useCase));
        assertEquals(String.join(System.lineSeparator(), "Project Summary", "Project ID: P-1",
                "Contract Version: contract-v1", "Schema Version: schema-v2", "Sources: 1", "Nodes: 1",
                "Relationships: 1", "Evidence: 3", "Declared Changes: 1") + System.lineSeparator(),
                found.stdout());
        assertEquals("", found.stderr());

        QaipCliApplicationTest.Streams missing = new QaipCliApplicationTest.Streams();
        assertEquals(3, QaipCliApplication.run(new String[]{"summary", "missing"},
                missing.out, missing.err, useCase));
        assertEquals("Project not found: missing" + System.lineSeparator(), missing.stdout());
        assertEquals("", missing.stderr());
    }

    private static Project project() {
        EvidenceManifest evidence = new EvidenceManifest("evidence", "source", Map.of("ignored", 1),
                "normalization", "canonicalization", "fingerprint", List.of(Map.of()), List.of(Map.of()),
                List.of(Map.of()));
        return new Project("contract-v1", "schema-v2", new Metadata("P-1", "name", null, null, Map.of()),
                List.of(Map.of("source", 1)), new Subject("local"),
                List.of(new Node("N", "TYPE", "name", null, null, List.of(), List.of(), Map.of(), Map.of())),
                List.of(new Relationship("R", "N", "TYPE", "N", Map.of(), List.of())), evidence,
                List.of(new DeclaredChange("NODE", "N", "ADDED", "1", null, Map.of())), Map.of());
    }
}
