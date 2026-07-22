package ru.kuznetsov.qagraph.change.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalIdentityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "A", "NODE-1", "node_1", "project.node:1", "A.B-C_D:1"
    })
    void shouldAcceptAndPreserveSchemaCompliantIdentity(String value) {
        CanonicalIdentity identity = new CanonicalIdentity(value);

        assertEquals(value, identity.value());
    }

    @Test
    void shouldAcceptMaximumLengthIdentity() {
        String value = "A".repeat(120);

        assertEquals(value, new CanonicalIdentity(value).value());
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldRejectEmptyAndBlankIdentity(String value) {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentity(value));
    }

    @Test
    void shouldRejectNullIdentity() {
        assertThrows(NullPointerException.class,
                () -> new CanonicalIdentity(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " NODE-1", "NODE-1 ", "-NODE", "_NODE", ".NODE", ":NODE",
            "NODE/1", "NODE 1", "NODE@1", "ТЕСТ"
    })
    void shouldRejectIdentityOutsideSchemaPattern(String value) {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentity(value));
    }

    @Test
    void shouldRejectIdentityLongerThanSchemaMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalIdentity("A".repeat(121)));
    }

    @Test
    void shouldPreserveCaseAndProvideValueEquality() {
        assertEquals(new CanonicalIdentity("Node-1"),
                new CanonicalIdentity("Node-1"));
        assertNotEquals(new CanonicalIdentity("Node-1"),
                new CanonicalIdentity("NODE-1"));
    }
}
