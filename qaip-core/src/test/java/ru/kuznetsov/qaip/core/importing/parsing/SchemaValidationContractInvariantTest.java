package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.schema.JsonSchemaLocation;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationAccepted;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFinding;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationFindingCode;
import ru.kuznetsov.qaip.core.importing.schema.SchemaValidationRejected;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaValidationContractInvariantTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void acceptedRequiresAProofAndHasValueSemantics() throws Exception {
        assertThrows(NullPointerException.class, () -> new SchemaValidationAccepted(null));
        SchemaValidProjectDocument first = proof("{\"a\":1}");
        SchemaValidationAccepted accepted = new SchemaValidationAccepted(first);
        SchemaValidationAccepted equivalent = new SchemaValidationAccepted(proof("{\"a\":1}"));
        assertEquals(first, accepted.document());
        assertEquals(accepted, equivalent);
        assertEquals(accepted.hashCode(), equivalent.hashCode());
    }

    @Test
    void rejectedDefensivelyCopiesDeduplicatesAndSortsWithoutDocument() {
        assertThrows(NullPointerException.class, () -> new SchemaValidationRejected(null));
        assertThrows(IllegalArgumentException.class, () -> new SchemaValidationRejected(List.of()));
        List<SchemaValidationFinding> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new SchemaValidationRejected(withNull));

        SchemaValidationFinding later = finding("/z", "type", "later");
        SchemaValidationFinding earlier = finding("/a", "required", "earlier");
        List<SchemaValidationFinding> caller = new ArrayList<>(List.of(later, earlier, earlier));
        List<SchemaValidationFinding> original = List.copyOf(caller);
        SchemaValidationRejected rejected = new SchemaValidationRejected(caller);

        assertEquals(original, caller);
        assertEquals(List.of(earlier, later), rejected.findings());
        caller.clear();
        assertEquals(List.of(earlier, later), rejected.findings());
        assertThrows(UnsupportedOperationException.class, () -> rejected.findings().add(later));
        assertEquals(rejected, new SchemaValidationRejected(List.of(earlier, later)));
        assertEquals(rejected.hashCode(), new SchemaValidationRejected(List.of(earlier, later)).hashCode());
        assertFalse(List.of(SchemaValidationRejected.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getType() == SchemaValidProjectDocument.class));
    }

    @Test
    void findingEnforcesEveryInvariant() {
        JsonInstanceLocation root = JsonInstanceLocation.ROOT;
        JsonSchemaLocation schema = new JsonSchemaLocation("https://example/schema#/type");
        assertThrows(NullPointerException.class, () -> new SchemaValidationFinding(null, root, schema, "type", "message"));
        assertThrows(NullPointerException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, null, schema, "type", "message"));
        assertThrows(NullPointerException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, null, "type", "message"));
        assertThrows(NullPointerException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, null, "message"));
        assertThrows(IllegalArgumentException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, "", "message"));
        assertThrows(IllegalArgumentException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, " ", "message"));
        assertThrows(NullPointerException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, "type", null));
        assertThrows(IllegalArgumentException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, "type", ""));
        assertThrows(IllegalArgumentException.class, () -> new SchemaValidationFinding(
                SchemaValidationFindingCode.SCHEMA_VIOLATION, root, schema, "type", "\t"));
        assertEquals("message", new SchemaValidationFinding(SchemaValidationFindingCode.SCHEMA_VIOLATION,
                root, schema, "type", "message").message());
    }

    @Test
    void schemaLocationPreservesExactNonBlankRepresentation() {
        assertThrows(NullPointerException.class, () -> new JsonSchemaLocation(null));
        assertThrows(IllegalArgumentException.class, () -> new JsonSchemaLocation(""));
        assertThrows(IllegalArgumentException.class, () -> new JsonSchemaLocation(" \t"));
        String exact = "https://example/schema#/$defs/node/type";
        assertEquals(exact, new JsonSchemaLocation(exact).value());
    }

    @Test
    void schemaValidProofOwnsItsTreeAndRemainsOpaque() throws Exception {
        var original = MAPPER.readTree("{\"id\":\"A\"}");
        SchemaValidProjectDocument proof = new SchemaValidProjectDocument(original);
        ((com.fasterxml.jackson.databind.node.ObjectNode) original).put("id", "outside");
        assertEquals("A", proof.internalJsonTreeCopy().get("id").textValue());
        var firstCopy = proof.internalJsonTreeCopy();
        var secondCopy = proof.internalJsonTreeCopy();
        assertFalse(firstCopy == secondCopy);
        ((com.fasterxml.jackson.databind.node.ObjectNode) firstCopy).put("id", "copy");
        assertEquals("A", proof.internalJsonTreeCopy().get("id").textValue());
        assertEquals(proof, proof("{\"id\":\"A\"}"));
        assertEquals(proof.hashCode(), proof("{\"id\":\"A\"}").hashCode());
        assertEquals(0, SchemaValidProjectDocument.class.getConstructors().length);
        assertFalse(List.of(SchemaValidProjectDocument.class.getMethods()).stream()
                .anyMatch(method -> method.getReturnType().getName().startsWith("com.fasterxml.jackson")));
    }

    private static SchemaValidationFinding finding(String path, String keyword, String message) {
        return new SchemaValidationFinding(SchemaValidationFindingCode.SCHEMA_VIOLATION,
                new JsonInstanceLocation(path), new JsonSchemaLocation("https://example/schema#/" + keyword),
                keyword, message);
    }

    private static SchemaValidProjectDocument proof(String json) throws Exception {
        return new SchemaValidProjectDocument(MAPPER.readTree(json));
    }
}
