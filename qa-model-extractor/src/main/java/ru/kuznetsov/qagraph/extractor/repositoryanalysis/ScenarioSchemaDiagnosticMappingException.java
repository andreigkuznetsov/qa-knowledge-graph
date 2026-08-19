package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Objects;

/** Fail-closed processing/compatibility failure outside ordinary schema diagnostics. */
public final class ScenarioSchemaDiagnosticMappingException extends RuntimeException {
    public static final String FAILURE_CODE = "UNSUPPORTED_SCHEMA_DIAGNOSTIC_MAPPING";

    private final String failureCode;

    ScenarioSchemaDiagnosticMappingException(String reason) {
        super(Objects.requireNonNull(reason, "reason"));
        this.failureCode = FAILURE_CODE;
    }

    ScenarioSchemaDiagnosticMappingException(String reason, Throwable cause) {
        super(Objects.requireNonNull(reason, "reason"), Objects.requireNonNull(cause, "cause"));
        this.failureCode = FAILURE_CODE;
    }

    public String failureCode() {
        return failureCode;
    }
}
