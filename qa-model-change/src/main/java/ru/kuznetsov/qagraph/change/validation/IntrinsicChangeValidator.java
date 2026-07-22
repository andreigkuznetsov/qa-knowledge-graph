package ru.kuznetsov.qagraph.change.validation;

import ru.kuznetsov.qagraph.change.equality.ArtifactSemanticEquality;
import ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_EQUAL;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_AFTER_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ADDED_BEFORE_STATE_PRESENT;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ARTIFACT_CATEGORY_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.ARTIFACT_IDENTITY_MISMATCH;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.CONTRADICTORY_CHANGE_TARGET;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.CROSS_VERSION_CHANGE_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.DUPLICATE_CHANGE_TARGET;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_AFTER_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_BEFORE_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.MODIFIED_STATE_UNCHANGED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_AFTER_STATE_PRESENT;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.REMOVED_BEFORE_STATE_MISSING;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.STATE_SEMANTICS_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.UNSUPPORTED_SCHEMA_VERSION;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.AMBIGUOUS;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.STRUCTURALLY_INVALID;
import static ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification.UNSUPPORTED;

/**
 * Validates untrusted declarations without consulting a Base Model.
 */
public final class IntrinsicChangeValidator {

    private final ArtifactSemanticEquality semanticEquality;

    public IntrinsicChangeValidator() {
        this(new ArtifactSemanticEquality());
    }

    IntrinsicChangeValidator(ArtifactSemanticEquality semanticEquality) {
        this.semanticEquality = Objects.requireNonNull(
                semanticEquality,
                "semanticEquality must not be null"
        );
    }

    public IntrinsicChangeResult validate(DeclaredChange declaration) {
        return validate(declaration, 0);
    }

    public IntrinsicChangeSetResult validate(DeclaredChangeSet changeSet) {
        Objects.requireNonNull(changeSet, "changeSet must not be null");

        List<IntrinsicallyValidChange> valid = new ArrayList<>();
        List<IntrinsicallyInvalidChange> failed = new ArrayList<>();
        Map<Target, List<IndexedDeclaration>> byTarget =
                new LinkedHashMap<>();

        for (int index = 0; index < changeSet.changes().size(); index++) {
            DeclaredChange declaration = changeSet.changes().get(index);
            IntrinsicChangeResult result = validate(declaration, index);
            if (result instanceof IntrinsicallyValidChange candidate) {
                valid.add(candidate);
            } else if (result instanceof IntrinsicallyInvalidChange failure) {
                failed.add(failure);
            }
            byTarget.computeIfAbsent(
                    new Target(declaration.category(), declaration.identity()),
                    ignored -> new ArrayList<>()
            ).add(new IndexedDeclaration(index, declaration, result));
        }

        List<ChangeSetAmbiguity> ambiguities = new ArrayList<>();
        for (Map.Entry<Target, List<IndexedDeclaration>> entry
                : byTarget.entrySet()) {
            List<IndexedDeclaration> declarations = entry.getValue();
            if (declarations.size() < 2 || declarations.stream().anyMatch(
                    value -> value.result()
                            instanceof IntrinsicallyInvalidChange)) {
                continue;
            }

            ChangeDiagnosticCode code = declarationsEquivalent(declarations)
                    ? DUPLICATE_CHANGE_TARGET
                    : CONTRADICTORY_CHANGE_TARGET;
            int firstIndex = declarations.getFirst().index();
            Target target = entry.getKey();
            ChangeDiagnostic diagnostic = diagnostic(
                    code,
                    AMBIGUOUS,
                    firstIndex,
                    target.category(),
                    target.identity(),
                    "changes",
                    "Multiple declarations target the same artifact"
            );
            List<Integer> indices = declarations.stream()
                    .map(IndexedDeclaration::index)
                    .toList();
            ambiguities.add(new ChangeSetAmbiguity(
                    target.category(),
                    target.identity(),
                    indices,
                    diagnostic
            ));
            valid.removeIf(value -> indices.contains(value.declarationIndex()));
        }

        return new IntrinsicChangeSetResult(
                Optional.of(changeSet),
                valid,
                failed,
                ambiguities
        );
    }

