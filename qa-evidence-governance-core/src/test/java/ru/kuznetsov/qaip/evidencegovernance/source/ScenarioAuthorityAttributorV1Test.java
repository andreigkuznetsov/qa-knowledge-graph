package ru.kuznetsov.qaip.evidencegovernance.source;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityAttributorV1Test {
    private final ScenarioAuthorityExactJsonParserV1 parser = new ScenarioAuthorityExactJsonParserV1();
    private final ScenarioAuthorityAttributorV1 attributor = new ScenarioAuthorityAttributorV1();

    @Test void preservesEveryAcceptedBoundaryCharacterWithoutTransformation() {
        for (String authority : List.of("A", "aZ09._:/-", "Mixed/Case:V1")) {
            var attributed = attributor.attribute(parsed("{\"authority\":\"" + authority + "\"}"));
            assertEquals(authority, attributed.authority());
            assertEquals(ScenarioAuthorityAttributorV1.CONTRACT_IDENTIFIER,
                    attributed.attributionContractIdentifier());
        }
        assertEquals("a".repeat(200), attributor.attribute(parsed(
                "{\"authority\":\"" + "a".repeat(200) + "\"}")).authority());
    }

    @Test void rejectsEveryFiniteCauseAtExactLocation() {
        assertCode(ScenarioAuthorityAttributionRejectionV1.Code.NON_OBJECT_ROOT, "[]", "");
        assertCode(ScenarioAuthorityAttributionRejectionV1.Code.MISSING_AUTHORITY, "{}", "/authority");
        assertCode(ScenarioAuthorityAttributionRejectionV1.Code.AUTHORITY_NOT_STRING,
                "{\"authority\":null}", "/authority");
        assertCode(ScenarioAuthorityAttributionRejectionV1.Code.AUTHORITY_NOT_STRING,
                "{\"authority\":1}", "/authority");
        for (String value : List.of("", "a".repeat(201), "-bad", "bad value", " bad", "bad ", "Р°"))
            assertCode(ScenarioAuthorityAttributionRejectionV1.Code.INVALID_AUTHORITY,
                    "{\"authority\":\"" + value + "\"}", "/authority");
        assertEquals(4, ScenarioAuthorityAttributionRejectionV1.Code.values().length);
    }

    @Test void resultRetainsExactParserProofAndIsDeterministic() {
        var parsed = parsed("{\"authority\":\"Orders/V1\"}");
        var first = attributor.attribute(parsed); var second = attributor.attribute(parsed);
        assertSame(parsed, first.parsedJson());
        assertEquals(first.authority(), second.authority());
        assertThrows(NullPointerException.class, () -> attributor.attribute(null));
        assertTrue(java.util.Arrays.stream(ScenarioAuthorityAttributorV1.class.getMethods())
                .noneMatch(method -> java.util.Arrays.asList(method.getParameterTypes())
                        .contains(com.fasterxml.jackson.databind.JsonNode.class)));
        assertTrue(java.util.Arrays.stream(ScenarioAuthorityParsedJsonV1.class.getDeclaredConstructors())
                .noneMatch(constructor -> java.lang.reflect.Modifier.isPublic(constructor.getModifiers())));
    }

    private ScenarioAuthorityParsedJsonV1 parsed(String json) {
        return parser.parseExactBytes(json.getBytes(StandardCharsets.UTF_8));
    }
    private void assertCode(ScenarioAuthorityAttributionRejectionV1.Code code, String json, String location) {
        var rejection = assertThrows(ScenarioAuthorityAttributionRejectionV1.class,
                () -> attributor.attribute(parsed(json)));
        assertEquals(code, rejection.code()); assertEquals(location, rejection.structuralLocation());
    }
}
