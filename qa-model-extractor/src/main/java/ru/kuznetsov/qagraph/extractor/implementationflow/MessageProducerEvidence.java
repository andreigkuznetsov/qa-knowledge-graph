package ru.kuznetsov.qagraph.extractor.implementationflow;

import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.util.Objects;

public record MessageProducerEvidence(
        RestOperationEvidence operation,
        String ownerClass,
        String ownerMethod,
        String producerField,
        String publishingMethod,
        DependencyInjectionKind injectionKind,
        SourceLocation publishingLocation
) {
    public MessageProducerEvidence {
        Objects.requireNonNull(operation, "operation");
        requireNonBlank(ownerClass, "ownerClass");
        requireNonBlank(ownerMethod, "ownerMethod");
        requireNonBlank(producerField, "producerField");
        requireNonBlank(publishingMethod, "publishingMethod");
        Objects.requireNonNull(injectionKind, "injectionKind");
        Objects.requireNonNull(publishingLocation, "publishingLocation");
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
