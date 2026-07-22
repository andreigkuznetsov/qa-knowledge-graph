package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** Immutable, mutation-safe reconstructed Canonical QA Model root. */
public final class ProposedCanonicalRoot {

    private final ObjectNode snapshot;

    public ProposedCanonicalRoot(JsonNode snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (!snapshot.isObject()) {
            throw new IllegalArgumentException("snapshot must be an object");
        }
        this.snapshot = ((ObjectNode) snapshot).deepCopy();
    }

    public ObjectNode snapshot() {
        return snapshot.deepCopy();
    }
}
