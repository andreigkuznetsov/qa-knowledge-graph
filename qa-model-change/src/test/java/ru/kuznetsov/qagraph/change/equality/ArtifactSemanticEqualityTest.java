package ru.kuznetsov.qagraph.change.equality;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_EQUAL;
import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_UNEQUAL;
import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.UNSUPPORTED;

class ArtifactSemanticEqualityTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .build();
    private final ArtifactSemanticEquality equality =
            new ArtifactSemanticEquality();

    @Test
    void objectMemberOrderShouldNotAffectEquality() throws Exception {
        assertComparison(SEMANTICALLY_EQUAL,
                node("""
                        {"id":"N-1","type":"CHECK","name":"Check",
                         "metadata":{"a":1,"b":true}}
                        """),
                node("""
                        {"metadata":{"b":true,"a":1},"name":"Check",
                         "type":"CHECK","id":"N-1"}
                        """));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{} | {\"description\":null}",
            "{} | {\"tags\":[]}",
            "{} | {\"metadata\":{}}"
    })
    void absenceShouldDifferFromNullEmptyArrayAndEmptyObject(
            String leftExtra,
            String rightExtra
    ) throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra(leftExtra), checkWithExtra(rightExtra));
    }

    @Test
    void explicitSchemaDefaultShouldDifferFromAbsence() throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{}"),
                checkWithExtra("{\"sourceReferences\":[]}"));
    }

    @Test
    void extensionPropertyDifferenceShouldBeMeaningful() throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"extension\":1}"),
                checkWithExtra("{\"extension\":2}"));
    }

    @Test
    void scalarKindsAndValuesShouldRemainDistinct() throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"metadata\":{\"value\":\"Check\"}}"),
                checkWithExtra("{\"metadata\":{\"value\":\"check\"}}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"metadata\":{\"value\":\"a b\"}}"),
                checkWithExtra("{\"metadata\":{\"value\":\"a  b\"}}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"metadata\":{\"value\":true}}"),
                checkWithExtra("{\"metadata\":{\"value\":false}}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"metadata\":{\"value\":\"1\"}}"),
                checkWithExtra("{\"metadata\":{\"value\":1}}"));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1, 1.0",
            "1.00, 1e0",
            "1.01, 1.010",
            "-0.0, 0",
            "123456789012345678901234567890, 123456789012345678901234567890.0",
            "0.123456789012345678901234567890, 0.1234567890123456789012345678900"
    })
    void mathematicallyEqualNumbersShouldCompareExactly(
            String left,
            String right
    ) throws Exception {
        assertComparison(SEMANTICALLY_EQUAL,
                numberState(left), numberState(right));
    }

    @Test
    void mathematicallyDifferentNumbersShouldCompareUnequal()
            throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                numberState("1.01"), numberState("1.02"));
    }

    @Test
    void tagsShouldUseOrderInsensitiveMultisetComparison() throws Exception {
        assertComparison(SEMANTICALLY_EQUAL,
                checkWithExtra("{\"tags\":[\"api\",\"security\"]}"),
                checkWithExtra("{\"tags\":[\"security\",\"api\"]}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"tags\":[\"api\"]}"),
                checkWithExtra("{\"tags\":[\"API\"]}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"tags\":[\"api\",\"api\"]}"),
                checkWithExtra("{\"tags\":[\"api\"]}"));
    }

    @Test
    void sourceReferencesShouldIgnoreOrderButCompareFullContent()
            throws Exception {
        String first = """
                {"sourceReferences":[
                  {"sourceId":"S-1","location":{"type":"OTHER","value":"A"}},
                  {"sourceId":"S-2","location":{"type":"OTHER","value":"B"}}]}
                """;
        String reordered = """
                {"sourceReferences":[
                  {"location":{"value":"B","type":"OTHER"},"sourceId":"S-2"},
                  {"location":{"value":"A","type":"OTHER"},"sourceId":"S-1"}]}
                """;
        String changed = """
                {"sourceReferences":[
                  {"sourceId":"S-1","location":{"type":"OTHER","value":"A"}},
                  {"sourceId":"S-2","location":{"type":"OTHER","value":"C"}}]}
                """;

        assertComparison(SEMANTICALLY_EQUAL,
                checkWithExtra(first), checkWithExtra(reordered));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra(first), checkWithExtra(changed));
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"sourceReferences\":[{\"sourceId\":\"S-1\"},{\"sourceId\":\"S-1\"}]}"),
                checkWithExtra("{\"sourceReferences\":[{\"sourceId\":\"S-1\"}]}"));
    }

    @Test
    void relationshipSourceReferencesShouldIgnoreOrder() throws Exception {
        assertComparison(SEMANTICALLY_EQUAL,
                relationship("""
                        {"id":"R-1","from":"A","type":"RELATED_TO","to":"B",
                         "sourceReferences":[{"sourceId":"S-1"},{"sourceId":"S-2"}]}
                        """),
                relationship("""
                        {"id":"R-1","from":"A","type":"RELATED_TO","to":"B",
                         "sourceReferences":[{"sourceId":"S-2"},{"sourceId":"S-1"}]}
                        """));
    }

    @Test
    void scenarioCollectionsShouldComparePositionally() throws Exception {
        for (String collection : new String[]{"given", "when", "then"}) {
            assertComparison(SEMANTICALLY_UNEQUAL,
                    scenario(collection, "S-1", "S-2"),
                    scenario(collection, "S-2", "S-1"));
        }
    }

    @Test
    void testPreconditionsShouldComparePositionally() throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                testImplementation("[\"authenticated\",\"account exists\"]", "[]"),
                testImplementation("[\"account exists\",\"authenticated\"]", "[]"));
    }

    @Test
    void testStepsShouldUseExplicitOrderInsteadOfPhysicalPosition()
            throws Exception {
        String ordered = """
                [{"order":1,"action":"Open"},{"order":2,"action":"Submit"}]
                """;
        String physicallyReordered = """
                [{"order":2,"action":"Submit"},{"order":1,"action":"Open"}]
                """;
        String orderChanged = """
                [{"order":2,"action":"Open"},{"order":1,"action":"Submit"}]
                """;
        String contentChanged = """
                [{"order":1,"action":"Close"},{"order":2,"action":"Submit"}]
                """;

        assertComparison(SEMANTICALLY_EQUAL,
                testImplementation("[]", ordered),
                testImplementation("[]", physicallyReordered));
        assertComparison(SEMANTICALLY_UNEQUAL,
                testImplementation("[]", ordered),
                testImplementation("[]", orderChanged));
        assertComparison(SEMANTICALLY_UNEQUAL,
                testImplementation("[]", ordered),
                testImplementation("[]", contentChanged));
    }

    @Test
    void invalidExplicitStepOrderShouldBeUnsupported() throws Exception {
        assertComparison(UNSUPPORTED,
                testImplementation("[]", "[{\"action\":\"Open\"}]"),
                testImplementation("[]", "[{\"action\":\"Open\"}]"));
        assertComparison(UNSUPPORTED,
                testImplementation("[]", "[{\"order\":1,\"action\":\"A\"},{\"order\":1,\"action\":\"B\"}]"),
                testImplementation("[]", "[{\"order\":1,\"action\":\"A\"},{\"order\":1,\"action\":\"B\"}]"));
        assertComparison(UNSUPPORTED,
                testImplementation("[]", "[{\"order\":0,\"action\":\"Open\"}]"),
                testImplementation("[]", "[{\"order\":0,\"action\":\"Open\"}]"));
    }

    @Test
    void arraysInMetadataDetailsAndParametersShouldRemainOrdered()
            throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"metadata\":{\"values\":[1,2]}}"),
                checkWithExtra("{\"metadata\":{\"values\":[2,1]}}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                technicalImplementation("[1,2]"),
                technicalImplementation("[2,1]"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                scenarioWithParameters("[[1,2]]"),
                scenarioWithParameters("[[2,1]]"));
    }

    @Test
    void unknownExtensionArraysShouldUseOrderedFallback() throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                checkWithExtra("{\"extension\":[{\"a\":1},2]}"),
                checkWithExtra("{\"extension\":[2,{\"a\":1}]}"));
        assertComparison(SEMANTICALLY_EQUAL,
                checkWithExtra("{\"extension\":{\"a\":1,\"b\":2}}"),
                checkWithExtra("{\"extension\":{\"b\":2,\"a\":1}}"));
    }

    @Test
    void artifactCategoryAndIdentityMismatchShouldBeUnequal()
            throws Exception {
        assertComparison(SEMANTICALLY_UNEQUAL,
                node("{" +
                        "\"id\":\"N-1\",\"type\":\"CHECK\",\"name\":\"Check\"}"),
                relationship("{" +
                        "\"id\":\"N-1\",\"from\":\"A\"," +
                        "\"type\":\"RELATED_TO\",\"to\":\"B\"}"));
        assertComparison(SEMANTICALLY_UNEQUAL,
                node("{" +
                        "\"id\":\"N-1\",\"type\":\"CHECK\",\"name\":\"Check\"}"),
                node("{" +
                        "\"id\":\"N-2\",\"type\":\"CHECK\",\"name\":\"Check\"}"));
    }

    @Test
    void unsupportedAndDifferentVersionsShouldBeExplicitlyUnsupported()
            throws Exception {
        JsonNode snapshot = json("""
                {"id":"N-1","type":"CHECK","name":"Check"}
                """);
        NodeArtifactState supported = new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, snapshot);
        NodeArtifactState unsupported = new NodeArtifactState(
                new CanonicalQaModelVersion("0.2"), snapshot);

        assertComparison(UNSUPPORTED, supported, unsupported);
        assertComparison(UNSUPPORTED, unsupported, unsupported);
    }

    @Test
    void nullStateShouldBeRejectedAsProgrammerError() {
        assertThrows(NullPointerException.class,
                () -> equality.compare(null, null));
    }

    private NodeArtifactState numberState(String number) throws Exception {
        return checkWithExtra("{\"metadata\":{\"number\":" + number + "}}");
    }

    private NodeArtifactState checkWithExtra(String extraObject)
            throws Exception {
        JsonNode extra = json(extraObject);
        var node = mapper.createObjectNode()
                .put("id", "N-1")
                .put("type", "CHECK")
                .put("name", "Check");
        extra.fields().forEachRemaining(entry ->
                node.set(entry.getKey(), entry.getValue()));
        return new NodeArtifactState(CanonicalQaModelVersion.V0_1, node);
    }

    private NodeArtifactState scenario(
            String collection,
            String first,
            String second
    ) throws Exception {
        return node("""
                {"id":"N-1","type":"SCENARIO","name":"Scenario",
                 "scenario":{"code":"SC-1","given":%s,"when":%s,"then":%s}}
                """.formatted(
                steps(collection.equals("given") ? first : "G-1",
                        collection.equals("given") ? second : null),
                steps(collection.equals("when") ? first : "W-1",
                        collection.equals("when") ? second : null),
                steps(collection.equals("then") ? first : "T-1",
                        collection.equals("then") ? second : null)
        ));
    }

    private String steps(String first, String second) {
        if (second == null) {
            return "[{\"id\":\"" + first + "\",\"text\":\"" + first + "\"}]";
        }
        return "[{\"id\":\"" + first + "\",\"text\":\"" + first
                + "\"},{\"id\":\"" + second + "\",\"text\":\""
                + second + "\"}]";
    }

    private NodeArtifactState testImplementation(
            String preconditions,
            String steps
    ) throws Exception {
        return node("""
                {"id":"N-1","type":"TEST_IMPLEMENTATION","name":"Test",
                 "testImplementation":{"code":"TC-1","executionType":"AUTOMATED",
                 "preconditions":%s,"steps":%s}}
                """.formatted(preconditions, steps));
    }

    private NodeArtifactState technicalImplementation(String values)
            throws Exception {
        return node("""
                {"id":"N-1","type":"TECHNICAL_IMPLEMENTATION","name":"API",
                 "technicalImplementation":{"implementationType":"API",
                 "system":"S","details":{"values":%s}}}
                """.formatted(values));
    }

    private NodeArtifactState scenarioWithParameters(String values)
            throws Exception {
        return node("""
                {"id":"N-1","type":"SCENARIO","name":"Scenario",
                 "scenario":{"code":"SC-1","given":[],
                 "when":[{"id":"W-1","text":"When",
                 "parameters":{"values":%s}}],
                 "then":[{"id":"T-1","text":"Then"}]}}
                """.formatted(values));
    }

    private NodeArtifactState node(String value) throws Exception {
        return new NodeArtifactState(
                CanonicalQaModelVersion.V0_1, json(value));
    }

    private RelationshipArtifactState relationship(String value)
            throws Exception {
        return new RelationshipArtifactState(
                CanonicalQaModelVersion.V0_1, json(value));
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }

    private void assertComparison(
            SemanticEqualityResult expected,
            ru.kuznetsov.qagraph.change.model.ArtifactState left,
            ru.kuznetsov.qagraph.change.model.ArtifactState right
    ) {
        assertEquals(expected, equality.compare(left, right));
    }
}
