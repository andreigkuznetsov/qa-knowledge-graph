package ru.kuznetsov.qaip.evidence;

import java.util.*;
import java.util.regex.Pattern;

final class ManifestValidator {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    Optional<ImpactAnalysisFailure> validate(FrozenEvidenceManifest m, SliceAnalysisContext context) {
        if (!m.contractVersion().equals(ImpactEvidenceVersions.MANIFEST)
                || !m.canonicalizationVersion().equals(ImpactEvidenceVersions.CANONICAL)
                || !m.normalizationVersion().equals(ImpactEvidenceVersions.NORMALIZATION)
                || !context.qualificationVersion().equals(ImpactEvidenceVersions.QUALIFICATION)
                || !context.influenceVersion().equals(ImpactEvidenceVersions.INFLUENCE)
                || !context.algorithmVersion().equals(ImpactEvidenceVersions.ALGORITHM))
            return failure(FailureCode.UNSUPPORTED_VERSION, "UNSUPPORTED_VERSION", "unsupported evidence semantics version", "");
        if (!m.sourceId().equals(m.snapshot().sourceId()))
            return failure(FailureCode.INVALID_MANIFEST, "SOURCE_MISMATCH", "manifest source differs from snapshot", m.sourceId());
        if (!HASH.matcher(m.snapshot().contentFingerprint()).matches())
            return failure(FailureCode.INVALID_MANIFEST, "INVALID_HASH", "snapshot fingerprint must be lowercase SHA-256", m.snapshot().snapshotId());
        Set<String> assertions = new HashSet<>(), locals = new HashSet<>(), data = new HashSet<>(), prov = new HashSet<>();
        for (ArtifactIdentityAssertion a : m.identityAssertions()) {
            if (!assertions.add(a.assertionId()) || !locals.add(a.localArtifactId()))
                return failure(FailureCode.INVALID_MANIFEST, "DUPLICATE_IDENTITY", "identity assertion IDs and local IDs must be unique", a.assertionId());
            if (!HASH.matcher(a.contentFingerprint()).matches()) return badHash(a.assertionId());
            if (!a.snapshot().equals(m.snapshot()))
                return failure(FailureCode.INVALID_MANIFEST, "ASSERTION_SNAPSHOT_MISMATCH", "assertion belongs to another snapshot", a.assertionId());
        }
        for (RelationshipEvidence r : m.relationships()) {
            if (!data.add(r.datumId())) return failure(FailureCode.INVALID_MANIFEST, "DUPLICATE_DATUM", "datum IDs must be unique", r.datumId());
            if (!HASH.matcher(r.contentFingerprint()).matches()) return badHash(r.datumId());
            if (!locals.contains(r.sourceLocalId()) || !locals.contains(r.targetLocalId()))
                return failure(FailureCode.INVALID_MANIFEST, "BROKEN_ENDPOINT_REFERENCE", "relationship endpoint does not resolve", r.datumId());
        }
        for (ProvenanceRef p : m.provenance()) {
            if (!prov.add(p.provenanceId())) return failure(FailureCode.INVALID_MANIFEST, "DUPLICATE_PROVENANCE", "provenance IDs must be unique", p.provenanceId());
            if (!HASH.matcher(p.originHash()).matches()) return badHash(p.provenanceId());
        }
        for (ArtifactIdentityAssertion a : m.identityAssertions()) if (!prov.contains(a.provenanceId()))
            return failure(FailureCode.INVALID_MANIFEST, "BROKEN_ASSERTION_PROVENANCE", "assertion provenance does not resolve", a.assertionId());
        String expected = ManifestCanonicalizer.fingerprint(m);
        if (!expected.equals(m.manifestFingerprint()))
            return failure(FailureCode.INTEGRITY_MISMATCH, "MANIFEST_FINGERPRINT_MISMATCH", "manifest semantic fingerprint does not match", m.sourceId());
        return Optional.empty();
    }
    private Optional<ImpactAnalysisFailure> badHash(String id) { return failure(FailureCode.INVALID_MANIFEST, "INVALID_HASH", "fingerprint must be lowercase SHA-256", id); }
    private Optional<ImpactAnalysisFailure> failure(FailureCode f, String c, String m, String id) {
        return Optional.of(new ImpactAnalysisFailure(f, List.of(new AnalysisDiagnostic(c, m, id))));
    }
}
