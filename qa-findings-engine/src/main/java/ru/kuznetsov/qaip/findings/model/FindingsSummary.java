package ru.kuznetsov.qaip.findings.model;

public record FindingsSummary(
        int total,
        int high,
        int medium,
        int low
) {
}
