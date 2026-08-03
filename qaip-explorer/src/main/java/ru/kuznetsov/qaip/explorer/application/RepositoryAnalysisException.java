package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public final class RepositoryAnalysisException extends RuntimeException {
    private final RepositoryAnalysisErrorCode code;

    public RepositoryAnalysisException(RepositoryAnalysisErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public RepositoryAnalysisErrorCode code() {
        return code;
    }
}
