package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Objects;

/** Deterministic schema diagnostic; human text is retained as non-authoritative metadata. */
public record ScenarioSchemaDiagnostic(
        Code code,
        String instanceLocation,
        String keyword,
        String machineStableDetail,
        String humanMessage
) {
    public ScenarioSchemaDiagnostic {
        Objects.requireNonNull(code, "code");
        instanceLocation = requireJsonPointer(instanceLocation);
        keyword = requireNonBlank(keyword, "keyword");
        machineStableDetail = requireNonBlank(machineStableDetail, "machineStableDetail");
        humanMessage = requireNonBlank(humanMessage, "humanMessage");
    }

    public enum Code {
        SCHEMA_VIOLATION
    }

    private static String requireJsonPointer(String value) {
        Objects.requireNonNull(value, "instanceLocation");
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException("instanceLocation must be an RFC 6901 JSON Pointer");
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
