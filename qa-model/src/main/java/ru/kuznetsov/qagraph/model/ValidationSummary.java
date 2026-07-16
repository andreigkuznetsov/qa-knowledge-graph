package ru.kuznetsov.qagraph.model;

public record ValidationSummary(
        int errors,
        int warnings,
        int total
) {
}
