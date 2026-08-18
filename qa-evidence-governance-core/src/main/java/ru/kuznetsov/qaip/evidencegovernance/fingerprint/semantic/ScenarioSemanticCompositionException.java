package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Deterministic ADR-015 Scenario composition processing/integrity failure. */
public final class ScenarioSemanticCompositionException extends IllegalArgumentException {
    private final Code code;

    public ScenarioSemanticCompositionException(Code code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        UNSUPPORTED_CONTRACT,
        STEP_ATTESTATION_MISMATCH,
        STEP_SCENARIO_IDENTITY_MISMATCH,
        STEP_PHASE_MISMATCH,
        STEP_ORDINAL_MISMATCH,
        OPERATION_REFERENCE_ATTESTATION_MISMATCH,
        OPERATION_REFERENCE_SCENARIO_IDENTITY_MISMATCH,
        BUSINESS_RULE_REFERENCE_ATTESTATION_MISMATCH,
        BUSINESS_RULE_REFERENCE_SCENARIO_IDENTITY_MISMATCH,
        BUSINESS_RULE_REFERENCE_POSITION_MISMATCH
    }
}
