package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public final class RepositoryAnalysisException extends RuntimeException {
    private final RepositoryAnalysisErrorCode code;
    private final String repositoryId;

    public RepositoryAnalysisException(RepositoryAnalysisErrorCode code, String message) {
        this(code, message, null);
    }

    public RepositoryAnalysisException(
            RepositoryAnalysisErrorCode code,
            String message,
            String repositoryId
    ) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.repositoryId = repositoryId;
    }

    public RepositoryAnalysisErrorCode code() {
        return code;
    }

    public String repositoryId() {
        return repositoryId;
    }
}
