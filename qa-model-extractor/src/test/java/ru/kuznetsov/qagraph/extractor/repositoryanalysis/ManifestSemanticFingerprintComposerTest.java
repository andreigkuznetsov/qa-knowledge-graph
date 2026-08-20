package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticCompositionOutcomeV1;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

class ManifestSemanticFingerprintComposerTest {
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    @Test
    void validAdmittedManifestEqualsDirectEncoderAndPreservesAuthoredOrder() {
        NormalizedManifestDatum manifest = manifest("one", "orders", "A", "B");
        List<NormalizedScenarioSemanticAttestation> attestations = attestations(manifest);

        ManifestSemanticCompositionResult result = ManifestSemanticFingerprintComposer.compose(
                manifest, attestations);

        assertEquals(ManifestSemanticFingerprintEncoder.fingerprint(result.acceptedInput()),
                result.fingerprint());
        assertEquals(attestations.stream().map(NormalizedScenarioSemanticAttestation::fingerprint).toList(),
                result.acceptedInput().scenarioSemanticFingerprints());
    }

    @Test
    void authoritativeOutcomeMapperUsesVerifiedHandoffAndEvidenceGovernanceAttempt() {
        ScenarioAuthorityNormalizedProcessingV1 handoff = handoff("mapped", "orders", "A", "B");
        NormalizedManifestDatum manifest = (NormalizedManifestDatum) handoff.normalizationResult()
                .memberOutcomes().getFirst();

        ManifestSemanticCompositionOutcomeV1.Composed outcome = assertInstanceOf(
                ManifestSemanticCompositionOutcomeV1.Composed.class,
                new ManifestSemanticOutcomeMapperV1().attempt(handoff, manifest));

        assertEquals(2, outcome.childOutcomes().size());
        assertEquals(outcome.fingerprint(), ManifestSemanticFingerprintEncoder.fingerprint(
                new ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprintInput(
                        ManifestSemanticFingerprintEncoder.ENCODING_IDENTIFIER, manifest.claimedAuthority(),
                        manifest.format(), manifest.schemaVersion(), manifest.scenarioIdentityScheme(),
                        ManifestSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                        outcome.scenarioFingerprints())));
    }

    @Test
    void scenarioFromAnotherManifestIsRejected() {
        NormalizedManifestDatum target = manifest("target", "orders", "A");
        NormalizedScenarioSemanticAttestation foreign = attestations(
                manifest("foreign", "orders", "A")).getFirst();

        assertFailure(ManifestSemanticCompositionException.Code.SCENARIO_MANIFEST_OCCURRENCE_MISMATCH,
                target, List.of(foreign));
    }

    @Test
    void scenarioAuthorityMismatchIsRejected() {
        NormalizedManifestDatum manifest = manifest("one", "orders", "A");
        NormalizedScenarioDeclarationOccurrence changed = withAuthority(
                manifest.scenarioDeclarations().getFirst(), "payments");
        NormalizedScenarioSemanticAttestation attestation =
                NormalizedScenarioSemanticAttestation.create(changed);

        assertFailure(ManifestSemanticCompositionException.Code.SCENARIO_AUTHORITY_MISMATCH,
                manifest, List.of(attestation));
    }

    @Test
    void missingAndExtraScenariosAreRejectedWithoutFingerprint() {
        NormalizedManifestDatum manifest = manifest("one", "orders", "A", "B");
        List<NormalizedScenarioSemanticAttestation> attestations = attestations(manifest);
        assertFailure(ManifestSemanticCompositionException.Code.SCENARIO_COUNT_MISMATCH,
                manifest, List.of(attestations.getFirst()));
        assertFailure(ManifestSemanticCompositionException.Code.SCENARIO_COUNT_MISMATCH,
                manifest, List.of(attestations.getFirst(), attestations.get(1), attestations.getFirst()));
    }

    @Test
    void wrongScenarioPositionAndOrderAreRejected() {
        NormalizedManifestDatum manifest = manifest("one", "orders", "A", "B");
        List<NormalizedScenarioSemanticAttestation> attestations = attestations(manifest);

        assertFailure(ManifestSemanticCompositionException.Code.SCENARIO_POSITION_MISMATCH,
                manifest, List.of(attestations.get(1), attestations.getFirst()));
    }

    @Test
    void substitutedFingerprintCannotBecomeAnAttestation() {
        for (Constructor<?> constructor : NormalizedScenarioSemanticAttestation.class.getDeclaredConstructors()) {
            assertFalse(Modifier.isPublic(constructor.getModifiers()));
        }
        NormalizedManifestDatum manifest = manifest("one", "orders", "A");
        NormalizedScenarioSemanticAttestation attestation = attestations(manifest).getFirst();
        assertEquals(new ScenarioNormalizedSemanticFingerprinter()
                        .fingerprint(attestation.declaration()).fingerprint(),
                attestation.fingerprint());
    }

    @Test
    void unsupportedManifestV1ContractIsRejected() {
        NormalizedManifestDatum admitted = manifest("one", "orders", "A");
        NormalizedManifestDatum unsupported = new NormalizedManifestDatum(
                admitted.sourceAdmission(), admitted.occurrenceIdentity(), admitted.claimedAuthority(),
                "qaip-scenario-authority-manifest-v2", admitted.schemaVersion(),
                admitted.scenarioIdentityScheme(), admitted.scenarioDeclarations());

        assertFailure(ManifestSemanticCompositionException.Code.UNSUPPORTED_MANIFEST_CONTRACT,
                unsupported, attestations(unsupported));
    }

