package ru.kuznetsov.qagraph.change.verification;

public enum FinalVerificationStage {
    SCHEMA_VALIDATION,
    SEMANTIC_VALIDATION,
    VALIDATION_INFRASTRUCTURE,
    UNSUPPORTED_VERSION,
    FINAL_EVIDENCE_CONSISTENCY
}
