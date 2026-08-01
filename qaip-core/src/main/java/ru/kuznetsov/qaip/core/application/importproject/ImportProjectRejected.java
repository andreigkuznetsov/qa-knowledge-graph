package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.importing.ProjectImportFailure;

import java.util.Objects;

public record ImportProjectRejected(ProjectImportFailure failure) implements ImportProjectUseCaseResult {
    public ImportProjectRejected {
        Objects.requireNonNull(failure, "failure");
    }
}
