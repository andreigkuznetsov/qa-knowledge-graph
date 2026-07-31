package ru.kuznetsov.qaip.core.domain;

import java.util.List;
import java.util.Map;

public record EvidenceManifest(String contractVersion, String sourceId,
                               Map<String, Object> snapshot, String normalizationVersion,
                               String canonicalizationVersion, String manifestFingerprint,
                               List<Map<String, Object>> identityAssertions,
                               List<Map<String, Object>> relationships,
                               List<Map<String, Object>> provenance) {
    public EvidenceManifest {
        snapshot = Metadata.immutableMap(snapshot);
        identityAssertions = immutableMaps(identityAssertions);
        relationships = immutableMaps(relationships);
        provenance = immutableMaps(provenance);
    }

    private static List<Map<String, Object>> immutableMaps(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream().map(Metadata::immutableMap).toList();
    }
}
