package ru.kuznetsov.qaip.evidencegovernance.source;

import java.util.Objects;

/** Closed finite rejection from exact-byte Scenario Authority JSON parsing. */
public final class ScenarioAuthorityJsonParseRejectionV1 extends IllegalArgumentException {
    private final Code code;

    public ScenarioAuthorityJsonParseRejectionV1(Code code) {
        super(Objects.requireNonNull(code).name());
        this.code = code;
    }

    public Code code() { return code; }

    public enum Code {
        INVALID_UTF8,
        MALFORMED_JSON,
        DUPLICATE_JSON_MEMBER,
        TRAILING_JSON_CONTENT
    }
}
