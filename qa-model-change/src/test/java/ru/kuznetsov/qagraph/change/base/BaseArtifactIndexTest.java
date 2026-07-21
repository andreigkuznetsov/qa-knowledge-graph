package ru.kuznetsov.qagraph.change.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseArtifactIndexTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldRejectNullCollectionAndMembers() {
        assertThrows(NullPointerException.class, () ->
                new BaseArtifactIndex(CanonicalQaModelVersion.V0_1, null));
        List<ArtifactState> values = new ArrayList<>();
        values.add(null);
        assertThrows(IllegalArgumentException.class, () ->
                new BaseArtifactIndex(
                        CanonicalQaModelVersion.V0_1,
                        values
                ));
    }

    @Test
    void shouldCopyInputAndExposeImmutableArtifacts() throws Exception {
        ArtifactState state = node("N-1", "Node");
        List<ArtifactState> source = new ArrayList<>(List.of(state));
        BaseArtifactIndex index = new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                source
        );
        source.clear();

        assertEquals(List.of(state), index.artifacts());
        assertThrows(UnsupportedOperationException.class,
                () -> index.artifacts().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> index.duplicates().clear());
    }

    @Test
    void shouldLookupByExactCategoryAndIdentity() throws Exception {
        ArtifactState node = node("SAME", "Node");
        ArtifactState relationship = relationship("SAME");
        BaseArtifactIndex index = index(node, relationship);

        BaseArtifactFound nodeResult = assertInstanceOf(
                BaseArtifactFound.class,
                index.lookup(
                        ArtifactCategory.NODE,
                        new CanonicalIdentity("SAME")
                )
        );
        BaseArtifactFound relationshipResult = assertInstanceOf(
                BaseArtifactFound.class,
                index.lookup(
                        ArtifactCategory.RELATIONSHIP,
                        new CanonicalIdentity("SAME")
                )
        );

        assertEquals(node, nodeResult.state());
        assertEquals(relationship, relationshipResult.state());
        assertInstanceOf(BaseArtifactMissing.class, index.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("same")
        ));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentity(" SAME "));
    }

    @Test
    void shouldDetectDuplicateNodeAndRelationshipTargets()
            throws Exception {
        BaseArtifactIndex index = index(
                relationship("R-1"),
                node("N-1", "Second"),
                relationship("R-1"),
                node("N-1", "First")
        );

        assertEquals(List.of(
                new BaseArtifactDuplicate(
                        ArtifactCategory.NODE,
                        new CanonicalIdentity("N-1"),
                        2
                ),
                new BaseArtifactDuplicate(
                        ArtifactCategory.RELATIONSHIP,
                        new CanonicalIdentity("R-1"),
                        2
                )
        ), index.duplicates());
        BaseArtifactAmbiguous nodeLookup = assertInstanceOf(
                BaseArtifactAmbiguous.class,
                index.lookup(
                        ArtifactCategory.NODE,
                        new CanonicalIdentity("N-1")
                )
        );
        assertEquals(2, nodeLookup.states().size());
        assertThrows(UnsupportedOperationException.class,
                () -> nodeLookup.states().clear());
    }

    @Test
    void duplicateEvidenceShouldBeDeterministicAcrossInputOrder()
            throws Exception {
        List<BaseArtifactDuplicate> first = index(
                relationship("R-1"),
                node("N-1", "One"),
                node("N-1", "Two"),
                relationship("R-1")
        ).duplicates();
        List<BaseArtifactDuplicate> second = index(
                node("N-1", "Two"),
                relationship("R-1"),
                relationship("R-1"),
                node("N-1", "One")
        ).duplicates();

        assertEquals(first, second);
    }

    @Test
    void shouldExposeUnsupportedArtifactVersionsDeterministically()
            throws Exception {
        CanonicalQaModelVersion unsupported =
                new CanonicalQaModelVersion("9.9");
        ArtifactState second = new NodeArtifactState(
                unsupported,
                node("N-2", "Second").snapshot()
        );
        ArtifactState first = new NodeArtifactState(
                unsupported,
                node("N-1", "First").snapshot()
        );
        BaseArtifactIndex index = index(second, first);

        assertEquals(List.of(
                new CanonicalIdentity("N-1"),
                new CanonicalIdentity("N-2")
        ), index.unsupportedVersionArtifacts().stream()
                .map(ArtifactState::identity)
                .toList());
        assertEquals(2, index.incompatibleVersionArtifacts().size());
        assertThrows(UnsupportedOperationException.class,
                () -> index.unsupportedVersionArtifacts().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> index.incompatibleVersionArtifacts().clear());
    }

    private BaseArtifactIndex index(ArtifactState... states) {
        return new BaseArtifactIndex(
                CanonicalQaModelVersion.V0_1,
                List.of(states)
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

    private ArtifactState relationship(String id) throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"RELATED_TO",
                         "from":"N-1","to":"N-2"}
                        """.formatted(id))
        );
    }
}
