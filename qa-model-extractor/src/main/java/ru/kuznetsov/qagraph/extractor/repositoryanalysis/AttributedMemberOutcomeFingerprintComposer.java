package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AttributedMemberOutcomeFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AttributedMemberOutcomeFingerprintEncoder;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AttributedMemberOutcomeFingerprintInput;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ManifestSemanticFingerprint;

import java.util.Objects;
import java.util.Optional;

/** Maps source-owned ADR-014 records into the single Evidence Governance fingerprint domain. */
public final class AttributedMemberOutcomeFingerprintComposer {
    private AttributedMemberOutcomeFingerprintComposer() {
    }

    public static AttributedMemberOutcomeFingerprint fingerprint(
            AttributedMemberSchemaAdmissionOutcome outcome,
            Optional<ScenarioSourceNormalizedRecords.ManifestOccurrenceIdentity> manifestOccurrenceIdentity,
            Optional<ManifestSemanticFingerprint> manifestSemanticFingerprint
    ) {
        return AttributedMemberOutcomeFingerprintEncoder.fingerprint(
                input(outcome, manifestOccurrenceIdentity, manifestSemanticFingerprint));
    }

    static AttributedMemberOutcomeFingerprintInput input(
            AttributedMemberSchemaAdmissionOutcome outcome,
            Optional<ScenarioSourceNormalizedRecords.ManifestOccurrenceIdentity> manifestOccurrenceIdentity,
            Optional<ManifestSemanticFingerprint> manifestSemanticFingerprint
    ) {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(manifestOccurrenceIdentity, "manifestOccurrenceIdentity");
        Objects.requireNonNull(manifestSemanticFingerprint, "manifestSemanticFingerprint");
        ParentCapturedMemberRef parent = outcome.parentMemberRef();
        return new AttributedMemberOutcomeFingerprintInput(
                AttributedMemberOutcomeFingerprintEncoder.ENCODING_IDENTIFIER,
                new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(
                        parent.parentSourceId(), parent.parentSnapshotId(), parent.parentContentFingerprint(),
                        parent.normalizedRepositoryRelativePath(), parent.rawByteLength(),
                        parent.rawMemberFingerprint()),
                outcome.claimedAuthority(),
                outcome.parserContractIdentifier(),
                outcome.attributionContractIdentifier(),
                AttributedMemberOutcomeFingerprintInput.ParseOutcome.PARSED,
                AttributedMemberOutcomeFingerprintInput.AttributionOutcome.ATTRIBUTED,
                outcome.attributionStructuralLocation(),
                outcome.schemaContractIdentifier(),
                switch (outcome.structuralAdmissionState()) {
                    case STRUCTURALLY_ADMITTED ->
                            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_ADMITTED;
                    case STRUCTURALLY_REJECTED ->
                            AttributedMemberOutcomeFingerprintInput.StructuralAdmissionState.STRUCTURALLY_REJECTED;
                },
                outcome.schemaDiagnostics(),
                manifestOccurrenceIdentity.map(AttributedMemberOutcomeFingerprintComposer::manifestIdentity),
                manifestSemanticFingerprint);
    }

    private static AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity manifestIdentity(
            ScenarioSourceNormalizedRecords.ManifestOccurrenceIdentity identity
    ) {
        return new AttributedMemberOutcomeFingerprintInput.ManifestOccurrenceIdentity(
                identity.parentSourceId(), identity.parentSnapshotId(), identity.parentContentFingerprint(),
                identity.normalizedRepositoryRelativePath(), identity.identityVersion());
    }
}
