package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AttributedMemberOutcomeFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.SemanticProvenanceAttestation;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.SemanticProvenanceOutputReference;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryDerivationReportFingerprintComposerTest {
    private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT =
            new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
                    "qaip-source-snapshot-contract-v1", "qaip-scenario-authority-repository-json-v1",
                    "scenario-authority-repository-discovery-v1", "scenario-authority-repository-path-v1",
                    "unicode-code-point-order-v1", RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

    @Test
    void mapsExactCaptureAndAdmissionWithoutOwningCanonicalSerialization() {
        var parent = candidate(member("attributed", invalidManifest()), member("broken", "not-json"));
        var processing = new ScenarioLogicalSourceMemberProcessor().process(parent);
        var admission = new ScenarioLogicalSourceSchemaAdmission().admit(processing);
        var attributed = assertInstanceOf(
                AttributedMemberSchemaAdmissionOutcome.class, admission.memberOutcomes().getFirst());
        var attributedComposition = AttributedMemberOutcomeFingerprintComposer.fingerprintRejected(attributed);
        var input = attributedComposition.input();
        var output = SemanticProvenanceOutputReference.verified(attributedComposition);
        var evidence = new RepositoryDerivationReportFingerprintComposer.AttributedMemberEvidence(
                attributed, output, SemanticProvenanceAttestation.deriveAttributedMemberOutcome(output));

        var composition = RepositoryDerivationReportFingerprintComposer.compose(
                parent, admission, List.of(evidence));

        assertEquals(2, composition.input().memberOutcomes().size());
        assertInstanceOf(ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic
                        .RepositoryDerivationReportFingerprintInput.AttributedMemberRetained.class,
                composition.input().memberOutcomes().getFirst());
        assertInstanceOf(ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic
                        .RepositoryDerivationReportFingerprintInput.UnattributableMemberRetained.class,
                composition.input().memberOutcomes().get(1));
        assertEquals(1, composition.input().provenance().size());
        assertEquals(composition.fingerprint(),
                ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic
                        .RepositoryDerivationReportFingerprintEncoder.fingerprint(composition.input()));
        assertThrows(IllegalArgumentException.class, () ->
                RepositoryDerivationReportFingerprintComposer.compose(parent, admission, List.of()));
    }

    private static ScenarioRepositoryCaptureSnapshotCandidate candidate(
            ScenarioManifestStableCaptureResult.CapturedMember... members
    ) {
        var capture = new ScenarioManifestStableCaptureResult.Completed(
                List.of(members), List.of(), RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
        return ScenarioRepositoryCaptureSnapshotCandidate.create(
                capture, "repository:orders", "capture:17", CONTRACT,
                ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
    }

    private static ScenarioManifestStableCaptureResult.CapturedMember member(String name, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new ScenarioManifestStableCaptureResult.CapturedMember(
                ".qaip/scenarios/" + name + ".scenario.json", bytes, bytes.length,
                RawSourceMemberFingerprint.calculate(bytes));
    }

    private static String invalidManifest() {
        return """
                {"format":"qaip-scenario-authority-manifest-v1","schemaVersion":"1.0",
                 "authority":"orders","scenarioIdentityScheme":"qaip-scenario-identity-v1","scenarios":[{
                 "scenarioKey":"CREATE","title":"Scenario","given":[],"when":["An action"],
                 "then":["An outcome"],"operationRef":{"identityScheme":"qaip-http-operation-reference-v1",
                 "method":"POST","path":"/api/orders"},"ruleRefs":[]}]}
                """;
    }
}
