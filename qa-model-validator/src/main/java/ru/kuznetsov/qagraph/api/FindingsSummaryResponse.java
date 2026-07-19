package ru.kuznetsov.qagraph.api;

public record FindingsSummaryResponse(
        int total,
        int high,
        int medium,
        int low
) {
}
