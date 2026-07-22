package ru.kuznetsov.qagraph.change.base;

import ru.kuznetsov.qagraph.change.equality.ArtifactSemanticEquality;
import ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.validation.ChangeDiagnostic;
import ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode;
import ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_EQUAL;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_TARGET_ALREADY_EXISTS;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_MODEL_DUPLICATE_TARGET;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_MODEL_VERSION_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_STATE_COMPARISON_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.BASE_STATE_VERSION_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_BEFORE_STATE_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_TARGET_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_BEFORE_STATE_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_TARGET_NOT_FOUND;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNVERIFIABLE;

/**
 * Verifies intrinsic candidates against immutable Base Model evidence.
 */
public final class BaseChangeVerifier {

    private final BaseArtifactIndex baseIndex;
    private final Optional<CanonicalBaseModelEvidence> baseEvidence;
    private final ArtifactSemanticEquality semanticEquality;

    public BaseChangeVerifier(BaseArtifactIndex baseIndex) {
        this(baseIndex, Optional.empty(), new ArtifactSemanticEquality());
    }

    public BaseChangeVerifier(CanonicalBaseModelEvidence baseEvidence) {
        this(
                Objects.requireNonNull(
                        baseEvidence,
                        "baseEvidence must not be null"
                ).artifactIndex(),
                Optional.of(baseEvidence),
                new ArtifactSemanticEquality()
        );
    }

    BaseChangeVerifier(
            BaseArtifactIndex baseIndex,
            ArtifactSemanticEquality semanticEquality
    ) {
        this(baseIndex, Optional.empty(), semanticEquality);
    }

    private BaseChangeVerifier(
            BaseArtifactIndex baseIndex,
            Optional<CanonicalBaseModelEvidence> baseEvidence,
            ArtifactSemanticEquality semanticEquality
    ) {
        this.baseIndex = Objects.requireNonNull(
                baseIndex,
                "baseIndex must not be null"
        );
        this.baseEvidence = Objects.requireNonNull(
                baseEvidence,
                "baseEvidence must not be null"
        );
        this.semanticEquality = Objects.requireNonNull(
                semanticEquality,
                "semanticEquality must not be null"
        );
    }

