package ru.kuznetsov.qaip.core.application.query.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.ValidationReport;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationMappingArchitectureTest {
    @Test
    void mapper_is_public_final_stateless_with_one_public_map_method() throws Exception {
        assertTrue(Modifier.isPublic(ValidationReportMapper.class.getModifiers()));
        assertTrue(Modifier.isFinal(ValidationReportMapper.class.getModifiers()));
        assertEquals(0, ValidationReportMapper.class.getDeclaredFields().length);
        assertEquals(1, Arrays.stream(ValidationReportMapper.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).count());
        assertEquals(ValidationReportResult.class,
                ValidationReportMapper.class.getMethod("map", ValidationReport.class).getReturnType());
    }

    @Test
    void mapped_records_expose_only_scalar_values_and_dto_lists() {
        assertTrue(ValidationIssueResult.class.isRecord());
        assertTrue(Arrays.stream(ValidationIssueResult.class.getRecordComponents())
                .allMatch(component -> component.getType() == String.class));
        assertTrue(ValidationReportResult.class.isRecord());
        assertEquals(List.of(boolean.class, long.class, long.class, List.class), Arrays.stream(
                ValidationReportResult.class.getRecordComponents()).map(component -> component.getType()).toList());
        String api = Arrays.toString(ValidationIssueResult.class.getRecordComponents())
                + Arrays.toString(ValidationReportResult.class.getRecordComponents());
        for (String forbidden : List.of("ValidationIssue", "ValidationSeverity", "ValidationReport",
                "Project", "Node", "Relationship", "Map", "Object")) {
            assertFalse(api.contains("application.validation." + forbidden));
            assertFalse(api.contains("core.domain." + forbidden));
        }
    }

    @Test
    void query_package_has_no_execution_infrastructure_or_delivery_dependencies() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/query/validation");
        for (Path file : Files.list(packagePath).toList()) {
            String source = Files.readString(file);
            for (String forbidden : List.of("ValidationEngine", "ProjectValidationRule", ".rule.", "ProjectReader",
                    "Repository", "persistence", "importing", "cli", "Jackson", "springframework",
                    "jakarta.persistence", "sort(", "filter(")) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
