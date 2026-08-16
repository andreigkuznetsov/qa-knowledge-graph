package ru.kuznetsov.qagraph.extractor.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioAuthorityManifestSchemaTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream input = ScenarioAuthorityManifestSchemaTest.class.getResourceAsStream(
                "/schemas/qaip-scenario-authority-manifest-v1.schema.json")) {
            assertNotNull(input);
            schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(input);
        }
    }

    @Test
    void acceptsMinimumManifestWithNoScenarios() throws Exception {
        ObjectNode manifest = baseManifest();

        assertValid(manifest);
    }

    @Test
    void acceptsScenarioWithOrderedStepsAndZeroRuleReferences() throws Exception {
        ObjectNode manifest = baseManifest();
        ((ArrayNode) manifest.get("scenarios")).add(scenario());

        assertValid(manifest);
    }

    @Test
    void acceptsMultipleAuthorityQualifiedBusinessRuleReferences() throws Exception {
        ObjectNode scenario = scenario();
        ArrayNode references = (ArrayNode) scenario.get("ruleRefs");
        references.add(ruleReference("order-domain-ba", "ORDER-AMOUNT-MINIMUM"));
        references.add(ruleReference("order-service-repository", "order.create.currency.required"));
        ObjectNode manifest = baseManifest();
        ((ArrayNode) manifest.get("scenarios")).add(scenario);

        assertValid(manifest);
    }

    @Test
    void rejectsWrongFormatAndSchemaVersions() throws Exception {
        ObjectNode wrongFormat = baseManifest().put("format", "qaip-scenario-authority-manifest-v2");
        ObjectNode wrongSchema = baseManifest().put("schemaVersion", "2.0");

        assertInvalid(wrongFormat, "const");
        assertInvalid(wrongSchema, "const");
    }

    @Test
    void requiresEveryScenarioContractField() throws Exception {
        for (String field : new String[]{
                "scenarioKey", "title", "given", "when", "then", "operationRef", "ruleRefs"}) {
            ObjectNode candidate = scenario();
            candidate.remove(field);
            ObjectNode manifest = baseManifest();
            ((ArrayNode) manifest.get("scenarios")).add(candidate);

            assertInvalid(manifest, "required");
        }
    }

    @Test
    void requiresNonEmptyNonBlankGivenWhenThenSteps() throws Exception {
        for (String phase : new String[]{"given", "when", "then"}) {
            ObjectNode empty = scenario();
            empty.putArray(phase);
            ObjectNode emptyManifest = baseManifest();
            ((ArrayNode) emptyManifest.get("scenarios")).add(empty);
            assertInvalid(emptyManifest, "minItems");

            ObjectNode blank = scenario();
            blank.putArray(phase).add("   ");
            ObjectNode blankManifest = baseManifest();
            ((ArrayNode) blankManifest.get("scenarios")).add(blank);
            assertInvalid(blankManifest, "pattern");
        }
    }

    @Test
    void requiresExactOperationReferenceShape() throws Exception {
        ObjectNode missingPath = scenario();
        ((ObjectNode) missingPath.get("operationRef")).remove("path");
        ObjectNode missingPathManifest = baseManifest();
        ((ArrayNode) missingPathManifest.get("scenarios")).add(missingPath);
        assertInvalid(missingPathManifest, "required");

        ObjectNode embeddedOperation = scenario();
        ((ObjectNode) embeddedOperation.get("operationRef")).put("operationBody", "copied operation");
        ObjectNode embeddedOperationManifest = baseManifest();
        ((ArrayNode) embeddedOperationManifest.get("scenarios")).add(embeddedOperation);
        assertInvalid(embeddedOperationManifest, "additionalProperties");
    }

    @Test
    void requiresExactBusinessRuleReferenceShapeAndRejectsExactDuplicates() throws Exception {
        ObjectNode embeddedRule = scenario();
        ObjectNode reference = ruleReference("order-domain-ba", "ORDER-AMOUNT-MINIMUM");
        reference.put("ruleText", "Amount must be positive");
        ((ArrayNode) embeddedRule.get("ruleRefs")).add(reference);
        ObjectNode embeddedRuleManifest = baseManifest();
        ((ArrayNode) embeddedRuleManifest.get("scenarios")).add(embeddedRule);
        assertInvalid(embeddedRuleManifest, "additionalProperties");

        ObjectNode duplicates = scenario();
        ObjectNode duplicate = ruleReference("order-domain-ba", "ORDER-AMOUNT-MINIMUM");
        ((ArrayNode) duplicates.get("ruleRefs")).add(duplicate).add(duplicate.deepCopy());
        ObjectNode duplicatesManifest = baseManifest();
        ((ArrayNode) duplicatesManifest.get("scenarios")).add(duplicates);
        assertInvalid(duplicatesManifest, "uniqueItems");
    }

    @Test
    void rejectsCanonicalIdsAndUnknownFieldsAtEveryAuthoredBoundary() throws Exception {
        ObjectNode rootId = baseManifest().put("canonicalProjectId", "PROJECT-1");
        assertInvalid(rootId, "additionalProperties");

        ObjectNode scenarioId = scenario().put("id", "SC-1");
        ObjectNode scenarioIdManifest = baseManifest();
        ((ArrayNode) scenarioIdManifest.get("scenarios")).add(scenarioId);
        assertInvalid(scenarioIdManifest, "additionalProperties");

        ObjectNode relationshipId = scenario().put("relationshipId", "REL-1");
        ObjectNode relationshipIdManifest = baseManifest();
        ((ArrayNode) relationshipIdManifest.get("scenarios")).add(relationshipId);
        assertInvalid(relationshipIdManifest, "additionalProperties");
    }

    @Test
    void rejectsMalformedAuthorityKeysAndIdentitySchemes() throws Exception {
        assertInvalid(baseManifest().put("authority", "has spaces"), "pattern");

        ObjectNode badScenarioKey = scenario().put("scenarioKey", "bad key");
        ObjectNode badScenarioKeyManifest = baseManifest();
        ((ArrayNode) badScenarioKeyManifest.get("scenarios")).add(badScenarioKey);
        assertInvalid(badScenarioKeyManifest, "pattern");

        ObjectNode badRuleScheme = scenario();
        ObjectNode reference = ruleReference("order-domain-ba", "ORDER-AMOUNT-MINIMUM");
        reference.put("identityScheme", "bad scheme");
        ((ArrayNode) badRuleScheme.get("ruleRefs")).add(reference);
        ObjectNode badRuleSchemeManifest = baseManifest();
        ((ArrayNode) badRuleSchemeManifest.get("scenarios")).add(badRuleScheme);
        assertInvalid(badRuleSchemeManifest, "pattern");
    }

    private static ObjectNode baseManifest() {
        ObjectNode manifest = MAPPER.createObjectNode();
        manifest.put("format", "qaip-scenario-authority-manifest-v1");
        manifest.put("schemaVersion", "1.0");
        manifest.put("authority", "order-domain-scenarios");
        manifest.put("scenarioIdentityScheme", "qaip-scenario-identity-v1");
        manifest.putArray("scenarios");
        return manifest;
    }

    private static ObjectNode scenario() {
        ObjectNode scenario = MAPPER.createObjectNode();
        scenario.put("scenarioKey", "CREATE-ORDER-SUCCESS");
        scenario.put("title", "Create an order");
        scenario.putArray("given").add("A valid order request");
        scenario.putArray("when").add("The client creates the order");
        scenario.putArray("then").add("The order is accepted");
        ObjectNode operation = scenario.putObject("operationRef");
        operation.put("identityScheme", "qaip-http-operation-reference-v1");
        operation.put("method", "POST");
        operation.put("path", "/api/orders");
        scenario.putArray("ruleRefs");
        return scenario;
    }

    private static ObjectNode ruleReference(String authority, String stableRuleKey) {
        ObjectNode reference = MAPPER.createObjectNode();
        reference.put("authority", authority);
        reference.put("stableRuleKey", stableRuleKey);
        reference.put("identityScheme", "qaip-business-rule-reference-v1");
        return reference;
    }

    private static void assertValid(JsonNode candidate) {
        Set<ValidationMessage> messages = schema.validate(candidate);
        assertTrue(messages.isEmpty(), () -> "Expected valid manifest but got " + messages);
    }

    private static void assertInvalid(JsonNode candidate, String keyword) {
        Set<ValidationMessage> messages = schema.validate(candidate);
        assertFalse(messages.isEmpty(), "Expected invalid manifest");
        assertTrue(messages.stream().anyMatch(message -> keyword.equals(message.getType())),
                () -> "Expected " + keyword + " but got " + messages);
    }
}
