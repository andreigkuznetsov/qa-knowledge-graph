package ru.kuznetsov.qaip.core.importing.binding;

import java.util.Objects;

public record BindingFinding(String code, String message, String location) {
    public BindingFinding {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");
    }
}
