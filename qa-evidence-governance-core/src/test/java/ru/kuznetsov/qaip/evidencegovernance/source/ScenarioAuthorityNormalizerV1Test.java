package ru.kuznetsov.qaip.evidencegovernance.source;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityNormalizerV1Test {
    private final ScenarioAuthorityExactJsonParserV1 parser = new ScenarioAuthorityExactJsonParserV1();
    private final ScenarioAuthorityAttributorV1 attributor = new ScenarioAuthorityAttributorV1();
    private final ScenarioAuthorityNormalizerV1 normalizer = new ScenarioAuthorityNormalizerV1();

    @Test void normalizesCompleteSemanticsInExactAuthoredOrder() {
        var result = normalize(manifest(scenario("SAME", " First ", "post", "/Path/{ID}",
                "[\"  g1  \",\"g2\"]", "[\"w1\",\"w2\"]", "[\"t\"]", rules()) + "," +
                scenario("SAME", "Second", "GET", "/second", "[\"g\"]", "[\"w\"]", "[\"t\"]", "[]")));
        assertEquals("Orders/V1", result.claimedAuthority());
        assertEquals(List.of(0, 1), result.authoredScenarios().stream().map(s -> s.authoredIndex()).toList());
        assertEquals(List.of("/scenarios/0", "/scenarios/1"), result.authoredScenarios().stream()
                .map(s -> s.structuralPath()).toList());
        assertEquals(result.authoredScenarios().get(0).claimedIdentity(),
                result.authoredScenarios().get(1).claimedIdentity());
        var first = result.authoredScenarios().getFirst();
        assertEquals(" First ", first.title());
        assertEquals(List.of("  g1  ", "g2"), first.given());
        assertEquals(List.of(EvidenceGovernanceNormalizedManifestV1.StepPhase.GIVEN,
                EvidenceGovernanceNormalizedManifestV1.StepPhase.GIVEN,
                EvidenceGovernanceNormalizedManifestV1.StepPhase.WHEN,
                EvidenceGovernanceNormalizedManifestV1.StepPhase.WHEN,
                EvidenceGovernanceNormalizedManifestV1.StepPhase.THEN),
                first.steps().stream().map(s -> s.phase()).toList());
        assertEquals(List.of(0, 1, 0, 1, 0), first.steps().stream().map(s -> s.ordinal()).toList());
        assertEquals("post", first.operationReference().method());
        assertEquals("/Path/{ID}", first.operationReference().path());
        assertEquals(first.claimedIdentity(), first.operationReference().owner());
        assertEquals(List.of("rule-b", "rule-a"), first.businessRuleReferences().stream()
                .map(r -> r.stableRuleKey()).toList());
        assertEquals(List.of(0, 1), first.businessRuleReferences().stream()
                .map(r -> r.authoredPosition()).toList());
        assertEquals("custom-rule-identity-v7", first.businessRuleReferences().getFirst().identityScheme());
    }

    @Test void locksEveryFixedIdentifier() {
        var c = normalize(manifest("")).contracts();
        assertEquals(List.of("scenario-authority-json-parser-v1", "scenario-authority-attribution-v1",
                "qaip-scenario-authority-manifest-schema-v1",
                "sha256:7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1",
                "scenario-authority-schema-diagnostic-mapping-v1", "scenario-authority-source-normalization-v1",
                "qaip-scenario-authority-manifest-v1", "1.0",
                "qaip-scenario-manifest-occurrence-identity-v1",
                "qaip-scenario-declaration-occurrence-identity-v1", "qaip-scenario-identity-v1",
                "scenario-authority-scenario-semantic-c14n-v1", "qaip-scenario-step-identity-v1",
                "scenario-authority-step-semantic-c14n-v1", "OPERATION_REF",
                "qaip-http-operation-reference-v1", "qaip-scenario-operation-reference-datum-identity-v1",
                "scenario-authority-http-operation-reference-semantic-c14n-v1",
                "qaip-scenario-business-rule-reference-datum-identity-v1",
                "scenario-authority-business-rule-reference-semantic-c14n-v1"),
                List.of(c.parserContractIdentifier(), c.attributionContractIdentifier(), c.schemaContractIdentifier(),
                        c.schemaContentIdentity(), c.diagnosticMappingContractIdentifier(), c.sourceNormalizationVersion(),
                        c.manifestFormat(), c.manifestSchemaVersion(), c.manifestOccurrenceIdentityVersion(),
                        c.scenarioDeclarationOccurrenceIdentityVersion(), c.scenarioIdentityScheme(),
                        c.scenarioSemanticVersion(), c.stepIdentityVersion(), c.stepSemanticVersion(), c.operationRole(),
                        c.operationTargetProfile(), c.operationDatumIdentityVersion(), c.operationSemanticVersion(),
                        c.businessRuleDatumIdentityVersion(), c.businessRuleSemanticVersion()));
    }

    @Test void emptyCollectionsAreImmutableAndNormalizationIsDeterministic() {
        var first = normalize(manifest("")); var second = normalize(manifest(""));
        assertEquals(first, second); assertEquals(List.of(), first.authoredScenarios());
        assertThrows(UnsupportedOperationException.class, () -> first.authoredScenarios().add(null));
        var withScenario = normalize(manifest(scenario("ONE", "One", "POST", "/x",
                "[\"g\"]", "[\"w\"]", "[\"t\"]", "[]")));
        assertThrows(UnsupportedOperationException.class,
                () -> withScenario.authoredScenarios().getFirst().steps().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> withScenario.authoredScenarios().getFirst().businessRuleReferences().clear());
    }

    @Test void requiresExactProofBindingAndPinnedSchemaAdmission() {
        var first = parsed(manifest("")); var second = parsed(manifest(""));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeStructurallyAdmitted(
                second, attributor.attribute(first), ScenarioAuthorityNormalizationContractsV1.selectedV1()));
        var invalid = parsed("{\"authority\":\"Orders/V1\"}");
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalizeStructurallyAdmitted(
                invalid, attributor.attribute(invalid), ScenarioAuthorityNormalizationContractsV1.selectedV1()));
        assertThrows(NullPointerException.class, () -> normalizer.normalizeStructurallyAdmitted(null, null, null));
        assertTrue(java.util.Arrays.stream(EvidenceGovernanceNormalizedManifestV1.class.getDeclaredConstructors())
                .noneMatch(c -> java.lang.reflect.Modifier.isPublic(c.getModifiers())));
        assertTrue(java.util.Arrays.stream(ScenarioAuthorityNormalizerV1.class.getMethods())
                .noneMatch(method -> java.util.Arrays.asList(method.getParameterTypes())
                        .contains(com.fasterxml.jackson.databind.JsonNode.class)));
    }

    private EvidenceGovernanceNormalizedManifestV1 normalize(String json) {
        var parsed = parsed(json);
        return normalizer.normalizeStructurallyAdmitted(parsed, attributor.attribute(parsed),
                ScenarioAuthorityNormalizationContractsV1.selectedV1());
    }
    private ScenarioAuthorityParsedJsonV1 parsed(String json) {
        return parser.parseExactBytes(json.getBytes(StandardCharsets.UTF_8));
    }
    private static String manifest(String scenarios) {
        return "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\"," +
                "\"authority\":\"Orders/V1\",\"scenarioIdentityScheme\":\"qaip-scenario-identity-v1\"," +
                "\"scenarios\":[" + scenarios + "]}";
    }
    private static String scenario(String key, String title, String method, String path,
                                   String given, String when, String then, String rules) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"" + title + "\",\"given\":" + given +
                ",\"when\":" + when + ",\"then\":" + then +
                ",\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\"," +
                "\"method\":\"" + method + "\",\"path\":\"" + path + "\"},\"ruleRefs\":" + rules + "}";
    }
    private static String rules() {
        return "[{\"authority\":\"policy\",\"stableRuleKey\":\"rule-b\"," +
                "\"identityScheme\":\"custom-rule-identity-v7\"},{\"authority\":\"policy\"," +
                "\"stableRuleKey\":\"rule-a\",\"identityScheme\":\"qaip-business-rule-identity-v1\"}]";
    }
}
