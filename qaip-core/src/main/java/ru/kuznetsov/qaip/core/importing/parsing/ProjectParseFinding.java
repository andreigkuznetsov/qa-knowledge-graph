package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Objects;
import java.util.Optional;

/** Stable parser diagnostic independent of the underlying JSON library. */
public record ProjectParseFinding(
        ProjectParseFindingCode code,
        JsonInstanceLocation location,
        Optional<JsonSourcePosition> sourcePosition,
        String message) {
    public ProjectParseFinding {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(sourcePosition, "sourcePosition");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
