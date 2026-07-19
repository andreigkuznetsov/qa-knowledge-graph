package ru.kuznetsov.qagraph.api;

public record ModelRegistrationResponse(
        String modelId,
        int nodeCount,
        int relationshipCount,
        int warnings
) {
}
