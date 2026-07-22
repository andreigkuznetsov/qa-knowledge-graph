package ru.kuznetsov.qagraph.change.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.aggregate.*;
import ru.kuznetsov.qagraph.change.base.*;
import ru.kuznetsov.qagraph.change.complete.*;
import ru.kuznetsov.qagraph.change.materialization.*;
import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.root.*;
import ru.kuznetsov.qagraph.change.validation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Integrated release gate using only public production entry points. */
class CanonicalChangeReleaseGateTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mixedKindsCategoryCollisionAndPermutationShouldRemainDeterministic()
            throws Exception {
        var baseJson = mapper.readTree("""
            {"schemaVersion":"0.1","project":{"id":"P-1","name":"P"},
             "sources":[],"nodes":[
              {"id":"T-1","type":"TEST_IMPLEMENTATION","name":"test","testImplementation":{"code":"T-1","executionType":"MANUAL","preconditions":[],"steps":[{"order":1,"action":"run"}]}},
              {"id":"N-MOD","type":"CHECK","name":"old","check":{"checkType":"SQL","assertion":"ok"}},
              {"id":"N-REMOVE","type":"CHECK","name":"remove","check":{"checkType":"SQL","assertion":"ok"}},
              {"id":"N-END","type":"CHECK","name":"end","check":{"checkType":"SQL","assertion":"ok"}}],
             "relationships":[
              {"id":"R-MOD","from":"T-1","type":"HAS_CHECK","to":"N-END"},
              {"id":"R-REMOVE","from":"T-1","type":"HAS_CHECK","to":"N-REMOVE"}]}
            """);
        CanonicalBaseModelEvidence base = ((CanonicalBaseEvidenceExtracted)
                new CanonicalBaseEvidenceExtractor().extract(baseJson)).evidence();
        ArtifactState oldNode = base.artifactIndex().lookup(
                ArtifactCategory.NODE, new CanonicalIdentity("N-MOD"))
                instanceof BaseArtifactFound found ? found.state() : null;
        ArtifactState removeNode = ((BaseArtifactFound) base.artifactIndex()
                .lookup(ArtifactCategory.NODE,
                        new CanonicalIdentity("N-REMOVE"))).state();
        ArtifactState oldRel = ((BaseArtifactFound) base.artifactIndex()
                .lookup(ArtifactCategory.RELATIONSHIP,
                        new CanonicalIdentity("R-MOD"))).state();
        ArtifactState removeRel = ((BaseArtifactFound) base.artifactIndex()
                .lookup(ArtifactCategory.RELATIONSHIP,
                        new CanonicalIdentity("R-REMOVE"))).state();
        NodeArtifactState modified = node("N-MOD", "new");
        NodeArtifactState added = node("SHARED", "added");
        RelationshipArtifactState modifiedRel = relationship(
                "R-MOD", "T-1", "SHARED");
        RelationshipArtifactState sharedRel = relationship(
                "SHARED", "T-1", "N-MOD");
        List<DeclaredChange> changes = List.of(
                change(ChangeKind.MODIFIED, oldNode, modified),
                change(ChangeKind.REMOVED, removeNode, null),
                change(ChangeKind.ADDED, null, added),
                change(ChangeKind.MODIFIED, oldRel, modifiedRel),
                change(ChangeKind.REMOVED, removeRel, null),
                change(ChangeKind.ADDED, null, sharedRel));

        VerifiedChangeSet first = run(base, changes);
        List<DeclaredChange> reversed = new ArrayList<>(changes);
        java.util.Collections.reverse(reversed);
        VerifiedChangeSet permuted = run(base, reversed);

        assertTrue(first.proposedModel().lookup(
                ArtifactCategory.NODE, new CanonicalIdentity("SHARED")).isPresent());
        assertTrue(first.proposedModel().lookup(
                ArtifactCategory.RELATIONSHIP, new CanonicalIdentity("SHARED")).isPresent());
        assertEquals(first.proposedModel(), permuted.proposedModel());
        assertEquals(first.proposedRoot().snapshot(),
                permuted.proposedRoot().snapshot());
        assertEquals(first.warnings(), permuted.warnings());
        assertEquals(first.schemaVersion(), permuted.schemaVersion());
        assertEquals(first, new FinalChangeSetVerifier().verify(
                first.completeValidation()));
        assertEquals("old", oldNode.snapshot().path("name").asText());
        assertEquals(4, base.artifactIndex().artifacts().stream()
                .filter(value -> value.category() == ArtifactCategory.NODE)
                .count());
    }

    private VerifiedChangeSet run(CanonicalBaseModelEvidence base,
                                  List<DeclaredChange> changes) {
        IntrinsicChangeSetResult intrinsic = new IntrinsicChangeValidator()
                .validate(new DeclaredChangeSet(changes));
        BaseChangeSetResult verified = new BaseChangeVerifier(base)
                .verify(intrinsic);
        ProposedModelMaterialized materialized = assertInstanceOf(
                ProposedModelMaterialized.class,
                new ProposedModelMaterializer().materialize(
                        base.artifactIndex(), verified));
        AggregateTransitionValid aggregate = assertInstanceOf(
                AggregateTransitionValid.class,
                new AggregateTransitionValidator().validate(materialized));
        ProposedRootReconstructed root = assertInstanceOf(
                ProposedRootReconstructed.class,
                new ProposedCanonicalRootReconstructor().reconstruct(
                        base, aggregate));
        CompleteProposedRootValidationResult completeResult =
                new CompleteProposedRootValidator().validate(root);
        CompleteProposedRootValid complete = assertInstanceOf(
                CompleteProposedRootValid.class, completeResult,
                completeResult::toString);
        return assertInstanceOf(VerifiedChangeSet.class,
                new FinalChangeSetVerifier().verify(complete));
    }

    private DeclaredChange change(ChangeKind kind, ArtifactState before,
                                  ArtifactState after) {
        ArtifactState target = before == null ? after : before;
        return new DeclaredChange(target.category(), target.identity(), kind,
                CanonicalQaModelVersion.V0_1, Optional.ofNullable(before),
                Optional.ofNullable(after));
    }
    private NodeArtifactState node(String id, String name) throws Exception {
        return new NodeArtifactState(CanonicalQaModelVersion.V0_1,
                mapper.readTree("{\"id\":\"" + id + "\",\"type\":\"CHECK\",\"name\":\"" + name + "\",\"check\":{\"checkType\":\"SQL\",\"assertion\":\"ok\"}}"));
    }
    private RelationshipArtifactState relationship(String id, String from,
                                                     String to) throws Exception {
        return new RelationshipArtifactState(CanonicalQaModelVersion.V0_1,
                mapper.readTree("{\"id\":\"" + id + "\",\"from\":\"" + from + "\",\"type\":\"HAS_CHECK\",\"to\":\"" + to + "\"}"));
    }
}
