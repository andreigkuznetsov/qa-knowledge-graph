package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

class ScenarioSourceDeclarationNormalizerTest {
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    private final ScenarioLogicalSourceMemberProcessor processor = new ScenarioLogicalSourceMemberProcessor();
    private final ScenarioLogicalSourceSchemaAdmission admission = new ScenarioLogicalSourceSchemaAdmission();
    private final ScenarioSourceDeclarationNormalizer normalizer = new ScenarioSourceDeclarationNormalizer();

    @Test
    void freezesIdentityContractsAndNormalizesOneScenarioWithoutResolvingReferences() {
        NormalizedManifestDatum manifest = normalized(normalize(member("one", manifest(
                scenario("CREATE", "Create order", "[\"  exact text  \",\"second\"]",
                        "[\"act\"]", "[\"done\"]", rules())))));
        NormalizedScenarioDeclarationOccurrence scenario = manifest.scenarioDeclarations().getFirst();

        assertEquals("qaip-scenario-manifest-occurrence-identity-v1", MANIFEST_OCCURRENCE_IDENTITY_VERSION);
        assertEquals("qaip-scenario-declaration-occurrence-identity-v1",
                DECLARATION_OCCURRENCE_IDENTITY_VERSION);
        assertEquals("qaip-scenario-identity-v1", CLAIMED_SCENARIO_IDENTITY_VERSION);
        assertEquals("qaip-scenario-step-identity-v1", STEP_IDENTITY_VERSION);
        assertEquals(new ClaimedScenarioIdentity("orders", "CREATE", "qaip-scenario-identity-v1"),
                scenario.claimedScenarioIdentity());
        assertEquals("/scenarios/0", scenario.occurrenceIdentity().structuralPath());
        assertNotEquals(scenario.occurrenceIdentity(), scenario.claimedScenarioIdentity());
        assertEquals(List.of("  exact text  ", "second"), scenario.given());
        assertEquals(List.of(StepPhase.GIVEN, StepPhase.GIVEN, StepPhase.WHEN, StepPhase.THEN),
                scenario.steps().stream().map(step -> step.identity().phase()).toList());
        assertEquals(List.of(0, 1, 0, 0),
                scenario.steps().stream().map(step -> step.identity().ordinal()).toList());
        assertEquals("  exact text  ", scenario.steps().getFirst().exactAuthoredText());
        assertEquals("OPERATION_REF", scenario.operationReference().identity().role());
        assertEquals("qaip-http-operation-reference-v1", scenario.operationReference().targetProfile());
        assertEquals("POST", scenario.operationReference().method());
        assertEquals("/api/orders", scenario.operationReference().path());
        assertEquals(List.of("rule-b", "rule-a"), scenario.businessRuleReferences().stream()
                .map(rule -> rule.identity().stableRuleKey()).toList());
        assertEquals(List.of(0, 1), scenario.businessRuleReferences().stream()
                .map(NormalizedBusinessRuleReferenceDatum::authoredArrayPosition).toList());
    }

    @Test
    void claimedIdentityExcludesTitleContentOccurrenceAndReferences() {
        String first = scenario("SAME", "First", "[\"one\"]", "[\"act\"]", "[\"done\"]", "[]");
        String second = scenario("SAME", "Second", "[\"different\"]", "[\"act\"]", "[\"done\"]", rules());
        NormalizedManifestDatum manifest = normalized(normalize(member("multiple", manifest(first + ',' + second))));

        assertEquals(2, manifest.scenarioDeclarations().size());
        assertEquals(manifest.scenarioDeclarations().get(0).claimedScenarioIdentity(),
                manifest.scenarioDeclarations().get(1).claimedScenarioIdentity());
        assertNotEquals(manifest.scenarioDeclarations().get(0).occurrenceIdentity(),
                manifest.scenarioDeclarations().get(1).occurrenceIdentity());
        assertEquals(List.of("/scenarios/0", "/scenarios/1"), manifest.scenarioDeclarations().stream()
                .map(value -> value.occurrenceIdentity().structuralPath()).toList());
    }

    @Test
    void rejectedAndUnattributableMembersPassThroughAndProduceNoDeclarations() {
        String rejected = manifest(scenario("BAD", "Bad", "[]", "[\"act\"]", "[\"done\"]", "[]"));
        ScenarioSchemaAdmissionResult before = admit(
                member("a-admitted", manifest(scenario("GOOD", "Good", "[\"given\"]", "[\"act\"]", "[\"done\"]", "[]"))),
                member("b-rejected", rejected), member("c-unattributable", "not-json"));

        ScenarioSourceNormalizationResult after = normalizer.normalize(before);

        assertInstanceOf(NormalizedManifestDatum.class, after.memberOutcomes().get(0));
        assertSame(before.memberOutcomes().get(1), after.memberOutcomes().get(1));
        assertSame(before.memberOutcomes().get(2), after.memberOutcomes().get(2));
        assertEquals(before.parentIdentity(), after.parentIdentity());
        assertEquals(before.memberOutcomes().stream().map(value -> value.parentMemberRef()
                        .normalizedRepositoryRelativePath()).toList(),
                after.memberOutcomes().stream().map(value -> value.parentMemberRef()
                        .normalizedRepositoryRelativePath()).toList());
    }

