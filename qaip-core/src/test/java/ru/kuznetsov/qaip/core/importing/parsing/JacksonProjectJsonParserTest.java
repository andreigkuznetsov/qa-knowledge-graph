package ru.kuznetsov.qaip.core.importing.parsing;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonProjectJsonParserTest {
    private final ProjectJsonParser parser = new JacksonProjectJsonParser();

    @Test
    void rawInputPreservesTextAndEnforcesOnlyNonNull() {
        String exact = " \n {\"id\":\"  A  \"} \t";
        assertEquals(exact, new RawProjectJson(exact).value());
        assertEquals("", new RawProjectJson("").value());
        assertEquals("  ", new RawProjectJson("  ").value());
        assertThrows(NullPointerException.class, () -> new RawProjectJson(null));
        assertThrows(NullPointerException.class, () -> parser.parse(null));
        assertCode("", ProjectParseFindingCode.MALFORMED_JSON);
        assertCode(" \r\n\t", ProjectParseFindingCode.MALFORMED_JSON);
    }

    @Test
    void acceptsEveryValidJsonRootType() {
        for (String json : List.of("{}", "[]", "\"project\"", "123", "1.2300", "true", "null",
                "{\"object\":{},\"array\":[1,\"x\",false,null,[{}]]}")) {
            assertInstanceOf(ProjectParseAccepted.class, parser.parse(new RawProjectJson(json)), json);
        }
    }

    @Test
    void rejectsOrdinaryMalformedJson() {
        for (String json : List.of("{\"a\":1", "[1,2", "\"unterminated", "\"\\x\"", "1e",
                "@", "{\"a\":1 \"b\":2}", "{\"a\" 1}", "{")) {
            assertCode(json, ProjectParseFindingCode.MALFORMED_JSON);
        }
    }

    @Test
    void detectsDuplicateMembersAtEveryNestingLevelButUsesExactObjectLocalNames() {
        assertCode("{\"id\":1,\"id\":2}", ProjectParseFindingCode.DUPLICATE_JSON_MEMBER);
        assertCode("{\"nested\":{\"id\":1,\"id\":2}}", ProjectParseFindingCode.DUPLICATE_JSON_MEMBER);
        assertCode("[{\"id\":1,\"id\":2}]", ProjectParseFindingCode.DUPLICATE_JSON_MEMBER);
        assertCode("{\"exact\":1,\"exact\":2}", ProjectParseFindingCode.DUPLICATE_JSON_MEMBER);
        assertAccepted("{\"id\":1,\"ID\":2}");
        assertAccepted("[{\"id\":1},{\"id\":2}]");
    }

    @Test
    void rejectsTrailingValuesButAllowsTrailingWhitespace() {
        for (String json : List.of("{} {}", "[] true", "null 1", "\"first\" \"second\"")) {
            assertCode(json, ProjectParseFindingCode.TRAILING_JSON_CONTENT);
        }
        assertAccepted("{}   ");
        assertAccepted("{}\t\t");
        assertAccepted("{}\r\n\n");
    }

    @Test
    void preservesTheJsonDataModelWithoutNormalization() {
        String json = "{\"id\":\"  NODE-1  \",\"unknownProperty\":\"preserve me\","
                + "\"values\":[3,2,2,1],\"explicitNull\":null,\"emptyObject\":{},\"emptyArray\":[],"
                + "\"huge\":1234567890123456789012345678901234567890,"
                + "\"decimal\":1234567890.123456789012345678900,"
                + "\"a/b\":{\"m~n\":\"escaped names\"}}";
        ParsedProjectDocument document = accepted(json).document();
        JsonNode tree = document.internalJsonTreeCopy();

        assertEquals("  NODE-1  ", tree.get("id").textValue());
        assertEquals("preserve me", tree.get("unknownProperty").textValue());
        assertEquals(List.of(3, 2, 2, 1), toIntegers(tree.get("values")));
        assertTrue(tree.get("explicitNull").isNull());
        assertTrue(tree.get("emptyObject").isObject());
        assertTrue(tree.get("emptyObject").isEmpty());
        assertTrue(tree.get("emptyArray").isArray());
        assertTrue(tree.get("emptyArray").isEmpty());
        assertEquals(new BigInteger("1234567890123456789012345678901234567890"), tree.get("huge").bigIntegerValue());
        assertEquals(0, new BigDecimal("1234567890.123456789012345678900")
                .compareTo(tree.get("decimal").decimalValue()));
        assertEquals("escaped names", tree.at("/a~1b/m~0n").textValue());

        ((com.fasterxml.jackson.databind.node.ObjectNode) tree).put("id", "mutated copy");
        assertEquals("  NODE-1  ", document.internalJsonTreeCopy().get("id").textValue());
    }

    @Test
    void enforcesResultAndFindingInvariants() {
        assertThrows(NullPointerException.class, () -> new ProjectParseAccepted(null));
        assertThrows(NullPointerException.class, () -> new ProjectParseRejected(null));
        assertThrows(IllegalArgumentException.class, () -> new ProjectParseRejected(List.of()));
        List<ProjectParseFinding> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ProjectParseRejected(withNull));

        ProjectParseFinding finding = new ProjectParseFinding(ProjectParseFindingCode.MALFORMED_JSON,
                JsonInstanceLocation.ROOT, Optional.empty(), "Malformed input");
        ProjectParseRejected rejected = new ProjectParseRejected(List.of(finding, finding));
        assertEquals(List.of(finding), rejected.findings());
        assertThrows(UnsupportedOperationException.class, () -> rejected.findings().add(finding));
        assertNotNull(parser.parse(new RawProjectJson("{}")));
        assertFalse(ProjectParseRejected.class.getRecordComponents()[0].getGenericType().getTypeName()
                .contains("ParsedProjectDocument"));
    }

    @Test
    void publicParsingContractsDoNotExposeJacksonTypes() {
        for (Class<?> type : List.of(RawProjectJson.class, ProjectJsonParser.class, ProjectParseResult.class,
                ProjectParseAccepted.class, ProjectParseRejected.class, ProjectParseFinding.class,
                ProjectParseFindingCode.class, JsonInstanceLocation.class, JsonSourcePosition.class,
                ParsedProjectDocument.class)) {
            for (Method method : type.getMethods()) {
                assertNoJackson(type, method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) assertNoJackson(type, parameter);
            }
            for (Constructor<?> constructor : type.getConstructors()) {
                for (Type parameter : constructor.getGenericParameterTypes()) assertNoJackson(type, parameter);
            }
            for (Field field : type.getFields()) assertNoJackson(type, field.getGenericType());
            for (RecordComponent component : type.getRecordComponents() == null
                    ? new RecordComponent[0] : type.getRecordComponents()) assertNoJackson(type, component.getGenericType());
        }
    }

    @Test
    void repeatedParsingIsDeterministic() {
        ProjectParseAccepted firstAccepted = accepted("{\"values\":[3,2,1]}");
        ProjectParseAccepted secondAccepted = accepted("{\"values\":[3,2,1]}");
        assertEquals(firstAccepted, secondAccepted);

        ProjectParseRejected firstRejected = rejected("{\"id\":1,\"id\":2}");
        ProjectParseRejected secondRejected = rejected("{\"id\":1,\"id\":2}");
        assertEquals(firstRejected, secondRejected);
    }

    @Test
    void sharedParserSupportsConcurrentCalls() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<ProjectParseResult>> futures = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                int value = index;
                futures.add(executor.submit(() -> parser.parse(new RawProjectJson("{\"value\":" + value + "}"))));
            }
            for (int index = 0; index < futures.size(); index++) {
                ProjectParseAccepted result = assertInstanceOf(ProjectParseAccepted.class, futures.get(index).get());
                assertEquals(index, result.document().internalJsonTreeCopy().get("value").intValue());
            }
        }
    }

    private void assertAccepted(String json) {
        assertInstanceOf(ProjectParseAccepted.class, parser.parse(new RawProjectJson(json)));
    }

    private void assertCode(String json, ProjectParseFindingCode code) {
        ProjectParseRejected result = rejected(json);
        assertEquals(1, result.findings().size());
        ProjectParseFinding finding = result.findings().getFirst();
        assertEquals(code, finding.code());
        assertEquals("", finding.location().value());
        finding.sourcePosition().ifPresent(position -> {
            assertTrue(position.line() >= 1);
            assertTrue(position.column() >= 1);
            assertTrue(position.characterOffset() >= 0);
        });
    }

    private ProjectParseAccepted accepted(String json) {
        return assertInstanceOf(ProjectParseAccepted.class, parser.parse(new RawProjectJson(json)));
    }

    private ProjectParseRejected rejected(String json) {
        return assertInstanceOf(ProjectParseRejected.class, parser.parse(new RawProjectJson(json)));
    }

    private static List<Integer> toIntegers(JsonNode array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(value -> values.add(value.intValue()));
        return values;
    }

    private static void assertNoJackson(Class<?> owner, Type exposedType) {
        assertFalse(exposedType.getTypeName().contains("com.fasterxml.jackson"),
                () -> owner.getName() + " exposes " + exposedType.getTypeName());
    }
}
