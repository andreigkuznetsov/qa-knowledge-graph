package ru.kuznetsov.qagraph.change.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactStateTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void nodeStateShouldExposeCategoryIdentityVersionAndExactSnapshot()
            throws Exception {
        JsonNode snapshot = json("""
                {"id":"N-1","type":"CHECK","name":"Check",
                 "metadata":{"nested":[1,2]}}
                """);

        NodeArtifactState state = new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, snapshot);

        assertEquals(ArtifactCategory.NODE, state.category());
        assertEquals(new CanonicalIdentity("N-1"), state.identity());
        assertEquals(CanonicalQaModelVersion.V0_1, state.schemaVersion());
        assertEquals(snapshot, state.snapshot());
    }

    @Test
    void relationshipStateShouldExposeCategoryIdentityAndExactSnapshot()
            throws Exception {
        JsonNode snapshot = json("""
                {"id":"R-1","from":"T-1","type":"HAS_CHECK",
                 "to":"C-1","properties":{"weight":1.00}}
                """);

        RelationshipArtifactState state = new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1, snapshot);

        assertEquals(ArtifactCategory.RELATIONSHIP, state.category());
        assertEquals(new CanonicalIdentity("R-1"), state.identity());
        assertEquals(snapshot, state.snapshot());
    }

    @Test
    void nodeStateShouldDeepCopyOnIngressAndEgress() throws Exception {
        ObjectNode original = (ObjectNode) json("""
                {"id":"N-1","type":"CHECK","name":"Check",
                 "metadata":{"nested":[1,2]}}
                """);
        NodeArtifactState state = new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, original);

        ((ObjectNode) original.path("metadata")).withArray("nested").add(3);
        original.put("name", "Changed");
        ObjectNode exposed = (ObjectNode) state.snapshot();
        ((ObjectNode) exposed.path("metadata")).withArray("nested").removeAll();
        exposed.put("name", "Changed again");

        assertEquals("Check", state.snapshot().path("name").asText());
        assertEquals(2,
                state.snapshot().path("metadata").path("nested").size());
        assertEquals(new CanonicalIdentity("N-1"), state.identity());
        assertEquals(CanonicalQaModelVersion.V0_1, state.schemaVersion());
    }

    @Test
    void relationshipStateShouldDeepCopyOnIngressAndEgress()
            throws Exception {
        ObjectNode original = (ObjectNode) json("""
                {"id":"R-1","from":"A","type":"RELATED_TO","to":"B",
                 "properties":{"nested":[1,2]}}
                """);
        RelationshipArtifactState state = new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1, original);

        original.put("to", "CHANGED");
        ObjectNode exposed = (ObjectNode) state.snapshot();
        exposed.put("from", "CHANGED");
        exposed.with("properties").withArray("nested").add(3);

        assertEquals("A", state.snapshot().path("from").asText());
        assertEquals("B", state.snapshot().path("to").asText());
        assertEquals(2,
                state.snapshot().path("properties").path("nested").size());
    }

    @Test
    void wrappersShouldRejectNullAndNonObjectSnapshots() {
        assertThrows(NullPointerException.class,
                () -> new NodeArtifactState(
                        CanonicalQaModelVersion.V0_1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new NodeArtifactState(
                        CanonicalQaModelVersion.V0_1,
                        mapper.createArrayNode()));
        assertThrows(NullPointerException.class,
                () -> new RelationshipArtifactState(
                        null, mapper.createObjectNode()));
    }

    @Test
    void wrappersShouldRejectMissingNonTextualAndInvalidIdentity()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> nodeState("{" +
                        "\"type\":\"CHECK\",\"name\":\"Check\"}"));
        assertThrows(IllegalArgumentException.class,
                () -> nodeState("{" +
                        "\"id\":1,\"type\":\"CHECK\",\"name\":\"Check\"}"));
        assertThrows(IllegalArgumentException.class,
                () -> nodeState("{" +
                        "\"id\":\" invalid\",\"type\":\"CHECK\"," +
                        "\"name\":\"Check\"}"));
    }

    @Test
    void nodeStateShouldRejectRelationshipAndUnknownType() {
        assertThrows(IllegalArgumentException.class,
                () -> nodeState("""
                        {"id":"R-1","from":"A","type":"HAS_CHECK","to":"B"}
                        """));
        assertThrows(IllegalArgumentException.class,
                () -> nodeState("""
                        {"id":"N-1","type":"UNKNOWN","name":"Unknown"}
                        """));
    }

    @Test
    void relationshipStateShouldRejectNodeShapeAndMissingEndpoints() {
        assertThrows(IllegalArgumentException.class,
                () -> relationshipState("""
                        {"id":"N-1","type":"CHECK","name":"Check"}
                        """));
        assertThrows(IllegalArgumentException.class,
                () -> relationshipState("""
                        {"id":"R-1","from":"A","type":"HAS_CHECK"}
                        """));
    }

    @Test
    void JavaValueEqualityShouldUseStoredExactSnapshot() throws Exception {
        NodeArtifactState first = nodeState("""
                {"id":"N-1","type":"CHECK","name":"Check","metadata":{"n":1}}
                """);
        NodeArtifactState equal = nodeState("""
                {"metadata":{"n":1},"name":"Check","type":"CHECK","id":"N-1"}
                """);
        NodeArtifactState differentExactNumber = nodeState("""
                {"id":"N-1","type":"CHECK","name":"Check","metadata":{"n":1.0}}
                """);

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, differentExactNumber);
    }

    private NodeArtifactState nodeState(String value) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, json(value));
    }

    private RelationshipArtifactState relationshipState(String value)
            throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1, json(value));
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }
}
