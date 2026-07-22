package ru.kuznetsov.qagraph.change.complete;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.root.ProposedRootReconstructed;
import ru.kuznetsov.qagraph.validationcore.QaModelValidationEngine;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationLayer;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSeverity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.SCHEMA_INVALID;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.SEMANTICALLY_INVALID;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.UNSUPPORTED_VERSION;
import static ru.kuznetsov.qagraph.change.complete.CompleteValidationClassification.VALIDATION_INFRASTRUCTURE_FAILURE;

/** Complete Phase 8 validation of successful Phase 7 evidence. */
public final class CompleteProposedRootValidator {

    private static final String SUPPORTED_VERSION = "0.1";
    private final CompleteValidationBackend injectedBackend;

    public CompleteProposedRootValidator() {
        this.injectedBackend = null;
    }

    CompleteProposedRootValidator(CompleteValidationBackend backend) {
        this.injectedBackend = Objects.requireNonNull(
                backend,
                "backend must not be null"
        );
    }

    public CompleteProposedRootValidationResult validate(
            ProposedRootReconstructed reconstructed
    ) {
        Objects.requireNonNull(reconstructed, "reconstructed must not be null");
        if (!hasCoherentSupportedVersion(reconstructed)) {
            return invalid(
                    reconstructed,
                    UNSUPPORTED_VERSION,
                    CompleteValidationStage.VERSION,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    Optional.empty()
            );
        }

        CompleteValidationBackend backend;
        try {
            backend = injectedBackend == null
                    ? new ValidationCoreBackend(new QaModelValidationEngine())
                    : injectedBackend;
        } catch (RuntimeException exception) {
            return infrastructureFailure(
                    reconstructed,
                    CompleteValidationStage.SCHEMA,
                    Optional.empty(),
                    "Authoritative schema validator initialization failed"
            );
        }

        JsonNode root = reconstructed.proposedRoot().snapshot();
        SchemaValidationEvidence schemaEvidence;
        try {
            schemaEvidence = schemaEvidence(backend.validateSchema(root));
        } catch (RuntimeException exception) {
            return infrastructureFailure(
                    reconstructed,
                    CompleteValidationStage.SCHEMA,
                    Optional.empty(),
                    "Authoritative schema validation failed"
            );
        }
        if (!schemaEvidence.valid()) {
            return invalid(
                    reconstructed,
                    SCHEMA_INVALID,
                    CompleteValidationStage.SCHEMA,
                    Optional.of(schemaEvidence),
                    Optional.empty(),
                    schemaEvidence.diagnostics(),
                    Optional.empty()
            );
        }

        SemanticValidationEvidence semanticEvidence;
        try {
            semanticEvidence = semanticEvidence(
                    backend.validateSemantic(root)
            );
        } catch (RuntimeException exception) {
            return invalid(
                    reconstructed,
                    VALIDATION_INFRASTRUCTURE_FAILURE,
                    CompleteValidationStage.SEMANTIC,
                    Optional.of(schemaEvidence),
                    Optional.empty(),
                    List.of(),
                    Optional.of(
                            "Authoritative semantic validation failed"
                    )
            );
        }
        if (!semanticEvidence.valid()) {
            return invalid(
                    reconstructed,
                    SEMANTICALLY_INVALID,
                    CompleteValidationStage.SEMANTIC,
                    Optional.of(schemaEvidence),
                    Optional.of(semanticEvidence),
                    semanticEvidence.diagnostics(),
                    Optional.empty()
            );
        }
        return new CompleteProposedRootValid(
                reconstructed,
                schemaEvidence,
                semanticEvidence
        );
    }

    private boolean hasCoherentSupportedVersion(
            ProposedRootReconstructed reconstructed
    ) {
        JsonNode versionNode = reconstructed.proposedRoot().snapshot()
                .get("schemaVersion");
        ProposedArtifactModel model = reconstructed.aggregateTransition()
                .materialization().proposedModel();
        return versionNode != null
                && versionNode.isTextual()
                && SUPPORTED_VERSION.equals(versionNode.textValue())
                && SUPPORTED_VERSION.equals(reconstructed.baseEvidence()
                .rootContext().schemaVersion().value())
                && SUPPORTED_VERSION.equals(reconstructed.baseEvidence()
                .artifactIndex().schemaVersion().value())
                && SUPPORTED_VERSION.equals(model.schemaVersion().value())
                && model.nodes().stream().allMatch(this::supported)
                && model.relationships().stream().allMatch(this::supported)
                && reconstructed.aggregateTransition().materialization()
                .baseEvidence() == reconstructed.baseEvidence();
    }

