package ru.kuznetsov.qagraph.extractor.rest;

import java.util.Objects;

public record RestOperationEvidence(
        RestHttpMethod httpMethod,
        String endpointPath,
        String controllerClass,
        String controllerMethod,
        String javaPackage,
        SourceLocation sourceLocation
) {
    public RestOperationEvidence {
        Objects.requireNonNull(httpMethod, "httpMethod");
        requireNonBlank(endpointPath, "endpointPath");
        requireNonBlank(controllerClass, "controllerClass");
        requireNonBlank(controllerMethod, "controllerMethod");
        Objects.requireNonNull(javaPackage, "javaPackage");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
