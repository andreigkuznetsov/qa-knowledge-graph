package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.ValidationMessage;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic.*;

/** Authoritative finite adapter for scenario-authority-schema-diagnostic-mapping-v1. */
public final class ScenarioSchemaDiagnosticAdapterV1 {
    public static final String CONTRACT_IDENTIFIER =
            ScenarioSchemaDiagnostic.MAPPING_CONTRACT_IDENTIFIER;
    public static final String RULE_PREFIX =
            ScenarioSchemaDiagnostic.RULE_PREFIX;
    public static final String SCHEMA_CONTENT_IDENTITY =
            ScenarioManifestSchemaValidator.SCHEMA_CONTENT_IDENTITY;
    public static final String V1_SCHEMA_MAPPING_BINDING =
            CONTRACT_IDENTIFIER + "|" + SCHEMA_CONTENT_IDENTITY;

    private static final Map<String, Rule> RULE_BY_POINTER = rulesByPointer();

    private final ScenarioManifestSchemaValidator validator;

    public ScenarioSchemaDiagnosticAdapterV1() {
        this(new ScenarioManifestSchemaValidator());
    }

    ScenarioSchemaDiagnosticAdapterV1(ScenarioManifestSchemaValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
        if (!SCHEMA_CONTENT_IDENTITY.equals(validator.schemaContentIdentity())) {
            fail("validator schema content identity is not bound to the V1 mapping contract");
        }
    }

