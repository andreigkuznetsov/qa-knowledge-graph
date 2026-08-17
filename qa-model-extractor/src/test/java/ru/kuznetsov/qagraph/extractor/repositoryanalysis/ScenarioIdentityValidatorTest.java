package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioIdentityValidatorTest {
    private final ScenarioManifestJsonParser parser = new ScenarioManifestJsonParser();
    private final ScenarioManifestSchemaValidator schemaValidator = new ScenarioManifestSchemaValidator();
    private final ScenarioIdentityValidator identityValidator = new ScenarioIdentityValidator();

    @Test
    void acceptsUniqueScenariosWithinOneAuthority() {
        ScenarioIdentityValidationResult result = validate(
                member("unique", manifest("order-scenarios", "CREATE-ORDER", "CANCEL-ORDER")));

        assertEquals(List.of("CANCEL-ORDER", "CREATE-ORDER"),
                result.identities().stream().map(ScenarioIdentityValidationResult.IdentityGroup::scenarioKey).toList());
        assertTrue(result.identities().stream().allMatch(ScenarioIdentityValidationResult.IdentityGroup::unique));
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void reportsDuplicateKeyInOneFileWithEveryDeclarationLocation() {
        ScenarioIdentityValidationResult result = validate(
                member("same-file", manifest("order-scenarios", "CREATE-ORDER", "CREATE-ORDER")));

        ScenarioIdentityValidationResult.DuplicateDiagnostic diagnostic = result.diagnostics().getFirst();
        assertEquals("order-scenarios", diagnostic.authority());
        assertEquals("CREATE-ORDER", diagnostic.scenarioKey());
        assertEquals(List.of("/scenarios/0", "/scenarios/1"),
                diagnostic.declarations().stream()
                        .map(ScenarioIdentityValidationResult.DeclarationSource::instanceLocation)
                        .toList());
    }

    @Test
    void reportsDuplicateKeyAcrossFilesWithoutChoosingWinner() {
        ScenarioIdentityValidationResult result = validate(
                member("z-file", manifest("order-scenarios", "CREATE-ORDER")),
                member("a-file", manifest("order-scenarios", "CREATE-ORDER")));

        assertEquals(1, result.identities().size());
        assertFalse(result.identities().getFirst().unique());
        assertEquals(List.of(
                        ".qaip/scenarios/a-file.scenario.json",
                        ".qaip/scenarios/z-file.scenario.json"),
                result.diagnostics().getFirst().declarations().stream()
                        .map(ScenarioIdentityValidationResult.DeclarationSource::repositoryRelativePath)
                        .toList());
    }

    @Test
    void sameScenarioKeyUnderDifferentAuthoritiesRemainsIndependent() {
        ScenarioIdentityValidationResult result = validate(
                member("one", manifest("authority-one", "CREATE-ORDER")),
                member("two", manifest("authority-two", "CREATE-ORDER")));

        assertEquals(2, result.identities().size());
        assertTrue(result.identities().stream().allMatch(ScenarioIdentityValidationResult.IdentityGroup::unique));
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void retainsMultipleDuplicateGroups() {
        ScenarioIdentityValidationResult result = validate(
                member("first", manifest("orders", "CREATE", "CANCEL", "CREATE", "CANCEL")),
                member("second", manifest("payments", "PAY", "PAY")));

        assertEquals(3, result.diagnostics().size());
        assertEquals(List.of("orders:CANCEL", "orders:CREATE", "payments:PAY"),
                result.diagnostics().stream()
                        .map(value -> value.authority() + ':' + value.scenarioKey())
                        .toList());
    }

    @Test
    void diagnosticOrderingIsCodePointLexicalAndSourceOrderingIsStable() {
        ScenarioIdentityValidationResult result = validate(
                member("z", manifest("z-authority", "B", "B")),
                member("second", manifest("a-authority", "Z", "A", "Z", "A")),
                member("a", manifest("z-authority", "B")));

        assertEquals(List.of("a-authority:A", "a-authority:Z", "z-authority:B"),
                result.diagnostics().stream()
                        .map(value -> value.authority() + ':' + value.scenarioKey())
                        .toList());
        assertEquals(List.of(
                        ".qaip/scenarios/a.scenario.json",
                        ".qaip/scenarios/z.scenario.json",
                        ".qaip/scenarios/z.scenario.json"),
                result.diagnostics().get(2).declarations().stream()
                        .map(ScenarioIdentityValidationResult.DeclarationSource::repositoryRelativePath)
                        .toList());
    }

    @Test
    void structurallyInvalidMemberIsExcludedFromIdentityFormation() {
        String invalid = manifest("orders", "CREATE")
                .replace("\"authority\":\"orders\",", "\"authority\":\"orders\",\"unknown\":true,");
        ScenarioManifestSchemaValidationResult schemaResult = schemaValidated(
                member("valid", manifest("orders", "CREATE")),
                member("invalid", invalid));

        ScenarioIdentityValidationResult result = identityValidator.validate(schemaResult);

        assertEquals(1, result.identities().size());
        assertTrue(result.identities().getFirst().unique());
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(List.of(".qaip/scenarios/invalid.scenario.json"), result.excludedMemberPaths());
    }

    @Test
    void resultAndDeclarationDocumentsAreImmutable() {
        ScenarioIdentityValidationResult result = validate(
                member("immutable", manifest("orders", "CREATE", "CREATE")));
        ObjectNode exposed = (ObjectNode) result.identities().getFirst().declarations().getFirst().document();
        exposed.put("scenarioKey", "CHANGED");

        assertEquals("CREATE",
                result.identities().getFirst().declarations().getFirst().document().path("scenarioKey").asText());
        assertThrows(UnsupportedOperationException.class, () -> result.identities().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.diagnostics().getFirst().declarations().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.excludedMemberPaths().clear());
    }

    private ScenarioIdentityValidationResult validate(
            ScenarioManifestCaptureResult.CapturedMember... members) {
        return identityValidator.validate(schemaValidated(members));
    }

    private ScenarioManifestSchemaValidationResult schemaValidated(
            ScenarioManifestCaptureResult.CapturedMember... members) {
        ScenarioManifestJsonParseResult.Completed parsed = assertInstanceOf(
                ScenarioManifestJsonParseResult.Completed.class,
                parser.parse(new ScenarioManifestCaptureResult.Completed(List.of(members))));
        return schemaValidator.validate(parsed);
    }

    private ScenarioManifestCaptureResult.CapturedMember member(String name, String json) {
        return new ScenarioManifestCaptureResult.CapturedMember(
                Path.of(name + ".scenario.json"),
                ".qaip/scenarios/" + name + ".scenario.json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static String manifest(String authority, String... scenarioKeys) {
        String scenarios = java.util.Arrays.stream(scenarioKeys)
                .map(ScenarioIdentityValidatorTest::scenario)
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "format":"qaip-scenario-authority-manifest-v1",
                  "schemaVersion":"1.0",
                  "authority":"%s",
                  "scenarioIdentityScheme":"qaip-scenario-identity-v1",
                  "scenarios":[%s]
                }
                """.formatted(authority, scenarios);
    }

    private static String scenario(String key) {
        return """
                {
                  "scenarioKey":"%s",
                  "title":"Scenario %s",
                  "given":["A precondition"],
                  "when":["An action occurs"],
                  "then":["An outcome occurs"],
                  "operationRef":{
                    "identityScheme":"qaip-http-operation-reference-v1",
                    "method":"POST",
                    "path":"/api/orders"
                  },
                  "ruleRefs":[]
                }
                """.formatted(key, key);
    }
}
