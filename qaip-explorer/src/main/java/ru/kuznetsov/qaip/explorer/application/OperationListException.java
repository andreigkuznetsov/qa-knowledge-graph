package ru.kuznetsov.qaip.explorer.application;

import java.util.Objects;

public final class OperationListException extends RuntimeException {
    private final OperationListErrorCode code;

    public OperationListException(OperationListErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public OperationListException(OperationListErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public OperationListErrorCode code() {
        return code;
    }
}
