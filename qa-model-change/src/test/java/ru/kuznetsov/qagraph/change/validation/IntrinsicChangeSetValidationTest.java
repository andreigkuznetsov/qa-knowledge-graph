package ru.kuznetsov.qagraph.change.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.CONTRADICTORY_CHANGE_TARGET;
import static ru.kuznetsov.qagraph.change.validation.ChangeDiagnosticCode.DUPLICATE_CHANGE_TARGET;

class IntrinsicChangeSetValidationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntrinsicChangeValidator validator =
            new IntrinsicChangeValidator();

    @Test
    void uniqueValidDeclarationsShouldRemainCandidatesInInputOrder()
            throws Exception {
        DeclaredChange first = added("N-1", "First");
        DeclaredChange second = added("N-2", "Second");

        IntrinsicChangeSetResult result = validator.validate(
                new DeclaredChangeSet(List.of(first, second)));

        assertEquals(List.of(first, second), result.validCandidates().stream()
                .map(IntrinsicallyValidChange::declaration)
                .toList());
        assertTrue(result.failedDeclarations().isEmpty());
        assertTrue(result.ambiguities().isEmpty());
    }

    @Test
    void identicalDuplicateTargetShouldProduceOneDeterministicAmbiguity()
            throws Exception {
        DeclaredChange first = added("N-1", "Same");
        DeclaredChange equivalent = addedWithJson("""
                {"name":"Same","type":"CHECK","id":"N-1"}
                """);

        IntrinsicChangeSetResult result = validator.validate(
                new DeclaredChangeSet(List.of(first, equivalent)));

        assertTrue(result.validCandidates().isEmpty());
        assertEquals(1, result.ambiguities().size());
        assertEquals(DUPLICATE_CHANGE_TARGET,
                result.ambiguities().getFirst().diagnostic().code());
        assertEquals(List.of(0, 1),
                result.ambiguities().getFirst().declarationIndices());
        assertThrows(UnsupportedOperationException.class,
                () -> result.ambiguities().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.ambiguities().getFirst()
                        .declarationIndices().clear());
    }

    @Test
    void differentKindsAndClaimsShouldBeContradictory() throws Exception {
        DeclaredChange added = added("N-1", "After");
        DeclaredChange removed = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.REMOVED,
                CanonicalQaModelVersion.V0_1,
                Optional.of(node("N-1", "Before")),
                Optional.empty()
        );

        IntrinsicChangeSetResult result = validator.validate(
                new DeclaredChangeSet(List.of(added, removed)));

        assertEquals(CONTRADICTORY_CHANGE_TARGET,
                result.ambiguities().getFirst().diagnostic().code());
    }

    @Test
    void permutationShouldPreserveTargetClassification() throws Exception {
        DeclaredChange added = added("N-1", "After");
        DeclaredChange removed = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.REMOVED,
                CanonicalQaModelVersion.V0_1,
                Optional.of(node("N-1", "Before")),
                Optional.empty()
        );

        ChangeDiagnosticCode forward = validator.validate(
                        new DeclaredChangeSet(List.of(added, removed)))
                .ambiguities().getFirst().diagnostic().code();
        ChangeDiagnosticCode reverse = validator.validate(
                        new DeclaredChangeSet(List.of(removed, added)))
                .ambiguities().getFirst().diagnostic().code();

        assertEquals(forward, reverse);
        assertEquals(CONTRADICTORY_CHANGE_TARGET, reverse);
    }

    @Test
    void structuralFailureShouldSuppressAmbiguityForTheTarget()
            throws Exception {
        DeclaredChange invalid = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.empty()
        );
        DeclaredChange valid = added("N-1", "After");

        IntrinsicChangeSetResult result = validator.validate(
                new DeclaredChangeSet(List.of(invalid, valid)));

        assertEquals(1, result.failedDeclarations().size());
        assertTrue(result.ambiguities().isEmpty());
    }

    @Test
    void resultCollectionsAndDiagnosticSourceShouldBeImmutable()
            throws Exception {
        DeclaredChange declaration = added("N-1", "After");
        List<IntrinsicallyValidChange> source = new ArrayList<>();
        source.add(new IntrinsicallyValidChange(0, declaration));
        IntrinsicChangeSetResult result = new IntrinsicChangeSetResult(
                new DeclaredChangeSet(List.of(declaration)),
                source, List.of(), List.of());
        source.clear();

        assertEquals(1, result.validCandidates().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.validCandidates().clear());

        ChangeDiagnostic diagnostic = new ChangeDiagnostic(
                ChangeDiagnosticCode.ADDED_AFTER_STATE_MISSING,
                ChangeFailureClassification.STRUCTURALLY_INVALID,
                0,
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                "afterState",
                "missing"
        );
        List<ChangeDiagnostic> diagnosticSource =
                new ArrayList<>(List.of(diagnostic));
        IntrinsicallyInvalidChange failure = new IntrinsicallyInvalidChange(
                0,
                declaration,
                ChangeFailureClassification.STRUCTURALLY_INVALID,
                diagnosticSource
        );
        diagnosticSource.clear();

        assertEquals(1, failure.diagnostics().size());
        assertThrows(UnsupportedOperationException.class,
                () -> failure.diagnostics().clear());
    }

    private DeclaredChange added(String id, String name) throws Exception {
        return new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity(id),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.of(node(id, name))
        );
    }

    private DeclaredChange addedWithJson(String json) throws Exception {
        ArtifactState state = new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree(json)
        );
        return new DeclaredChange(
                ArtifactCategory.NODE,
                state.identity(),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.of(state)
        );
    }

    private ArtifactState node(String id, String name) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"CHECK","name":"%s"}
                        """.formatted(id, name))
        );
    }
}
