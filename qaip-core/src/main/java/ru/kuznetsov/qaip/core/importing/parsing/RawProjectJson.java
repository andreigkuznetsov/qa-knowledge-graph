package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Objects;

/** Raw, unmodified JSON text supplied to the project parsing boundary. */
public record RawProjectJson(String value) {
    public RawProjectJson {
        Objects.requireNonNull(value, "value");
    }
}