    private IntrinsicChangeResult validate(
            DeclaredChange declaration,
            int declarationIndex
    ) {
        Objects.requireNonNull(declaration, "declaration must not be null");

        Optional<ChangeDiagnostic> versionFailure = versionFailure(
                declaration,
                declarationIndex
        );
        if (versionFailure.isPresent()) {
            return failure(
                    declarationIndex,
                    declaration,
                    UNSUPPORTED,
                    List.of(versionFailure.orElseThrow())
            );
        }

        List<ChangeDiagnostic> structural = new ArrayList<>();
        validatePresence(declaration, declarationIndex, structural);
        validateStateConsistency(
                declaration,
                declarationIndex,
                "beforeState",
                declaration.beforeState(),
                structural
        );
        validateStateConsistency(
                declaration,
                declarationIndex,
                "afterState",
                declaration.afterState(),
                structural
        );
        if (!structural.isEmpty()) {
            return failure(
                    declarationIndex,
                    declaration,
                    STRUCTURALLY_INVALID,
                    structural
            );
        }

        if (declaration.kind()
                == ru.kuznetsov.qagraph.change.model.ChangeKind.MODIFIED) {
            SemanticEqualityResult comparison = semanticEquality.compare(
                    declaration.beforeState().orElseThrow(),
                    declaration.afterState().orElseThrow()
            );
            if (comparison == SemanticEqualityResult.UNSUPPORTED) {
                return failure(
                        declarationIndex,
                        declaration,
                        ChangeFailureClassification.UNSUPPORTED,
                        List.of(diagnostic(
                                STATE_SEMANTICS_UNSUPPORTED,
                                ChangeFailureClassification.UNSUPPORTED,
                                declarationIndex,
                                declaration,
                                "beforeState/afterState",
                                "State semantic equality is unsupported"
                        ))
                );
            }
            if (comparison == SEMANTICALLY_EQUAL) {
                return failure(
                        declarationIndex,
                        declaration,
                        STRUCTURALLY_INVALID,
                        List.of(diagnostic(
                                MODIFIED_STATE_UNCHANGED,
                                STRUCTURALLY_INVALID,
                                declarationIndex,
                                declaration,
                                "beforeState/afterState",
                                "MODIFIED states are semantically unchanged"
                        ))
                );
            }
        }

        return new IntrinsicallyValidChange(declarationIndex, declaration);
    }

    private Optional<ChangeDiagnostic> versionFailure(
            DeclaredChange declaration,
            int index
    ) {
        List<ArtifactState> states = new ArrayList<>();
        declaration.beforeState().ifPresent(states::add);
        declaration.afterState().ifPresent(states::add);
        boolean crossVersion = states.stream().anyMatch(state ->
                !state.schemaVersion().equals(declaration.schemaVersion()));
        if (crossVersion) {
            return Optional.of(diagnostic(
                    CROSS_VERSION_CHANGE_UNSUPPORTED,
                    UNSUPPORTED,
                    index,
                    declaration,
                    "schemaVersion",
                    "Cross-version change declarations are unsupported"
            ));
        }
        if (!declaration.schemaVersion().isSupported()) {
            return Optional.of(diagnostic(
                    UNSUPPORTED_SCHEMA_VERSION,
                    UNSUPPORTED,
                    index,
                    declaration,
                    "schemaVersion",
                    "Canonical QA Model schema version is unsupported"
            ));
        }
        return Optional.empty();
    }

