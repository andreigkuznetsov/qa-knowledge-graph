package ru.kuznetsov.qagraph.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentHealthTest {

    @Test
    void shouldResolvePassForValidModelWithoutActionableFindings() {
        assertEquals(AssessmentHealth.PASS, AssessmentHealth.from(true, 0, 0));
    }

    @Test
    void shouldResolveWarningForValidModelWithMediumOrHighFindings() {
        assertEquals(AssessmentHealth.WARNING, AssessmentHealth.from(true, 0, 1));
        assertEquals(AssessmentHealth.WARNING, AssessmentHealth.from(true, 1, 0));
    }

    @Test
    void shouldResolveFailForInvalidModel() {
        assertEquals(AssessmentHealth.FAIL, AssessmentHealth.from(false, 0, 0));
    }
}
