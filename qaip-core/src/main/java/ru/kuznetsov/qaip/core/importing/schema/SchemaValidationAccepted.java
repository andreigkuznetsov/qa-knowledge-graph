package ru.kuznetsov.qaip.core.importing.schema;

import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;

import java.util.Objects;

/** Proves schema conformance only, not binding, application or domain validity. */
public record SchemaValidationAccepted(SchemaValidProjectDocument document) implements SchemaValidationResult {
    public SchemaValidationAccepted {
        Objects.requireNonNull(document, "document");
    }
}
