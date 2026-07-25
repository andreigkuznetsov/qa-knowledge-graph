package ru.kuznetsov.qaip.core.importing.parsing;

/**
 * Signals an unexpected parser infrastructure failure, never ordinary invalid
 * JSON input. This public type lets boundary callers distinguish an internal
 * failure from an explicit {@link ProjectParseRejected} result.
 */
public final class ProjectJsonParsingContractException extends RuntimeException {
    public ProjectJsonParsingContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
