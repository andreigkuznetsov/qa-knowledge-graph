package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Evidence Governance attestation binding an exact Business Rule-reference input to its fingerprint. */
public final class FingerprintBusinessRuleReferenceAttestation {
    private final BusinessRuleReferenceSemanticFingerprintInput input;
    private final BusinessRuleReferenceSemanticFingerprint fingerprint;

    private FingerprintBusinessRuleReferenceAttestation(
            BusinessRuleReferenceSemanticFingerprintInput input
    ) {
        this.input = Objects.requireNonNull(input, "input");
        this.fingerprint = BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input);
    }

    public static FingerprintBusinessRuleReferenceAttestation create(
            BusinessRuleReferenceSemanticFingerprintInput input
    ) {
        return new FingerprintBusinessRuleReferenceAttestation(input);
    }

    public BusinessRuleReferenceSemanticFingerprintInput input() {
        return input;
    }

    public BusinessRuleReferenceSemanticFingerprint fingerprint() {
        return fingerprint;
    }

    boolean isAuthoritativelyBound() {
        return fingerprint.equals(BusinessRuleReferenceSemanticFingerprintEncoder.fingerprint(input));
    }
}
