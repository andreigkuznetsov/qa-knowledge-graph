package ru.kuznetsov.qaip.core.persistence;

/**
 * Reports an unusable canonical project identity or an infrastructure failure while accessing project
 * persistence. Duplicate project identity is an expected {@link ProjectAlreadyExists} result instead.
 */
public class ProjectPersistenceException extends RuntimeException {
    public ProjectPersistenceException(String message) {
        super(message);
    }

    public ProjectPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
