package ru.kuznetsov.qaip.evidence;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable, canonically ordered normalized evidence captured from one snapshot. */
public record FrozenEvidenceManifest(String contractVersion, String sourceId,
        EvidenceSnapshotRef snapshot, String normalizationVersion,
        String canonicalizationVersion, String manifestFingerprint,
        List<ArtifactIdentityAssertion> identityAssertions,
        List<RelationshipEvidence> relationships, List<ProvenanceRef> provenance) {
    public FrozenEvidenceManifest {
        EvidenceSnapshotRef.requireText(contractVersion, "contractVersion");
        EvidenceSnapshotRef.requireText(sourceId, "sourceId");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        EvidenceSnapshotRef.requireText(normalizationVersion, "normalizationVersion");
        EvidenceSnapshotRef.requireText(canonicalizationVersion, "canonicalizationVersion");
        EvidenceSnapshotRef.requireText(manifestFingerprint, "manifestFingerprint");
        identityAssertions = List.copyOf(Objects.requireNonNull(identityAssertions)).stream()
                .sorted(Comparator.comparing(ArtifactIdentityAssertion::assertionId)).toList();
        relationships = List.copyOf(Objects.requireNonNull(relationships)).stream()
                .sorted(Comparator.comparing(RelationshipEvidence::datumId)).toList();
        provenance = List.copyOf(Objects.requireNonNull(provenance)).stream()
                .sorted(Comparator.comparing(ProvenanceRef::provenanceId)).toList();
    }

    /** Creates a supported manifest and computes its semantic fingerprint. */
    public static FrozenEvidenceManifest create(String sourceId, EvidenceSnapshotRef snapshot,
            List<ArtifactIdentityAssertion> assertions, List<RelationshipEvidence> relationships,
            List<ProvenanceRef> provenance) {
        FrozenEvidenceManifest unsigned = new FrozenEvidenceManifest(
                ImpactEvidenceVersions.MANIFEST, sourceId, snapshot,
                ImpactEvidenceVersions.NORMALIZATION, ImpactEvidenceVersions.CANONICAL,
                "pending", assertions, relationships, provenance);
        return new FrozenEvidenceManifest(unsigned.contractVersion, unsigned.sourceId,
                unsigned.snapshot, unsigned.normalizationVersion, unsigned.canonicalizationVersion,
                ManifestCanonicalizer.fingerprint(unsigned), unsigned.identityAssertions,
                unsigned.relationships, unsigned.provenance);
    }
}
