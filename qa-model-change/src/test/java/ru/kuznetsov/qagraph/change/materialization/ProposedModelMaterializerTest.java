package ru.kuznetsov.qagraph.change.materialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.base.BaseChangeVerifier;
import ru.kuznetsov.qagraph.change.base.BaseVerifiedChange;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.ChangeKind;
import ru.kuznetsov.qagraph.change.model.DeclaredChange;
import ru.kuznetsov.qagraph.change.model.DeclaredChangeSet;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.CHANGE_SET_NOT_MATERIALIZABLE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_ADDED_TARGET_PRESENT;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_BASE_EVIDENCE_MISMATCH;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_BASE_UNSUPPORTED;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_DUPLICATE_TARGET_WRITE;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_MODIFIED_TARGET_MISSING;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_REMOVED_TARGET_MISSING;
import static ru.kuznetsov.qagraph.change.materialization.MaterializationDiagnosticCode.MATERIALIZATION_STATE_INCONSISTENT;

class ProposedModelMaterializerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IntrinsicChangeValidator intrinsic =
            new IntrinsicChangeValidator();
    private final ProposedModelMaterializer materializer =
            new ProposedModelMaterializer();

    @Test
    void shouldAddNodeAndRelationshipAndPreserveBase() throws Exception {
        NodeArtifactState existing = node("N-1", "Existing");
        BaseArtifactIndex base = base(existing);
        NodeArtifactState addedNode = node("N-2", "Added");
        RelationshipArtifactState addedRelationship = relationship(
                "R-1", "N-1", "N-2"
        );
        BaseChangeSetResult verified = verify(base, List.of(
                added(addedNode),
                added(addedRelationship)
        ));

        ProposedArtifactModel proposed = success(base, verified);

        assertEquals(List.of("N-1", "N-2"), proposed.nodes().stream()
                .map(value -> value.identity().value()).toList());
        assertEquals(List.of(addedRelationship), proposed.relationships());
        assertEquals(addedNode.snapshot(), proposed.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-2")
        ).orElseThrow().snapshot());
        assertEquals(List.of(existing), base.artifacts());
    }

    @Test
    void addedSameIdentityInDifferentCategoryShouldNotConflict()
            throws Exception {
        BaseArtifactIndex base = base(node("SAME", "Node"));
        RelationshipArtifactState relationship = relationship(
                "SAME", "N-1", "N-2"
        );

        ProposedArtifactModel proposed = success(
                base,
                verify(base, List.of(added(relationship)))
        );

        assertTrue(proposed.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("SAME")
        ).isPresent());
        assertEquals(relationship, proposed.lookup(
                ArtifactCategory.RELATIONSHIP,
                new CanonicalIdentity("SAME")
        ).orElseThrow());
    }

    @Test
    void shouldRemoveExactTargetsWithoutCascade() throws Exception {
        NodeArtifactState removedNode = node("N-1", "Removed");
        NodeArtifactState preservedNode = node("N-2", "Preserved");
        RelationshipArtifactState preservedRelationship = relationship(
                "R-1", "N-1", "N-2"
        );
        RelationshipArtifactState removedRelationship = relationship(
                "R-2", "N-2", "N-1"
        );
        BaseArtifactIndex base = base(
                removedNode,
                preservedNode,
                preservedRelationship,
                removedRelationship
        );

        ProposedArtifactModel proposed = success(base, verify(base, List.of(
                removed(removedNode),
                removed(removedRelationship)
        )));

        assertEquals(List.of(preservedNode), proposed.nodes());
        assertEquals(List.of(preservedRelationship),
                proposed.relationships());
        assertEquals(4, base.artifacts().size());
    }

    @Test
    void shouldReplaceExactStatesWithoutMerging() throws Exception {
        NodeArtifactState beforeNode = state("""
                {"id":"N-1","type":"CHECK","name":"Before",
                 "metadata":{"mustDisappear":true}}
                """);
        NodeArtifactState afterNode = node("N-1", "After");
        RelationshipArtifactState beforeRelationship = relationship(
                "R-1", "N-1", "N-2"
        );
        RelationshipArtifactState afterRelationship = relationshipWithFlag(
                "R-1", false
        );
        NodeArtifactState unrelated = node("N-2", "Unrelated");
        BaseArtifactIndex base = base(
                beforeNode,
                beforeRelationship,
                unrelated
        );

        ProposedArtifactModel proposed = success(base, verify(base, List.of(
                modified(beforeNode, afterNode),
                modified(beforeRelationship, afterRelationship)
        )));

        ArtifactState proposedNode = proposed.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1")
        ).orElseThrow();
        assertEquals(afterNode.snapshot(), proposedNode.snapshot());
        assertTrue(proposedNode.snapshot().get("metadata") == null);
        assertEquals(afterRelationship.snapshot(), proposed.lookup(
                ArtifactCategory.RELATIONSHIP,
                new CanonicalIdentity("R-1")
        ).orElseThrow().snapshot());
        assertEquals(unrelated, proposed.nodes().get(1));
        assertEquals("Before", beforeNode.snapshot().path("name").asText());
    }

    @Test
    void independentChangePermutationShouldProduceEqualCanonicalModel()
            throws Exception {
        NodeArtifactState removed = node("N-1", "Removed");
        NodeArtifactState before = node("N-2", "Before");
        NodeArtifactState after = node("N-2", "After");
        NodeArtifactState added = node("N-3", "Added");
        BaseArtifactIndex base = base(removed, before);
        List<DeclaredChange> forward = List.of(
                added(added),
                removed(removed),
                modified(before, after)
        );
        List<DeclaredChange> reverse = List.of(
                modified(before, after),
                removed(removed),
                added(added)
        );

        ProposedArtifactModel first = success(
                base, verify(base, forward));
        ProposedArtifactModel second = success(
                base, verify(base, reverse));

        assertEquals(first, second);
        assertEquals(List.of("N-2", "N-3"), first.nodes().stream()
                .map(value -> value.identity().value()).toList());
    }

    @Test
    void earlierFailuresShouldPreventPartialMaterialization()
            throws Exception {
        NodeArtifactState existing = node("N-1", "Existing");
        BaseArtifactIndex base = base(existing);
        DeclaredChange validAddition = added(node("N-2", "Added"));
        DeclaredChange invalid = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-3"),
                ChangeKind.ADDED,
                CanonicalQaModelVersion.V0_1,
                Optional.empty(),
                Optional.empty()
        );
        BaseChangeSetResult result = verify(
                base,
                List.of(validAddition, invalid)
        );

        ProposedModelMaterializationFailure failure = failure(
                base, result, CHANGE_SET_NOT_MATERIALIZABLE);

        assertEquals(1, failure.sourceResult()
                .intrinsicFailures().size());
        assertEquals(1, failure.sourceResult()
                .baseVerifiedCandidates().size());
        assertTrue(failure.diagnostics().size() == 1);
    }

    @Test
    void ambiguityAndBaseFailureShouldPreventMaterialization()
            throws Exception {
        BaseArtifactIndex base = base(node("N-1", "Existing"));
        DeclaredChange duplicateOne = added(node("N-2", "Same"));
        DeclaredChange duplicateTwo = added(node("N-2", "Same"));
        BaseChangeSetResult ambiguous = verify(
                base,
                List.of(duplicateOne, duplicateTwo)
        );
        failure(base, ambiguous, CHANGE_SET_NOT_MATERIALIZABLE);
        assertEquals(1, ambiguous.ambiguities().size());

        BaseChangeSetResult baseFailure = verify(
                base,
                List.of(added(node("N-1", "Conflicting")))
        );
        failure(base, baseFailure, CHANGE_SET_NOT_MATERIALIZABLE);
        assertEquals(1, baseFailure.baseFailures().size());
    }

    @Test
    void unsupportedIntrinsicFailureShouldRemainAccessible()
            throws Exception {
        CanonicalQaModelVersion unsupported =
                new CanonicalQaModelVersion("9.9");
        DeclaredChange declaration = new DeclaredChange(
                ArtifactCategory.NODE,
                new CanonicalIdentity("N-1"),
                ChangeKind.ADDED,
                unsupported,
                Optional.empty(),
                Optional.empty()
        );
        BaseArtifactIndex base = base();
        BaseChangeSetResult result = verify(base, List.of(declaration));

        ProposedModelMaterializationFailure failure = failure(
                base, result, CHANGE_SET_NOT_MATERIALIZABLE);

        assertEquals(
                ru.kuznetsov.qagraph.change.validation
                        .ChangeFailureClassification.UNSUPPORTED,
                failure.sourceResult().intrinsicFailures()
                        .getFirst().classification()
        );
    }

    @Test
    void staleOrUnrelatedBaseEvidenceShouldBeRejected() throws Exception {
        NodeArtifactState state = node("N-1", "Same");
        BaseArtifactIndex verifiedBase = base(state);
        BaseChangeSetResult result = verify(
                verifiedBase,
                List.of(removed(state))
        );
        BaseArtifactIndex equalButDifferentInstance = base(state);
        BaseArtifactIndex changedUnrelated = base(
                state,
                node("OTHER", "Changed")
        );

        failure(equalButDifferentInstance, result,
                MATERIALIZATION_BASE_EVIDENCE_MISMATCH);
        failure(changedUnrelated, result,
                MATERIALIZATION_BASE_EVIDENCE_MISMATCH);
        success(verifiedBase, result);
    }

    @Test
    void unsupportedBaseShouldFailWithoutProposedModel() throws Exception {
        BaseArtifactIndex unsupported = new BaseArtifactIndex(
                new CanonicalQaModelVersion("9.9"),
                List.of()
        );
        BaseChangeSetResult forged = new BaseChangeSetResult(
                unsupported,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        failure(unsupported, forged, MATERIALIZATION_BASE_UNSUPPORTED);
    }

    @Test
    void defensiveConsistencyFailuresShouldReturnNoPartialModel()
            throws Exception {
        NodeArtifactState existing = node("N-1", "Existing");
        BaseArtifactIndex base = base(existing);
        BaseVerifiedChange forgedAdded = new BaseVerifiedChange(
                new IntrinsicallyValidChange(
                        0,
                        added(node("N-1", "After"))
                ),
                Optional.empty()
        );
        BaseChangeSetResult addedPresent = forged(
                base,
                List.of(forgedAdded)
        );
        failure(base, addedPresent, MATERIALIZATION_ADDED_TARGET_PRESENT);

        BaseArtifactIndex empty = base();
        BaseVerifiedChange forgedRemoved = new BaseVerifiedChange(
                new IntrinsicallyValidChange(0, removed(existing)),
                Optional.of(existing)
        );
        failure(empty, forged(empty, List.of(forgedRemoved)),
                MATERIALIZATION_REMOVED_TARGET_MISSING);

        NodeArtifactState after = node("N-1", "After");
        BaseVerifiedChange forgedModified = new BaseVerifiedChange(
                new IntrinsicallyValidChange(
                        0,
                        modified(existing, after)
                ),
                Optional.of(existing)
        );
        failure(empty, forged(empty, List.of(forgedModified)),
                MATERIALIZATION_MODIFIED_TARGET_MISSING);
    }

    @Test
    void duplicateWritesAndInconsistentAfterVersionShouldFail()
            throws Exception {
        BaseArtifactIndex base = base();
        BaseVerifiedChange first = verifiedAdded(
                0, node("N-1", "First"));
        BaseVerifiedChange second = verifiedAdded(
                1, node("N-1", "Second"));
        failure(base, forged(base, List.of(first, second)),
                MATERIALIZATION_DUPLICATE_TARGET_WRITE);

        NodeArtifactState normal = node("N-2", "After");
        NodeArtifactState wrongVersion = new NodeArtifactState(
                new CanonicalQaModelVersion("9.9"),
                normal.snapshot()
        );
        BaseVerifiedChange inconsistent = verifiedAdded(0, wrongVersion);
        failure(base, forged(base, List.of(inconsistent)),
                MATERIALIZATION_STATE_INCONSISTENT);
    }

    @Test
    void resultsAndSnapshotsShouldRemainImmutableAndDeterministic()
            throws Exception {
        BaseArtifactIndex base = base();
        BaseChangeSetResult verified = verify(
                base,
                List.of(added(node("N-1", "Added")))
        );

        ProposedArtifactModel first = success(base, verified);
        ProposedArtifactModel second = success(base, verified);
        first.nodes().getFirst().snapshot().withObject("/metadata")
                .put("changed", true);

        assertEquals(first, second);
        assertTrue(first.nodes().getFirst().snapshot()
                .get("metadata") == null);
        assertThrows(UnsupportedOperationException.class,
                () -> first.nodes().clear());
    }

    private ProposedArtifactModel success(
            BaseArtifactIndex base,
            BaseChangeSetResult result
    ) {
        return assertInstanceOf(
                ProposedModelMaterialized.class,
                materializer.materialize(base, result)
        ).proposedModel();
    }

    private ProposedModelMaterializationFailure failure(
            BaseArtifactIndex base,
            BaseChangeSetResult result,
            MaterializationDiagnosticCode code
    ) {
        ProposedModelMaterializationFailure failure = assertInstanceOf(
                ProposedModelMaterializationFailure.class,
                materializer.materialize(base, result)
        );
        assertEquals(List.of(code), failure.diagnostics().stream()
                .map(MaterializationDiagnostic::code)
                .toList());
        assertThrows(UnsupportedOperationException.class,
                () -> failure.diagnostics().clear());
        return failure;
    }

    private BaseChangeSetResult verify(
            BaseArtifactIndex base,
            List<DeclaredChange> declarations
    ) {
        IntrinsicChangeSetResult intrinsicResult = intrinsic.validate(
                new DeclaredChangeSet(declarations)
        );
        return new BaseChangeVerifier(base).verify(intrinsicResult);
    }

    private BaseChangeSetResult forged(
            BaseArtifactIndex base,
            List<BaseVerifiedChange> changes
    ) {
        return new BaseChangeSetResult(
                base,
                List.of(),
                List.of(),
                changes,
                List.of()
        );
    }

    private BaseVerifiedChange verifiedAdded(
            int index,
            ArtifactState state
    ) {
        return new BaseVerifiedChange(
                new IntrinsicallyValidChange(index, added(state)),
                Optional.empty()
        );
    }

    private DeclaredChange added(ArtifactState after) {
        return declaration(
                after.category(),
                after.identity(),
                ChangeKind.ADDED,
                after.schemaVersion(),
                Optional.empty(),
                Optional.of(after)
        );
    }

    private DeclaredChange removed(ArtifactState before) {
        return declaration(
                before.category(),
                before.identity(),
                ChangeKind.REMOVED,
                before.schemaVersion(),
                Optional.of(before),
                Optional.empty()
        );
    }

    private DeclaredChange modified(
            ArtifactState before,
            ArtifactState after
    ) {
        return declaration(
                before.category(),
                before.identity(),
                ChangeKind.MODIFIED,
                before.schemaVersion(),
                Optional.of(before),
                Optional.of(after)
        );
    }

    private DeclaredChange declaration(
            ArtifactCategory category,
            CanonicalIdentity identity,
            ChangeKind kind,
            CanonicalQaModelVersion version,
            Optional<ArtifactState> before,
            Optional<ArtifactState> after
    ) {
        return new DeclaredChange(
                category,
                identity,
                kind,
                version,
                before,
                after
        );
    }

    private BaseArtifactIndex base(ArtifactState... states) {
        return new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                List.of(states)
        );
    }

    private NodeArtifactState node(String id, String name) throws Exception {
        return state("""
                {"id":"%s","type":"CHECK","name":"%s"}
                """.formatted(id, name));
    }

    private NodeArtifactState state(String json) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree(json)
        );
    }

    private RelationshipArtifactState relationship(
            String id,
            String from,
            String to
    ) throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"RELATED_TO",
                         "from":"%s","to":"%s"}
                        """.formatted(id, from, to))
        );
    }

    private RelationshipArtifactState relationshipWithFlag(
            String id,
            boolean flag
    ) throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"RELATED_TO",
                         "from":"N-1","to":"N-2",
                         "properties":{"flag":%s}}
                        """.formatted(id, flag))
        );
    }
}
