package ru.kuznetsov.qagraph.change.materialization;

import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.base.BaseVerifiedChange;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.CHANGE_SET_NOT_MATERIALIZABLE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_ADDED_TARGET_PRESENT;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_BASE_EVIDENCE_MISMATCH;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_BASE_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_DUPLICATE_TARGET_WRITE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_MODIFIED_TARGET_MISSING;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_REMOVED_TARGET_MISSING;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_STATE_INCONSISTENT;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationFailureKind.CONSISTENCY_FAILURE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationFailureKind.INELIGIBLE_CHANGE_SET;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationFailureKind.STALE_BASE_EVIDENCE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationFailureKind.UNSUPPORTED_BASE;

/**
 * Deterministically applies a complete set of Base-verified declarations.
 */
public final class ProposedModelMaterializer {

    private static final Comparator<BaseVerifiedChange> CHANGE_ORDER =
            Comparator
                    .comparingInt((BaseVerifiedChange value) -> categoryRank(
                            value.candidate().declaration().category()))
                    .thenComparing(value -> value.candidate().declaration()
                            .identity().value())
                    .thenComparingInt(value -> value.candidate().declaration()
                            .kind().canonicalOrder());

    public ProposedModelMaterializationResult materialize(
            BaseArtifactIndex baseIndex,
            BaseChangeSetResult changeSetResult
    ) {
        Objects.requireNonNull(baseIndex, "baseIndex must not be null");
        Objects.requireNonNull(
                changeSetResult,
                "changeSetResult must not be null"
        );

        if (!changeSetResult.intrinsicFailures().isEmpty()
                || !changeSetResult.ambiguities().isEmpty()
                || !changeSetResult.baseFailures().isEmpty()) {
            return failure(
                    changeSetResult,
                    INELIGIBLE_CHANGE_SET,
                    CHANGE_SET_NOT_MATERIALIZABLE,
                    -1,
                    Optional.empty(),
                    Optional.empty(),
                    "changeSet",
                    "Change Set contains earlier validation failures"
            );
        }
        if (baseIndex != changeSetResult.baseIndex()) {
            return failure(
                    changeSetResult,
                    STALE_BASE_EVIDENCE,
                    MATERIALIZATION_BASE_EVIDENCE_MISMATCH,
                    -1,
                    Optional.empty(),
                    Optional.empty(),
                    "baseModel",
                    "Materialization requires the exact verified Base evidence"
            );
        }
        if (!baseIndex.schemaVersion().isSupported()
                || !baseIndex.duplicates().isEmpty()
                || !baseIndex.unsupportedVersionArtifacts().isEmpty()
                || !baseIndex.incompatibleVersionArtifacts().isEmpty()) {
            return failure(
                    changeSetResult,
                    UNSUPPORTED_BASE,
                    MATERIALIZATION_BASE_UNSUPPORTED,
                    -1,
                    Optional.empty(),
                    Optional.empty(),
                    "baseModel",
                    "Base Model is not coherent under supported semantics"
            );
        }

        List<BaseVerifiedChange> changes = changeSetResult
                .baseVerifiedCandidates().stream()
                .sorted(CHANGE_ORDER)
                .toList();
        Optional<ProposedModelMaterializationFailure> duplicateFailure =
                duplicateWriteFailure(changeSetResult, changes);
        if (duplicateFailure.isPresent()) {
            return duplicateFailure.orElseThrow();
        }

        Map<Key, ArtifactState> candidate = new TreeMap<>(Key.ORDER);
        for (ArtifactState artifact : baseIndex.artifacts()) {
            candidate.put(new Key(
                    artifact.category(),
                    artifact.identity()
            ), artifact);
        }

        for (BaseVerifiedChange verified : changes) {
            Optional<ProposedModelMaterializationFailure> failure = apply(
                    changeSetResult,
                    baseIndex,
                    candidate,
                    verified
            );
            if (failure.isPresent()) {
                return failure.orElseThrow();
            }
        }

        List<NodeArtifactState> nodes = new ArrayList<>();
        List<RelationshipArtifactState> relationships = new ArrayList<>();
        for (ArtifactState artifact : candidate.values()) {
            if (artifact instanceof NodeArtifactState node) {
                nodes.add(node);
            } else if (artifact
                    instanceof RelationshipArtifactState relationship) {
                relationships.add(relationship);
            } else {
                throw new IllegalStateException(
                        "Unknown ArtifactState implementation"
                );
            }
        }
        return new ProposedModelMaterialized(
                new ProposedArtifactModel(
                        baseIndex.schemaVersion(),
                        nodes,
                        relationships
                ),
                changeSetResult.baseEvidence()
        );
    }

