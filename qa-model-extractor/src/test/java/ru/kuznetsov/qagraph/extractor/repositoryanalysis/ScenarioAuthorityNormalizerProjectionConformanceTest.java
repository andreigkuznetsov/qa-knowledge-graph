package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import ru.kuznetsov.qaip.evidencegovernance.source.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

class ScenarioAuthorityNormalizerProjectionConformanceTest {
    @Test void provesCompleteModelBSemanticProjectionOrderAndContractSharing() throws Exception {
        String source = manifest(
                scenario("ZETA", "Middle title", "post", "/Z/{Id}",
                        "[\" g-z1 \",\"g-z2\"]", "[\"w-z\"]", "[\"t-z\"]",
                        rules("rule-z2", "custom-rule-identity-v7", "rule-z1")),
                scenario("ALPHA", "Zulu title", "GET", "/alpha",
                        "[\"g-a\"]", "[\"w-a1\",\"w-a2\"]", "[\"t-a\"]", "[]"),
                scenario("MU", "Alpha title", "Patch", "/Mu Path",
                        "[\"g-m\"]", "[\"w-m\"]", "[\"t-m1\",\"t-m2\"]",
                        rules("rule-m2", "qaip-business-rule-identity-v1", "rule-m1")));
        ScenarioRepositoryCaptureSnapshotCandidate parent = candidate(source);
        var processing = new ScenarioLogicalSourceMemberProcessor().process(parent);
        var admissions = new ScenarioLogicalSourceSchemaAdmission().admit(processing);
        var admission = assertInstanceOf(AttributedMemberSchemaAdmissionOutcome.class,
                admissions.memberOutcomes().getFirst());

        ScenarioAuthorityNormalizationContractsV1 selected = ScenarioAuthorityNormalizationContractsV1.selectedV1();
        EvidenceGovernanceNormalizedManifestV1 authoritative = new ScenarioAuthorityNormalizerV1()
                .normalizeStructurallyAdmitted(admission.authoritativeParsedJson(),
                        admission.authoritativeAttribution(), selected);
        ScenarioSourceNormalizationResult compatibilityResult =
                new ScenarioSourceDeclarationNormalizer().normalize(admissions);
        NormalizedManifestDatum compatibility = assertInstanceOf(NormalizedManifestDatum.class,
                compatibilityResult.memberOutcomes().getFirst());
        ScenarioAuthorityNormalizedProcessingV1 handoff = ScenarioAuthorityNormalizedProcessingV1.verified(
                parent, compatibilityResult, ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1());

        assertManifest(authoritative, compatibility);
        assertScenarios(authoritative, compatibility, handoff);
        assertSharedContracts(authoritative, compatibility, handoff);
        assertProofMetadataExclusions(authoritative, compatibility);
        assertAntiDriftSources();
    }

    private static void assertManifest(EvidenceGovernanceNormalizedManifestV1 eg, NormalizedManifestDatum ex) {
        assertEquals(eg.claimedAuthority(), ex.claimedAuthority());
        assertEquals(eg.format(), ex.format());
        assertEquals(eg.schemaVersion(), ex.schemaVersion());
        assertEquals(eg.scenarioIdentityScheme(), ex.scenarioIdentityScheme());
        assertEquals(eg.authoredScenarios().size(), ex.scenarioDeclarations().size());
        assertEquals(eg.contracts().manifestOccurrenceIdentityVersion(), ex.occurrenceIdentity().identityVersion());
    }

