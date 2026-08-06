package ru.kuznetsov.qaip.core.application.query.eventpath;

import java.util.Objects;

public record EventPathFound(EventPathResult path) implements EventPathQueryResult {
    public EventPathFound {
        Objects.requireNonNull(path, "path");
    }
}
