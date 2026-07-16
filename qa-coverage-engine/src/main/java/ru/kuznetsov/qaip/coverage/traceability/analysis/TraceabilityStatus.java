package ru.kuznetsov.qaip.coverage.traceability.analysis;

public enum TraceabilityStatus {
    FULLY_TRACEABLE,
    BROKEN_AT_OPERATION,
    BROKEN_AT_RULE,
    BROKEN_AT_SCENARIO,
    BROKEN_AT_TEST_IMPLEMENTATION,
    BROKEN_AT_CHECK
}
