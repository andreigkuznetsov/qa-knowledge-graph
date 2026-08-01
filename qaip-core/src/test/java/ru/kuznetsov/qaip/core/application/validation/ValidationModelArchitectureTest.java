package ru.kuznetsov.qaip.core.application.validation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationModelArchitectureTest {
    @Test
    void severity_has_exactly_error_and_warning_and_models_are_records() {
        assertEquals(List.of(ValidationSeverity.ERROR, ValidationSeverity.WARNING),
                List.of(ValidationSeverity.values()));
        assertTrue(ValidationIssue.class.isRecord());
        assertTrue(ValidationReport.class.isRecord());
        assertEquals(List.of("ruleId", "code", "severity", "message", "nodeId", "relationshipId"),
                Arrays.stream(ValidationIssue.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertEquals(List.of("issues"), Arrays.stream(ValidationReport.class.getRecordComponents())
                .map(component -> component.getName()).toList());
    }

    @Test
    void package_is_standard_library_only_and_contains_no_execution_types() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/validation");
        assertEquals(Set.of("ValidationSeverity.java", "ValidationIssue.java", "ValidationReport.java"),
                Files.list(packagePath).map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet()));
        for (Path file : Files.list(packagePath).toList()) {
            String source = Files.readString(file);
            for (String forbidden : List.of("core.domain", "Project", "Node;", "Relationship;", "Validator",
                    "ValidationEngine", "importing", "persistence", "cli", "trace", "com.fasterxml",
                    "springframework", "jakarta.persistence", "Map<", "Throwable")) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
