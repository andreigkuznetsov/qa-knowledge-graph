package ru.kuznetsov.qaip.explorer.application;

import ru.kuznetsov.qaip.explorer.view.EventPathView;

import java.util.Objects;

public record EventPathProjectionFound(EventPathView path) implements EventPathProjectionResult {
    public EventPathProjectionFound {
        Objects.requireNonNull(path, "path");
    }
}
