package ru.kuznetsov.qaip.core.importing.schema;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable deterministic findings with no partial schema-valid proof. */
public record SchemaValidationRejected(List<SchemaValidationFinding> findings) implements SchemaValidationResult {
    private static final Comparator<SchemaValidationFinding> ORDER = Comparator
            .comparing((SchemaValidationFinding finding) -> finding.instanceLocation().value())
            .thenComparing(finding -> finding.schemaLocation().value())
            .thenComparing(SchemaValidationFinding::code)
            .thenComparing(SchemaValidationFinding::keyword)
            .thenComparing(SchemaValidationFinding::message);

    public SchemaValidationRejected {
        Objects.requireNonNull(findings, "findings");
        if (findings.isEmpty()) {
            throw new IllegalArgumentException("findings must not be empty");
        }
        if (findings.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("findings must not contain null");
        }
        findings = findings.stream().distinct().sorted(ORDER).toList();
    }
}