    private Optional<ProposedModelMaterializationFailure> apply(
            BaseChangeSetResult source,
            BaseArtifactIndex baseIndex,
            Map<Key, ArtifactState> candidate,
            BaseVerifiedChange verified
    ) {
        DeclaredChange declaration = verified.candidate().declaration();
        Key key = new Key(declaration.category(), declaration.identity());
        ArtifactState current = candidate.get(key);

        if (declaration.kind() == ChangeKind.ADDED) {
            if (current != null) {
                return Optional.of(consistencyFailure(
                        source,
                        verified,
                        MATERIALIZATION_ADDED_TARGET_PRESENT,
                        "target",
                        "ADDED target unexpectedly exists"
                ));
            }
            Optional<ArtifactState> after = declaration.afterState();
            if (after.isEmpty() || !stateMatchesDeclaration(
                    after.orElseThrow(), declaration, baseIndex)) {
                return Optional.of(stateFailure(source, verified));
            }
            candidate.put(key, after.orElseThrow());
            return Optional.empty();
        }

        if (current == null) {
            MaterializationDiagnosticCode code = declaration.kind()
                    == ChangeKind.REMOVED
                    ? MATERIALIZATION_REMOVED_TARGET_MISSING
                    : MATERIALIZATION_MODIFIED_TARGET_MISSING;
            return Optional.of(consistencyFailure(
                    source,
                    verified,
                    code,
                    "target",
                    declaration.kind() + " target unexpectedly missing"
            ));
        }
        if (verified.matchedBaseState().isEmpty()
                || !verified.matchedBaseState().orElseThrow().equals(current)) {
            return Optional.of(stateFailure(source, verified));
        }

        if (declaration.kind() == ChangeKind.REMOVED) {
            candidate.remove(key);
            return Optional.empty();
        }

        Optional<ArtifactState> after = declaration.afterState();
        if (after.isEmpty() || !stateMatchesDeclaration(
                after.orElseThrow(), declaration, baseIndex)) {
            return Optional.of(stateFailure(source, verified));
        }
        candidate.put(key, after.orElseThrow());
        return Optional.empty();
    }

    private Optional<ProposedModelMaterializationFailure>
            duplicateWriteFailure(
                    BaseChangeSetResult source,
                    List<BaseVerifiedChange> changes
            ) {
        Set<Key> targets = new HashSet<>();
        for (BaseVerifiedChange change : changes) {
            DeclaredChange declaration = change.candidate().declaration();
            Key key = new Key(
                    declaration.category(),
                    declaration.identity()
            );
            if (!targets.add(key)) {
                return Optional.of(consistencyFailure(
                        source,
                        change,
                        MATERIALIZATION_DUPLICATE_TARGET_WRITE,
                        "changes",
                        "Multiple Base-verified changes write one target"
                ));
            }
        }
        return Optional.empty();
    }

    private boolean stateMatchesDeclaration(
            ArtifactState state,
            DeclaredChange declaration,
            BaseArtifactIndex baseIndex
    ) {
        return state.category() == declaration.category()
                && state.identity().equals(declaration.identity())
                && state.schemaVersion().equals(declaration.schemaVersion())
                && state.schemaVersion().equals(baseIndex.schemaVersion());
    }

    private ProposedModelMaterializationFailure stateFailure(
            BaseChangeSetResult source,
            BaseVerifiedChange change
    ) {
        return consistencyFailure(
                source,
                change,
                MATERIALIZATION_STATE_INCONSISTENT,
                "state",
                "Base-verified state evidence is internally inconsistent"
        );
    }

    private ProposedModelMaterializationFailure consistencyFailure(
            BaseChangeSetResult source,
            BaseVerifiedChange change,
            MaterializationDiagnosticCode code,
            String path,
            String message
    ) {
        DeclaredChange declaration = change.candidate().declaration();
        return failure(
                source,
                CONSISTENCY_FAILURE,
                code,
                change.candidate().declarationIndex(),
                Optional.of(declaration.category()),
                Optional.of(declaration.identity()),
                path,
                message
        );
    }

    private ProposedModelMaterializationFailure failure(
            BaseChangeSetResult source,
            MaterializationFailureKind kind,
            MaterializationDiagnosticCode code,
            int declarationIndex,
            Optional<ArtifactCategory> category,
            Optional<CanonicalIdentity> identity,
            String path,
            String message
    ) {
        return new ProposedModelMaterializationFailure(
                source,
                kind,
                List.of(new MaterializationDiagnostic(
                        code,
                        kind,
                        declarationIndex,
                        category,
                        identity,
                        path,
                        message
                ))
        );
    }

    private static int categoryRank(ArtifactCategory category) {
        return switch (category) {
            case NODE -> 0;
            case RELATIONSHIP -> 1;
        };
    }

    private record Key(
            ArtifactCategory category,
            CanonicalIdentity identity
    ) {
        private static final Comparator<Key> ORDER = Comparator
                .comparingInt((Key value) -> categoryRank(value.category()))
                .thenComparing(value -> value.identity().value());
    }
}
