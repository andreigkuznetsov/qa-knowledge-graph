package ru.kuznetsov.qaip.core.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationValidationContractsTest {
    @Test
    void finding_rejects_null_and_blank_fields() {
        assertThrows(NullPointerException.class, () -> finding(null, ApplicationValidationSeverity.ERROR, "message", "location"));
        assertThrows(NullPointerException.class, () -> finding("CODE", null, "message", "location"));
        assertThrows(NullPointerException.class, () -> finding("CODE", ApplicationValidationSeverity.ERROR, null, "location"));
        assertThrows(NullPointerException.class, () -> finding("CODE", ApplicationValidationSeverity.ERROR, "message", null));
        for (String blank : List.of("", " ", "\t")) {
            assertThrows(IllegalArgumentException.class,
                    () -> finding(blank, ApplicationValidationSeverity.ERROR, "message", "location"));
            assertThrows(IllegalArgumentException.class,
                    () -> finding("CODE", ApplicationValidationSeverity.ERROR, blank, "location"));
            assertThrows(IllegalArgumentException.class,
                    () -> finding("CODE", ApplicationValidationSeverity.ERROR, "message", blank));
        }
    }

    @Test
    void result_collections_are_immutable_and_enforce_result_meaning() {
        Project project = ValidationFixtures.project(List.of(), List.of());
        ApplicationValidProjectDocument proof = new ApplicationValidProjectDocument(project);
        ApplicationValidationFinding warning = finding(
                "WARNING", ApplicationValidationSeverity.WARNING, "warning", "project.nodes");
        List<ApplicationValidationFinding> callerWarnings = new ArrayList<>(List.of(warning));
        ApplicationValidationSuccess success = new ApplicationValidationSuccess(proof, callerWarnings);
        callerWarnings.clear();
        assertEquals(List.of(warning), success.warnings());
        assertThrows(UnsupportedOperationException.class, () -> success.warnings().clear());
        assertThrows(IllegalArgumentException.class, () -> new ApplicationValidationSuccess(proof, List.of(
                finding("ERROR", ApplicationValidationSeverity.ERROR, "error", "project.nodes"))));

        ApplicationValidationFinding error = finding(
                "ERROR", ApplicationValidationSeverity.ERROR, "error", "project.nodes");
        List<ApplicationValidationFinding> callerFindings = new ArrayList<>(List.of(error, warning));
        ApplicationValidationFailure failure = new ApplicationValidationFailure(callerFindings);
        callerFindings.clear();
        assertEquals(List.of(error, warning), failure.findings());
        assertThrows(UnsupportedOperationException.class, () -> failure.findings().clear());
        assertThrows(IllegalArgumentException.class, () -> new ApplicationValidationFailure(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationValidationFailure(List.of(warning)));
    }

    @Test
    void application_valid_proof_is_readable_but_not_publicly_constructible() throws Exception {
        var constructor = ApplicationValidProjectDocument.class.getDeclaredConstructor(Project.class);
        assertFalse(Modifier.isPublic(constructor.getModifiers()));
        assertEquals(Project.class, ApplicationValidProjectDocument.class.getMethod("project").getReturnType());
        assertEquals(0, List.of(ApplicationValidProjectDocument.class.getMethods()).stream()
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == ApplicationValidProjectDocument.class)
                .count());
    }

    @Test
    void application_validation_exception_preserves_internal_cause() {
        RuntimeException cause = new RuntimeException("internal");
        ApplicationValidationException exception = new ApplicationValidationException("failed", cause);
        assertEquals("failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    private static ApplicationValidationFinding finding(
            String code, ApplicationValidationSeverity severity, String message, String location) {
        return new ApplicationValidationFinding(code, severity, message, location);
    }
}
