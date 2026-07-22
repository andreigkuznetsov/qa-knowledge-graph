package ru.kuznetsov.qagraph.change.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalQaModelVersionTest {

    @Test
    void shouldPreserveAndRecognizeSupportedVersion() {
        assertEquals("0.1", CanonicalQaModelVersion.V0_1.value());
        assertTrue(new CanonicalQaModelVersion("0.1").isSupported());
    }

    @Test
    void shouldPreserveUnknownVersionWithoutTreatingItAsSupported() {
        CanonicalQaModelVersion version =
                new CanonicalQaModelVersion("0.2");

        assertEquals("0.2", version.value());
        assertFalse(version.isSupported());
    }

    @Test
    void shouldRejectNullAndBlankVersion() {
        assertThrows(NullPointerException.class,
                () -> new CanonicalQaModelVersion(null));
        assertThrows(IllegalArgumentException.class,
                () -> new CanonicalQaModelVersion(" "));
    }
}
