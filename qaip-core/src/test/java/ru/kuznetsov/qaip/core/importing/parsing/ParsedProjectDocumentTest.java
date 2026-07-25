package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedProjectDocumentTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void ownsInputAndReturnsIndependentCopies() {
        ObjectNode original = MAPPER.createObjectNode().put("id", "A");
        ParsedProjectDocument document = new ParsedProjectDocument(original);
        original.put("id", "changed outside");
        assertEquals("A", document.internalJsonTreeCopy().get("id").textValue());

        JsonNode firstCopy = document.internalJsonTreeCopy();
        JsonNode secondCopy = document.internalJsonTreeCopy();
        assertNotSame(firstCopy, secondCopy);
        ((ObjectNode) firstCopy).put("id", "changed copy");
        assertEquals("A", secondCopy.get("id").textValue());
        assertEquals("A", document.internalJsonTreeCopy().get("id").textValue());
    }

    @Test
    void equalityAndHashCodeUseRepresentedJsonValue() {
        ParsedProjectDocument first = new ParsedProjectDocument(MAPPER.createObjectNode().put("id", "A"));
        ParsedProjectDocument equivalent = new ParsedProjectDocument(MAPPER.createObjectNode().put("id", "A"));
        ParsedProjectDocument different = new ParsedProjectDocument(MAPPER.createObjectNode().put("id", "B"));
        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertFalse(first.equals(different));
        assertFalse(first.equals(null));
    }

    @Test
    void remainsOpaqueToPublicCallers() {
        assertEquals(0, ParsedProjectDocument.class.getConstructors().length);
        assertTrue(Arrays.stream(ParsedProjectDocument.class.getDeclaredConstructors())
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())));
        assertTrue(Arrays.stream(ParsedProjectDocument.class.getMethods())
                .noneMatch(method -> method.getReturnType().getName().startsWith("com.fasterxml.jackson")));
        assertTrue(Arrays.stream(ParsedProjectDocument.class.getFields())
                .noneMatch(field -> field.getType().getName().startsWith("com.fasterxml.jackson")));
        assertThrows(NullPointerException.class, () -> new ParsedProjectDocument(null));
    }
}
