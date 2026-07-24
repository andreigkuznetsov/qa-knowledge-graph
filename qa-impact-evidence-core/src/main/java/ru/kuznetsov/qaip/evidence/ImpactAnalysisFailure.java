package ru.kuznetsov.qaip.evidence;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Invalid input/integrity/version outcome; it is not an UNKNOWN conclusion. */
public record ImpactAnalysisFailure(FailureCode code, List<AnalysisDiagnostic> diagnostics) {
    public ImpactAnalysisFailure {
        Objects.requireNonNull(code); diagnostics = List.copyOf(Objects.requireNonNull(diagnostics)).stream()
                .sorted(Comparator.comparing(AnalysisDiagnostic::objectId).thenComparing(AnalysisDiagnostic::code)).toList();
        if (diagnostics.isEmpty()) throw new IllegalArgumentException("diagnostics must not be empty");
    }
}
