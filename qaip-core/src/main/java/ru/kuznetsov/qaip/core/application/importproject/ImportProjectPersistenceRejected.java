package ru.kuznetsov.qaip.core.application.importproject;

import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;

import java.util.Objects;

public record ImportProjectPersistenceRejected(
        PersistProjectRejected rejection) implements ImportProjectUseCaseResult {
    public ImportProjectPersistenceRejected {
        Objects.requireNonNull(rejection, "rejection");
    }
}
