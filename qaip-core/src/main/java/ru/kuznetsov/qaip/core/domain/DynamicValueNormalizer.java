package ru.kuznetsov.qaip.core.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicValueNormalizer {
    private DynamicValueNormalizer() { }

    static Map<String, Object> map(Map<?, ?> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (!(key instanceof String stringKey)) {
                throw new IllegalArgumentException("dynamic value maps must have string keys");
            }
            copy.put(stringKey, value(value));
        });
        return Collections.unmodifiableMap(copy);
    }

    private static List<Object> list(List<?> source) {
        if (source.isEmpty()) return List.of();
        List<Object> copy = new ArrayList<>(source.size());
        source.forEach(item -> copy.add(value(item)));
        return Collections.unmodifiableList(copy);
    }

    private static Object value(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Integer || value instanceof Long
                || value instanceof BigInteger || value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Map<?, ?> map) return map(map);
        if (value instanceof List<?> list) return list(list);
        throw new IllegalArgumentException("unsupported dynamic value type: " + value.getClass().getName());
    }
}
