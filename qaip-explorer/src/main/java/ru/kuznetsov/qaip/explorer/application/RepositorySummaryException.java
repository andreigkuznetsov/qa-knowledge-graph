package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public final class RepositorySummaryException extends RuntimeException {
    private final RepositorySummaryErrorCode code;

    public RepositorySummaryException(RepositorySummaryErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public RepositorySummaryException(RepositorySummaryErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public RepositorySummaryErrorCode code() {
        return code;
    }
}
