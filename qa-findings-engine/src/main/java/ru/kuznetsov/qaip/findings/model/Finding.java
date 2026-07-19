package ru.kuznetsov.qaip.findings.model;

public record Finding(
        FindingCode code,
        FindingSeverity severity,
        String nodeId,
        String nodeType,
        String message,
        String recommendation
) {
}