    /** Validates and maps completely, or throws one fail-closed compatibility failure. */
    public List<ScenarioSchemaDiagnostic> validate(JsonNode document) {
        if (document == null) fail("validated document is missing");
        try {
            return mapValidationMessages(document, validator.validationMessages(document));
        } catch (ScenarioSchemaDiagnosticMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "validator output is malformed or unusable", exception);
        }
    }

    List<ScenarioSchemaDiagnostic> mapValidationMessages(
            JsonNode document,
            List<ValidationMessage> messages
    ) {
        if (document == null) fail("validated document is missing");
        if (messages == null) fail("validation messages are missing");
        try {
            List<ValidationSignal> signals = new ArrayList<>(messages.size());
            for (ValidationMessage message : messages) signals.add(signal(message));
            return map(document, signals);
        } catch (ScenarioSchemaDiagnosticMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(
                    "validator output is malformed or unusable", exception);
        }
    }

    List<ScenarioSchemaDiagnostic> map(JsonNode document, List<ValidationSignal> signals) {
        if (document == null) fail("validated document is missing");
        if (signals == null) fail("validation signals are missing");
        for (ValidationSignal signal : signals) {
            if (signal == null) fail("validation signal is missing");
        }
        List<ValidationSignal> snapshot = List.copyOf(signals);
        if (snapshot.isEmpty()) return List.of();

        Set<ScenarioSchemaDiagnostic> consolidated = new HashSet<>();
        for (ValidationSignal signal : snapshot) {
            Rule rule = RULE_BY_POINTER.get(signal.schemaPointer());
            if (rule == null) fail("unknown schema rule");
            if (!rule.keyword.equals(signal.keyword())) fail("schema rule and keyword disagree");
            JsonNode instance = resolve(document, signal.instancePointer());
            if (instance == null) fail("instance location does not resolve");
            List<ScenarioSchemaDiagnostic> mapped = mapRule(rule, signal.instancePointer(), instance);
            if (mapped.isEmpty()) fail("validator signal is inconsistent with schema and instance");
            consolidated.addAll(mapped);
        }
        return consolidated.stream().sorted(ScenarioSchemaDiagnostic.canonicalOrder()).toList();
    }

    static int ruleCount() {
        return RULE_BY_POINTER.size();
    }

    private static ValidationSignal signal(ValidationMessage message) {
        if (message == null) fail("validation message is missing");
        String keyword = message.getType();
        if (keyword == null || keyword.isBlank()) fail("validator keyword is missing");
        if (message.getSchemaLocation() == null) fail("validator schema location is missing");
        String schemaPointer = pathPointer(message.getSchemaLocation().getFragment());
        String instancePointer = pathPointer(message.getInstanceLocation());
        return new ValidationSignal(keyword, schemaPointer, instancePointer);
    }

    private static List<ScenarioSchemaDiagnostic> mapRule(
            Rule rule,
            String instancePointer,
            JsonNode instance
    ) {
        return switch (rule.keyword) {
            case "type" -> mapType(rule, instancePointer, instance);
            case "required" -> mapRequired(rule, instancePointer, instance);
            case "additionalProperties" -> mapAdditionalProperties(rule, instancePointer, instance);
            case "const" -> mapConst(rule, instancePointer, instance);
            case "minLength" -> mapMinLength(rule, instancePointer, instance);
            case "maxLength" -> mapMaxLength(rule, instancePointer, instance);
            case "pattern" -> mapPattern(rule, instancePointer, instance);
            case "minItems" -> mapMinItems(rule, instancePointer, instance);
            case "uniqueItems" -> mapUniqueItems(rule, instancePointer, instance);
            default -> throw new IllegalStateException("unreachable keyword " + rule.keyword);
        };
    }

    private static List<ScenarioSchemaDiagnostic> mapType(
            Rule rule, String location, JsonNode instance) {
        String expected = rule.textValue;
        boolean matches = switch (expected) {
            case "object" -> instance.isObject();
            case "array" -> instance.isArray();
            case "string" -> instance.isTextual();
            default -> throw new IllegalStateException("unsupported fixed type " + expected);
        };
        return matches ? List.of() : List.of(diagnostic(rule, location,
                List.of(new TextParameter("expectedType", expected))));
    }

    private static List<ScenarioSchemaDiagnostic> mapRequired(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isObject()) fail("required applies only to an object");
        List<ScenarioSchemaDiagnostic> result = new ArrayList<>();
        for (String property : rule.properties) {
            if (!instance.has(property)) {
                result.add(diagnostic(rule, location,
                        List.of(new TextParameter("missingProperty", property))));
            }
        }
        return result;
    }

    private static List<ScenarioSchemaDiagnostic> mapAdditionalProperties(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isObject()) fail("additionalProperties applies only to an object");
        List<String> unexpected = new ArrayList<>();
        instance.fieldNames().forEachRemaining(name -> {
            if (!rule.propertySet.contains(name)) unexpected.add(name);
        });
        unexpected.sort(UnicodeCodePointOrder::compare);
        return unexpected.stream()
                .map(property -> diagnostic(rule, location,
                        List.of(new TextParameter("unexpectedProperty", property))))
                .toList();
    }

    private static List<ScenarioSchemaDiagnostic> mapConst(
            Rule rule, String location, JsonNode instance) {
        if (instance.isTextual() && rule.textValue.equals(instance.textValue())) return List.of();
        return List.of(diagnostic(rule, location,
                List.of(new TextParameter("expectedText", rule.textValue))));
    }

    private static List<ScenarioSchemaDiagnostic> mapMinLength(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isTextual()) fail("minLength applies only to a string");
        long length = instance.textValue().codePointCount(0, instance.textValue().length());
        return length >= rule.unsignedValue ? List.of() : List.of(diagnostic(rule, location,
                List.of(new Unsigned64Parameter("minimumCodePointLength", rule.unsignedValue))));
    }

    private static List<ScenarioSchemaDiagnostic> mapMaxLength(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isTextual()) fail("maxLength applies only to a string");
        long length = instance.textValue().codePointCount(0, instance.textValue().length());
        return length <= rule.unsignedValue ? List.of() : List.of(diagnostic(rule, location,
                List.of(new Unsigned64Parameter("maximumCodePointLength", rule.unsignedValue))));
    }

    private static List<ScenarioSchemaDiagnostic> mapPattern(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isTextual()) fail("pattern applies only to a string");
        boolean matches = Pattern.compile(rule.textValue, Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(instance.textValue()).find();
        return matches ? List.of() : List.of(diagnostic(rule, location,
                List.of(new TextParameter("requiredPattern", rule.textValue))));
    }

    private static List<ScenarioSchemaDiagnostic> mapMinItems(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isArray()) fail("minItems applies only to an array");
        return instance.size() >= rule.unsignedValue ? List.of() : List.of(diagnostic(rule, location,
                List.of(new Unsigned64Parameter("minimumItemCount", rule.unsignedValue))));
    }

    private static List<ScenarioSchemaDiagnostic> mapUniqueItems(
            Rule rule, String location, JsonNode instance) {
        if (!instance.isArray()) fail("uniqueItems applies only to an array");
        List<ScenarioSchemaDiagnostic> result = new ArrayList<>();
        boolean[] assigned = new boolean[instance.size()];
        for (int first = 0; first < instance.size(); first++) {
            if (assigned[first]) continue;
            for (int later = first + 1; later < instance.size(); later++) {
                if (jsonSchemaEqual(instance.get(first), instance.get(later))) {
                    assigned[later] = true;
                    result.add(diagnostic(rule, location, List.of(
                            new Unsigned64Parameter("duplicateIndex", later),
                            new Unsigned64Parameter("firstIndex", first))));
                }
            }
        }
        return result;
    }

    private static ScenarioSchemaDiagnostic diagnostic(
            Rule rule,
            String location,
            List<CanonicalTypedParameter> parameters
    ) {
        List<CanonicalTypedParameter> ordered = parameters.stream()
                .sorted((left, right) -> UnicodeCodePointOrder.compare(left.name(), right.name()))
                .toList();
        return ScenarioSchemaDiagnostic.v1(
                location,
                rule.keyword,
                RULE_PREFIX + rule.pointer,
                ordered);
    }

    private static boolean jsonSchemaEqual(JsonNode left, JsonNode right) {
        if (left.isNumber() && right.isNumber()) {
            BigDecimal leftNumber = left.decimalValue();
            BigDecimal rightNumber = right.decimalValue();
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (left.getNodeType() != right.getNodeType()) return false;
        if (left.isArray()) {
            if (left.size() != right.size()) return false;
            for (int index = 0; index < left.size(); index++) {
                if (!jsonSchemaEqual(left.get(index), right.get(index))) return false;
            }
            return true;
        }
        if (left.isObject()) {
            if (left.size() != right.size()) return false;
            Iterator<Map.Entry<String, JsonNode>> fields = left.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode other = right.get(field.getKey());
                if (other == null || !jsonSchemaEqual(field.getValue(), other)) return false;
            }
            return true;
        }
        return left.equals(right);
    }

    private static JsonNode resolve(JsonNode document, String pointer) {
        if (pointer.isEmpty()) return document;
        if (!pointer.startsWith("/")) return null;
        JsonNode current = document;
        String[] tokens = pointer.substring(1).split("/", -1);
        for (String encoded : tokens) {
            String token;
            try {
                token = decodeToken(encoded);
            } catch (IllegalArgumentException exception) {
                return null;
            }
            if (current.isObject()) {
                current = current.get(token);
            } else if (current.isArray() && token.matches("0|[1-9][0-9]*")) {
                try {
                    int index = Integer.parseInt(token);
                    current = index < current.size() ? current.get(index) : null;
                } catch (NumberFormatException exception) {
                    return null;
                }
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    private static String decodeToken(String encoded) {
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < encoded.length(); index++) {
            char character = encoded.charAt(index);
            if (character != '~') {
                decoded.append(character);
            } else {
                if (++index >= encoded.length()) throw new IllegalArgumentException("invalid JSON Pointer escape");
                char escape = encoded.charAt(index);
                if (escape == '0') decoded.append('~');
                else if (escape == '1') decoded.append('/');
                else throw new IllegalArgumentException("invalid JSON Pointer escape");
            }
        }
        return decoded.toString();
    }

    private static String pathPointer(JsonNodePath path) {
        if (path == null) fail("validator path is missing");
        StringBuilder pointer = new StringBuilder();
        for (int index = 0; index < path.getNameCount(); index++) {
            String token = String.valueOf(path.getElement(index));
            pointer.append('/').append(token.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }

    private static Map<String, Rule> rulesByPointer() {
        Map<String, Rule> result = new HashMap<>();
        for (Rule rule : EnumSet.allOf(Rule.class)) {
            if (result.put(rule.pointer, rule) != null) throw new IllegalStateException("duplicate rule pointer");
        }
        if (result.size() != 36) throw new IllegalStateException("V1 must contain exactly 36 rules");
        return Map.copyOf(result);
    }

    private static void fail(String reason) {
        throw new ScenarioSchemaDiagnosticMappingException(reason);
    }

    record ValidationSignal(String keyword, String schemaPointer, String instancePointer) {
        ValidationSignal {
            if (keyword == null || keyword.isBlank()) fail("validator keyword is missing");
            if (schemaPointer == null || !schemaPointer.startsWith("/")) fail("schema pointer is malformed");
            if (instancePointer == null || (!instancePointer.isEmpty() && !instancePointer.startsWith("/"))) {
                fail("instance pointer is malformed");
            }
        }
    }

    private enum Rule {
        ROOT_TYPE("/type", "type", "object"),
        ROOT_ADDITIONAL("/additionalProperties", "additionalProperties",
                Set.of("format", "schemaVersion", "authority", "scenarioIdentityScheme", "scenarios")),
        ROOT_REQUIRED("/required", "required",
                List.of("format", "schemaVersion", "authority", "scenarioIdentityScheme", "scenarios")),
        FORMAT_CONST("/properties/format/const", "const", "qaip-scenario-authority-manifest-v1"),
        SCHEMA_CONST("/properties/schemaVersion/const", "const", "1.0"),
        SCENARIO_IDENTITY_CONST("/properties/scenarioIdentityScheme/const", "const",
                "qaip-scenario-identity-v1"),
        SCENARIOS_TYPE("/properties/scenarios/type", "type", "array"),
        AUTHORITY_TYPE("/$defs/authority/type", "type", "string"),
        AUTHORITY_MIN("/$defs/authority/minLength", "minLength", 1),
        AUTHORITY_MAX("/$defs/authority/maxLength", "maxLength", 200),
        AUTHORITY_PATTERN("/$defs/authority/pattern", "pattern", "^[A-Za-z0-9][A-Za-z0-9._:/-]*$"),
        STABLE_KEY_TYPE("/$defs/stableKey/type", "type", "string"),
        STABLE_KEY_MIN("/$defs/stableKey/minLength", "minLength", 1),
        STABLE_KEY_MAX("/$defs/stableKey/maxLength", "maxLength", 160),
        STABLE_KEY_PATTERN("/$defs/stableKey/pattern", "pattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$"),
        IDENTITY_TYPE("/$defs/identityScheme/type", "type", "string"),
        IDENTITY_MIN("/$defs/identityScheme/minLength", "minLength", 1),
        IDENTITY_MAX("/$defs/identityScheme/maxLength", "maxLength", 160),
        IDENTITY_PATTERN("/$defs/identityScheme/pattern", "pattern", "^[A-Za-z0-9][A-Za-z0-9._:-]*$"),
        NON_BLANK_TYPE("/$defs/nonBlankString/type", "type", "string"),
        NON_BLANK_MIN("/$defs/nonBlankString/minLength", "minLength", 1),
        NON_BLANK_PATTERN("/$defs/nonBlankString/pattern", "pattern", ".*\\S.*"),
        STEPS_TYPE("/$defs/steps/type", "type", "array"),
        STEPS_MIN("/$defs/steps/minItems", "minItems", 1),
        SCENARIO_TYPE("/$defs/scenario/type", "type", "object"),
        SCENARIO_ADDITIONAL("/$defs/scenario/additionalProperties", "additionalProperties",
                Set.of("scenarioKey", "title", "given", "when", "then", "operationRef", "ruleRefs")),
        SCENARIO_REQUIRED("/$defs/scenario/required", "required",
                List.of("scenarioKey", "title", "given", "when", "then", "operationRef", "ruleRefs")),
        RULE_REFS_TYPE("/$defs/scenario/properties/ruleRefs/type", "type", "array"),
        RULE_REFS_UNIQUE("/$defs/scenario/properties/ruleRefs/uniqueItems", "uniqueItems", true),
        OPERATION_TYPE("/$defs/httpOperationReference/type", "type", "object"),
        OPERATION_ADDITIONAL("/$defs/httpOperationReference/additionalProperties", "additionalProperties",
                Set.of("identityScheme", "method", "path")),
        OPERATION_REQUIRED("/$defs/httpOperationReference/required", "required",
                List.of("identityScheme", "method", "path")),
        OPERATION_IDENTITY_CONST("/$defs/httpOperationReference/properties/identityScheme/const", "const",
                "qaip-http-operation-reference-v1"),
        RULE_TYPE("/$defs/businessRuleReference/type", "type", "object"),
        RULE_ADDITIONAL("/$defs/businessRuleReference/additionalProperties", "additionalProperties",
                Set.of("authority", "stableRuleKey", "identityScheme")),
        RULE_REQUIRED("/$defs/businessRuleReference/required", "required",
                List.of("authority", "stableRuleKey", "identityScheme"));

        private final String pointer;
        private final String keyword;
        private final String textValue;
        private final long unsignedValue;
        private final List<String> properties;
        private final Set<String> propertySet;

        Rule(String pointer, String keyword, String textValue) {
            this(pointer, keyword, textValue, 0, List.of(), Set.of());
        }

        Rule(String pointer, String keyword, long unsignedValue) {
            this(pointer, keyword, null, unsignedValue, List.of(), Set.of());
        }

        Rule(String pointer, String keyword, List<String> properties) {
            this(pointer, keyword, null, 0, List.copyOf(properties), Set.of());
        }

        Rule(String pointer, String keyword, Set<String> properties) {
            this(pointer, keyword, null, 0, List.of(), Set.copyOf(properties));
        }

        Rule(String pointer, String keyword, boolean ignored) {
            this(pointer, keyword, null, 0, List.of(), Set.of());
        }

        Rule(String pointer, String keyword, String textValue, long unsignedValue,
             List<String> properties, Set<String> propertySet) {
            this.pointer = pointer;
            this.keyword = keyword;
            this.textValue = textValue;
            this.unsignedValue = unsignedValue;
            this.properties = properties;
            this.propertySet = propertySet;
        }
    }
}
