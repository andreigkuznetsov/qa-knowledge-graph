package ru.kuznetsov.qaip.core.importing.schema;

/** Validator resource, configuration or compilation failure; never an ordinary schema violation. */
public final class ProjectSchemaValidationContractException extends RuntimeException {
    public ProjectSchemaValidationContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
