package ru.kuznetsov.qagraph.change.complete;

import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompleteProposedRootInvalid(
        ProposedRootReconstructed reconstructedRoot,
        CompleteValidationClassification classification,
        CompleteValidationStage stage,
        Optional<SchemaValidationEvidence> schemaEvidence,
        Optional<SemanticValidationEvidence> semanticEvidence,
        List<CompleteValidationDiagnostic> diagnostics,
        Optional<String> infrastructureFailure
) implements CompleteProposedRootValidationResult {
    public CompleteProposedRootInvalid {
        Objects.requireNonNull(reconstructedRoot, "reconstructedRoot is required");
        Objects.requireNonNull(classification, "classification is required");
        Objects.requireNonNull(stage, "stage is required");
        Objects.requireNonNull(schemaEvidence, "schemaEvidence is required");
        Objects.requireNonNull(semanticEvidence, "semanticEvidence is required");
        Objects.requireNonNull(diagnostics, "diagnostics is required");
        Objects.requireNonNull(
                infrastructureFailure,
                "infrastructureFailure is required"
        );
        diagnostics = diagnostics.stream()
                .peek(Objects::requireNonNull)
                .sorted(CompleteValidationDiagnostic.ORDER)
                .toList();
        boolean infrastructure = classification
                == CompleteValidationClassification
                .VALIDATION_INFRASTRUCTURE_FAILURE;
        if (infrastructure != infrastructureFailure.isPresent()) {
            throw new IllegalArgumentException(
                    "only infrastructure failures carry failure details"
            );
        }
        if (infrastructureFailure.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException(
                    "infrastructure failure details must not be blank"
            );
        }
    }
}
