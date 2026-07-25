package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Objects;

/** Proof that the input contained exactly one syntactically valid JSON value. */
public record ProjectParseAccepted(ParsedProjectDocument document) implements ProjectParseResult {
    public ProjectParseAccepted {
        Objects.requireNonNull(document, "document");
    }
}
