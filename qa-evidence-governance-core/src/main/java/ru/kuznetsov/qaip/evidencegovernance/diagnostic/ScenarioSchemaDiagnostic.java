package ru.kuznetsov.qaip.evidencegovernance.diagnostic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Evidence Governance-owned canonical Scenario Authority schema diagnostic.
 * Instances can only be created through the finite V1 factory.
 */
public final class ScenarioSchemaDiagnostic {
    public static final String MAPPING_CONTRACT_IDENTIFIER =
            "scenario-authority-schema-diagnostic-mapping-v1";
    public static final String DIAGNOSTIC_CONTRACT_VERSION =
            "scenario-authority-schema-diagnostic-v1";
    public static final String RULE_PREFIX =
            "qaip-scenario-authority-manifest-schema-v1#";

    private static final Map<String, RuleDefinition> RULES = rules();
    private static final Comparator<ScenarioSchemaDiagnostic> CANONICAL_ORDER =
            (left, right) -> {
                int compared = compareCodePoints(left.instanceLocation, right.instanceLocation);
                if (compared != 0) return compared;
                compared = compareCodePoints(left.normativeSchemaKeyword, right.normativeSchemaKeyword);
                if (compared != 0) return compared;
                compared = compareCodePoints(left.schemaRuleIdentifier, right.schemaRuleIdentifier);
                if (compared != 0) return compared;
                compared = compareCodePoints(left.stableCode.name(), right.stableCode.name());
                if (compared != 0) return compared;
                return compareUnsigned(parameterBytes(left.typedParameters), parameterBytes(right.typedParameters));
            };

    private final String diagnosticContractVersion;
    private final Code stableCode;
    private final String instanceLocation;
    private final String normativeSchemaKeyword;
    private final String schemaRuleIdentifier;
    private final List<CanonicalTypedParameter> typedParameters;

    private ScenarioSchemaDiagnostic(
            String instanceLocation,
            String normativeSchemaKeyword,
            String schemaRuleIdentifier,
            List<CanonicalTypedParameter> typedParameters
    ) {
        this.diagnosticContractVersion = DIAGNOSTIC_CONTRACT_VERSION;
        this.stableCode = Code.SCHEMA_VIOLATION;
        this.instanceLocation = requireJsonPointer(instanceLocation);
        this.normativeSchemaKeyword = requireNonBlank(normativeSchemaKeyword, "normativeSchemaKeyword");
        this.schemaRuleIdentifier = requireNonBlank(schemaRuleIdentifier, "schemaRuleIdentifier");
        this.typedParameters = validateParameters(normativeSchemaKeyword, schemaRuleIdentifier, typedParameters);
    }

    public static ScenarioSchemaDiagnostic v1(
            String instanceLocation,
            String normativeSchemaKeyword,
            String schemaRuleIdentifier,
            List<CanonicalTypedParameter> typedParameters
    ) {
        return new ScenarioSchemaDiagnostic(
                instanceLocation, normativeSchemaKeyword, schemaRuleIdentifier, typedParameters);
    }

    /** The authoritative ADR-015 ordering, including canonical typed-parameter bytes. */
    public static Comparator<ScenarioSchemaDiagnostic> canonicalOrder() {
        return CANONICAL_ORDER;
    }

    /** Exact ADR-015 bytes used as the final diagnostic ordering key. */
    public static byte[] canonicalTypedParameterBytes(ScenarioSchemaDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        return parameterBytes(diagnostic.typedParameters);
    }

    /** Writes one complete authoritative diagnostic without exposing a second serialization truth. */
    public static void writeCanonical(
            CanonicalBinaryWriter writer,
            ScenarioSchemaDiagnostic diagnostic
    ) {
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(diagnostic, "diagnostic");
        writer.writeText(diagnostic.diagnosticContractVersion)
                .writeText(diagnostic.stableCode.name())
                .writeText(diagnostic.instanceLocation)
                .writeText(diagnostic.normativeSchemaKeyword)
                .writeText(diagnostic.schemaRuleIdentifier)
                .writeOrderedCollection(diagnostic.typedParameters, (parameterWriter, parameter) -> {
                    parameterWriter.writeText(parameter.name()).writeText(parameter.type().name());
                    if (parameter instanceof TextParameter text) parameterWriter.writeText(text.value());
                    else parameterWriter.writeUnsigned64(((Unsigned64Parameter) parameter).value());
                });
    }

    /** Exact Unicode code-point ordering for canonical typed-parameter names. */
    public static Comparator<CanonicalTypedParameter> canonicalParameterNameOrder() {
        return (left, right) -> compareCodePoints(left.name(), right.name());
    }

