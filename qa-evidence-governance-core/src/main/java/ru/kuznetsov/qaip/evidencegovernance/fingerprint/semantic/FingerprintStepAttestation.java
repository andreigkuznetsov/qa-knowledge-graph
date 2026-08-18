package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Evidence Governance attestation binding an exact Step input to its authoritative fingerprint. */
public final class FingerprintStepAttestation {
    private final StepSemanticFingerprintInput input;
    private final StepSemanticFingerprint fingerprint;

    private FingerprintStepAttestation(StepSemanticFingerprintInput input) {
        this.input = Objects.requireNonNull(input, "input");
        this.fingerprint = StepSemanticFingerprintEncoder.fingerprint(input);
    }

    public static FingerprintStepAttestation create(StepSemanticFingerprintInput input) {
        return new FingerprintStepAttestation(input);
    }

    public StepSemanticFingerprintInput input() {
        return input;
    }

    public StepSemanticFingerprint fingerprint() {
        return fingerprint;
    }

    boolean isAuthoritativelyBound() {
        return fingerprint.equals(StepSemanticFingerprintEncoder.fingerprint(input));
    }
}
