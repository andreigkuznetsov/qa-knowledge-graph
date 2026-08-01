package ru.kuznetsov.qaip.core.domain;

import java.util.List;
import java.util.Map;

public record Relationship(String id, String from, String type, String to,
                           Map<String, Object> properties,
                           List<Map<String, Object>> sourceReferences) {
    public Relationship {
        properties = Metadata.immutableMap(properties);
        sourceReferences = sourceReferences == null || sourceReferences.isEmpty()
                ? List.of()
                : sourceReferences.stream().map(Metadata::immutableMap).toList();
    }
}