    /** Validates one already consolidated and canonically ordered V1 diagnostic collection. */
    public static List<ScenarioSchemaDiagnostic> canonicalCollection(
            List<ScenarioSchemaDiagnostic> diagnostics
    ) {
        List<ScenarioSchemaDiagnostic> immutable = List.copyOf(
                Objects.requireNonNull(diagnostics, "diagnostics"));
        if (new HashSet<>(immutable).size() != immutable.size()) {
            throw new IllegalArgumentException(
                    "canonical schema diagnostic collection must not contain duplicate tuples");
        }
        for (int index = 1; index < immutable.size(); index++) {
            if (CANONICAL_ORDER.compare(immutable.get(index - 1), immutable.get(index)) >= 0) {
                throw new IllegalArgumentException(
                        "canonical schema diagnostic collection must use canonical order");
            }
        }
        return immutable;
    }

    public String diagnosticContractVersion() { return diagnosticContractVersion; }
    public Code stableCode() { return stableCode; }
    public String instanceLocation() { return instanceLocation; }
    public String normativeSchemaKeyword() { return normativeSchemaKeyword; }
    public String schemaRuleIdentifier() { return schemaRuleIdentifier; }
    public List<CanonicalTypedParameter> typedParameters() { return typedParameters; }

    public enum Code { SCHEMA_VIOLATION }

    public enum ParameterType { TEXT, UINT64 }

    public sealed interface CanonicalTypedParameter permits TextParameter, Unsigned64Parameter {
        String name();
        ParameterType type();
    }

    public record TextParameter(String name, String value) implements CanonicalTypedParameter {
        public TextParameter {
            name = requireNonBlank(name, "name");
            Objects.requireNonNull(value, "value");
            new CanonicalBinaryWriter().writeText(name).writeText(value);
        }

        @Override public ParameterType type() { return ParameterType.TEXT; }
    }

    public record Unsigned64Parameter(String name, BigInteger value)
            implements CanonicalTypedParameter {
        public Unsigned64Parameter {
            name = requireNonBlank(name, "name");
            Objects.requireNonNull(value, "value");
            new CanonicalBinaryWriter().writeText(name).writeUnsigned64(value);
        }

        public Unsigned64Parameter(String name, long value) {
            this(name, BigInteger.valueOf(value));
        }

        @Override public ParameterType type() { return ParameterType.UINT64; }
    }

    private static List<CanonicalTypedParameter> validateParameters(
            String keyword,
            String ruleIdentifier,
            List<CanonicalTypedParameter> parameters
    ) {
        RuleDefinition rule = RULES.get(ruleIdentifier);
        if (rule == null) throw new IllegalArgumentException("unsupported V1 schema rule identifier");
        if (!rule.keyword.equals(keyword)) {
            throw new IllegalArgumentException("schema rule and keyword are not a supported V1 combination");
        }
        List<CanonicalTypedParameter> immutable = List.copyOf(
                Objects.requireNonNull(parameters, "typedParameters"));
        if (immutable.size() != rule.parameters.size()) {
            throw new IllegalArgumentException("typed parameters do not match the V1 keyword shape");
        }
        String previous = null;
        for (int index = 0; index < immutable.size(); index++) {
            CanonicalTypedParameter parameter = Objects.requireNonNull(immutable.get(index), "typedParameter");
            if (previous != null && compareCodePoints(previous, parameter.name()) >= 0) {
                throw new IllegalArgumentException(
                        "typed parameter names must be unique and ordered by Unicode code point");
            }
            ParameterDefinition expected = rule.parameters.get(index);
            if (!expected.name.equals(parameter.name()) || expected.type != parameter.type()) {
                throw new IllegalArgumentException("typed parameters do not match the V1 keyword shape");
            }
            if (expected.fixedValue != null && !expected.fixedValue.equals(parameterValue(parameter))) {
                throw new IllegalArgumentException("typed parameter value does not match the V1 schema rule");
            }
            previous = parameter.name();
        }
        validateDerivedValues(ruleIdentifier, immutable);
        return immutable;
    }