    private boolean supported(ArtifactState artifact) {
        return SUPPORTED_VERSION.equals(artifact.schemaVersion().value());
    }

    private SchemaValidationEvidence schemaEvidence(
            List<ValidationIssue> issues
    ) {
        List<ValidationIssue> originals = authoritative(
                issues,
                ValidationLayer.JSON_SCHEMA
        );
        return new SchemaValidationEvidence(
                originals,
                normalize(
                        originals,
                        CompleteValidationDiagnosticOrigin.SCHEMA
                )
        );
    }

    private SemanticValidationEvidence semanticEvidence(
            List<ValidationIssue> issues
    ) {
        List<ValidationIssue> originals = authoritative(
                issues,
                ValidationLayer.SEMANTIC
        );
        return new SemanticValidationEvidence(
                originals,
                normalize(
                        originals,
                        CompleteValidationDiagnosticOrigin.SEMANTIC
                )
        );
    }

    private List<ValidationIssue> authoritative(
            List<ValidationIssue> issues,
            ValidationLayer expectedLayer
    ) {
        Objects.requireNonNull(issues, "validator issues must not be null");
        if (issues.stream().anyMatch(Objects::isNull)
                || issues.stream().anyMatch(value ->
                value.layer() != expectedLayer)) {
            throw new IllegalStateException(
                    "Authoritative validator returned an impossible result"
            );
        }
        return issues.stream()
                .sorted(authoritativeOrder())
                .toList();
    }

    private Comparator<ValidationIssue> authoritativeOrder() {
        return Comparator
                .comparingInt((ValidationIssue value) ->
                        value.severity() == ValidationSeverity.ERROR ? 0 : 1)
                .thenComparing(value -> normalizePath(value.path()))
                .thenComparing(ValidationIssue::code)
                .thenComparing(ValidationIssue::message)
                .thenComparing(value -> value.objectId() == null
                        ? "" : value.objectId());
    }

    private List<CompleteValidationDiagnostic> normalize(
            List<ValidationIssue> issues,
            CompleteValidationDiagnosticOrigin origin
    ) {
        Map<DiagnosticIdentity, CompleteValidationDiagnostic> unique =
                new LinkedHashMap<>();
        issues.stream().map(issue -> new CompleteValidationDiagnostic(
                        origin,
                        Objects.requireNonNull(issue.severity()),
                        Objects.requireNonNull(issue.code()),
                        normalizePath(issue.path()),
                        Objects.requireNonNull(issue.message()),
                        issue.objectId(),
                        issue
                ))
                .sorted(CompleteValidationDiagnostic.ORDER)
                .forEach(diagnostic -> unique.putIfAbsent(
                        new DiagnosticIdentity(
                                diagnostic.origin(),
                                diagnostic.code(),
                                diagnostic.path(),
                                diagnostic.message()
                        ),
                        diagnostic
                ));
        return List.copyOf(unique.values());
    }

    private String normalizePath(String path) {
        return path == null || path.isBlank() ? "$" : path;
    }

    private CompleteProposedRootInvalid infrastructureFailure(
            ProposedRootReconstructed reconstructed,
            CompleteValidationStage stage,
            Optional<SchemaValidationEvidence> schemaEvidence,
            String detail
    ) {
        return invalid(
                reconstructed,
                VALIDATION_INFRASTRUCTURE_FAILURE,
                stage,
                schemaEvidence,
                Optional.empty(),
                List.of(),
                Optional.of(detail)
        );
    }

    private CompleteProposedRootInvalid invalid(
            ProposedRootReconstructed reconstructed,
            CompleteValidationClassification classification,
            CompleteValidationStage stage,
            Optional<SchemaValidationEvidence> schemaEvidence,
            Optional<SemanticValidationEvidence> semanticEvidence,
            List<CompleteValidationDiagnostic> diagnostics,
            Optional<String> infrastructureFailure
    ) {
        return new CompleteProposedRootInvalid(
                reconstructed,
                classification,
                stage,
                schemaEvidence,
                semanticEvidence,
                diagnostics,
                infrastructureFailure
        );
    }

    private record DiagnosticIdentity(
            CompleteValidationDiagnosticOrigin origin,
            String code,
            String path,
            String message
    ) {
    }
}
