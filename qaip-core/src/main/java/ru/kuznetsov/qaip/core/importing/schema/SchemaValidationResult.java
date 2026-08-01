package ru.kuznetsov.qaip.core.importing.schema;

/** Result of canonical QAIP Project JSON Schema validation. */
public sealed interface SchemaValidationResult permits SchemaValidationAccepted, SchemaValidationRejected {
}
