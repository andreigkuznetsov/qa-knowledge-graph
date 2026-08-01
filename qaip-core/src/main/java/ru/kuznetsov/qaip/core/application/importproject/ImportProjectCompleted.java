package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.importing.ProjectImportSuccess;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;

import java.util.Objects;

public record ImportProjectCompleted(ProjectImportSuccess imported,
                                     PersistProjectAccepted persisted) implements ImportProjectUseCaseResult {
    public ImportProjectCompleted {
        Objects.requireNonNull(imported, "imported");
        Objects.requireNonNull(persisted, "persisted");
    }
}
