package ru.kuznetsov.qagraph.change.materialization;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProposedArtifactModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldRejectNullVersionCollectionsAndMembers() throws Exception {
        assertThrows(NullPointerException.class, () ->
                new ProposedArtifactModel(null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () ->
                new ProposedArtifactModel(
                        CanonicalQaModelVersion.V0_1,
                        null,
                        List.of()
                ));
        List<NodeArtifactState> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () ->
                new ProposedArtifactModel(
                        CanonicalQaModelVersion.V0_1,
                        withNull,
                        List.of()
                ));
    }

    @Test
    void shouldCopySortAndExposeImmutableSeparateCollections()
            throws Exception {
        NodeArtifactState second = node("N-2", "Second");
        NodeArtifactState first = node("N-1", "First");
        RelationshipArtifactState relationship = relationship("R-1");
        List<NodeArtifactState> source = new ArrayList<>(
                List.of(second, first)
        );

        ProposedArtifactModel model = new ProposedArtifactModel(
                CanonicalQaModelVersion.V0_1,
                source,
                List.of(relationship)
        );
        source.clear();

        assertEquals(List.of(
                new CanonicalIdentity("N-1"),
                new CanonicalIdentity("N-2")
        ), model.nodes().stream().map(ArtifactState::identity).toList());
        assertEquals(List.of(relationship), model.relationships());
        assertThrows(UnsupportedOperationException.class,
                () -> model.nodes().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> model.relationships().clear());
    }

    @Test
    void lookupShouldBeExactAndCategorySensitive() throws Exception {
        NodeArtifactState node = node("SAME", "Node");
        RelationshipArtifactState relationship = relationship("SAME");
        ProposedArtifactModel model = new ProposedArtifactModel(
                CanonicalQaModelVersion.V0_1,
                List.of(node),
                List.of(relationship)
        );

        assertEquals(node, model.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("SAME")
        ).orElseThrow());
        assertEquals(relationship, model.lookup(
                ArtifactCategory.RELATIONSHIP,
                new CanonicalIdentity("SAME")
        ).orElseThrow());
        assertTrue(model.lookup(
                ArtifactCategory.NODE,
                new CanonicalIdentity("same")
        ).isEmpty());
    }

    @Test
    void shouldRejectDuplicateTargetsAndVersionMismatch() throws Exception {
        NodeArtifactState first = node("N-1", "First");
        NodeArtifactState duplicate = node("N-1", "Second");
        assertThrows(IllegalArgumentException.class, () ->
                new ProposedArtifactModel(
                        CanonicalQaModelVersion.V0_1,
                        List.of(first, duplicate),
                        List.of()
                ));

        NodeArtifactState wrongVersion = new NodeArtifactState(
                new CanonicalQaModelVersion("9.9"),
                first.snapshot()
        );
        assertThrows(IllegalArgumentException.class, () ->
                new ProposedArtifactModel(
                        CanonicalQaModelVersion.V0_1,
                        List.of(wrongVersion),
                        List.of()
                ));
    }

    private NodeArtifactState node(String id, String name) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"CHECK","name":"%s"}
                        """.formatted(id, name))
        );
    }

    private RelationshipArtifactState relationship(String id)
            throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1,
                mapper.readTree("""
                        {"id":"%s","type":"RELATED_TO",
                         "from":"N-1","to":"N-2"}
                        """.formatted(id))
        );
    }
}