    private static void validateDerivedValues(
            String ruleIdentifier,
            List<CanonicalTypedParameter> parameters
    ) {
        if (ruleIdentifier.equals(RULE_PREFIX + "/required")) {
            requireMissingProperty(parameters, Set.of(
                    "format", "schemaVersion", "authority", "scenarioIdentityScheme", "scenarios"));
        } else if (ruleIdentifier.equals(RULE_PREFIX + "/$defs/scenario/required")) {
            requireMissingProperty(parameters, Set.of(
                    "scenarioKey", "title", "given", "when", "then", "operationRef", "ruleRefs"));
        } else if (ruleIdentifier.equals(RULE_PREFIX + "/$defs/httpOperationReference/required")) {
            requireMissingProperty(parameters, Set.of("identityScheme", "method", "path"));
        } else if (ruleIdentifier.equals(RULE_PREFIX + "/$defs/businessRuleReference/required")) {
            requireMissingProperty(parameters, Set.of("authority", "stableRuleKey", "identityScheme"));
        } else if (ruleIdentifier.equals(
                RULE_PREFIX + "/$defs/scenario/properties/ruleRefs/uniqueItems")) {
            BigInteger duplicate = ((Unsigned64Parameter) parameters.get(0)).value();
            BigInteger first = ((Unsigned64Parameter) parameters.get(1)).value();
            if (first.compareTo(duplicate) >= 0) {
                throw new IllegalArgumentException(
                        "uniqueItems requires firstIndex lower than duplicateIndex");
            }
        }
    }

    private static void requireMissingProperty(
            List<CanonicalTypedParameter> parameters,
            Set<String> properties
    ) {
        String property = ((TextParameter) parameters.get(0)).value();
        if (!properties.contains(property)) {
            throw new IllegalArgumentException("missingProperty is not declared by the V1 schema rule");
        }
    }

    private static Object parameterValue(CanonicalTypedParameter parameter) {
        if (parameter instanceof TextParameter text) return text.value();
        return ((Unsigned64Parameter) parameter).value();
    }

    private static byte[] parameterBytes(List<CanonicalTypedParameter> parameters) {
        return new CanonicalBinaryWriter()
                .writeOrderedCollection(parameters, (writer, parameter) -> {
                    writer.writeText(parameter.name()).writeText(parameter.type().name());
                    if (parameter instanceof TextParameter text) writer.writeText(text.value());
                    else writer.writeUnsigned64(((Unsigned64Parameter) parameter).value());
                })
                .toByteArray();
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        int length = Math.min(left.length, right.length);
        for (int index = 0; index < length; index++) {
            int compared = Integer.compare(Byte.toUnsignedInt(left[index]), Byte.toUnsignedInt(right[index]));
            if (compared != 0) return compared;
        }
        return Integer.compare(left.length, right.length);
    }

    private static int compareCodePoints(String left, String right) {
        int leftOffset = 0;
        int rightOffset = 0;
        while (leftOffset < left.length() && rightOffset < right.length()) {
            int leftCodePoint = left.codePointAt(leftOffset);
            int rightCodePoint = right.codePointAt(rightOffset);
            int compared = Integer.compare(leftCodePoint, rightCodePoint);
            if (compared != 0) return compared;
            leftOffset += Character.charCount(leftCodePoint);
            rightOffset += Character.charCount(rightCodePoint);
        }
        return Integer.compare(left.length() - leftOffset, right.length() - rightOffset);
    }

