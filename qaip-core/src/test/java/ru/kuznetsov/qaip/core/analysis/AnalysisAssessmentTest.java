package ru.kuznetsov.qaip.core.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisAssessmentTest {

    @Test
    void skippedAssessmentShouldBeImmutable() {
        AnalysisAssessment assessment =
                AnalysisAssessment.skipped(
                        "validator",
                        "Validator",
                        "invalid input"
                );

        assertEquals(
                AssessmentStatus.SKIPPED,
                assessment.status()
        );
        assertTrue(assessment.findings().isEmpty());
        assertTrue(assessment.metrics().isEmpty());
        assertEquals(
                "invalid input",
                assessment.data().get("reason")
        );
    }
}
