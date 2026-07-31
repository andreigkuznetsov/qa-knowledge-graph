package ru.kuznetsov.qaip.core.application.persistence;

import java.util.Objects;

public record PersistProjectRejected(PersistProjectFinding finding) implements PersistProjectResult {
    public PersistProjectRejected {
        Objects.requireNonNull(finding, "finding");
    }
}
