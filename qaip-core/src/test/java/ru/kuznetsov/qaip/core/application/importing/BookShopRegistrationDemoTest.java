package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.binding.DefaultProjectBinder;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;
import ru.kuznetsov.qaip.core.validation.DefaultProjectApplicationValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookShopRegistrationDemoTest {
    private static final Set<String> EXPECTED_NODE_IDS = Set.of(
            "US-REG-001", "BO-REG-001",
            "BR-REG-001", "BR-REG-002", "BR-REG-003", "BR-REG-004",
            "SC-REG-001", "SC-REG-002", "SC-REG-003", "SC-REG-004",
            "TI-REG-001", "TI-REG-002", "TI-REG-003",
            "TEST-REG-001", "CHECK-REG-001", "CHECK-REG-002");

    private static final Set<String> EXPECTED_RELATIONSHIP_TRIPLES = Set.of(
            triple("US-REG-001", "DESCRIBES", "BO-REG-001"),
            triple("BO-REG-001", "GOVERNED_BY", "BR-REG-001"),
            triple("BO-REG-001", "SPECIFIED_BY", "SC-REG-001"),
            triple("BO-REG-001", "SPECIFIED_BY", "SC-REG-002"),
            triple("BO-REG-001", "SPECIFIED_BY", "SC-REG-003"),
            triple("BO-REG-001", "SPECIFIED_BY", "SC-REG-004"),
            triple("BO-REG-001", "IMPLEMENTED_BY", "TI-REG-001"),
            triple("BO-REG-001", "IMPLEMENTED_BY", "TI-REG-002"),
            triple("BO-REG-001", "IMPLEMENTED_BY", "TI-REG-003"),
            triple("TEST-REG-001", "VALIDATES", "SC-REG-001"),
            triple("TEST-REG-001", "VALIDATES", "SC-REG-002"),
            triple("TEST-REG-001", "VALIDATES", "SC-REG-003"),
            triple("TEST-REG-001", "VALIDATES", "SC-REG-004"),
            triple("TEST-REG-001", "USES", "TI-REG-001"),
            triple("TEST-REG-001", "HAS_CHECK", "CHECK-REG-001"),
            triple("TEST-REG-001", "HAS_CHECK", "CHECK-REG-002"),
            triple("SC-REG-001", "COVERS", "BR-REG-004"),
            triple("SC-REG-002", "COVERS", "BR-REG-001"),
            triple("SC-REG-003", "COVERS", "BR-REG-002"),
            triple("SC-REG-004", "COVERS", "BR-REG-003"));

    private final ProjectImporter importer = new DefaultProjectImporter(
            new JacksonProjectJsonParser(), new NetworkntProjectSchemaValidator(),
            new DefaultProjectBinder(), new DefaultProjectApplicationValidator());

    @Test
    void production_import_stages_accept_exact_canonical_graph_without_persistence() throws IOException {
        Path example = findRepositoryRoot().resolve("examples/bookshop-registration-demo.json");
        ProjectImportSuccess success = assertInstanceOf(ProjectImportSuccess.class,
                importer.importProject(new RawProjectJson(Files.readString(example))));
        ApplicationValidProjectDocument proof = assertInstanceOf(
                ApplicationValidProjectDocument.class, success.document());

        var project = proof.project();
        assertTrue(success.warnings().isEmpty());
        assertEquals(16, project.nodes().size());
        assertEquals(20, project.relationships().size());
        assertEquals(EXPECTED_NODE_IDS,
                project.nodes().stream().map(node -> node.id()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(EXPECTED_RELATIONSHIP_TRIPLES,
                project.relationships().stream()
                        .map(relationship -> triple(relationship.from(), relationship.type(), relationship.to()))
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(20, new LinkedHashSet<>(project.relationships().stream()
                .map(relationship -> triple(relationship.from(), relationship.type(), relationship.to()))
                .toList()).size());
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("Repository root not found");
        return current;
    }

    private static String triple(String from, String type, String to) {
        return from + '\u0000' + type + '\u0000' + to;
    }
}
