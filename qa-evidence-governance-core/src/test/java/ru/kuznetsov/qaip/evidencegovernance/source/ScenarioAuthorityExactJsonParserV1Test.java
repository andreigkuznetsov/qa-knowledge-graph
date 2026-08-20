package ru.kuznetsov.qaip.evidencegovernance.source;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioAuthorityExactJsonParserV1Test {
    private final ScenarioAuthorityExactJsonParserV1 parser = new ScenarioAuthorityExactJsonParserV1();

    @Test void parsesEveryValidRootAndRetainsExactImmutableBytes() {
        for (String json : List.of("{\"authority\":\"orders\"}", "[1,2]", "true", "null", "\"text\""))
            assertNotNull(parser.parseExactBytes(json.getBytes(StandardCharsets.UTF_8)).document());
        byte[] bytes = "{\"authority\":\"orders\"}".getBytes(StandardCharsets.UTF_8);
        var parsed = parser.parseExactBytes(bytes); bytes[0] = '[';
        assertEquals('{', parsed.exactRawBytes()[0]);
        ObjectNode exposed = (ObjectNode) parsed.document(); exposed.put("authority", "changed");
        assertEquals("orders", parsed.document().path("authority").textValue());
        assertEquals(ScenarioAuthorityExactJsonParserV1.CONTRACT_IDENTIFIER, parsed.parserContractIdentifier());
    }

    @Test void rejectsMalformedAndUnmappableUtf8WithoutReplacement() {
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.INVALID_UTF8, new byte[]{(byte)0xc3, 0x28});
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.INVALID_UTF8, new byte[]{(byte)0xff});
    }

    @Test void rejectsDuplicateMembersAtEveryDepth() {
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.DUPLICATE_JSON_MEMBER,
                "{\"a\":1,\"a\":2}".getBytes(StandardCharsets.UTF_8));
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.DUPLICATE_JSON_MEMBER,
                "{\"nested\":{\"a\":1,\"a\":2}}".getBytes(StandardCharsets.UTF_8));
    }

    @Test void preservesArbitraryPrecisionNumbers() {
        var document = parser.parseExactBytes(("{\"integer\":1234567890123456789012345678901234567890,"
                + "\"decimal\":12345678901234567890.12345678901234567890}")
                .getBytes(StandardCharsets.UTF_8)).document();
        assertEquals(new BigInteger("1234567890123456789012345678901234567890"),
                document.path("integer").bigIntegerValue());
        assertEquals(0, new BigDecimal("12345678901234567890.12345678901234567890")
                .compareTo(document.path("decimal").decimalValue()));
    }

    @Test void requiresExactlyOneValueAndAllowsOnlyTrailingWhitespace() {
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.MALFORMED_JSON, new byte[0]);
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.MALFORMED_JSON, "{".getBytes(StandardCharsets.UTF_8));
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.TRAILING_JSON_CONTENT,
                "{} []".getBytes(StandardCharsets.UTF_8));
        assertCode(ScenarioAuthorityJsonParseRejectionV1.Code.TRAILING_JSON_CONTENT,
                "{} x".getBytes(StandardCharsets.UTF_8));
        assertTrue(parser.parseExactBytes("{} \r\n\t".getBytes(StandardCharsets.UTF_8)).document().isObject());
    }

    @Test void repeatedParsingIsDeterministicAndUnexpectedNullPropagates() {
        byte[] bytes = "{\"n\":1}".getBytes(StandardCharsets.UTF_8);
        assertEquals(parser.parseExactBytes(bytes).document(), parser.parseExactBytes(bytes).document());
        assertThrows(NullPointerException.class, () -> parser.parseExactBytes(null));
        assertEquals(4, ScenarioAuthorityJsonParseRejectionV1.Code.values().length);
    }

    private void assertCode(ScenarioAuthorityJsonParseRejectionV1.Code code, byte[] bytes) {
        assertEquals(code, assertThrows(ScenarioAuthorityJsonParseRejectionV1.class,
                () -> parser.parseExactBytes(bytes)).code());
    }
}