    private static void assertScenarios(EvidenceGovernanceNormalizedManifestV1 eg, NormalizedManifestDatum ex,
                                        ScenarioAuthorityNormalizedProcessingV1 handoff) {
        List<String> expectedKeys = List.of("ZETA", "ALPHA", "MU");
        List<String> expectedTitles = List.of("Middle title", "Zulu title", "Alpha title");
        List<String> expectedPaths = List.of("/scenarios/0", "/scenarios/1", "/scenarios/2");
        List<String> expectedIdentities = List.of("Orders/V1:ZETA", "Orders/V1:ALPHA", "Orders/V1:MU");
        assertEquals(expectedKeys, eg.authoredScenarios().stream().map(s -> s.scenarioKey()).toList());
        assertEquals(expectedTitles, eg.authoredScenarios().stream().map(s -> s.title()).toList());
        assertEquals(List.of(0, 1, 2), eg.authoredScenarios().stream().map(s -> s.authoredIndex()).toList());
        assertEquals(expectedPaths, eg.authoredScenarios().stream().map(s -> s.structuralPath()).toList());
        assertEquals(expectedIdentities, eg.authoredScenarios().stream()
                .map(s -> s.claimedIdentity().authority() + ":" + s.claimedIdentity().scenarioKey()).toList());
        assertEquals(expectedKeys, ex.scenarioDeclarations().stream().map(s -> s.scenarioKey()).toList());
        assertEquals(expectedTitles, ex.scenarioDeclarations().stream().map(s -> s.title()).toList());
        assertEquals(expectedPaths, ex.scenarioDeclarations().stream()
                .map(s -> s.occurrenceIdentity().structuralPath()).toList());
        assertEquals(expectedIdentities, ex.scenarioDeclarations().stream().map(s ->
                s.claimedScenarioIdentity().authority() + ":" + s.claimedScenarioIdentity().scenarioKey()).toList());
        assertEquals(expectedKeys, handoff.occurrences().stream().map(o -> o.declaration().scenarioKey()).toList());
        assertEquals(expectedTitles, handoff.occurrences().stream().map(o -> o.declaration().title()).toList());
        assertEquals(expectedPaths, handoff.occurrences().stream()
                .map(o -> o.declaration().occurrenceIdentity().structuralPath()).toList());
        assertEquals(expectedIdentities, handoff.occurrences().stream().map(o ->
                o.declaration().claimedScenarioIdentity().authority() + ":"
                        + o.declaration().claimedScenarioIdentity().scenarioKey()).toList());

        for (int index = 0; index < eg.authoredScenarios().size(); index++) {
            var a = eg.authoredScenarios().get(index);
            var b = ex.scenarioDeclarations().get(index);
            assertEquals(index, a.authoredIndex());
            assertEquals(index, Integer.parseInt(b.occurrenceIdentity().structuralPath()
                    .substring("/scenarios/".length())));
            assertEquals(a.structuralPath(), b.occurrenceIdentity().structuralPath());
            assertEquals(a.declarationOccurrenceIdentityVersion(), b.occurrenceIdentity().identityVersion());
            assertEquals(eg.contracts().scenarioDeclarationOccurrenceIdentityVersion(),
                    b.occurrenceIdentity().identityVersion());
            assertIdentity(a.claimedIdentity(), b.claimedScenarioIdentity());
            assertEquals(eg.contracts().scenarioIdentityScheme(), b.claimedScenarioIdentity().identityScheme());
            assertEquals(a.scenarioKey(), b.scenarioKey());
            assertEquals(a.title(), b.title());
            assertEquals(a.given(), b.given()); assertEquals(a.when(), b.when()); assertEquals(a.then(), b.then());
            assertSteps(a, b); assertOperation(a, b); assertRules(a, b);
        }
        assertNotEquals(expectedKeys.stream().sorted().toList(), expectedKeys);
        assertNotEquals(expectedTitles.stream().sorted().toList(), expectedTitles);
    }

    private static void assertSteps(EvidenceGovernanceNormalizedManifestV1.Scenario a,
                                    NormalizedScenarioDeclarationOccurrence b) {
        assertEquals(a.steps().size(), b.steps().size());
        for (int i = 0; i < a.steps().size(); i++) {
            var x = a.steps().get(i); var y = b.steps().get(i);
            assertIdentity(x.owner(), y.identity().claimedScenarioIdentity());
            assertEquals(x.phase().name(), y.identity().phase().name());
            assertEquals(x.ordinal(), y.identity().ordinal());
            assertEquals(x.identityVersion(), y.identity().identityVersion());
            assertEquals(ScenarioAuthorityNormalizationContractsV1.selectedV1().stepIdentityVersion(),
                    y.identity().identityVersion());
            assertEquals(x.exactAuthoredText(), y.exactAuthoredText());
        }
    }

