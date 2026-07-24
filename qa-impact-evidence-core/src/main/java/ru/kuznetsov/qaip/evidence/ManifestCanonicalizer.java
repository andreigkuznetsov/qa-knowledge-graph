package ru.kuznetsov.qaip.evidence;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class ManifestCanonicalizer {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    static String fingerprint(FrozenEvidenceManifest m) {
        ManifestCanonicalizer c = new ManifestCanonicalizer();
        c.text(m.contractVersion()); c.text(m.sourceId()); c.snapshot(m.snapshot());
        c.text(m.normalizationVersion()); c.text(m.canonicalizationVersion());
        c.count(m.identityAssertions().size());
        m.identityAssertions().forEach(a -> { c.text(a.assertionId()); c.snapshot(a.snapshot());
            c.text(a.localArtifactId()); c.text(a.nodeType().name());
            if (a.resolution() instanceof ResolvedIdentity r) { c.text("RESOLVED"); c.text(r.identity().value()); }
            else { c.text("UNRESOLVED"); c.text(((UnresolvedIdentity) a.resolution()).reasonCode()); }
            c.text(a.contentFingerprint()); c.text(a.provenanceId()); });
        c.count(m.relationships().size());
        m.relationships().forEach(r -> { c.text(r.datumId()); c.snapshot(r.snapshot());
            c.text(r.sourceLocalId()); c.text(r.targetLocalId()); c.text(r.relationshipType().name());
            c.text(r.sourceNativeType()); c.text(r.normalizationVersion());
            c.text(r.contentFingerprint()); c.text(r.provenanceId()); });
        c.count(m.provenance().size());
        m.provenance().forEach(p -> { c.text(p.provenanceId()); c.text(p.originLocator());
            c.text(p.originHash()); c.text(p.normalizationActivity()); c.text(p.normalizationVersion()); });
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(c.out.toByteArray()));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void snapshot(EvidenceSnapshotRef s) { text(s.sourceId()); text(s.snapshotId()); text(s.contentFingerprint()); }
    private void count(int value) { out.writeBytes(ByteBuffer.allocate(4).putInt(value).array()); }
    private void text(String value) { byte[] bytes = value.getBytes(StandardCharsets.UTF_8); count(bytes.length); out.writeBytes(bytes); }
}
