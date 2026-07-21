package ru.kuznetsov.qagraph.change.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeKindTest {

    @Test
    void shouldExposeOnlyKindsInCanonicalTieBreakerOrder() {
        assertEquals(
                List.of("ADDED", "MODIFIED", "REMOVED"),
                Arrays.stream(ChangeKind.values()).map(Enum::name).toList()
        );
        assertEquals(
                List.of(0, 1, 2),
                Arrays.stream(ChangeKind.values())
                        .map(ChangeKind::canonicalOrder)
                        .toList()
        );
    }
}
