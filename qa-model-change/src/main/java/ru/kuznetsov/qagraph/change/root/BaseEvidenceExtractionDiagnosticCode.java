package ru.kuznetsov.qagraph.change.root;

/**
 * Stable Canonical Base root extraction failure vocabulary.
 */
public enum BaseEvidenceExtractionDiagnosticCode {
    BASE_ROOT_NOT_AVAILABLE(0),
    ROOT_INPUT_INVALID(1),
    ROOT_SCHEMA_VERSION_MISSING(2),
    ROOT_SCHEMA_VERSION_INVALID(3),
    ROOT_VERSION_UNSUPPORTED(4),
    ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING(5),
    ROOT_ARTIFACT_COLLECTION_MISSING(6),
    ROOT_ARTIFACT_COLLECTION_INVALID(7),
    ROOT_ARTIFACT_EXTRACTION_FAILED(8);

    private final int priority;

    BaseEvidenceExtractionDiagnosticCode(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
