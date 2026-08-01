package ru.kuznetsov.qagraph.validationcore.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRootSchemaTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalProjectSchemaResolver RESOLVER = new LocalProjectSchemaResolver();
    private static final JsonSchema SCHEMA = RESOLVER.resolve(LocalProjectSchemaResolver.PROJECT);
    private static final Path FIXTURES = Path.of(System.getProperty("qaip.repositoryRoot"))
            .resolve("schemas/project/v1/examples/valid");

    @Test
    void acceptsAllRootCompositionFixtures() throws IOException {
        assertValid("minimal-project.json");
        assertValid("representative-project.json");
        assertValid("non-canonical-order-project.json");
        assertValid("valid-structurally-semantically-unverified-project.json");
    }

    @Test
    void rejectsInvalidRootShapeAndVersion() throws IOException {
        assertInvalid(MAPPER.nullNode(), "", "type");
        assertInvalid(MAPPER.createArrayNode(), "", "type");
        assertInvalid(MAPPER.getNodeFactory().textNode("project"), "", "type");
        assertInvalid(MAPPER.createObjectNode(), "", "required");

        ObjectNode valid = fixture("minimal-project.json");
        assertInvalid(valid.deepCopy().put("unknown", true), "", "additionalProperties");
        assertInvalid(without(valid, "projectContractVersion"), "", "required");
        assertInvalid(valid.deepCopy().putNull("projectContractVersion"), "/projectContractVersion", "type");
        assertInvalid(valid.deepCopy().put("projectContractVersion", 1), "/projectContractVersion", "type");
        for (String version : new String[]{"qaip-project-v2", "QAIP-PROJECT-V1", "qaip-project-V1",
                "qaip-project-v01", " qaip-project-v1", "qaip-project-v1 "}) {
            assertInvalid(valid.deepCopy().put("projectContractVersion", version), "/projectContractVersion", "const");
        }
    }

    @Test
    void propagatesEveryNestedContractFailure() throws IOException {
        ObjectNode valid = fixture("representative-project.json");
        for (String member : new String[]{"baseModel", "declaredChanges", "evidenceManifest", "subject", "analysisContext"}) {
            assertInvalid(without(valid, member), "", "required");
            assertInvalid(valid.deepCopy().putNull(member), "/" + member, "type");
            assertInvalid(valid.deepCopy().put(member, "wrong-type"), "/" + member, "type");
        }

        ObjectNode baseUnknown = valid.deepCopy();
        ((ObjectNode) baseUnknown.get("baseModel")).put("unknown", true);
        assertInvalid(baseUnknown, "/baseModel", "additionalProperties");
        ObjectNode badModel = valid.deepCopy();
        ((ObjectNode) badModel.get("baseModel")).remove("project");
        assertInvalid(badModel, "/baseModel", "required");

        ObjectNode emptyChanges = valid.deepCopy();
        emptyChanges.set("declaredChanges", MAPPER.createArrayNode());
        assertInvalid(emptyChanges, "/declaredChanges", "minItems");
        ObjectNode nullChange = valid.deepCopy();
        ((ArrayNode) nullChange.get("declaredChanges")).addNull();
        assertInvalid(nullChange, "/declaredChanges/2", "type");
        ObjectNode invalidChange = valid.deepCopy();
        ((ObjectNode) invalidChange.at("/declaredChanges/0")).remove("changeKind");
        assertInvalid(invalidChange, "/declaredChanges/0", "required");

        ObjectNode badManifest = valid.deepCopy();
        ((ObjectNode) badManifest.get("evidenceManifest")).put("contractVersion", "future");
        assertInvalid(badManifest, "/evidenceManifest/contractVersion", "const");
        ObjectNode badUnion = valid.deepCopy();
        ((ObjectNode) badUnion.at("/evidenceManifest/identityAssertions/0/resolution")).put("reasonCode", "mixed");
        assertInvalid(badUnion, "/evidenceManifest/identityAssertions/0/resolution", "oneOf");
        ObjectNode unknownManifest = valid.deepCopy();
        ((ObjectNode) unknownManifest.get("evidenceManifest")).put("unknown", true);
        assertInvalid(unknownManifest, "/evidenceManifest", "additionalProperties");

        ObjectNode badSubject = valid.deepCopy();
        ((ObjectNode) badSubject.get("subject")).put("localArtifactId", " ");
        assertInvalid(badSubject, "/subject/localArtifactId", "pattern");
        ObjectNode missingSubjectId = valid.deepCopy();
        ((ObjectNode) missingSubjectId.get("subject")).remove("localArtifactId");
        assertInvalid(missingSubjectId, "/subject", "required");
        ObjectNode unknownSubject = valid.deepCopy();
        ((ObjectNode) unknownSubject.get("subject")).put("unknown", true);
        assertInvalid(unknownSubject, "/subject", "additionalProperties");
        ObjectNode badContext = valid.deepCopy();
        ((ObjectNode) badContext.get("analysisContext")).remove("algorithmVersion");
        assertInvalid(badContext, "/analysisContext", "required");
        ObjectNode alteredContext = valid.deepCopy();
        ((ObjectNode) alteredContext.get("analysisContext")).put("algorithmVersion", "future");
        assertInvalid(alteredContext, "/analysisContext/algorithmVersion", "const");
        ObjectNode unknownContext = valid.deepCopy();
        ((ObjectNode) unknownContext.get("analysisContext")).put("unknown", true);
        assertInvalid(unknownContext, "/analysisContext", "additionalProperties");
    }

    @Test
    void rejectsCanonicalNodeRelationshipMismatchesThroughRootReferenceChain() throws IOException {
        ObjectNode valid = fixture("representative-project.json");
        ObjectNode nodeAsRelationship = valid.deepCopy();
        nodeAsRelationship.at("/declaredChanges/0").deepCopy();
        ((ObjectNode) nodeAsRelationship.at("/declaredChanges/0")).set("afterState",
                valid.at("/declaredChanges/1/afterState").deepCopy());
        assertInvalid(nodeAsRelationship, "/declaredChanges/0/afterState", "oneOf");

        ObjectNode relationshipAsNode = valid.deepCopy();
        ((ObjectNode) relationshipAsNode.at("/declaredChanges/1")).set("afterState",
                valid.at("/declaredChanges/0/afterState").deepCopy());
        assertFalse(SCHEMA.validate(relationshipAsNode).isEmpty());
    }

    @Test
    void resolverIsExactOfflineAndFailsWhenRequiredMappingIsAbsent() throws IOException {
        assertTrue(RESOLVER.resolve(LocalProjectSchemaResolver.PROJECT).validate(fixture("minimal-project.json")).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> RESOLVER.resolve("https://example.local/schemas/unknown-root.schema.json"));
        LocalProjectSchemaResolver incomplete = new LocalProjectSchemaResolver(LocalProjectSchemaResolver.MANIFEST);
        JsonSchema incompleteRoot = incomplete.resolve(LocalProjectSchemaResolver.PROJECT);
        assertThrows(RuntimeException.class, () -> incompleteRoot.validate(fixture("minimal-project.json")));
    }

    private static void assertValid(String name) throws IOException {
        assertTrue(SCHEMA.validate(fixture(name)).isEmpty(), name);
    }

    private static void assertInvalid(JsonNode value, String instanceLocation, String keyword) {
        Set<ValidationMessage> errors = SCHEMA.validate(value);
        assertTrue(errors.stream().anyMatch(error ->
                        error.getInstanceLocation().toString().startsWith(jsonPath(instanceLocation))
                                && (keyword.equals(error.getType()) || keyword.equals(error.getMessageKey()))),
                () -> "Expected " + keyword + " at " + instanceLocation + " but got " + errors);
    }

    private static String jsonPath(String pointer) {
        if (pointer.isEmpty()) {
            return "$";
        }
        StringBuilder result = new StringBuilder("$");
        for (String segment : pointer.substring(1).split("/")) {
            if (segment.chars().allMatch(Character::isDigit)) {
                result.append('[').append(segment).append(']');
            } else {
                result.append('.').append(segment);
            }
        }
        return result.toString();
    }

    private static ObjectNode fixture(String name) throws IOException {
        return (ObjectNode) MAPPER.readTree(FIXTURES.resolve(name).toFile());
    }

    private static ObjectNode without(ObjectNode source, String member) {
        ObjectNode copy = source.deepCopy();
        copy.remove(member);
        return copy;
    }
}
