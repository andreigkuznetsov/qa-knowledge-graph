package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Objects;

/** Deterministic ADR-015 Manifest composition processing/integrity failure. */
public final class ManifestSemanticCompositionException extends IllegalArgumentException {
    private final Code code;

    public ManifestSemanticCompositionException(Code code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        STRUCTURALLY_UNADMITTED_MANIFEST,
        SCENARIO_SEMANTIC_UNAVAILABLE,
        SCENARIO_ATTESTATION_MISMATCH,
        SCENARIO_MANIFEST_OCCURRENCE_MISMATCH,
        SCENARIO_AUTHORITY_MISMATCH,
        SCENARIO_COUNT_MISMATCH,
        SCENARIO_POSITION_MISMATCH,
        SCENARIO_DECLARATION_SUBSTITUTION,
        UNSUPPORTED_MANIFEST_CONTRACT
    }
}