    private static void assertOperation(EvidenceGovernanceNormalizedManifestV1.Scenario a,
                                        NormalizedScenarioDeclarationOccurrence b) {
        var x = a.operationReference(); var y = b.operationReference();
        assertIdentity(x.owner(), y.identity().claimedScenarioIdentity());
        assertEquals(x.role(), y.identity().role());
        assertEquals(ScenarioAuthorityNormalizationContractsV1.selectedV1().operationRole(), y.identity().role());
        assertEquals(x.datumIdentityVersion(), y.identity().identityVersion());
        assertEquals(ScenarioAuthorityNormalizationContractsV1.selectedV1().operationDatumIdentityVersion(),
                y.identity().identityVersion());
        assertEquals(x.targetProfile(), y.targetProfile());
        assertEquals(ScenarioAuthorityNormalizationContractsV1.selectedV1().operationTargetProfile(),
                y.targetProfile());
        assertEquals(x.method(), y.method()); assertEquals(x.path(), y.path());
    }

    private static void assertRules(EvidenceGovernanceNormalizedManifestV1.Scenario a,
                                    NormalizedScenarioDeclarationOccurrence b) {
        assertEquals(a.businessRuleReferences().size(), b.businessRuleReferences().size());
        for (int i = 0; i < a.businessRuleReferences().size(); i++) {
            var x = a.businessRuleReferences().get(i); var y = b.businessRuleReferences().get(i);
            assertEquals(x.authoredPosition(), y.authoredArrayPosition());
            assertIdentity(x.owner(), y.identity().claimedScenarioIdentity());
            assertEquals(x.referencedAuthority(), y.identity().referencedAuthority());
            assertEquals(x.stableRuleKey(), y.identity().stableRuleKey());
            assertEquals(x.identityScheme(), y.identity().identityScheme());
            assertEquals(x.datumIdentityVersion(), y.identity().identityVersion());
            assertEquals(ScenarioAuthorityNormalizationContractsV1.selectedV1()
                    .businessRuleDatumIdentityVersion(), y.identity().identityVersion());
        }
    }

    private static void assertSharedContracts(EvidenceGovernanceNormalizedManifestV1 eg, NormalizedManifestDatum ex,
                                              ScenarioAuthorityNormalizedProcessingV1 handoff) {
        var c = eg.contracts(); var h = handoff.contracts(); var admission = ex.sourceAdmission();
        assertEquals(c.parserContractIdentifier(), admission.parserContractIdentifier());
        assertEquals(c.attributionContractIdentifier(), admission.attributionContractIdentifier());
        assertEquals(c.schemaContractIdentifier(), admission.schemaContractIdentifier());
        assertEquals(c.sourceNormalizationVersion(), h.sourceNormalizationVersion());
        assertEquals(c.scenarioSemanticVersion(), h.scenarioSemanticCanonicalizationVersion());
        assertEquals(c.stepSemanticVersion(), h.stepSemanticCanonicalizationVersion());
        assertEquals(c.operationSemanticVersion(), h.operationReferenceSemanticCanonicalizationVersion());
        assertEquals(c.businessRuleSemanticVersion(), h.businessRuleReferenceSemanticCanonicalizationVersion());
        for (var scenario : eg.authoredScenarios()) {
            assertEquals(c.scenarioSemanticVersion(), scenario.scenarioSemanticVersion());
            scenario.steps().forEach(step -> assertEquals(c.stepSemanticVersion(), step.semanticVersion()));
            assertEquals(c.operationSemanticVersion(), scenario.operationReference().semanticVersion());
            scenario.businessRuleReferences().forEach(rule ->
                    assertEquals(c.businessRuleSemanticVersion(), rule.semanticVersion()));
        }
        assertEquals(ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER, c.scenarioSemanticVersion());
        assertEquals(StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER, c.stepSemanticVersion());
        assertEquals(HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                c.operationSemanticVersion());
        assertEquals(BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                c.businessRuleSemanticVersion());
    }

