package ru.kuznetsov.qaip.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Metadata(String id, String name, String description, String version,
                       Map<String, Object> attributes) {
    public Metadata {
        attributes = immutableMap(attributes);
    }

    static Map<String, Object> immutableMap(Map<String, ?> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, immutableValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    static List<Object> immutableList(List<?> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<Object> copy = new ArrayList<>(source.size());
        source.forEach(value -> copy.add(immutableValue(value)));
        return Collections.unmodifiableList(copy);
    }

    @SuppressWarnings("unchecked")
    static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) return immutableMap((Map<String, ?>) map);
        if (value instanceof List<?> list) return immutableList(list);
        return value;
    }
}
