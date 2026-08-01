package ru.kuznetsov.qaip.core.importing.binding;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.importing.parsing.JacksonProjectJsonParser;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.ProjectParseAccepted;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProjectBinderTest {
    private final ProjectBinder binder = new DefaultProjectBinder();

    @Test
    void bind_valid_project_returns_success() {
        BindingSuccess success = assertInstanceOf(BindingSuccess.class, binder.bind(validDocument("representative-project.json")));
        assertEquals("P-1", success.document().project().metadata().id());
    }

    @Test
    void bind_metadata() {
        Project project = bind("representative-project.json");
        assertEquals("qaip-project-v1", project.projectContractVersion());
        assertEquals("0.1", project.schemaVersion());
        assertEquals("Project", project.metadata().name());
        assertEquals("impact-evidence-analysis-v1", project.analysisContext().get("algorithmVersion"));
    }

    @Test
    void bind_subject() {
        assertEquals("local-1", bind("representative-project.json").subject().localArtifactId());
    }

    @Test
    void bind_all_nodes() {
        Project project = bind("representative-project.json");
        assertEquals(List.of("BR-1", "BR-2"), project.nodes().stream().map(node -> node.id()).toList());
        assertEquals("first", ((Map<?, ?>) project.nodes().getFirst().attributes().get("rule")).get("text"));
    }

    @Test
    void bind_all_relationships() {
        var relationship = bind("representative-project.json").relationships().getFirst();
        assertEquals("REL-1", relationship.id());
        assertEquals("BR-1", relationship.from());
        assertEquals("BR-2", relationship.to());
        assertEquals("DEPENDS_ON", relationship.type());
    }

    @Test
    void bind_evidence_manifest() {
        var evidence = bind("representative-project.json").evidenceManifest();
        assertEquals("jira", evidence.sourceId());
        assertEquals(2, evidence.identityAssertions().size());
        assertEquals(1, evidence.relationships().size());
        assertEquals(1, evidence.provenance().size());
    }

    @Test
    void bind_declared_changes() {
        var changes = bind("representative-project.json").declaredChanges();
        assertEquals(2, changes.size());
        assertEquals("NODE", changes.getFirst().artifactCategory());
        assertNull(changes.getFirst().beforeState());
        assertEquals("BR-1", changes.getFirst().afterState().get("id"));
    }

    @Test
    void bind_empty_collections_when_allowed() {
        Project project = bind("minimal-project.json");
        assertTrue(project.sources().isEmpty());
        assertTrue(project.nodes().isEmpty());
        assertTrue(project.relationships().isEmpty());
        assertTrue(project.evidenceManifest().identityAssertions().isEmpty());
        assertTrue(project.evidenceManifest().relationships().isEmpty());
        assertTrue(project.evidenceManifest().provenance().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void domain_model_is_immutable() {
        Project project = bind("representative-project.json");
        assertTrue(project.getClass().isRecord());
        assertTrue(Modifier.isFinal(project.getClass().getModifiers()));
        assertThrows(UnsupportedOperationException.class, () -> project.nodes().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> project.nodes().getFirst().attributes().put("changed", true));
        assertThrows(UnsupportedOperationException.class,
                () -> ((Map<String, Object>) project.evidenceManifest().identityAssertions().getFirst()
                        .get("resolution")).put("status", "CHANGED"));
    }

    @Test
    void binding_exception_is_propagated() {
        BindingException failure = new BindingException("internal");
        DefaultProjectBinder failingBinder = new DefaultProjectBinder(document -> { throw failure; });
        assertEquals(failure, assertThrows(BindingException.class,
                () -> failingBinder.bind(validDocument("representative-project.json"))));
    }

    private Project bind(String resource) {
        BindingSuccess success = assertInstanceOf(BindingSuccess.class, binder.bind(validDocument(resource)));
        return success.document().project();
    }

    private static SchemaValidProjectDocument validDocument(String resource) {
        try (var stream = DefaultProjectBinderTest.class.getResourceAsStream("/schema/valid/" + resource)) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            ProjectParseAccepted parsed = assertInstanceOf(ProjectParseAccepted.class,
                    new JacksonProjectJsonParser().parse(new RawProjectJson(json)));
            SchemaValidationAccepted valid = assertInstanceOf(SchemaValidationAccepted.class,
                    new NetworkntProjectSchemaValidator().validate(parsed.document()));
            return valid.document();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
