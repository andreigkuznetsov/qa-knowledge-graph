package ru.kuznetsov.qaip.evidencegovernance.source;

import java.util.Objects;

/** Closed finite rejection from safe Scenario Authority V1 attribution. */
public final class ScenarioAuthorityAttributionRejectionV1 extends IllegalArgumentException {
    private final Code code;
    private final String structuralLocation;

    public ScenarioAuthorityAttributionRejectionV1(Code code, String structuralLocation) {
        super(Objects.requireNonNull(code).name());
        this.code = code;
        this.structuralLocation = Objects.requireNonNull(structuralLocation);
    }

    public Code code() { return code; }
    public String structuralLocation() { return structuralLocation; }

    public enum Code {
        NON_OBJECT_ROOT,
        MISSING_AUTHORITY,
        AUTHORITY_NOT_STRING,
        INVALID_AUTHORITY
    }
}
