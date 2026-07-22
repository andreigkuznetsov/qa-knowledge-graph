package ru.kuznetsov.qagraph.change.root;

import java.util.Comparator;
import java.util.Objects;

public record RootReconstructionDiagnostic(
        RootReconstructionDiagnosticCode code,
        RootReconstructionFailureKind failureKind,
        String path,
        String message
) {
    public static final Comparator<RootReconstructionDiagnostic> ORDER =
            Comparator.comparing(RootReconstructionDiagnostic::path)
                    .thenComparingInt(value -> value.code().rank());

    public RootReconstructionDiagnostic {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(failureKind, "failureKind must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
