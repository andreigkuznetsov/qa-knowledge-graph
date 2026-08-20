package ru.kuznetsov.qagraph.validationcore.scenarioauthority;

import java.util.List;
import java.util.Objects;

/** Stable generic structural signals from the pinned Manifest V1 schema. */
public record ScenarioAuthorityManifestSchemaValidationV1(
        String schemaContractIdentifier,
        String schemaContentIdentity,
        List<Signal> signals
) {
    public ScenarioAuthorityManifestSchemaValidationV1 {
        Objects.requireNonNull(schemaContractIdentifier);
        Objects.requireNonNull(schemaContentIdentity);
        signals = List.copyOf(Objects.requireNonNull(signals));
    }

    public boolean structurallyValid() { return signals.isEmpty(); }

    /** Message is compatibility presentation only and is never canonical evidence. */
    public record Signal(String keyword, String schemaPointer, String instancePointer, String message) {
        public Signal {
            requireText(keyword, "keyword");
            if (schemaPointer == null || !schemaPointer.startsWith("/"))
                throw new IllegalArgumentException("schemaPointer must be an absolute JSON Pointer");
            if (instancePointer == null || (!instancePointer.isEmpty() && !instancePointer.startsWith("/")))
                throw new IllegalArgumentException("instancePointer must be an RFC 6901 JSON Pointer");
            requireText(message, "message");
        }
        private static void requireText(String value, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
