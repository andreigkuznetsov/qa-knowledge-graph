package ru.kuznetsov.qaip.core.importing.parsing;

/** Stable machine-readable parser rejection categories. */
public enum ProjectParseFindingCode {
    MALFORMED_JSON,
    DUPLICATE_JSON_MEMBER,
    TRAILING_JSON_CONTENT
}
