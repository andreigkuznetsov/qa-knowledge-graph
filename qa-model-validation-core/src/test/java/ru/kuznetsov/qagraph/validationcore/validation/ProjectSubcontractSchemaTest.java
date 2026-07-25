package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectSubcontractSchemaTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalProjectSchemaResolver RESOLVER = new LocalProjectSchemaResolver();
    private static final Path ROOT = Path.of(System.getProperty("qaip.repositoryRoot"))
            .resolve("schemas/project/v1");

    @Test
    void acceptsAllValidFixtures() throws IOException {
        assertValid("impact-analysis-context-v1.schema.json", "analysis-context.json");
        assertValid("qaip-project-subject-candidate-v1.schema.json", "subject-candidate.json");
        assertValid("impact-evidence-manifest-v1.schema.json", "manifest-minimal.json");
        assertValid("impact-evidence-manifest-v1.schema.json", "manifest-representative.json");
        assertValid("qaip-declared-changes-v1.schema.json", "declared-change-added-node.json");
        assertValid("qaip-declared-changes-v1.schema.json", "declared-change-modified-node.json");
        assertValid("qaip-declared-changes-v1.schema.json", "declared-change-removed-node.json");
        assertValid("qaip-declared-changes-v1.schema.json", "declared-change-added-relationship.json");
        assertValid("qaip-declared-changes-v1.schema.json", "declared-changes-ordered.json");
    }

    @Test
    void enforcesDeclaredChangeStructureAndCanonicalStateReferences() throws IOException {
        JsonSchema schema = schema("qaip-declared-changes-v1.schema.json");
        ArrayNode added = array("declared-change-added-node.json");
        ObjectNode declaration = (ObjectNode) added.get(0);
        assertInvalid(schema, without(declaration, "artifactCategory"));
        assertInvalid(schema, changed(declaration, "artifactCategory", "UNKNOWN"));
        assertInvalid(schema, changed(declaration, "artifactCategory", "node"));
        assertInvalid(schema, without(declaration, "canonicalIdentity"));
        assertInvalid(schema, nulled(declaration, "canonicalIdentity"));
        assertInvalid(schema, changed(declaration, "canonicalIdentity", " "));
        assertInvalid(schema, changed(declaration, "canonicalIdentity", "bad identity"));
        assertInvalid(schema, changed(declaration, "changeKind", "added"));
        assertInvalid(schema, with(declaration, "beforeState", declaration.get("afterState")));
        assertInvalid(schema, without(declaration, "afterState"));
        assertInvalid(schema, with(declaration, "unknown", MAPPER.createObjectNode()));
        assertInvalid(schema, MAPPER.createArrayNode());
        ArrayNode withNull = MAPPER.createArrayNode().addNull();
        assertInvalid(schema, withNull);

        ObjectNode modified = (ObjectNode) array("declared-change-modified-node.json").get(0);
        assertInvalid(schema, without(modified, "beforeState"));
        assertInvalid(schema, without(modified, "afterState"));
        assertInvalid(schema, nulled(modified, "beforeState"));
        assertInvalid(schema, nulled(modified, "afterState"));

        ObjectNode removed = (ObjectNode) array("declared-change-removed-node.json").get(0);
        assertInvalid(schema, without(removed, "beforeState"));
        assertInvalid(schema, with(removed, "afterState", removed.get("beforeState")));

        ObjectNode nodeAsRelationship = declaration.deepCopy();
        nodeAsRelationship.set("afterState", array("declared-change-added-relationship.json").get(0).get("afterState"));
        assertInvalid(schema, MAPPER.createArrayNode().add(nodeAsRelationship));
        ObjectNode relationshipAsNode = ((ObjectNode) array("declared-change-added-relationship.json").get(0)).deepCopy();
        relationshipAsNode.set("afterState", declaration.get("afterState"));
        assertInvalid(schema, MAPPER.createArrayNode().add(relationshipAsNode));
    }

    @Test
    void resolvesExactAbsoluteUrisOfflineAndRejectsUnknownUris() throws IOException {
        assertTrue(RESOLVER.resolve(LocalProjectSchemaResolver.QA_MODEL)
                .validate(MAPPER.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":[],\"relationships\":[]}"))
                .isEmpty());
        assertTrue(RESOLVER.resolve(LocalProjectSchemaResolver.DECLARED_CHANGES)
                .validate(array("declared-change-added-node.json")).isEmpty());
        assertTrue(RESOLVER.resolve(LocalProjectSchemaResolver.DECLARED_CHANGES)
                .validate(array("declared-change-added-relationship.json")).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> RESOLVER.resolve("https://example.local/schemas/unknown-schema-v1.schema.json"));
    }

    @Test
    void rejectsContextDefaultsAliasesNullsAndUnknownMembers() throws IOException {
        JsonSchema schema = schema("impact-analysis-context-v1.schema.json");
        ObjectNode valid = object("analysis-context.json");
        assertInvalid(schema, valid.deepCopy().remove("algorithmVersion"));
        assertInvalid(schema, valid.deepCopy().put("algorithmVersion", "IMPACT-EVIDENCE-ANALYSIS-V1"));
        assertInvalid(schema, valid.deepCopy().putNull("qualificationVersion"));
        assertInvalid(schema, valid.deepCopy().put("unknown", "value"));
    }

    @Test
    void rejectsInvalidSubjectCandidates() throws IOException {
        JsonSchema schema = schema("qaip-project-subject-candidate-v1.schema.json");
        ObjectNode valid = object("subject-candidate.json");
        assertInvalid(schema, valid.deepCopy().remove("localArtifactId"));
        assertInvalid(schema, valid.deepCopy().putNull("localArtifactId"));
        assertInvalid(schema, valid.deepCopy().put("localArtifactId", "  "));
        assertInvalid(schema, valid.deepCopy().put("canonicalIdentity", "BR-1"));
    }

    @Test
    void rejectsManifestVersionNullUnknownAndMissingCollection() throws IOException {
        JsonSchema schema = schema("impact-evidence-manifest-v1.schema.json");
        ObjectNode valid = object("manifest-minimal.json");
        assertInvalid(schema, valid.deepCopy().remove("contractVersion"));
        assertInvalid(schema, valid.deepCopy().put("contractVersion", "future"));
        assertInvalid(schema, valid.deepCopy().putNull("sourceId"));
        assertInvalid(schema, valid.deepCopy().remove("relationships"));
        assertInvalid(schema, valid.deepCopy().put("isValid", true));
        ObjectNode nullElement = valid.deepCopy();
        ((ArrayNode) nullElement.get("provenance")).addNull();
        assertInvalid(schema, nullElement);
    }

    @Test
    void enforcesApprovedIdentityResolutionUnion() throws IOException {
        JsonSchema schema = schema("impact-evidence-manifest-v1.schema.json");
        ObjectNode valid = object("manifest-representative.json");
        ObjectNode missingStatus = valid.deepCopy();
        ((ObjectNode) missingStatus.at("/identityAssertions/0/resolution")).remove("status");
        assertInvalid(schema, missingStatus);
        ObjectNode unknown = valid.deepCopy();
        ((ObjectNode) unknown.at("/identityAssertions/0/resolution")).put("status", "resolved");
        assertInvalid(schema, unknown);
        ObjectNode mixed = valid.deepCopy();
        ((ObjectNode) mixed.at("/identityAssertions/0/resolution")).put("reasonCode", "NO_MAPPING");
        assertInvalid(schema, mixed);
        ObjectNode unresolvedMixed = valid.deepCopy();
        ((ObjectNode) unresolvedMixed.at("/identityAssertions/1/resolution")).put("canonicalIdentity", "BR-2");
        assertInvalid(schema, unresolvedMixed);
    }

    @Test
    void permitsStructurallyValidDuplicateManifestEntries() throws IOException {
        JsonSchema schema = schema("impact-evidence-manifest-v1.schema.json");
        ObjectNode manifest = object("manifest-representative.json");
        ArrayNode assertions = (ArrayNode) manifest.get("identityAssertions");
        assertions.add(assertions.get(0).deepCopy());
        assertTrue(schema.validate(manifest).isEmpty());
    }

    private static void assertValid(String schemaName, String fixtureName) throws IOException {
        JsonNode fixture = MAPPER.readTree(ROOT.resolve("examples/valid").resolve(fixtureName).toFile());
        assertTrue(schema(schemaName).validate(fixture).isEmpty());
    }

    private static void assertInvalid(JsonSchema schema, JsonNode value) {
        assertFalse(schema.validate(value).isEmpty());
    }

    private static ObjectNode object(String fixtureName) throws IOException {
        return (ObjectNode) MAPPER.readTree(ROOT.resolve("examples/valid").resolve(fixtureName).toFile());
    }

    private static ArrayNode array(String fixtureName) throws IOException {
        return (ArrayNode) MAPPER.readTree(ROOT.resolve("examples/valid").resolve(fixtureName).toFile());
    }

    private static ArrayNode without(ObjectNode source, String field) {
        ObjectNode copy = source.deepCopy(); copy.remove(field); return MAPPER.createArrayNode().add(copy);
    }

    private static ArrayNode changed(ObjectNode source, String field, String value) {
        ObjectNode copy = source.deepCopy(); copy.put(field, value); return MAPPER.createArrayNode().add(copy);
    }

    private static ArrayNode nulled(ObjectNode source, String field) {
        ObjectNode copy = source.deepCopy(); copy.putNull(field); return MAPPER.createArrayNode().add(copy);
    }

    private static ArrayNode with(ObjectNode source, String field, JsonNode value) {
        ObjectNode copy = source.deepCopy(); copy.set(field, value.deepCopy()); return MAPPER.createArrayNode().add(copy);
    }

    private static JsonSchema schema(String name) throws IOException {
        return RESOLVER.resolve("https://example.local/schemas/" + name);
    }
}
