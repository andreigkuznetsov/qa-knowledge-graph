package ru.kuznetsov.qagraph.change.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.materialization.ProposedModelMaterialized;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.PROPOSED_ARTIFACT_VERSION_MISMATCH;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.PROPOSED_MODEL_VERSION_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_ENDPOINT_INVALID;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_ENDPOINT_MISSING;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_SOURCE_NODE_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_ENDPOINT_INVALID;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_ENDPOINT_MISSING;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionDiagnosticCode.RELATIONSHIP_TARGET_NODE_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionFailureKind.DANGLING_REFERENCE;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionFailureKind.STRUCTURALLY_INVALID;
import static ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionFailureKind.UNSUPPORTED;

/**
 * Validates final-state cross-artifact endpoint consistency.
 */
public final class AggregateTransitionValidator {

    public AggregateTransitionValidationResult validate(
            ProposedModelMaterialized materialization
    ) {
        Objects.requireNonNull(
                materialization,
                "materialization must not be null"
        );
        ProposedArtifactModel model = materialization.proposedModel();
        if (!model.schemaVersion().isSupported()) {
            return invalid(materialization, List.of(globalDiagnostic(
                    PROPOSED_MODEL_VERSION_UNSUPPORTED,
                    "schemaVersion",
                    "Proposed Model version is unsupported"
            )));
        }

        boolean incompatibleArtifact = model.nodes().stream().anyMatch(value ->
                !value.schemaVersion().equals(model.schemaVersion()))
                || model.relationships().stream().anyMatch(value ->
                !value.schemaVersion().equals(model.schemaVersion()));
        if (incompatibleArtifact) {
            return invalid(materialization, List.of(globalDiagnostic(
                    PROPOSED_ARTIFACT_VERSION_MISMATCH,
                    "artifacts.schemaVersion",
                    "An artifact version differs from the Proposed Model"
            )));
        }

        List<AggregateTransitionDiagnostic> diagnostics = new ArrayList<>();
        for (RelationshipArtifactState relationship
                : model.relationships()) {
            validateEndpoint(
                    model,
                    relationship,
                    RelationshipEndpointRole.SOURCE,
                    diagnostics
            );
            validateEndpoint(
                    model,
                    relationship,
                    RelationshipEndpointRole.TARGET,
                    diagnostics
            );
        }
        if (diagnostics.isEmpty()) {
            return new AggregateTransitionValid(materialization);
        }
        return invalid(materialization, diagnostics);
    }

    private void validateEndpoint(
            ProposedArtifactModel model,
            RelationshipArtifactState relationship,
            RelationshipEndpointRole role,
            List<AggregateTransitionDiagnostic> diagnostics
    ) {
        JsonNode endpoint = relationship.snapshot().get(role.property());
        if (endpoint == null) {
            diagnostics.add(relationshipDiagnostic(
                    relationship,
                    role,
                    missingCode(role),
                    STRUCTURALLY_INVALID,
                    Optional.empty(),
                    "Relationship endpoint property is missing"
            ));
            return;
        }
        if (!endpoint.isTextual()) {
            diagnostics.add(relationshipDiagnostic(
                    relationship,
                    role,
                    invalidCode(role),
                    STRUCTURALLY_INVALID,
                    Optional.of(endpoint.toString()),
                    "Relationship endpoint must be textual"
            ));
            return;
        }

        String value = endpoint.textValue();
        CanonicalIdentity identity;
        try {
            identity = new CanonicalIdentity(value);
        } catch (IllegalArgumentException exception) {
            diagnostics.add(relationshipDiagnostic(
                    relationship,
                    role,
                    invalidCode(role),
                    STRUCTURALLY_INVALID,
                    Optional.of(value),
                    "Relationship endpoint is not a Canonical Identity"
            ));
            return;
        }

        Optional<ArtifactState> node = model.lookup(
                ArtifactCategory.NODE,
                identity
        );
        if (node.isEmpty()) {
            diagnostics.add(relationshipDiagnostic(
                    relationship,
                    role,
                    notFoundCode(role),
                    DANGLING_REFERENCE,
                    Optional.of(value),
                    "Relationship endpoint Node was not found"
            ));
        }
    }

    private AggregateTransitionDiagnostic relationshipDiagnostic(
            RelationshipArtifactState relationship,
            RelationshipEndpointRole role,
            AggregateTransitionDiagnosticCode code,
            AggregateTransitionFailureKind kind,
            Optional<String> endpointValue,
            String message
    ) {
        return new AggregateTransitionDiagnostic(
                code,
                kind,
                Optional.of(ArtifactCategory.RELATIONSHIP),
                Optional.of(relationship.identity()),
                role,
                endpointValue,
                "relationships[*]." + role.property(),
                message
        );
    }

    private AggregateTransitionDiagnostic globalDiagnostic(
            AggregateTransitionDiagnosticCode code,
            String path,
            String message
    ) {
        return new AggregateTransitionDiagnostic(
                code,
                UNSUPPORTED,
                Optional.empty(),
                Optional.empty(),
                RelationshipEndpointRole.MODEL,
                Optional.empty(),
                path,
                message
        );
    }

    private AggregateTransitionDiagnosticCode missingCode(
            RelationshipEndpointRole role
    ) {
        return role == RelationshipEndpointRole.SOURCE
                ? RELATIONSHIP_SOURCE_ENDPOINT_MISSING
                : RELATIONSHIP_TARGET_ENDPOINT_MISSING;
    }

    private AggregateTransitionDiagnosticCode invalidCode(
            RelationshipEndpointRole role
    ) {
        return role == RelationshipEndpointRole.SOURCE
                ? RELATIONSHIP_SOURCE_ENDPOINT_INVALID
                : RELATIONSHIP_TARGET_ENDPOINT_INVALID;
    }

    private AggregateTransitionDiagnosticCode notFoundCode(
            RelationshipEndpointRole role
    ) {
        return role == RelationshipEndpointRole.SOURCE
                ? RELATIONSHIP_SOURCE_NODE_NOT_FOUND
                : RELATIONSHIP_TARGET_NODE_NOT_FOUND;
    }

    private AggregateTransitionInvalid invalid(
            ProposedModelMaterialized materialization,
            List<AggregateTransitionDiagnostic> diagnostics
    ) {
        return new AggregateTransitionInvalid(materialization, diagnostics);
    }
}
