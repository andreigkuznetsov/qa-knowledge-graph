package ru.kuznetsov.qaip.core.persistence.document;

public final class ProjectPersistenceDocumentException extends RuntimeException {
    public ProjectPersistenceDocumentException(String message) {
        super(message);
    }

    public ProjectPersistenceDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
