package ru.kuznetsov.qagraph.validationcore.scenarioauthority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityManifestSchemaValidatorV1Test {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ScenarioAuthorityManifestSchemaValidatorV1 validator =
            new ScenarioAuthorityManifestSchemaValidatorV1();

    @Test void ownsExactPinnedSchemaBytesAndRejectsReplacement() throws Exception {
        byte[] bytes;
        try (InputStream input = ScenarioAuthorityManifestSchemaValidatorV1.class.getResourceAsStream(
                ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_RESOURCE)) {
            assertNotNull(input); bytes = input.readAllBytes();
        }
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals("7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1", digest);
        assertEquals(digest, ScenarioAuthorityManifestSchemaValidatorV1.EXPECTED_SCHEMA_SHA256);
        assertEquals("sha256:" + digest, ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTENT_IDENTITY);
        byte[] altered = bytes.clone(); altered[altered.length - 1] ^= 1;
        assertThrows(ScenarioAuthoritySchemaValidationException.class,
                () -> new ScenarioAuthorityManifestSchemaValidatorV1(altered));
        assertThrows(ScenarioAuthoritySchemaValidationException.class,
                () -> new ScenarioAuthorityManifestSchemaValidatorV1(null));
        assertThrows(ScenarioAuthoritySchemaValidationException.class,
                () -> new ScenarioAuthorityManifestSchemaValidatorV1("{}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test void validatesRepresentativeAdmittedManifestDeterministically() throws Exception {
        JsonNode document = JSON.readTree(validManifest());
        var first = validator.validate(ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER,
                document);
        var second = validator.validate(ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER,
                document);
        assertTrue(first.structurallyValid());
        assertEquals(first, second);
        assertEquals(ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTENT_IDENTITY,
                first.schemaContentIdentity());
    }

    @Test void emitsCompleteStableOrderedSignalsForRepresentativeRejections() throws Exception {
        JsonNode document = JSON.readTree(validManifest());
        ((com.fasterxml.jackson.databind.node.ObjectNode) document).remove("format");
        ((com.fasterxml.jackson.databind.node.ObjectNode) document).put("unexpected", true);
        ((com.fasterxml.jackson.databind.node.ArrayNode) document.path("scenarios").get(0).path("given"))
                .removeAll();
        var first = validator.validate(ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER,
                document);
        var second = validator.validate(ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER,
                document);
        assertFalse(first.structurallyValid());
        assertEquals(first, second);
        assertEquals(List.copyOf(first.signals()), first.signals());
        assertTrue(first.signals().stream().anyMatch(x -> x.keyword().equals("required")));
        assertTrue(first.signals().stream().anyMatch(x -> x.keyword().equals("additionalProperties")));
        assertTrue(first.signals().stream().anyMatch(x -> x.keyword().equals("minItems")));
        assertTrue(first.signals().stream().allMatch(x -> x.schemaPointer().startsWith("/")
                && (x.instancePointer().isEmpty() || x.instancePointer().startsWith("/"))));
    }

    @Test void rejectsUnsupportedContractAndCannotAcceptCallerSchema() throws Exception {
        JsonNode document = JSON.readTree(validManifest());
        assertThrows(IllegalArgumentException.class, () -> validator.validate("future-schema", document));
        assertThrows(NullPointerException.class, () -> validator.validate(
                ScenarioAuthorityManifestSchemaValidatorV1.SCHEMA_CONTRACT_IDENTIFIER, null));
        assertTrue(java.util.Arrays.stream(ScenarioAuthorityManifestSchemaValidatorV1.class.getConstructors())
                .noneMatch(c -> java.util.Arrays.equals(c.getParameterTypes(), new Class<?>[]{byte[].class})));
    }

    private static String validManifest() {
        return """
                {"format":"qaip-scenario-authority-manifest-v1","schemaVersion":"1.0",
                 "authority":"orders","scenarioIdentityScheme":"qaip-scenario-identity-v1",
                 "scenarios":[
                   {"scenarioKey":"CREATE","title":"Create order","given":["ready"],
                    "when":["create"],"then":["created"],
                    "operationRef":{"identityScheme":"qaip-http-operation-reference-v1",
                      "method":"POST","path":"/orders"},
                    "ruleRefs":[{"authority":"rules","stableRuleKey":"R-1",
                      "identityScheme":"qaip-business-rule-identity-v1"}]},
                   {"scenarioKey":"CANCEL","title":"Cancel order","given":["exists"],
                    "when":["cancel"],"then":["cancelled"],
                    "operationRef":{"identityScheme":"qaip-http-operation-reference-v1",
                      "method":"DELETE","path":"/orders/{id}"},"ruleRefs":[]}
                 ]}
                """;
    }
}
