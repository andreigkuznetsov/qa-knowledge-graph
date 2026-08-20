package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioManifestSchemaValidatorTest {
    private final ScenarioManifestJsonParser parser = new ScenarioManifestJsonParser();
    private final ScenarioManifestSchemaValidator validator = new ScenarioManifestSchemaValidator();

    @Test
    void admitsValidManifestAndPreservesParsedSource() {
        ScenarioManifestJsonParseResult.Completed parsed = parsed(member("valid", validManifest()));

        ScenarioManifestSchemaValidationResult result = validator.validate(parsed);

        assertTrue(result.structurallyAdmitted());
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(parsed.members().getFirst().source(), result.members().getFirst().source().source());
        assertEquals("CREATE-ORDER-SUCCESS",
                result.members().getFirst().source().document()
                        .path("scenarios").get(0).path("scenarioKey").asText());
    }

    @Test
    void rejectsWrongFormatAndSchemaVersionWithBothDiagnostics() {
        String invalid = validManifest()
                .replace("qaip-scenario-authority-manifest-v1", "wrong-format")
                .replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"2.0\"");

        ScenarioManifestSchemaValidationResult result = validate(member("wrong-version", invalid));

        assertFalse(result.structurallyAdmitted());
        assertEquals(2, result.diagnostics().stream()
                .filter(value -> value.keyword().equals("const"))
                .count());
    }

    @Test
    void rejectsMissingRequiredRootField() {
        String invalid = validManifest().replace("\"authority\":\"order-scenarios\",", "");

        assertHasKeyword(validate(member("missing-authority", invalid)), "required");
    }

    @Test
    void rejectsForbiddenCanonicalAndUnknownFields() {
        String invalid = validManifest().replace(
                "\"schemaVersion\":\"1.0\",",
                "\"schemaVersion\":\"1.0\",\"canonicalNodeId\":\"SC-1\",");

        assertHasKeyword(validate(member("forbidden", invalid)), "additionalProperties");
    }

    @Test
    void rejectsInvalidScenarioStructure() {
        String invalid = validManifest().replace(
                "\"given\":[\"A valid request\"]",
                "\"given\":[]");

        assertHasKeyword(validate(member("invalid-scenario", invalid)), "minItems");
    }

    @Test
    void rejectsInvalidOperationReference() {
        String invalid = validManifest().replace(
                "\"path\":\"/api/orders\"",
                "\"unexpectedOperationBody\":{}"
        );

        ScenarioManifestSchemaValidationResult result = validate(member("invalid-operation", invalid));

        assertHasKeyword(result, "required");
        assertHasKeyword(result, "additionalProperties");
    }

    @Test
    void rejectsInvalidBusinessRuleReference() {
        String invalid = validManifest().replace(
                "\"ruleRefs\":[]",
                "\"ruleRefs\":[{\"authority\":\"order-rules\",\"identityScheme\":\"rule-v1\"}]"
        );

        assertHasKeyword(validate(member("invalid-rule", invalid)), "required");
    }

    @Test
    void validatesEveryMemberAndRetainsDiagnosticsInMemberOrder() {
        var first = member("z-first", validManifest().replace("\"title\":\"Create order\"", "\"title\":\"   \""));
        var second = member("a-second", validManifest().replace("\"then\":[\"The order is accepted\"]", "\"then\":[]"));

        ScenarioManifestSchemaValidationResult result = validator.validate(parsed(first, second));

        assertFalse(result.structurallyAdmitted());
        assertEquals(List.of(
                        ".qaip/scenarios/z-first.scenario.json",
                        ".qaip/scenarios/a-second.scenario.json"),
                result.members().stream()
                        .map(value -> value.source().source().repositoryRelativePath())
                        .toList());
        assertEquals(List.of(
                        ".qaip/scenarios/z-first.scenario.json",
                        ".qaip/scenarios/a-second.scenario.json"),
                result.diagnostics().stream()
                        .map(ScenarioManifestSchemaValidationResult.Diagnostic::repositoryRelativePath)
                        .distinct()
                        .toList());
    }

    @Test
    void emptyMembershipIsStructurallyAdmitted() {
        ScenarioManifestSchemaValidationResult result = validator.validate(parsed());

        assertTrue(result.structurallyAdmitted());
        assertEquals(List.of(), result.members());
        assertEquals(List.of(), result.diagnostics());
    }

    @Test
    void validationResultCollectionsAndParsedDocumentsAreImmutable() {
        ScenarioManifestSchemaValidationResult result = validate(member("immutable", validManifest()));
        ObjectNode exposed = (ObjectNode) result.members().getFirst().source().document();
        exposed.put("authority", "changed");

        assertEquals("order-scenarios",
                result.members().getFirst().source().document().path("authority").asText());
        assertThrows(UnsupportedOperationException.class, () -> result.members().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.members().getFirst().diagnostics().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    }

    @Test
    void exactFrozenV1SchemaFingerprintIsAcceptedAndAssociatedWithMapping() throws Exception {
        byte[] bytes;
        try (InputStream input = ScenarioManifestSchemaValidator.class.getResourceAsStream(
                ScenarioManifestSchemaValidator.SCHEMA_RESOURCE)) {
            bytes = input.readAllBytes();
        }
        String actual = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));

        assertEquals("7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1",
                ScenarioManifestSchemaValidator.EXPECTED_SCHEMA_SHA256);
        assertEquals(ScenarioManifestSchemaValidator.EXPECTED_SCHEMA_SHA256, actual);
        assertEquals("sha256:" + actual, validator.schemaContentIdentity());
        assertEquals(ScenarioManifestSchemaValidator.SCHEMA_CONTENT_IDENTITY,
                ScenarioSchemaDiagnosticAdapterV1.SCHEMA_CONTENT_IDENTITY);
        assertEquals(ScenarioSchemaDiagnosticAdapterV1.CONTRACT_IDENTIFIER + "|sha256:" + actual,
                ScenarioSchemaDiagnosticAdapterV1.V1_SCHEMA_MAPPING_BINDING);
    }

    private ScenarioManifestSchemaValidationResult validate(
            ScenarioManifestCaptureResult.CapturedMember... members) {
        return validator.validate(parsed(members));
    }

    private ScenarioManifestJsonParseResult.Completed parsed(
            ScenarioManifestCaptureResult.CapturedMember... members) {
        return assertInstanceOf(ScenarioManifestJsonParseResult.Completed.class,
                parser.parse(new ScenarioManifestCaptureResult.Completed(List.of(members))));
    }

    private ScenarioManifestCaptureResult.CapturedMember member(String name, String json) {
        return new ScenarioManifestCaptureResult.CapturedMember(
                Path.of(name + ".scenario.json"),
                ".qaip/scenarios/" + name + ".scenario.json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertHasKeyword(
            ScenarioManifestSchemaValidationResult result,
            String keyword
    ) {
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.keyword().equals(keyword)),
                () -> "Expected " + keyword + " in " + result.diagnostics());
    }

    private static String validManifest() {
        return """
                {
                  "format":"qaip-scenario-authority-manifest-v1",
                  "schemaVersion":"1.0",
                  "authority":"order-scenarios",
                  "scenarioIdentityScheme":"qaip-scenario-identity-v1",
                  "scenarios":[{
                    "scenarioKey":"CREATE-ORDER-SUCCESS",
                    "title":"Create order",
                    "given":["A valid request"],
                    "when":["The client creates an order"],
                    "then":["The order is accepted"],
                    "operationRef":{
                      "identityScheme":"qaip-http-operation-reference-v1",
                      "method":"POST",
                      "path":"/api/orders"
                    },
                    "ruleRefs":[]
                  }]
                }
                """;
    }
}
