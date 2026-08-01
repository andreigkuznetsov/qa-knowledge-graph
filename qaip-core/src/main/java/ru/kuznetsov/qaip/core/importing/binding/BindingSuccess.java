package ru.kuznetsov.qaip.core.importing.binding;

import java.util.Objects;

public record BindingSuccess(BoundProjectDocument document) implements BindingResult {
    public BindingSuccess {
        Objects.requireNonNull(document, "document");
    }
}
