package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Objects;

/**
 * Proof only that the input contained exactly one syntactically valid JSON
 * value; it makes no schema-validity or semantic-validity claim.
 */
public record ProjectParseAccepted(ParsedProjectDocument document) implements ProjectParseResult {
    public ProjectParseAccepted {
        Objects.requireNonNull(document, "document");
    }
}
