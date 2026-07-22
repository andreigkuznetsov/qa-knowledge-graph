package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable Base root evidence excluding reconstructed root fields.
 */
public final class CanonicalRootContext {

    private static final Set<String> RECONSTRUCTED_FIELDS = Set.of(
            "schemaVersion",
            "nodes",
            "relationships"
    );

    private final CanonicalQaModelVersion schemaVersion;
    private final ObjectNode retainedProperties;

    public CanonicalRootContext(
            CanonicalQaModelVersion schemaVersion,
            JsonNode retainedProperties
    ) {
        this.schemaVersion = Objects.requireNonNull(
                schemaVersion,
                "schemaVersion must not be null"
        );
        Objects.requireNonNull(
                retainedProperties,
                "retainedProperties must not be null"
        );
        if (!retainedProperties.isObject()) {
            throw new IllegalArgumentException(
                    "retainedProperties must be a JSON object"
            );
        }
        if (RECONSTRUCTED_FIELDS.stream().anyMatch(
                retainedProperties::has)) {
            throw new IllegalArgumentException(
                    "retainedProperties must exclude reconstructed fields"
            );
        }
        this.retainedProperties = retainedProperties.deepCopy();
    }

    public CanonicalQaModelVersion schemaVersion() {
        return schemaVersion;
    }

    public ObjectNode retainedProperties() {
        return retainedProperties.deepCopy();
    }
}
