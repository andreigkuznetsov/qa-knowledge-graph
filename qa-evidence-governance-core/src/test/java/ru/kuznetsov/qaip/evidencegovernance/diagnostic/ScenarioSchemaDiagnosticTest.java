package ru.kuznetsov.qaip.evidencegovernance.diagnostic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static ScenarioSchemaDiagnostic required(String property) {
        return ScenarioSchemaDiagnostic.v1("", "required", PREFIX + "/required",
                List.of(text("missingProperty", property)));
    }

    private static ScenarioSchemaDiagnostic additional(String property) {
        return ScenarioSchemaDiagnostic.v1("", "additionalProperties", PREFIX + "/additionalProperties",
                List.of(text("unexpectedProperty", property)));
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
