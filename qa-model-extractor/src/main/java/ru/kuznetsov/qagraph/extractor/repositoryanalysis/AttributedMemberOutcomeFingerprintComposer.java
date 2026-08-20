package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

import java.util.Objects;
import java.util.Optional;

/** Extractor mapping boundary; Evidence Governance derives admitted Manifest availability. */
public final class AttributedMemberOutcomeFingerprintComposer {
    private AttributedMemberOutcomeFingerprintComposer() {}

    public static AttributedMemberOutcomeComposerV1.Composition fingerprintAdmitted(
            ManifestSemanticCompositionOutcomeV1 manifestOutcome) {
        return AttributedMemberOutcomeComposerV1.composeAdmitted(manifestOutcome);
    }

    public static AttributedMemberOutcomeComposerV1.Composition fingerprintRejected(
            AttributedMemberSchemaAdmissionOutcome outcome) {
        Objects.requireNonNull(outcome);
        if (outcome.structuralAdmissionState()
                != AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_REJECTED)
            throw new IllegalArgumentException("rejected mapping requires STRUCTURALLY_REJECTED source evidence");
        ParentCapturedMemberRef parent=outcome.parentMemberRef();
        var input=new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER,
                new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(parent.parentSourceId(),
                        parent.parentSnapshotId(),parent.parentContentFingerprint(),
                        parent.normalizedRepositoryRelativePath(),parent.rawByteLength(),parent.rawMemberFingerprint()),
                outcome.claimedAuthority(),outcome.parserContractIdentifier(),outcome.attributionContractIdentifier(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                outcome.attributionStructuralLocation(),outcome.schemaContractIdentifier(),
                AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED,
                outcome.schemaDiagnostics(),Optional.empty(),Optional.empty());
        return AttributedMemberOutcomeComposerV1.composeRejected(input);
    }
}
