package ru.kuznetsov.qagraph.validationcore.model;

public record ValidationSummary(
        int errors,
        int warnings,
        int total
) {
}
