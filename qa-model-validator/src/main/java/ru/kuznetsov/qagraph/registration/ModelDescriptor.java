package ru.kuznetsov.qagraph.registration;

import java.time.Instant;

public record ModelDescriptor(
        String modelId,
        Instant createdAt,
        int nodeCount,
        int relationshipCount,
        int warningCount
) {
}