    private static void assertProofMetadataExclusions(EvidenceGovernanceNormalizedManifestV1 eg,
                                                      NormalizedManifestDatum ex) {
        assertEquals("sha256:7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1",
                eg.contracts().schemaContentIdentity());
        assertEquals("scenario-authority-schema-diagnostic-mapping-v1",
                eg.contracts().diagnosticMappingContractIdentifier());
        List<String> legacyFields = Arrays.stream(NormalizedManifestDatum.class.getRecordComponents())
                .map(component -> component.getName()).toList();
        assertFalse(legacyFields.contains("contracts"));
        assertFalse(legacyFields.contains("schemaContentIdentity"));
        assertFalse(legacyFields.contains("diagnosticMappingContractIdentifier"));
        assertSame(ex.sourceAdmission().authoritativeParsedJson(),
                ex.sourceAdmission().authoritativeAttribution().parsedJson());
    }

    private static void assertAntiDriftSources() throws Exception {
        Path root = repositoryRoot();
        String handoff = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioAuthorityNormalizedProcessingV1.java"));
        for (String literal : List.of("scenario-authority-source-normalization-v1",
                "scenario-authority-scenario-semantic-c14n-v1",
                "scenario-authority-step-semantic-c14n-v1",
                "scenario-authority-http-operation-reference-semantic-c14n-v1",
                "scenario-authority-business-rule-reference-semantic-c14n-v1"))
            assertFalse(handoff.contains("\"" + literal + "\""), literal);
        assertTrue(handoff.contains("ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION"));
        assertTrue(handoff.contains("StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER"));
        String aliases = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioSourceNormalizedRecords.java"));
        assertTrue(aliases.contains("ScenarioAuthorityNormalizationContractsV1"));
        String mapper = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioOccurrenceInputMapperV1.java"));
        assertTrue(mapper.contains("contracts.stepSemanticCanonicalizationVersion()"));
        assertTrue(mapper.contains("contracts.operationReferenceSemanticCanonicalizationVersion()"));
        assertTrue(mapper.contains("contracts.businessRuleReferenceSemanticCanonicalizationVersion()"));
        String legacyFingerprinter = Files.readString(root.resolve("qa-model-extractor/src/main/java/ru/kuznetsov/"
                + "qagraph/extractor/repositoryanalysis/ScenarioNormalizedSemanticFingerprinter.java"));
        assertTrue(legacyFingerprinter.contains("StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER"));
        assertTrue(legacyFingerprinter.contains("HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER"));
        assertTrue(legacyFingerprinter.contains("BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER"));
    }

    private static void assertIdentity(EvidenceGovernanceNormalizedManifestV1.ClaimedScenarioIdentity a,
                                       ClaimedScenarioIdentity b) {
        assertEquals(a.authority(), b.authority()); assertEquals(a.scenarioKey(), b.scenarioKey());
        assertEquals(a.identityScheme(), b.identityScheme());
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(String source) {
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        var member = new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/order.scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
        var capture = new ScenarioManifestStableCaptureResult.Completed(List.of(member), List.of(),
                RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        var contracts = new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(capture, "repository:orders", "capture:projection",
                contracts, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static String manifest(String... scenarios) {
        return "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\"," +
                "\"authority\":\"Orders/V1\",\"scenarioIdentityScheme\":\"qaip-scenario-identity-v1\"," +
                "\"scenarios\":[" + String.join(",", scenarios) + "]}";
    }
    private static String scenario(String key, String title, String method, String path,
                                   String given, String when, String then, String rules) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"" + title + "\",\"given\":" + given +
                ",\"when\":" + when + ",\"then\":" + then +
                ",\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\"," +
                "\"method\":\"" + method + "\",\"path\":\"" + path + "\"},\"ruleRefs\":" + rules + "}";
    }
    private static String rules(String firstKey, String firstScheme, String secondKey) {
        return "[{\"authority\":\"Policy/Z\",\"stableRuleKey\":\"" + firstKey + "\"," +
                "\"identityScheme\":\"" + firstScheme + "\"},{\"authority\":\"Policy/A\"," +
                "\"stableRuleKey\":\"" + secondKey + "\"," +
                "\"identityScheme\":\"qaip-business-rule-identity-v1\"}]";
    }
    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
        return java.util.Objects.requireNonNull(current);
    }
}
