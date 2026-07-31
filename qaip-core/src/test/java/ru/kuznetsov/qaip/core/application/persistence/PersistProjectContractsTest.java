package ru.kuznetsov.qaip.core.application.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersistProjectContractsTest {
    @Test
    void accepted_id_is_non_blank_and_value_based() {
        assertThrows(NullPointerException.class, () -> new PersistProjectAccepted(null));
        for (String blank : new String[] {"", " ", "\t"}) {
            assertThrows(IllegalArgumentException.class, () -> new PersistProjectAccepted(blank));
        }
        assertEquals(new PersistProjectAccepted("P-1"), new PersistProjectAccepted("P-1"));
        assertEquals(new PersistProjectAccepted("P-1").hashCode(), new PersistProjectAccepted("P-1").hashCode());
    }

    @Test
    void finding_and_rejection_enforce_all_invariants_and_value_semantics() {
        assertThrows(NullPointerException.class, () -> new PersistProjectFinding(null, "message", "P-1"));
        assertThrows(NullPointerException.class, () -> new PersistProjectFinding(
                PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, null, "P-1"));
        assertThrows(NullPointerException.class, () -> new PersistProjectFinding(
                PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, "message", null));
        for (String blank : new String[] {"", " ", "\t"}) {
            assertThrows(IllegalArgumentException.class, () -> new PersistProjectFinding(
                    PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, blank, "P-1"));
            assertThrows(IllegalArgumentException.class, () -> new PersistProjectFinding(
                    PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, "message", blank));
        }
        PersistProjectFinding finding = new PersistProjectFinding(
                PersistProjectFindingCode.PROJECT_ALREADY_EXISTS, "message", "P-1");
        assertThrows(NullPointerException.class, () -> new PersistProjectRejected(null));
        assertEquals(new PersistProjectRejected(finding), new PersistProjectRejected(finding));
        assertEquals(new PersistProjectRejected(finding).hashCode(), new PersistProjectRejected(finding).hashCode());
    }
}
