package ru.kuznetsov.qaip.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicDomainValueTest {
    @Test
    void nested_maps_and_lists_are_deeply_copied_and_unmodifiable() {
        List<Object> originalList = new ArrayList<>(List.of("first"));
        Map<String, Object> originalNestedMap = new LinkedHashMap<>();
        originalNestedMap.put("items", originalList);
        Map<String, Object> originalAttributes = new LinkedHashMap<>();
        originalAttributes.put("nested", originalNestedMap);

        Metadata metadata = new Metadata("P-1", "Project", null, null, originalAttributes);
        originalList.add("outside");
        originalNestedMap.put("outside", true);
        originalAttributes.put("outside", true);

        Map<String, Object> nested = nestedMap(metadata);
        List<Object> items = nestedList(nested);
        assertEquals(List.of("first"), items);
        assertEquals(Map.of("items", List.of("first")), nested);
        assertThrows(UnsupportedOperationException.class, () -> metadata.attributes().put("changed", true));
        assertThrows(UnsupportedOperationException.class, () -> nested.put("changed", true));
        assertThrows(UnsupportedOperationException.class, () -> items.add("changed"));
    }

    @Test
    void unsupported_mutable_values_are_rejected() {
        assertUnsupported(new byte[] {1});
        assertUnsupported(Set.of("value"));
        assertUnsupported(new StringBuilder("value"));
        assertUnsupported(new Object());
    }

    @Test
    void non_string_map_keys_are_rejected() {
        Map<Object, Object> invalid = new LinkedHashMap<>();
        invalid.put(1, "value");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("nested", invalid);
        assertThrows(IllegalArgumentException.class,
                () -> new Metadata("P-1", "Project", null, null, attributes));
    }

    @Test
    void supported_lossless_numbers_are_preserved() {
        BigInteger integer = new BigInteger("123456789012345678901234567890");
        BigDecimal decimal = new BigDecimal("1234567890.123456789012345678900");
        Metadata metadata = new Metadata("P-1", "Project", null, null,
                Map.of("integer", integer, "decimal", decimal));
        assertSame(integer, metadata.attributes().get("integer"));
        assertSame(decimal, metadata.attributes().get("decimal"));
    }

    private static void assertUnsupported(Object value) {
        assertThrows(IllegalArgumentException.class,
                () -> new Metadata("P-1", "Project", null, null, Map.of("value", value)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMap(Metadata metadata) {
        return (Map<String, Object>) metadata.attributes().get("nested");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> nestedList(Map<String, Object> nested) {
        return (List<Object>) nested.get("items");
    }
}
