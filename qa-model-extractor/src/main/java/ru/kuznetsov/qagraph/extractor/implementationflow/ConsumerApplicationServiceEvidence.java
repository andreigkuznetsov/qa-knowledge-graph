package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.Objects;

public record ConsumerApplicationServiceEvidence(
        MessageConsumerEvidence consumer,
        String serviceClass,
        String serviceMethod,
        DependencyInjectionKind injectionKind,
        ImplementationInvocationKind invocationKind,
        SourceLocation invocationLocation,
        SourceLocation serviceMethodLocation
) {
    public ConsumerApplicationServiceEvidence {
        Objects.requireNonNull(consumer, "consumer");
        requireNonBlank(serviceClass, "serviceClass");
        requireNonBlank(serviceMethod, "serviceMethod");
        Objects.requireNonNull(injectionKind, "injectionKind");
        Objects.requireNonNull(invocationKind, "invocationKind");
        Objects.requireNonNull(invocationLocation, "invocationLocation");
        Objects.requireNonNull(serviceMethodLocation, "serviceMethodLocation");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
