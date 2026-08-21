package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;
import ru.kuznetsov.qaip.evidencegovernance.source.EvidenceGovernanceNormalizedManifestV1;

/**
 * Immutable proof-bearing source for one claimed-authority Logical Source V2 partition.
 * This is verified construction input, not a Logical V2 canonical input or fingerprint.
 */
public final class VerifiedScenarioAuthorityPartitionSourceV2 {
    private final RepositoryCaptureAttestation capture;
    private final RepositoryDerivationReportFingerprintInput derivationReport;
    private final String claimedAuthority;
    private final List<AttributedMemberOutcomeComposerV1.Composition> attributedMemberOutcomes;
    private final List<ManifestSemanticCompositionOutcomeV1> manifestOutcomes;
    private final List<VerifiedScenarioIdentityGroupV1> scenarioIdentityGroups;

    VerifiedScenarioAuthorityPartitionSourceV2(
            RepositoryCaptureAttestation capture,
            RepositoryDerivationReportFingerprintInput derivationReport,
            String claimedAuthority,
            List<AttributedMemberOutcomeComposerV1.Composition> attributedMemberOutcomes,
            List<ManifestSemanticCompositionOutcomeV1> manifestOutcomes,
            List<VerifiedScenarioIdentityGroupV1> scenarioIdentityGroups
    ) {
        this.capture = Objects.requireNonNull(capture);
        this.derivationReport = Objects.requireNonNull(derivationReport);
        this.claimedAuthority = Objects.requireNonNull(claimedAuthority);
        this.attributedMemberOutcomes = List.copyOf(attributedMemberOutcomes);
        this.manifestOutcomes = List.copyOf(manifestOutcomes);
        this.scenarioIdentityGroups = List.copyOf(scenarioIdentityGroups);
    }

    public RepositoryCaptureAttestation capture() { return capture; }
    public RepositoryDerivationReportFingerprintInput derivationReport() { return derivationReport; }
    public String claimedAuthority() { return claimedAuthority; }
    public List<AttributedMemberOutcomeComposerV1.Composition> attributedMemberOutcomes() {
        return attributedMemberOutcomes;
    }
    public List<ManifestSemanticCompositionOutcomeV1> manifestOutcomes() { return manifestOutcomes; }
    public List<VerifiedScenarioIdentityGroupV1> scenarioIdentityGroups() { return scenarioIdentityGroups; }

    /** Proof-bound admitted Manifests eligible to supply later normalized MANIFEST data. */
    public List<VerifiedAdmittedManifestV1> composedManifests() {
        return manifestOutcomes.stream()
                .filter(ManifestSemanticCompositionOutcomeV1.Composed.class::isInstance)
                .map(ManifestSemanticCompositionOutcomeV1::manifest)
                .toList();
    }

    /** Exact normalized Manifest values eligible for later Logical V2 MANIFEST data. */
    public List<EvidenceGovernanceNormalizedManifestV1> composedNormalizedManifests() {
        return manifestOutcomes.stream()
                .filter(ManifestSemanticCompositionOutcomeV1.Composed.class::isInstance)
                .map(ManifestSemanticCompositionOutcomeV1::manifest)
                .map(VerifiedAdmittedManifestV1::normalizedManifest)
                .toList();
    }

    /** Complete authoritative occurrence evidence, including unavailable occurrences. */
    public List<ScenarioOccurrenceCompositionOutcomeV1> scenarioOccurrenceOutcomes() {
        return manifestOutcomes.stream()
                .flatMap(outcome -> outcome.childOutcomes().stream())
                .toList();
    }
}
