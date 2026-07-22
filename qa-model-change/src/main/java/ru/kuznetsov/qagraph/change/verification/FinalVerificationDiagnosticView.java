package ru.kuznetsov.qagraph.change.verification;

import ru.kuznetsov.qagraph.change.complete.CompleteValidationDiagnostic;

import java.util.Objects;
import java.util.Optional;

public record FinalVerificationDiagnosticView(
        FinalVerificationStage stage,
        String code,
        String path,
        String message,
        Optional<CompleteValidationDiagnostic> completeDiagnostic
) {
    public FinalVerificationDiagnosticView {
        Objects.requireNonNull(stage, "stage must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(
                completeDiagnostic,
                "completeDiagnostic must not be null"
        );
    }
}
