package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.Objects;

public record MessageConsumerEvidence(
        String technology,
        String listenerClass,
        String listenerMethod,
        String destinationName,
        String destinationKind,
        String consumerGroup,
        SourceLocation listenerLocation,
        SourceLocation destinationDeclarationLocation
) {
    public MessageConsumerEvidence {
        requireNonBlank(technology, "technology");
        requireNonBlank(listenerClass, "listenerClass");
        requireNonBlank(listenerMethod, "listenerMethod");
        requireNonBlank(destinationName, "destinationName");
        requireNonBlank(destinationKind, "destinationKind");
        if (consumerGroup != null && consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup must be null or non-blank");
        }
        Objects.requireNonNull(listenerLocation, "listenerLocation");
        Objects.requireNonNull(destinationDeclarationLocation, "destinationDeclarationLocation");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
