package ru.kuznetsov.qaip.core.application.importing;

import java.util.Objects;

public record ProjectImportFinding(ProjectImportStage stage, String code,
                                   ProjectImportSeverity severity, String message, String location) {
    public ProjectImportFinding {
        Objects.requireNonNull(stage, "stage");
        code = requireNonBlank(code, "code");
        Objects.requireNonNull(severity, "severity");
        message = requireNonBlank(message, "message");
        location = requireNonBlank(location, "location");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
