package ru.kuznetsov.qaip.core.persistence.document;

final class ProjectPersistenceDocumentException extends RuntimeException {
    ProjectPersistenceDocumentException(String message) {
        super(message);
    }

    ProjectPersistenceDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
