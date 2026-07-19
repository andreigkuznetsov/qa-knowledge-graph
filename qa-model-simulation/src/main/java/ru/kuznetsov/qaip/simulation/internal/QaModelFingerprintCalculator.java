package ru.kuznetsov.qaip.simulation.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qaip.simulation.error.SimulationErrorCode;
import ru.kuznetsov.qaip.simulation.error.SimulationException;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;

/**
 * Calculates {@code qamodel-c14n-v1:<lowercase SHA-256 hex>} fingerprints.
 * Object keys are sorted, array order is retained, and JSON scalar kinds are
 * retained. Numerically equal JSON numbers are normalized through
 * {@link BigDecimal#stripTrailingZeros()}, so 1, 1.0, 1.00 and 1e0 are equal.
 * Jackson emits the canonical tree as whitespace-free UTF-8 JSON.
 */
final class QaModelFingerprintCalculator {
    private static final String PREFIX = "qamodel-c14n-v1:";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    String calculate(JsonNode model) {
        if (model == null) {
            throw new SimulationException(
                    SimulationErrorCode.INVALID_SIMULATION_INPUT,
                    "currentModel must not be null"
            );
        }
        try {
            byte[] canonical = MAPPER.writeValueAsBytes(canonicalize(model));
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical);
            return PREFIX + HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new SimulationException(
                    SimulationErrorCode.INVALID_SIMULATION_INPUT,
                    "Unable to calculate the current model fingerprint"
            );
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = MAPPER.createObjectNode();
            var sorted = new TreeMap<String, JsonNode>();
            node.fields().forEachRemaining(entry ->
                    sorted.put(entry.getKey(), entry.getValue()));
            sorted.forEach((name, value) ->
                    result.set(name, canonicalize(value)));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = MAPPER.createArrayNode();
            node.forEach(element -> result.add(canonicalize(element)));
            return result;
        }
        if (node.isNumber()) {
            BigDecimal normalized = node.decimalValue().stripTrailingZeros();
            return DecimalNode.valueOf(normalized);
        }
        return node.deepCopy();
    }
}
