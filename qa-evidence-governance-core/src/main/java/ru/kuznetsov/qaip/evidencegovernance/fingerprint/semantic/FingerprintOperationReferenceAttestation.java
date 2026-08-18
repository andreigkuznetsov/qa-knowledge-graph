package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Evidence Governance attestation binding an exact Operation-reference input to its fingerprint. */
public final class FingerprintOperationReferenceAttestation {
    private final HttpOperationReferenceSemanticFingerprintInput input;
    private final HttpOperationReferenceSemanticFingerprint fingerprint;

    private FingerprintOperationReferenceAttestation(
            HttpOperationReferenceSemanticFingerprintInput input
    ) {
        this.input = Objects.requireNonNull(input, "input");
        this.fingerprint = HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input);
    }

    public static FingerprintOperationReferenceAttestation create(
            HttpOperationReferenceSemanticFingerprintInput input
    ) {
        return new FingerprintOperationReferenceAttestation(input);
    }

    public HttpOperationReferenceSemanticFingerprintInput input() {
        return input;
    }

    public HttpOperationReferenceSemanticFingerprint fingerprint() {
        return fingerprint;
    }

    boolean isAuthoritativelyBound() {
        return fingerprint.equals(HttpOperationReferenceSemanticFingerprintEncoder.fingerprint(input));
    }
}
