package ru.kuznetsov.qaip.core.importing.parsing;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Syntax rejection containing immutable, deduplicated and deterministically
 * ordered findings. A rejection never contains a partial document.
 */
public record ProjectParseRejected(List<ProjectParseFinding> findings) implements ProjectParseResult {
    private static final Comparator<ProjectParseFinding> ORDER = Comparator
            .comparingLong((ProjectParseFinding finding) -> finding.sourcePosition()
                    .map(JsonSourcePosition::characterOffset).orElse(Long.MAX_VALUE))
            .thenComparingLong(finding -> finding.sourcePosition().map(JsonSourcePosition::line).orElse(Long.MAX_VALUE))
            .thenComparingLong(finding -> finding.sourcePosition().map(JsonSourcePosition::column).orElse(Long.MAX_VALUE))
            .thenComparing(finding -> finding.location().value())
            .thenComparing(ProjectParseFinding::code)
            .thenComparing(ProjectParseFinding::message);

    public ProjectParseRejected {
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