    @Test
    void preservesExactParentBindingAndParentAndAuthoredOrdering() {
        ScenarioSourceNormalizationResult result = normalize(
                member("a", manifest(scenario("A", "A", "[\"g1\",\"g2\"]", "[\"w\"]", "[\"t\"]", "[]"))),
                member("b", manifest(scenario("B", "B", "[\"g\"]", "[\"w\"]", "[\"t\"]", "[]"))));

        assertEquals(List.of(".qaip/scenarios/a.scenario.json", ".qaip/scenarios/b.scenario.json"),
                result.memberOutcomes().stream().map(value -> value.parentMemberRef()
                        .normalizedRepositoryRelativePath()).toList());
        NormalizedManifestDatum first = (NormalizedManifestDatum) result.memberOutcomes().getFirst();
        ParentCapturedMemberRef ref = first.parentMemberRef();
        assertEquals(ref.parentSourceId(), first.occurrenceIdentity().parentSourceId());
        assertEquals(ref.parentSnapshotId(), first.occurrenceIdentity().parentSnapshotId());
        assertEquals(ref.parentContentFingerprint(), first.occurrenceIdentity().parentContentFingerprint());
        assertEquals(ref.normalizedRepositoryRelativePath(),
                first.occurrenceIdentity().normalizedRepositoryRelativePath());
        assertEquals(List.of("g1", "g2"), first.scenarioDeclarations().getFirst().given());
    }

    @Test
    void normalizedResultsAndNestedCollectionsAreImmutableAndDeterministic() {
        ScenarioSourceNormalizationResult firstResult = normalize(member("one", manifest(
                scenario("ONE", "One", "[\"g\"]", "[\"w\"]", "[\"t\"]", rules()))));
        ScenarioSourceNormalizationResult secondResult = normalize(member("one", manifest(
                scenario("ONE", "One", "[\"g\"]", "[\"w\"]", "[\"t\"]", rules()))));
        NormalizedManifestDatum first = (NormalizedManifestDatum) firstResult.memberOutcomes().getFirst();

        assertEquals(first, secondResult.memberOutcomes().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> firstResult.memberOutcomes().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.scenarioDeclarations().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> first.scenarioDeclarations().getFirst().given().add("changed"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.scenarioDeclarations().getFirst().businessRuleReferences().clear());
    }

    private ScenarioSourceNormalizationResult normalize(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        return normalizer.normalize(admit(members));
    }

    private ScenarioSchemaAdmissionResult admit(ScenarioManifestStableCaptureResult.CapturedMember... members) {
        return admission.admit(processor.process(candidate(members)));
    }

    private static NormalizedManifestDatum normalized(ScenarioSourceNormalizationResult result) {
        assertEquals(1, result.memberOutcomes().size());
        return assertInstanceOf(NormalizedManifestDatum.class, result.memberOutcomes().getFirst());
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult.CapturedMember... members) {
        ScenarioManifestStableCaptureResult.Completed capture = new ScenarioManifestStableCaptureResult.Completed(
                List.of(members), List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(capture, "repository:orders", "capture:17",
                CONTRACT, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + name + ".scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
    }

    private static String manifest(String scenarios) {
        return "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\"," +
                "\"authority\":\"orders\",\"scenarioIdentityScheme\":\"qaip-scenario-identity-v1\"," +
                "\"scenarios\":[" + scenarios + "]}";
    }

    private static String scenario(String key, String title, String given, String when, String then, String rules) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"" + title + "\",\"given\":" + given +
                ",\"when\":" + when + ",\"then\":" + then +
                ",\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\"," +
                "\"method\":\"POST\",\"path\":\"/api/orders\"},\"ruleRefs\":" + rules + "}";
    }

    private static String rules() {
        return "[{\"authority\":\"policy\",\"stableRuleKey\":\"rule-b\"," +
                "\"identityScheme\":\"qaip-business-rule-identity-v1\"}," +
                "{\"authority\":\"policy\",\"stableRuleKey\":\"rule-a\"," +
                "\"identityScheme\":\"qaip-business-rule-identity-v1\"}]";
    }
}
