package ru.kuznetsov.qagraph.model;

public record ValidationIssue(
        ValidationSeverity severity,
        ValidationLayer layer,
        String code,
        String message,
        String objectId,
        String path
) {

    public static ValidationIssue schemaError(
            String code,
            String message,
            String path
    ) {
        return new ValidationIssue(
                ValidationSeverity.ERROR,
                ValidationLayer.JSON_SCHEMA,
                code,
                message,
                null,
                path
        );
    }

    public static ValidationIssue semanticError(
            String code,
            String message,
            String objectId
    ) {
        return semanticError(code, message, objectId, null);
    }

    public static ValidationIssue semanticError(
            String code,
            String message,
            String objectId,
            String path
    ) {
        return new ValidationIssue(
                ValidationSeverity.ERROR,
                ValidationLayer.SEMANTIC,
                code,
                message,
                objectId,
                path
        );
    }

    public static ValidationIssue semanticWarning(
            String code,
            String message,
            String objectId
    ) {
        return semanticWarning(code, message, objectId, null);
    }

    public static ValidationIssue semanticWarning(
            String code,
            String message,
            String objectId,
            String path
    ) {
        return new ValidationIssue(
                ValidationSeverity.WARNING,
                ValidationLayer.SEMANTIC,
                code,
                message,
                objectId,
                path
        );
    }
}