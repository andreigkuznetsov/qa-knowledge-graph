package ru.kuznetsov.qaip.analysis.adapter.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;
import ru.kuznetsov.qagraph.validationcore.model.ValidationSummary;
import ru.kuznetsov.qaip.core.analysis.AnalysisAssessment;
import ru.kuznetsov.qaip.core.analysis.AssessmentStatus;
import ru.kuznetsov.qaip.core.finding.FindingCategory;
import ru.kuznetsov.qaip.core.finding.FindingSeverity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationAssessmentMapperTest {

    private final ValidationAssessmentMapper mapper =
            new ValidationAssessmentMapper();

    @Test
    void shouldMapInvalidResultToFailedAssessment() {
        QaModelValidationResult result = new QaModelValidationResult(
                false,
                "0.1",
                new ValidationSummary(1, 0, 1),
                List.of(
                        ValidationIssue.schemaError(
                                "REQUIRED",
                                "Отсутствует поле nodes",
                                "$"
                        )
                )
        );

        AnalysisAssessment assessment = mapper.map(result);

        assertEquals(AssessmentStatus.FAILED, assessment.status());
        assertEquals(1, assessment.findings().size());
        assertEquals(
                FindingSeverity.ERROR,
                assessment.findings().getFirst().severity()
        );
        assertEquals(
                FindingCategory.VALIDATION,
                assessment.findings().getFirst().category()
        );
        assertEquals(3, assessment.metrics().size());
        assertEquals(false, assessment.data().get("valid"));
    }

    @Test
    void shouldMapValidResultWithWarningsToPartial() {
        QaModelValidationResult result = new QaModelValidationResult(
                true,
                "0.1",
                new ValidationSummary(0, 1, 1),
                List.of(
                        ValidationIssue.semanticWarning(
                                "SCENARIO_WITHOUT_TEST",
                                "Сценарий не покрыт тестом",
                                "SC-001"
                        )
                )
        );

        AnalysisAssessment assessment = mapper.map(result);

        assertEquals(AssessmentStatus.PARTIAL, assessment.status());
        assertEquals(
                FindingSeverity.WARNING,
                assessment.findings().getFirst().severity()
        );
    }
}
