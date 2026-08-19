package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Verified typed binding from an attributed-member identity to its completed fingerprint. */
public final class SemanticProvenanceOutputReference {
    public static final String OUTPUT_KIND = SemanticProvenanceIdentityV1.OUTPUT_KIND;

    private final String outputKind;
    private final AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference outputIdentity;
    private final AttributedMemberOutcomeFingerprint fingerprint;
    private final String claimedAuthority;

    private SemanticProvenanceOutputReference(
            String outputKind,
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference outputIdentity,
            AttributedMemberOutcomeFingerprint fingerprint,
            String claimedAuthority
    ) {
        if (!OUTPUT_KIND.equals(Objects.requireNonNull(outputKind, "outputKind"))) {
            throw new IllegalArgumentException("outputKind must be " + OUTPUT_KIND);
        }
        this.outputKind = outputKind;
        this.outputIdentity = Objects.requireNonNull(outputIdentity, "outputIdentity");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.claimedAuthority = Objects.requireNonNull(claimedAuthority, "claimedAuthority");
    }

    public static SemanticProvenanceOutputReference verified(
            AttributedMemberOutcomeFingerprintInput authoritativeInput,
            AttributedMemberOutcomeFingerprint fingerprint
    ) {
        Objects.requireNonNull(authoritativeInput, "authoritativeInput");
        Objects.requireNonNull(fingerprint, "fingerprint");
        AttributedMemberOutcomeFingerprint expected =
                AttributedMemberOutcomeFingerprintEncoder.fingerprint(authoritativeInput);
        if (!expected.equals(fingerprint)) {
            throw new IllegalArgumentException(
                    "attributed-member output fingerprint does not match its authoritative input");
        }
        return new SemanticProvenanceOutputReference(
                OUTPUT_KIND, authoritativeInput.parentMemberReference(), fingerprint,
                authoritativeInput.claimedAuthority());
    }

    public String outputKind() { return outputKind; }
    public AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference outputIdentity() {
        return outputIdentity;
    }
    public AttributedMemberOutcomeFingerprint fingerprint() { return fingerprint; }
    public String claimedAuthority() { return claimedAuthority; }
}
