package ru.kuznetsov.qaip.core.domain;

import java.util.Map;

public record Metadata(String id, String name, String description, String version,
                       Map<String, Object> attributes) {
    public Metadata {
        attributes = immutableMap(attributes);
    }

    static Map<String, Object> immutableMap(Map<?, ?> source) {
        return DynamicValueNormalizer.map(source);
    }
}
