package ru.kuznetsov.qaip.core.importing.schema;

import ru.kuznetsov.qaip.core.importing.parsing.ParsedProjectDocument;

/**
 * Validates a parsed document against the canonical QAIP Project JSON Schema.
 * Ordinary violations are returned as rejection; null input is a programming error.
 */
public interface ProjectSchemaValidator {
    SchemaValidationResult validate(ParsedProjectDocument document);
}
