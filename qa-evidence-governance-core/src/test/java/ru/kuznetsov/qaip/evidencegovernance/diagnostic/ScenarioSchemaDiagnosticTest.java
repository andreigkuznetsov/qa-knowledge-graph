package ru.kuznetsov.qaip.evidencegovernance.diagnostic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioSchemaDiagnosticTest {
    private static final String PREFIX = ScenarioSchemaDiagnostic.RULE_PREFIX;

    @Test
    void rejectsUnsupportedKeywordRuleAndRuleKeywordCombination() {
        assertRejected("unknown", PREFIX + "/type", text("expectedType", "object"));
        assertRejected("type", PREFIX + "/unknown", text("expectedType", "object"));
        assertRejected("required", PREFIX + "/type", text("missingProperty", "authority"));
    }

    @Test
    void rejectsMissingExtraMisnamedAndMistypedParameters() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "", "type", PREFIX + "/type", List.of()));
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "", "type", PREFIX + "/type", List.of(
                        text("expectedType", "object"), text("extra", "value"))));
        assertRejected("type", PREFIX + "/type", text("wrongName", "object"));
        assertRejected("type", PREFIX + "/type",
                new ScenarioSchemaDiagnostic.Unsigned64Parameter("expectedType", 1));
    }

    @Test
    void rejectsWrongFixedParameterValueAndNonCanonicalParameterOrder() {
        assertRejected("type", PREFIX + "/type", text("expectedType", "array"));
        assertRejected("required", PREFIX + "/required", text("missingProperty", "invented"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "/ruleRefs", "uniqueItems",
                PREFIX + "/$defs/scenario/properties/ruleRefs/uniqueItems",
                List.of(
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("firstIndex", 0),
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("duplicateIndex", 1))));
        assertThrows(IllegalArgumentException.class, () -> ScenarioSchemaDiagnostic.v1(
                "/ruleRefs", "uniqueItems",
                PREFIX + "/$defs/scenario/properties/ruleRefs/uniqueItems",
                List.of(
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("duplicateIndex", 0),
                        new ScenarioSchemaDiagnostic.Unsigned64Parameter("firstIndex", 1))));
    }

    @Test
    void evidenceGovernanceComparatorOwnsCanonicalTypedParameterByteOrder() {
        ScenarioSchemaDiagnostic shortValue = additional("z");
        ScenarioSchemaDiagnostic longerLexicallyEarlierValue = additional("aa");
        List<ScenarioSchemaDiagnostic> diagnostics = new ArrayList<>(
                List.of(longerLexicallyEarlierValue, shortValue));

        diagnostics.sort(ScenarioSchemaDiagnostic.canonicalOrder());

        assertEquals(List.of(shortValue, longerLexicallyEarlierValue), diagnostics);
    }

    @Test
    void factoryFixesCanonicalVersionAndStableCode() {
        ScenarioSchemaDiagnostic diagnostic = required("authority");

        assertEquals("scenario-authority-schema-diagnostic-v1",
                diagnostic.diagnosticContractVersion());
        assertEquals(ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION, diagnostic.stableCode());
        assertEquals("scenario-authority-schema-diagnostic-mapping-v1",
                ScenarioSchemaDiagnostic.MAPPING_CONTRACT_IDENTIFIER);
    }

    @Test
    void locksExactCanonicalBytesForEveryV1ParameterShape() {
        assertParameterHex(type("", "/type", "object"),
                "0000000000000001000000000000000c65787065637465645479706500000000000000045445585400000000000000066f626a656374");
        assertParameterHex(required("authority"),
                "0000000000000001000000000000000f6d697373696e6750726f70657274790000000000000004544558540000000000000009617574686f72697479");
        assertParameterHex(additional("extra"),
                "00000000000000010000000000000012756e657870656374656450726f706572747900000000000000045445585400000000000000056578747261");
        assertParameterHex(ScenarioSchemaDiagnostic.v1("", "const",
                        PREFIX + "/properties/schemaVersion/const", List.of(text("expectedText", "1.0"))),
                "0000000000000001000000000000000c6578706563746564546578740000000000000004544558540000000000000003312e30");
        assertParameterHex(unsigned("minLength", "/$defs/authority/minLength",
                        "minimumCodePointLength", 1),
                "000000000000000100000000000000166d696e696d756d436f6465506f696e744c656e677468000000000000000655494e5436340000000000000001");
        assertParameterHex(unsigned("maxLength", "/$defs/authority/maxLength",
                        "maximumCodePointLength", 200),
                "000000000000000100000000000000166d6178696d756d436f6465506f696e744c656e677468000000000000000655494e54363400000000000000c8");
        assertParameterHex(ScenarioSchemaDiagnostic.v1("", "pattern",
                        PREFIX + "/$defs/nonBlankString/pattern", List.of(text("requiredPattern", ".*\\S.*"))),
                "0000000000000001000000000000000f72657175697265645061747465726e00000000000000045445585400000000000000062e2a5c532e2a");
        assertParameterHex(unsigned("minItems", "/$defs/steps/minItems", "minimumItemCount", 1),
                "000000000000000100000000000000106d696e696d756d4974656d436f756e74000000000000000655494e5436340000000000000001");
        assertParameterHex(ScenarioSchemaDiagnostic.v1("", "uniqueItems",
                        PREFIX + "/$defs/scenario/properties/ruleRefs/uniqueItems", List.of(
                                new ScenarioSchemaDiagnostic.Unsigned64Parameter("duplicateIndex", 4),
                                new ScenarioSchemaDiagnostic.Unsigned64Parameter("firstIndex", 1))),
                "0000000000000002000000000000000e6475706c6963617465496e646578000000000000000655494e5436340000000000000004000000000000000a6669727374496e646578000000000000000655494e5436340000000000000001");
    }

    @Test
    void canonicalOrderExercisesEveryAvailableSuccessiveTupleKey() {
        ScenarioSchemaDiagnostic locationA = additional("same", "/a");
        ScenarioSchemaDiagnostic locationB = additional("same", "/b");
        assertTrue(ScenarioSchemaDiagnostic.canonicalOrder().compare(locationA, locationB) < 0);

        ScenarioSchemaDiagnostic keywordAdditional = additional("same");
        ScenarioSchemaDiagnostic keywordRequired = required("authority");
        assertTrue(ScenarioSchemaDiagnostic.canonicalOrder()
                .compare(keywordAdditional, keywordRequired) < 0);

        ScenarioSchemaDiagnostic authorityType = type("", "/$defs/authority/type", "string");
        ScenarioSchemaDiagnostic stableKeyType = type("", "/$defs/stableKey/type", "string");
        assertTrue(ScenarioSchemaDiagnostic.canonicalOrder().compare(authorityType, stableKeyType) < 0);

        assertEquals(List.of(ScenarioSchemaDiagnostic.Code.SCHEMA_VIOLATION),
                List.of(ScenarioSchemaDiagnostic.Code.values()));

        ScenarioSchemaDiagnostic shortParameterBytes = additional("z");
        ScenarioSchemaDiagnostic longerParameterBytes = additional("aa");
        assertTrue(ScenarioSchemaDiagnostic.canonicalOrder()
                .compare(shortParameterBytes, longerParameterBytes) < 0);
    }

    @Test
    void parameterNamesUseUnicodeCodePointOrderNotUtf16OrLocaleOrder() {
        ScenarioSchemaDiagnostic.TextParameter supplementary = text("\uD800\uDC00", "value");
        ScenarioSchemaDiagnostic.TextParameter privateUseBmp = text("\uE000", "value");
        List<ScenarioSchemaDiagnostic.CanonicalTypedParameter> parameters =
                new ArrayList<>(List.of(supplementary, privateUseBmp));

        parameters.sort(ScenarioSchemaDiagnostic.canonicalParameterNameOrder());

        assertEquals(List.of(privateUseBmp, supplementary), parameters);
        assertTrue(supplementary.name().compareTo(privateUseBmp.name()) < 0,
                "UTF-16 String order must differ for this vector");
    }

    @Test
    void canonicalCollectionRejectsSurvivingDuplicatesAndNoncanonicalOrder() {
        ScenarioSchemaDiagnostic first = additional("a", "/a");
        ScenarioSchemaDiagnostic second = additional("b", "/b");

        assertEquals(List.of(first, second),
                ScenarioSchemaDiagnostic.canonicalCollection(List.of(first, second)));
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioSchemaDiagnostic.canonicalCollection(List.of(first, first)));
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioSchemaDiagnostic.canonicalCollection(List.of(second, first)));
    }

    private static ScenarioSchemaDiagnostic required(String property) {
        return ScenarioSchemaDiagnostic.v1("", "required", PREFIX + "/required",
                List.of(text("missingProperty", property)));
    }

    private static ScenarioSchemaDiagnostic additional(String property) {
        return additional(property, "");
    }

    private static ScenarioSchemaDiagnostic additional(String property, String location) {
        return ScenarioSchemaDiagnostic.v1(location, "additionalProperties", PREFIX + "/additionalProperties",
                List.of(text("unexpectedProperty", property)));
    }

    private static ScenarioSchemaDiagnostic type(String location, String pointer, String expected) {
        return ScenarioSchemaDiagnostic.v1(location, "type", PREFIX + pointer,
                List.of(text("expectedType", expected)));
    }

    private static ScenarioSchemaDiagnostic unsigned(
            String keyword, String pointer, String name, long value) {
        return ScenarioSchemaDiagnostic.v1("", keyword, PREFIX + pointer,
                List.of(new ScenarioSchemaDiagnostic.Unsigned64Parameter(name, value)));
    }

    private static void assertParameterHex(ScenarioSchemaDiagnostic diagnostic, String expected) {
        assertEquals(expected, HexFormat.of().formatHex(
                ScenarioSchemaDiagnostic.canonicalTypedParameterBytes(diagnostic)));
    }

    private static ScenarioSchemaDiagnostic.TextParameter text(String name, String value) {
        return new ScenarioSchemaDiagnostic.TextParameter(name, value);
    }

    private static void assertRejected(
            String keyword,
            String rule,
            ScenarioSchemaDiagnostic.CanonicalTypedParameter parameter
    ) {
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioSchemaDiagnostic.v1("", keyword, rule, List.of(parameter)));
    }
}
