package ru.kuznetsov.qaip.core.persistence.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonProjectPersistenceDocumentCodecTest {
    private final JacksonProjectPersistenceDocumentCodec codec = new JacksonProjectPersistenceDocumentCodec();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void complete_minimal_and_deep_projects_round_trip_with_strict_equality() {
        for (Project project : List.of(PersistenceDocumentFixtures.completeProject(),
                PersistenceDocumentFixtures.minimalProject())) {
            String first = codec.encode(project);
            assertEquals(first, codec.encode(project), "encoding must be deterministic");
            assertEquals(project, codec.decode(first));
        }
    }

    @Test
    void document_has_exact_versioned_root_and_explicit_fixed_nulls_and_empty_collections() throws Exception {
        JsonNode root = json.readTree(codec.encode(PersistenceDocumentFixtures.minimalProject()));
        assertEquals(List.of("persistenceDocumentVersion", "project"),
                root.propertyStream().map(Map.Entry::getKey).sorted().toList());
        assertEquals(JacksonProjectPersistenceDocumentCodec.VERSION,
                root.get("persistenceDocumentVersion").textValue());
        JsonNode project = root.get("project");
        assertTrue(project.get("metadata").has("description"));
        assertTrue(project.get("metadata").get("description").isNull());
        assertTrue(project.get("sources").isEmpty());
        assertTrue(project.get("nodes").isEmpty());
        assertTrue(project.get("relationships").isEmpty());
        assertTrue(project.get("declaredChanges").isEmpty());
        assertTrue(project.get("analysisContext").isEmpty());
    }

    @Test
    void every_dynamic_type_and_numeric_collision_preserves_exact_java_type_value_and_scale() {
        Project original = PersistenceDocumentFixtures.completeProject();
        Project decoded = codec.decode(codec.encode(original));
        Map<String, Object> before = original.metadata().attributes();
        Map<String, Object> after = decoded.metadata().attributes();

        assertEquals(before, after);
        for (String key : before.keySet()) {
            if (before.get(key) != null) assertEquals(before.get(key).getClass(), after.get(key).getClass(), key);
        }
        assertInstanceOf(Integer.class, after.get("intMax"));
        assertInstanceOf(Long.class, after.get("longMax"));
        assertInstanceOf(BigInteger.class, after.get("bigPositive"));
        assertEquals(new BigDecimal("1"), after.get("decimal1"));
        assertEquals(new BigDecimal("1.0"), after.get("decimal10"));
        assertEquals(new BigDecimal("1.00"), after.get("decimal100"));
        assertNotEquals(after.get("decimal1"), after.get("decimal10"));
        assertNotEquals(after.get("decimal10"), after.get("decimal100"));
    }

    @Test
    void dynamic_numbers_use_canonical_strings_and_decimal_components_while_boolean_remains_json_boolean()
            throws Exception {
        JsonNode entries = json.readTree(codec.encode(PersistenceDocumentFixtures.completeProject()))
                .get("project").get("metadata").get("attributes").get("entries");
        assertEquals("INTEGER", entries.get("intMax").get("type").textValue());
        assertTrue(entries.get("intMax").get("value").isTextual());
        assertEquals(Integer.toString(Integer.MAX_VALUE), entries.get("intMax").get("value").textValue());
        assertTrue(entries.get("true").get("value").isBoolean());
        JsonNode decimal = entries.get("decimal100");
        assertEquals("BIG_DECIMAL", decimal.get("type").textValue());
        assertEquals("100", decimal.get("unscaledValue").textValue());
        assertEquals(2, decimal.get("scale").intValue());
        assertFalse(decimal.has("value"));
    }

    @Test
    void dynamic_null_remains_distinct_from_absent_key_and_list_order_is_preserved() {
        Project decoded = codec.decode(codec.encode(PersistenceDocumentFixtures.completeProject()));
        Map<String, Object> nested = castMap(decoded.metadata().attributes().get("nested"));
        assertTrue(nested.containsKey("presentNull"));
        assertEquals(null, nested.get("presentNull"));
        assertFalse(nested.containsKey("absent"));
        assertEquals(Arrays.asList("first", null, 1, Map.of("nested", 2L)),
                decoded.metadata().attributes().get("list"));
    }

    @Test
    void document_version_is_mandatory_exact_and_root_is_closed() throws Exception {
        ObjectNode root = (ObjectNode) json.readTree(codec.encode(PersistenceDocumentFixtures.minimalProject()));
        for (JsonNode invalidVersion : List.of(
                json.nullNode(), json.getNodeFactory().textNode(""), json.getNodeFactory().textNode(" "),
                json.getNodeFactory().textNode("qaip-project-persistence-v2"))) {
            ObjectNode invalid = root.deepCopy();
            invalid.set("persistenceDocumentVersion", invalidVersion);
            assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(invalid.toString()));
        }
        ObjectNode missing = root.deepCopy();
        missing.remove("persistenceDocumentVersion");
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(missing.toString()));
        ObjectNode extra = root.deepCopy();
        extra.put("extra", true);
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(extra.toString()));
    }

    @Test
    void malformed_json_duplicate_members_and_trailing_content_are_rejected() {
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode("{"));
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(
                "{\"persistenceDocumentVersion\":\"qaip-project-persistence-v1\"," +
                        "\"persistenceDocumentVersion\":\"qaip-project-persistence-v1\",\"project\":{}}"));
        assertThrows(ProjectPersistenceDocumentException.class,
                () -> codec.decode(codec.encode(PersistenceDocumentFixtures.minimalProject()) + " {}"));
    }

    @Test
    void malformed_and_inconsistent_dynamic_values_are_rejected() throws Exception {
        assertMalformed(value("UNKNOWN"));
        assertMalformed("{}");
        assertMalformed("{\"type\":null}");
        assertMalformed("{\"type\":\" \"}");
        assertMalformed("{\"type\":\"NULL\",\"value\":\"x\"}");
        assertMalformed("{\"type\":\"STRING\",\"value\":true}");
        assertMalformed("{\"type\":\"BOOLEAN\",\"value\":\"true\"}");
        assertMalformed(value("INTEGER", "2147483648"));
        assertMalformed(value("LONG", "9223372036854775808"));
        for (String malformed : List.of("01", "+1", "1.0", "1e2", "--1", "-0")) {
            assertMalformed(value("BIG_INTEGER", malformed));
        }
        assertMalformed("{\"type\":\"BIG_DECIMAL\",\"unscaledValue\":\"x\",\"scale\":2}");
        assertMalformed("{\"type\":\"BIG_DECIMAL\",\"unscaledValue\":\"1\",\"scale\":1.5}");
        assertMalformed("{\"type\":\"BIG_DECIMAL\",\"unscaledValue\":\"1\",\"scale\":2147483648}");
        assertMalformed("{\"type\":\"LIST\"}");
        assertMalformed("{\"type\":\"OBJECT\"}");
        assertMalformed("{\"type\":\"LIST\",\"items\":{},\"value\":\"x\"}");
        assertMalformed("{\"type\":\"OBJECT\",\"entries\":[]}");
    }

    @Test
    void null_arguments_and_invalid_fixed_document_shapes_fail_clearly() throws Exception {
        assertThrows(NullPointerException.class, () -> codec.encode(null));
        assertThrows(NullPointerException.class, () -> codec.decode(null));
        ObjectNode root = (ObjectNode) json.readTree(codec.encode(PersistenceDocumentFixtures.minimalProject()));
        ((ObjectNode) root.get("project")).remove("subject");
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(root.toString()));
    }

    private void assertMalformed(String typedValue) throws Exception {
        ObjectNode root = (ObjectNode) json.readTree(codec.encode(PersistenceDocumentFixtures.minimalProject()));
        ((ObjectNode) root.get("project").get("metadata")).set("attributes", json.readTree(typedValue));
        assertThrows(ProjectPersistenceDocumentException.class, () -> codec.decode(root.toString()), typedValue);
    }

    private static String value(String type) {
        return "{\"type\":\"" + type + "\"}";
    }

    private static String value(String type, String value) {
        return "{\"type\":\"" + type + "\",\"value\":\"" + value + "\"}";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
