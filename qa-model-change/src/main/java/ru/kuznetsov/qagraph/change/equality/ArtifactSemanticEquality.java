package ru.kuznetsov.qagraph.change.equality;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.change.model.ArtifactCategory;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_EQUAL;
import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.SEMANTICALLY_UNEQUAL;
import static ru.kuznetsov.qagraph.change.equality.SemanticEqualityResult.UNSUPPORTED;

/**
 * Canonical QA Model semantic equality, separate from Java value equality.
 */
public final class ArtifactSemanticEquality {

    private static final Set<String> ORDER_INSENSITIVE_PATHS = Set.of(
            "nodes[*].tags",
            "nodes[*].sourceReferences",
            "relationships[*].sourceReferences"
    );

    private static final String TEST_STEPS_PATH =
            "nodes[*].testImplementation.steps";

    public SemanticEqualityResult compare(
            ArtifactState left,
            ArtifactState right
    ) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");

        if (!left.schemaVersion().equals(right.schemaVersion())
                || !left.schemaVersion().equals(CanonicalQaModelVersion.V0_1)
                || !right.schemaVersion().equals(CanonicalQaModelVersion.V0_1)) {
            return UNSUPPORTED;
        }
        if (left.category() != right.category()
                || !left.identity().equals(right.identity())) {
            return SEMANTICALLY_UNEQUAL;
        }

        String rootPath = left.category() == ArtifactCategory.NODE
                ? "nodes[*]"
                : "relationships[*]";
        return compareNode(left.snapshot(), right.snapshot(), rootPath);
    }

    private SemanticEqualityResult compareNode(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        if (left.isNumber() && right.isNumber()) {
            return compareNumbers(left, right);
        }
        if (left.getNodeType() != right.getNodeType()) {
            return SEMANTICALLY_UNEQUAL;
        }
        return switch (left.getNodeType()) {
            case OBJECT -> compareObjects(left, right, path);
            case ARRAY -> compareArrays(left, right, path);
            case STRING -> result(left.textValue().equals(right.textValue()));
            case BOOLEAN -> result(left.booleanValue() == right.booleanValue());
            case NULL -> SEMANTICALLY_EQUAL;
            default -> UNSUPPORTED;
        };
    }

    private SemanticEqualityResult compareNumbers(
            JsonNode left,
            JsonNode right
    ) {
        try {
            return result(new BigDecimal(left.asText()).compareTo(
                    new BigDecimal(right.asText())) == 0);
        } catch (ArithmeticException | NumberFormatException exception) {
            return UNSUPPORTED;
        }
    }

    private SemanticEqualityResult compareObjects(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        Set<String> leftProperties = propertyNames(left);
        Set<String> rightProperties = propertyNames(right);
        if (!leftProperties.equals(rightProperties)) {
            return SEMANTICALLY_UNEQUAL;
        }

        for (String property : leftProperties.stream().sorted().toList()) {
            SemanticEqualityResult comparison = compareNode(
                    left.get(property),
                    right.get(property),
                    path + "." + property
            );
            if (comparison != SEMANTICALLY_EQUAL) {
                return comparison;
            }
        }
        return SEMANTICALLY_EQUAL;
    }

    private Set<String> propertyNames(JsonNode object) {
        Set<String> names = new HashSet<>();
        Iterator<String> iterator = object.fieldNames();
        iterator.forEachRemaining(names::add);
        return names;
    }

    private SemanticEqualityResult compareArrays(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        if (TEST_STEPS_PATH.equals(path)) {
            return compareExplicitlyOrderedSteps(left, right, path);
        }
        if (ORDER_INSENSITIVE_PATHS.contains(path)) {
            return compareOrderInsensitive(left, right, path);
        }
        return comparePositionally(left, right, path);
    }

    private SemanticEqualityResult comparePositionally(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        if (left.size() != right.size()) {
            return SEMANTICALLY_UNEQUAL;
        }
        for (int index = 0; index < left.size(); index++) {
            SemanticEqualityResult comparison = compareNode(
                    left.get(index),
                    right.get(index),
                    path + "[*]"
            );
            if (comparison != SEMANTICALLY_EQUAL) {
                return comparison;
            }
        }
        return SEMANTICALLY_EQUAL;
    }

    private SemanticEqualityResult compareOrderInsensitive(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        if (left.size() != right.size()) {
            return SEMANTICALLY_UNEQUAL;
        }

        List<JsonNode> unmatched = new ArrayList<>();
        right.forEach(unmatched::add);
        boolean unsupportedSeen = false;

        for (JsonNode leftElement : left) {
            int equalIndex = -1;
            for (int index = 0; index < unmatched.size(); index++) {
                SemanticEqualityResult comparison = compareNode(
                        leftElement,
                        unmatched.get(index),
                        path + "[*]"
                );
                if (comparison == SEMANTICALLY_EQUAL) {
                    equalIndex = index;
                    break;
                }
                unsupportedSeen |= comparison == UNSUPPORTED;
            }
            if (equalIndex < 0) {
                return unsupportedSeen ? UNSUPPORTED : SEMANTICALLY_UNEQUAL;
            }
            unmatched.remove(equalIndex);
        }
        return SEMANTICALLY_EQUAL;
    }

    private SemanticEqualityResult compareExplicitlyOrderedSteps(
            JsonNode left,
            JsonNode right,
            String path
    ) {
        Map<BigInteger, JsonNode> leftByOrder = stepsByOrder(left);
        Map<BigInteger, JsonNode> rightByOrder = stepsByOrder(right);
        if (leftByOrder == null || rightByOrder == null) {
            return UNSUPPORTED;
        }
        if (!leftByOrder.keySet().equals(rightByOrder.keySet())) {
            return SEMANTICALLY_UNEQUAL;
        }
        for (BigInteger order : leftByOrder.keySet().stream().sorted().toList()) {
            SemanticEqualityResult comparison = compareNode(
                    leftByOrder.get(order),
                    rightByOrder.get(order),
                    path + "[*]"
            );
            if (comparison != SEMANTICALLY_EQUAL) {
                return comparison;
            }
        }
        return SEMANTICALLY_EQUAL;
    }

    private Map<BigInteger, JsonNode> stepsByOrder(JsonNode steps) {
        Map<BigInteger, JsonNode> byOrder = new HashMap<>();
        for (JsonNode step : steps) {
            JsonNode orderNode = step.get("order");
            if (!step.isObject()
                    || orderNode == null
                    || !orderNode.isIntegralNumber()) {
                return null;
            }
            BigInteger order = orderNode.bigIntegerValue();
            if (order.signum() <= 0 || byOrder.put(order, step) != null) {
                return null;
            }
        }
        return byOrder;
    }

    private SemanticEqualityResult result(boolean equal) {
        return equal ? SEMANTICALLY_EQUAL : SEMANTICALLY_UNEQUAL;
    }
}
