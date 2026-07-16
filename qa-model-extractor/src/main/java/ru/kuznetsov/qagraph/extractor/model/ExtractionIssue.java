package ru.kuznetsov.qagraph.extractor.model;

public record ExtractionIssue(
        ExtractionSeverity severity,
        String layer,
        String code,
        String message,
        String path
) {
    public static ExtractionIssue schemaError(String code, String message, String path) {
        return new ExtractionIssue(ExtractionSeverity.ERROR, "STORY_INPUT_SCHEMA", code, message, path);
    }

    public static ExtractionIssue mappingWarning(String code, String message, String path) {
        return new ExtractionIssue(ExtractionSeverity.WARNING, "MAPPING", code, message, path);
    }

    public static ExtractionIssue outputError(String code, String message, String path) {
        return new ExtractionIssue(ExtractionSeverity.ERROR, "QA_MODEL_SCHEMA", code, message, path);
    }
}
