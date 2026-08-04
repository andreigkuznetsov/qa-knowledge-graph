package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public final class OperationDetailsException extends RuntimeException {
    private final OperationDetailsErrorCode code;

    public OperationDetailsException(OperationDetailsErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public OperationDetailsException(OperationDetailsErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public OperationDetailsErrorCode code() {
        return code;
    }
}
