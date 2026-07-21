package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qagraph.change.aggregate.AggregateTransitionValid;
import ru.kuznetsov.qagraph.change.materialization.ProposedArtifactModel;
import ru.kuznetsov.qagraph.change.model.ArtifactState;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.BASE_ROOT_EVIDENCE_MISMATCH;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.BASE_ROOT_NOT_AVAILABLE;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.PROPOSED_MODEL_VERSION_MISMATCH;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.ROOT_RECONSTRUCTION_INCOMPLETE;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionDiagnosticCode.ROOT_VERSION_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionFailureKind.INCOMPLETE_ROOT;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionFailureKind.MISSING_EVIDENCE;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionFailureKind.STALE_EVIDENCE;
import static ru.kuznetsov.qagraph.change.root.RootReconstructionFailureKind.UNSUPPORTED_VERSION;

/** Reconstructs a full root only from bound, aggregate-valid evidence. */
public final class ProposedCanonicalRootReconstructor {

    private static final Set<String> ORDERED_CONTEXT_FIELDS = Set.of(
            "project",
            "sources"
    );

    public ProposedRootReconstructionResult reconstruct(
            CanonicalBaseModelEvidence baseEvidence,
            AggregateTransitionValid aggregateTransition
    ) {
        Objects.requireNonNull(
                aggregateTransition,
                "aggregateTransition must not be null"
        );
        if (baseEvidence == null) {
            return failure(
                    MISSING_EVIDENCE,
                    BASE_ROOT_NOT_AVAILABLE,
                    "$",
                    "Base root evidence is not available"
            );
        }
        if (aggregateTransition.materialization().baseEvidence().isEmpty()
                || aggregateTransition.materialization().baseEvidence()
                .orElseThrow() != baseEvidence) {
            return failure(
                    STALE_EVIDENCE,
                    BASE_ROOT_EVIDENCE_MISMATCH,
                    "$",
                    "Aggregate transition is not bound to this Base root"
            );
        }

        ProposedArtifactModel model = aggregateTransition.materialization()
                .proposedModel();
        if (!baseEvidence.rootContext().schemaVersion().isSupported()
                || !baseEvidence.artifactIndex().schemaVersion()
                .isSupported()) {
            return failure(
                    UNSUPPORTED_VERSION,
                    ROOT_VERSION_UNSUPPORTED,
                    "schemaVersion",
                    "Root evidence version is unsupported"
            );
        }
        if (!model.schemaVersion().equals(
                baseEvidence.rootContext().schemaVersion())
                || !model.schemaVersion().equals(
                baseEvidence.artifactIndex().schemaVersion())
                || model.nodes().stream().anyMatch(value ->
                !sameVersion(model, value))
                || model.relationships().stream().anyMatch(value ->
                !sameVersion(model, value))) {
            return failure(
                    UNSUPPORTED_VERSION,
                    PROPOSED_MODEL_VERSION_MISMATCH,
                    "schemaVersion",
                    "Proposed Model version differs from Base root evidence"
            );
        }

        ObjectNode retained = baseEvidence.rootContext().retainedProperties();
        if (!retained.has("project") || !retained.has("sources")) {
            return failure(
                    INCOMPLETE_ROOT,
                    ROOT_RECONSTRUCTION_INCOMPLETE,
                    "$",
                    "Required retained root properties are unavailable"
            );
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("schemaVersion", model.schemaVersion().value());
        root.set("project", retained.get("project").deepCopy());
        root.set("sources", retained.get("sources").deepCopy());
        retained.propertyStream()
                .filter(entry -> !ORDERED_CONTEXT_FIELDS.contains(
                        entry.getKey()))
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> root.set(
                        entry.getKey(),
                        entry.getValue().deepCopy()
                ));
        ArrayNode nodes = root.putArray("nodes");
        model.nodes().forEach(value -> nodes.add(value.snapshot()));
        ArrayNode relationships = root.putArray("relationships");
        model.relationships().forEach(value ->
                relationships.add(value.snapshot()));
        return new ProposedRootReconstructed(
                new ProposedCanonicalRoot(root),
                baseEvidence,
                aggregateTransition
        );
    }

    private boolean sameVersion(
            ProposedArtifactModel model,
            ArtifactState state
    ) {
        return state.schemaVersion().equals(model.schemaVersion());
    }

    private ProposedRootReconstructionFailure failure(
            RootReconstructionFailureKind kind,
            RootReconstructionDiagnosticCode code,
            String path,
            String message
    ) {
        return new ProposedRootReconstructionFailure(
                kind,
                List.of(new RootReconstructionDiagnostic(
                        code,
                        kind,
                        path,
                        message
                ))
        );
    }
}
