package ru.kuznetsov.qaip.core.quality;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QualityAssessmentTest {

    @Test
    void shouldAcceptValidScore() {
        QualityAssessment assessment =
                new QualityAssessment(
                        87.5,
                        QualityStatus.GOOD,
                        Map.of("coverage", 90)
                );

        assertEquals(87.5, assessment.score());
    }

    @Test
    void shouldRejectScoreOutsideRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QualityAssessment(
                        101.0,
                        QualityStatus.VALID,
                        Map.of()
                )
        );
    }
}
