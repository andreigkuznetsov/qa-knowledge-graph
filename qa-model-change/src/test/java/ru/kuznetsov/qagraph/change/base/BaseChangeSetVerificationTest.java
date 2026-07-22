package ru.kuznetsov.qagraph.change.base;

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
import ru.kuznetsov.qagraph.change.validation.ChangeFailureClassification;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseChangeSetVerificationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntrinsicChangeValidator intrinsic =
            new IntrinsicChangeValidator();

    @Test
    void shouldVerifyMixedEligibleCandidatesInDeclarationOrder()
            throws Exception {
        DeclaredChange removed = declaration(
                "N-1", ChangeKind.REMOVED,
                Optional.of(node("N-1", "Before")), Optional.empty());
        DeclaredChange added = declaration(
                "N-3", ChangeKind.ADDED,
                Optional.empty(), Optional.of(node("N-3", "Added")));
        DeclaredChange modified = declaration(
                "N-2", ChangeKind.MODIFIED,
                Optional.of(node("N-2", "Before")),
                Optional.of(node("N-2", "After")));

        IntrinsicChangeSetResult candidates = intrinsic.validate(
                new DeclaredChangeSet(List.of(removed, added, modified)));
        BaseChangeVerifier verifier = verifier(
                node("N-2", "Before"),
                node("N-1", "Before")
        );

        BaseChangeSetResult result = verifier.verify(candidates);

        assertEquals(List.of(0, 1, 2),
                result.baseVerifiedCandidates().stream()
                        .map(value -> value.candidate().declarationIndex())
                        .toList());
        assertTrue(result.intrinsicFailures().isEmpty());
        assertTrue(result.ambiguities().isEmpty());
        assertTrue(result.baseFailures().isEmpty());
    }

    @Test
    void shouldPreserveIntrinsicFailuresAndAmbiguitiesWithoutLookup()
            throws Exception {
        DeclaredChange structural = declaration(
                "N-1", ChangeKind.ADDED,
                Optional.empty(), Optional.empty());
        DeclaredChange unsupported = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-2"),
                ChangeKind.ADDED,
                new CanonicalQaModelVersion("9.9"),
                Optional.empty(),
                Optional.empty()
        );
        DeclaredChange duplicateOne = declaration(
                "N-3", ChangeKind.ADDED,
                Optional.empty(), Optional.of(node("N-3", "Same")));
        DeclaredChange duplicateTwo = declaration(
                "N-3", ChangeKind.ADDED,
                Optional.empty(), Optional.of(node("N-3", "Same")));
        IntrinsicChangeSetResult phaseThree = intrinsic.validate(
                new DeclaredChangeSet(List.of(
                        structural,
                        unsupported,
                        duplicateOne,
                        duplicateTwo
                ))
        );

        BaseChangeSetResult result = verifier(
                node("N-1", "Exists"),
                node("N-2", "Exists"),
                node("N-3", "Exists")
        ).verify(phaseThree);

        assertEquals(List.of(
                ChangeFailureClassification.STRUCTURALLY_INVALID,
                ChangeFailureClassification.UNSUPPORTED
        ), result.intrinsicFailures().stream()
                .map(value -> value.classification())
                .toList());
        assertEquals(1, result.ambiguities().size());
        assertEquals(List.of(2, 3),
                result.ambiguities().getFirst().declarationIndices());
        assertTrue(result.baseVerifiedCandidates().isEmpty());
        assertTrue(result.baseFailures().isEmpty());
    }

    @Test
    void shouldReturnBaseFailuresWithoutRelabelling() throws Exception {
        DeclaredChange missing = declaration(
                "N-1", ChangeKind.REMOVED,
                Optional.of(node("N-1", "Before")), Optional.empty());
        IntrinsicChangeSetResult phaseThree = intrinsic.validate(
                new DeclaredChangeSet(List.of(missing))
        );

        BaseChangeSetResult result = verifier().verify(phaseThree);

        assertEquals(ChangeFailureClassification.UNVERIFIABLE,
                result.baseFailures().getFirst().classification());
        assertEquals(0, result.baseFailures().getFirst()
                .candidate().declarationIndex());
    }

    @Test
    void resultShouldBeDeterministicAndExposeImmutableCollections()
            throws Exception {
        DeclaredChange added = declaration(
                "N-1", ChangeKind.ADDED,
                Optional.empty(), Optional.of(node("N-1", "After")));
        IntrinsicChangeSetResult phaseThree = intrinsic.validate(
                new DeclaredChangeSet(List.of(added))
        );
        BaseChangeVerifier verifier = verifier();

        BaseChangeSetResult first = verifier.verify(phaseThree);
        BaseChangeSetResult second = verifier.verify(phaseThree);

        assertEquals(first, second);
        assertThrows(UnsupportedOperationException.class,
                () -> first.intrinsicFailures().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> first.ambiguities().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> first.baseVerifiedCandidates().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> first.baseFailures().clear());
    }

    private BaseChangeVerifier verifier(ArtifactState... states) {
        BaseArtifactIndex index = new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                List.of(states)
        );
        var retained = new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode();
        retained.putObject("project").put("id", "P-1").put("name", "P");
        retained.putArray("sources");
        var evidence = ru.kuznetsov.qagraph.change.root.RootTestFixtures
                .evidence(new ru.kuznetsov.qagraph.change.root.CanonicalRootContext(
                        CanonicalQaModelVersion.V0_1, retained), index);
        return new BaseChangeVerifier(evidence);
    }

    private DeclaredChange declaration(
            String id,
            ChangeKind kind,
            Optional<ArtifactState> before,
            Optional<ArtifactState> after
    ) {
        return new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity(id),
                kind,
                CanonicalQaModelVersion.V0_1,
                before,
                after
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