    private static String requireJsonPointer(String value) {
        Objects.requireNonNull(value, "instanceLocation");
        if (!value.isEmpty() && !value.startsWith("/")) {
            throw new IllegalArgumentException("instanceLocation must be an RFC 6901 JSON Pointer");
        }
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '~') {
                if (++index >= value.length() || (value.charAt(index) != '0' && value.charAt(index) != '1')) {
                    throw new IllegalArgumentException("instanceLocation has an invalid RFC 6901 escape");
                }
            }
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static Map<String, RuleDefinition> rules() {
        Map<String, RuleDefinition> rules = new HashMap<>();
        text(rules, "/type", "type", "expectedType", "object");
        text(rules, "/additionalProperties", "additionalProperties", "unexpectedProperty", null);
        text(rules, "/required", "required", "missingProperty", null);
        text(rules, "/properties/format/const", "const", "expectedText", "qaip-scenario-authority-manifest-v1");
        text(rules, "/properties/schemaVersion/const", "const", "expectedText", "1.0");
        text(rules, "/properties/scenarioIdentityScheme/const", "const", "expectedText", "qaip-scenario-identity-v1");
        text(rules, "/properties/scenarios/type", "type", "expectedType", "array");
        text(rules, "/$defs/authority/type", "type", "expectedType", "string");
        unsigned(rules, "/$defs/authority/minLength", "minLength", "minimumCodePointLength", 1);
        unsigned(rules, "/$defs/authority/maxLength", "maxLength", "maximumCodePointLength", 200);
        text(rules, "/$defs/authority/pattern", "pattern", "requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:/-]*$");
        text(rules, "/$defs/stableKey/type", "type", "expectedType", "string");
        unsigned(rules, "/$defs/stableKey/minLength", "minLength", "minimumCodePointLength", 1);
        unsigned(rules, "/$defs/stableKey/maxLength", "maxLength", "maximumCodePointLength", 160);
        text(rules, "/$defs/stableKey/pattern", "pattern", "requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$");
        text(rules, "/$defs/identityScheme/type", "type", "expectedType", "string");
        unsigned(rules, "/$defs/identityScheme/minLength", "minLength", "minimumCodePointLength", 1);
        unsigned(rules, "/$defs/identityScheme/maxLength", "maxLength", "maximumCodePointLength", 160);
        text(rules, "/$defs/identityScheme/pattern", "pattern", "requiredPattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$");
        text(rules, "/$defs/nonBlankString/type", "type", "expectedType", "string");
        unsigned(rules, "/$defs/nonBlankString/minLength", "minLength", "minimumCodePointLength", 1);
        text(rules, "/$defs/nonBlankString/pattern", "pattern", "requiredPattern", ".*\\S.*");
        text(rules, "/$defs/steps/type", "type", "expectedType", "array");
        unsigned(rules, "/$defs/steps/minItems", "minItems", "minimumItemCount", 1);
        text(rules, "/$defs/scenario/type", "type", "expectedType", "object");
        text(rules, "/$defs/scenario/additionalProperties", "additionalProperties", "unexpectedProperty", null);
        text(rules, "/$defs/scenario/required", "required", "missingProperty", null);
        text(rules, "/$defs/scenario/properties/ruleRefs/type", "type", "expectedType", "array");
        add(rules, "/$defs/scenario/properties/ruleRefs/uniqueItems", "uniqueItems", List.of(
                new ParameterDefinition("duplicateIndex", ParameterType.UINT64, null),
                new ParameterDefinition("firstIndex", ParameterType.UINT64, null)));
        text(rules, "/$defs/httpOperationReference/type", "type", "expectedType", "object");
        text(rules, "/$defs/httpOperationReference/additionalProperties", "additionalProperties", "unexpectedProperty", null);
        text(rules, "/$defs/httpOperationReference/required", "required", "missingProperty", null);
        text(rules, "/$defs/httpOperationReference/properties/identityScheme/const", "const", "expectedText", "qaip-http-operation-reference-v1");
        text(rules, "/$defs/businessRuleReference/type", "type", "expectedType", "object");
        text(rules, "/$defs/businessRuleReference/additionalProperties", "additionalProperties", "unexpectedProperty", null);
        text(rules, "/$defs/businessRuleReference/required", "required", "missingProperty", null);
        if (rules.size() != 36) throw new IllegalStateException("V1 must contain exactly 36 rules");
        return Map.copyOf(rules);
    }

    private static void text(Map<String, RuleDefinition> rules, String pointer, String keyword,
                             String name, String fixedValue) {
        add(rules, pointer, keyword, List.of(new ParameterDefinition(name, ParameterType.TEXT, fixedValue)));
    }

    private static void unsigned(Map<String, RuleDefinition> rules, String pointer, String keyword,
                                 String name, long fixedValue) {
        add(rules, pointer, keyword, List.of(new ParameterDefinition(
                name, ParameterType.UINT64, BigInteger.valueOf(fixedValue))));
    }

    private static void add(Map<String, RuleDefinition> rules, String pointer, String keyword,
                            List<ParameterDefinition> parameters) {
        String identifier = RULE_PREFIX + pointer;
        if (rules.put(identifier, new RuleDefinition(keyword, parameters)) != null) {
            throw new IllegalStateException("duplicate V1 schema rule identifier");
        }
    }

    private record RuleDefinition(String keyword, List<ParameterDefinition> parameters) {}
    private record ParameterDefinition(String name, ParameterType type, Object fixedValue) {}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ScenarioSchemaDiagnostic that)) return false;
        return stableCode == that.stableCode
                && diagnosticContractVersion.equals(that.diagnosticContractVersion)
                && instanceLocation.equals(that.instanceLocation)
                && normativeSchemaKeyword.equals(that.normativeSchemaKeyword)
                && schemaRuleIdentifier.equals(that.schemaRuleIdentifier)
                && typedParameters.equals(that.typedParameters);
    }

    @Override public int hashCode() {
        return Objects.hash(diagnosticContractVersion, stableCode, instanceLocation,
                normativeSchemaKeyword, schemaRuleIdentifier, typedParameters);
    }

    @Override public String toString() {
        return "ScenarioSchemaDiagnostic[diagnosticContractVersion=" + diagnosticContractVersion
                + ", stableCode=" + stableCode + ", instanceLocation=" + instanceLocation
                + ", normativeSchemaKeyword=" + normativeSchemaKeyword
                + ", schemaRuleIdentifier=" + schemaRuleIdentifier
                + ", typedParameters=" + typedParameters + ']';
    }
}
