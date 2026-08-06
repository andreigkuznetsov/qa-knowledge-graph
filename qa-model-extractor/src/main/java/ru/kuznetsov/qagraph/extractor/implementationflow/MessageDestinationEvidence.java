package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.Objects;

public record MessageDestinationEvidence(
        MessageProducerEvidence producer,
        String technology,
        String destinationName,
        String destinationKind,
        SourceLocation declarationLocation
) {
    public MessageDestinationEvidence {
        Objects.requireNonNull(producer, "producer");
        requireNonBlank(technology, "technology");
        requireNonBlank(destinationName, "destinationName");
        requireNonBlank(destinationKind, "destinationKind");
        Objects.requireNonNull(declarationLocation, "declarationLocation");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