    private void validatePresence(
            DeclaredChange declaration,
            int index,
            List<ChangeDiagnostic> diagnostics
    ) {
        boolean before = declaration.beforeState().isPresent();
        boolean after = declaration.afterState().isPresent();
        switch (declaration.kind()) {
            case ADDED -> {
                if (before) {
                    diagnostics.add(diagnostic(
                            ADDED_BEFORE_STATE_PRESENT,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "beforeState",
                            "ADDED must not declare a before state"
                    ));
                }
                if (!after) {
                    diagnostics.add(diagnostic(
                            ADDED_AFTER_STATE_MISSING,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "afterState",
                            "ADDED requires an after state"
                    ));
                }
            }
            case REMOVED -> {
                if (!before) {
                    diagnostics.add(diagnostic(
                            REMOVED_BEFORE_STATE_MISSING,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "beforeState",
                            "REMOVED requires a before state"
                    ));
                }
                if (after) {
                    diagnostics.add(diagnostic(
                            REMOVED_AFTER_STATE_PRESENT,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "afterState",
                            "REMOVED must not declare an after state"
                    ));
                }
            }
            case MODIFIED -> {
                if (!before) {
                    diagnostics.add(diagnostic(
                            MODIFIED_BEFORE_STATE_MISSING,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "beforeState",
                            "MODIFIED requires a before state"
                    ));
                }
                if (!after) {
                    diagnostics.add(diagnostic(
                            MODIFIED_AFTER_STATE_MISSING,
                            STRUCTURALLY_INVALID,
                            index,
                            declaration,
                            "afterState",
                            "MODIFIED requires an after state"
                    ));
                }
            }
        }
    }

    private void validateStateConsistency(
            DeclaredChange declaration,
            int index,
            String path,
            Optional<ArtifactState> state,
            List<ChangeDiagnostic> diagnostics
    ) {
        state.ifPresent(value -> {
            if (value.category() != declaration.category()) {
                diagnostics.add(diagnostic(
                        ARTIFACT_CATEGORY_MISMATCH,
                        STRUCTURALLY_INVALID,
                        index,
                        declaration,
                        path + ".category",
                        "State category differs from the declaration"
                ));
            }
            if (!value.identity().equals(declaration.identity())) {
                diagnostics.add(diagnostic(
                        ARTIFACT_IDENTITY_MISMATCH,
                        STRUCTURALLY_INVALID,
                        index,
                        declaration,
                        path + ".identity",
                        "State identity differs from the declaration"
                ));
            }
        });
    }

    private boolean declarationsEquivalent(
            List<IndexedDeclaration> declarations
    ) {
        DeclaredChange first = declarations.getFirst().declaration();
        return declarations.stream().skip(1).allMatch(value -> {
            DeclaredChange candidate = value.declaration();
            return first.kind() == candidate.kind()
                    && statesEquivalent(
                            first.beforeState(),
                            candidate.beforeState()
                    )
                    && statesEquivalent(
                            first.afterState(),
                            candidate.afterState()
                    );
        });
    }

    private boolean statesEquivalent(
            Optional<ArtifactState> left,
            Optional<ArtifactState> right
    ) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() && right.isEmpty();
        }
        return semanticEquality.compare(left.orElseThrow(), right.orElseThrow())
                == SEMANTICALLY_EQUAL;
    }

    private IntrinsicallyInvalidChange failure(
            int index,
            DeclaredChange declaration,
            ChangeFailureClassification classification,
            List<ChangeDiagnostic> diagnostics
    ) {
        return new IntrinsicallyInvalidChange(
                index,
                declaration,
                classification,
                diagnostics
        );
    }

    private ChangeDiagnostic diagnostic(
            ChangeDiagnosticCode code,
            ChangeFailureClassification classification,
            int index,
            DeclaredChange declaration,
            String path,
            String message
    ) {
        return diagnostic(
                code,
                classification,
                index,
                declaration.category(),
                declaration.identity(),
                path,
                message
        );
    }

    private ChangeDiagnostic diagnostic(
            ChangeDiagnosticCode code,
            ChangeFailureClassification classification,
            int index,
            ArtifactCategory category,
            CanonicalIdentity identity,
            String path,
            String message
    ) {
        return new ChangeDiagnostic(
                code,
                classification,
                index,
                category,
                identity,
                path,
                message
        );
    }

    private record Target(
            ArtifactCategory category,
            CanonicalIdentity identity
    ) {
    }

    private record IndexedDeclaration(
            int index,
            DeclaredChange declaration,
            IntrinsicChangeResult result
    ) {
    }
}
