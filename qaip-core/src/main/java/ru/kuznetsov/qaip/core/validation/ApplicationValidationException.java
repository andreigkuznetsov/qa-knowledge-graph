package ru.kuznetsov.qaip.core.validation;

public final class ApplicationValidationException extends RuntimeException {
    public ApplicationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
