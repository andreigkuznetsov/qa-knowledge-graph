package ru.kuznetsov.qaip.core.persistence;

public class ProjectPersistenceException extends RuntimeException {
    public ProjectPersistenceException(String message) {
        super(message);
    }

    public ProjectPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