    @Test
    void inputAndSuccessfulResultAreImmutable() {
        NormalizedManifestDatum manifest = manifest("one", "orders", "A", "B");
        List<NormalizedScenarioSemanticAttestation> mutable = new ArrayList<>(attestations(manifest));
        ManifestSemanticCompositionResult result = ManifestSemanticFingerprintComposer.compose(manifest, mutable);
        mutable.clear();

        assertEquals(2, result.acceptedInput().scenarioSemanticFingerprints().size());
        assertThrows(UnsupportedOperationException.class,
                result.acceptedInput().scenarioSemanticFingerprints()::clear);
    }

    private static List<NormalizedScenarioSemanticAttestation> attestations(NormalizedManifestDatum manifest) {
        return manifest.scenarioDeclarations().stream()
                .map(NormalizedScenarioSemanticAttestation::create).toList();
    }

    private static NormalizedManifestDatum manifest(
            String memberName,
            String authority,
            String... keys
    ) {
        String scenarios = String.join(",", java.util.Arrays.stream(keys)
                .map(ManifestSemanticFingerprintComposerTest::scenario).toList());
        String json = "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\","
                + "\"authority\":\"" + authority + "\",\"scenarioIdentityScheme\":"
                + "\"qaip-scenario-identity-v1\",\"scenarios\":[" + scenarios + "]}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        var member = new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + memberName + ".scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
        var capture = new ScenarioManifestStableCaptureResult.Completed(
                List.of(member), List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        var candidate = ScenarioRepositoryCaptureSnapshotCandidate.create(capture, "repository:test", "capture:20",
                CONTRACT, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
        var processed = new ScenarioLogicalSourceMemberProcessor().process(candidate);
        var admitted = new ScenarioLogicalSourceSchemaAdmission().admit(processed);
        return (NormalizedManifestDatum) new ScenarioSourceDeclarationNormalizer()
                .normalize(admitted).memberOutcomes().getFirst();
    }

    private static ScenarioAuthorityNormalizedProcessingV1 handoff(
            String memberName, String authority, String... keys) {
        String scenarios = String.join(",", java.util.Arrays.stream(keys)
                .map(ManifestSemanticFingerprintComposerTest::scenario).toList());
        String json = "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\","
                + "\"authority\":\"" + authority + "\",\"scenarioIdentityScheme\":"
                + "\"qaip-scenario-identity-v1\",\"scenarios\":[" + scenarios + "]}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        var member = new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + memberName + ".scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
        var capture = new ScenarioManifestStableCaptureResult.Completed(
                List.of(member), List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        var candidate = ScenarioRepositoryCaptureSnapshotCandidate.create(capture, "repository:test", "capture:21",
                CONTRACT, ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
        var normalization = new ScenarioSourceDeclarationNormalizer().normalize(
                new ScenarioLogicalSourceSchemaAdmission().admit(
                        new ScenarioLogicalSourceMemberProcessor().process(candidate)));
        return ScenarioAuthorityNormalizedProcessingV1.verified(candidate, normalization,
                ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1());
    }

    private static String scenario(String key) {
        return "{\"scenarioKey\":\"" + key + "\",\"title\":\"Title " + key
                + "\",\"given\":[\"given " + key + "\"],\"when\":[\"act\"],\"then\":[\"done\"],"
                + "\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\","
                + "\"method\":\"POST\",\"path\":\"/api/" + key.toLowerCase() + "\"},\"ruleRefs\":[]}";
    }

    private static NormalizedScenarioDeclarationOccurrence withAuthority(
            NormalizedScenarioDeclarationOccurrence source,
            String authority
    ) {
        ClaimedScenarioIdentity identity = new ClaimedScenarioIdentity(
                authority, source.scenarioKey(), CLAIMED_SCENARIO_IDENTITY_VERSION);
        List<NormalizedScenarioStep> steps = source.steps().stream().map(step -> new NormalizedScenarioStep(
                new ScenarioStepIdentity(identity, step.identity().phase(), step.identity().ordinal(),
                        step.identity().identityVersion()), step.exactAuthoredText())).toList();
        NormalizedOperationReferenceDatum operation = new NormalizedOperationReferenceDatum(
                new OperationReferenceDatumIdentity(identity, source.operationReference().identity().role(),
                        source.operationReference().identity().identityVersion()),
                source.operationReference().targetProfile(), source.operationReference().method(),
                source.operationReference().path());
        List<NormalizedBusinessRuleReferenceDatum> rules = source.businessRuleReferences().stream()
                .map(rule -> new NormalizedBusinessRuleReferenceDatum(
                        new BusinessRuleReferenceDatumIdentity(identity, rule.identity().referencedAuthority(),
                                rule.identity().stableRuleKey(), rule.identity().identityScheme(),
                                rule.identity().identityVersion()), rule.authoredArrayPosition())).toList();
        return new NormalizedScenarioDeclarationOccurrence(source.occurrenceIdentity(), identity,
                source.scenarioKey(), source.title(), source.given(), source.when(), source.then(), steps,
                operation, rules);
    }

    private static void assertFailure(
            ManifestSemanticCompositionException.Code expected,
            NormalizedManifestDatum manifest,
            List<NormalizedScenarioSemanticAttestation> attestations
    ) {
        ManifestSemanticCompositionException failure = assertThrows(
                ManifestSemanticCompositionException.class,
                () -> ManifestSemanticFingerprintComposer.compose(manifest, attestations));
        assertEquals(expected, failure.code());
    }
}
