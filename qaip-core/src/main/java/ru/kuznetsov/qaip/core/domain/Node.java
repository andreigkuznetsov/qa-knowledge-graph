package ru.kuznetsov.qaip.core.domain;

import java.util.List;
import java.util.Map;

public record Node(String id, String type, String name, String description, String status,
                   List<String> tags, List<Map<String, Object>> sourceReferences,
                   Map<String, Object> metadata, Map<String, Object> attributes) {
    public Node {
        tags = tags == null ? List.of() : List.copyOf(tags);
        sourceReferences = immutableMaps(sourceReferences);
        metadata = Metadata.immutableMap(metadata);
        attributes = Metadata.immutableMap(attributes);
    }

    private static List<Map<String, Object>> immutableMaps(List<Map<String, Object>> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream().map(value -> Metadata.immutableMap(value)).toList();
    }
}