    public BaseVerificationResult verify(
            IntrinsicallyValidChange candidate
    ) {
        Objects.requireNonNull(candidate, "candidate must not be null");
        DeclaredChange declaration = candidate.declaration();

        if (!baseIndex.schemaVersion().isSupported()) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_MODEL_VERSION_UNSUPPORTED,
                    "baseModel.schemaVersion",
                    "Base Model schema version is unsupported"
            );
        }
        if (!baseIndex.unsupportedVersionArtifacts().isEmpty()) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_MODEL_VERSION_UNSUPPORTED,
                    "baseModel.artifacts.schemaVersion",
                    "Base Model contains an unsupported artifact version"
            );
        }
        if (!baseIndex.incompatibleVersionArtifacts().isEmpty()) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_STATE_VERSION_MISMATCH,
                    "baseModel.artifacts.schemaVersion",
                    "Base Model contains a version-incompatible artifact"
            );
        }
        if (!baseIndex.schemaVersion().equals(
                declaration.schemaVersion())) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_STATE_VERSION_MISMATCH,
                    "baseModel.schemaVersion",
                    "Base Model and declaration versions differ"
            );
        }

        BaseArtifactLookup lookup = baseIndex.lookup(
                declaration.category(),
                declaration.identity()
        );
        if (lookup instanceof BaseArtifactAmbiguous) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_MODEL_DUPLICATE_TARGET,
                    "baseModel.artifacts",
                    "Base Model contains a duplicate logical target"
            );
        }
        if (lookup instanceof BaseArtifactMissing) {
            return verifyMissing(candidate);
        }
        if (!(lookup instanceof BaseArtifactFound found)) {
            throw new IllegalStateException("Unknown Base lookup outcome");
        }

        ArtifactState baseState = found.state();
        if (!baseState.schemaVersion().isSupported()) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_MODEL_VERSION_UNSUPPORTED,
                    "baseModel.artifact.schemaVersion",
                    "Base artifact schema version is unsupported"
            );
        }
        if (!baseState.schemaVersion().equals(baseIndex.schemaVersion())
                || !baseState.schemaVersion().equals(
                declaration.schemaVersion())) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_STATE_VERSION_MISMATCH,
                    "baseModel.artifact.schemaVersion",
                    "Base artifact version is incompatible"
            );
        }

        if (declaration.kind() == ChangeKind.ADDED) {
            return failure(
                    candidate,
                    UNVERIFIABLE,
                    ADDED_TARGET_ALREADY_EXISTS,
                    "target",
                    "ADDED target already exists in the Base Model"
            );
        }
        return verifyBeforeState(candidate, baseState);
    }

    public BaseChangeSetResult verify(IntrinsicChangeSetResult intrinsic) {
        Objects.requireNonNull(intrinsic, "intrinsic must not be null");
        List<BaseVerifiedChange> verified = new ArrayList<>();
        List<BaseVerificationFailure> failures = new ArrayList<>();
        for (IntrinsicallyValidChange candidate
                : intrinsic.validCandidates()) {
            BaseVerificationResult result = verify(candidate);
            if (result instanceof BaseVerifiedChange success) {
                verified.add(success);
            } else if (result instanceof BaseVerificationFailure failure) {
                failures.add(failure);
            }
        }
        return new BaseChangeSetResult(
                baseIndex,
                baseEvidence,
                Optional.of(intrinsic),
                intrinsic.failedDeclarations(),
                intrinsic.ambiguities(),
                verified,
                failures
        );
    }

    private BaseVerificationResult verifyMissing(
            IntrinsicallyValidChange candidate
    ) {
        return switch (candidate.declaration().kind()) {
            case ADDED -> new BaseVerifiedChange(candidate, Optional.empty());
            case REMOVED -> failure(
                    candidate,
                    UNVERIFIABLE,
                    REMOVED_TARGET_NOT_FOUND,
                    "target",
                    "REMOVED target was not found in the Base Model"
            );
            case MODIFIED -> failure(
                    candidate,
                    UNVERIFIABLE,
                    MODIFIED_TARGET_NOT_FOUND,
                    "target",
                    "MODIFIED target was not found in the Base Model"
            );
        };
    }

    private BaseVerificationResult verifyBeforeState(
            IntrinsicallyValidChange candidate,
            ArtifactState baseState
    ) {
        DeclaredChange declaration = candidate.declaration();
        ArtifactState before = declaration.beforeState().orElseThrow();
        SemanticEqualityResult comparison = semanticEquality.compare(
                before,
                baseState
        );
        if (comparison == SemanticEqualityResult.UNSUPPORTED) {
            return failure(
                    candidate,
                    UNSUPPORTED,
                    BASE_STATE_COMPARISON_UNSUPPORTED,
                    "beforeState",
                    "Base and before states cannot be compared"
            );
        }
        if (comparison != SEMANTICALLY_EQUAL) {
            ChangeDiagnosticCode code = declaration.kind()
                    == ChangeKind.REMOVED
                    ? REMOVED_BEFORE_STATE_MISMATCH
                    : MODIFIED_BEFORE_STATE_MISMATCH;
            return failure(
                    candidate,
                    UNVERIFIABLE,
                    code,
                    "beforeState",
                    "Declared before state differs from the Base Model"
            );
        }
        return new BaseVerifiedChange(
                candidate,
                Optional.of(baseState)
        );
    }

    private BaseVerificationFailure failure(
            IntrinsicallyValidChange candidate,
            ChangeFailureClassification classification,
            ChangeDiagnosticCode code,
            String path,
            String message
    ) {
        DeclaredChange declaration = candidate.declaration();
        ChangeDiagnostic diagnostic = new ChangeDiagnostic(
                code,
                classification,
                candidate.declarationIndex(),
                declaration.category(),
                declaration.identity(),
                path,
                message
        );
        return new BaseVerificationFailure(
                candidate,
                classification,
                List.of(diagnostic)
        );
    }
}
