package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Verified reference to one completed DERIVE_ATTRIBUTED_MEMBER_OUTCOME provenance record. */
public record SemanticProvenanceAttestation(
        SemanticProvenanceFingerprintInput input,
        SemanticProvenanceFingerprint fingerprint
) {
    public SemanticProvenanceAttestation {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(fingerprint, "fingerprint");
        SemanticProvenanceFingerprint expected = SemanticProvenanceFingerprintEncoder.fingerprint(input);
        if (!expected.equals(fingerprint)) {
            throw new IllegalArgumentException("semantic provenance fingerprint does not match its input");
        }
    }

    public static SemanticProvenanceAttestation deriveAttributedMemberOutcome(
            SemanticProvenanceOutputReference output
    ) {
        SemanticProvenanceFingerprintInput input =
                SemanticProvenanceFingerprintInput.deriveAttributedMemberOutcome(output);
        return new SemanticProvenanceAttestation(
                input, SemanticProvenanceFingerprintEncoder.fingerprint(input));
    }
}
