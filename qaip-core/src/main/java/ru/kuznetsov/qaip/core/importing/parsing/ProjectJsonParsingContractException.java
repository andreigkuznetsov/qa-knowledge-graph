package ru.kuznetsov.qaip.core.importing.parsing;

/** Signals an unexpected internal failure, never an ordinary invalid JSON input. */
public final class ProjectJsonParsingContractException extends RuntimeException {
    public ProjectJsonParsingContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
