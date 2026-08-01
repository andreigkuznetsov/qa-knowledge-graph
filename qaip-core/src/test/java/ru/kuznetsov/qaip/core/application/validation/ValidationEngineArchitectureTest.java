package ru.kuznetsov.qaip.core.application.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineArchitectureTest {
    @Test
    void rule_contract_has_only_rule_id_and_project_validation() throws Exception {
        assertTrue(ProjectValidationRule.class.isInterface());
        assertEquals(Set.of("ruleId", "validate"), Arrays.stream(ProjectValidationRule.class.getDeclaredMethods())
                .map(method -> method.getName()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(String.class, ProjectValidationRule.class.getMethod("ruleId").getReturnType());
        assertEquals(List.class, ProjectValidationRule.class.getMethod("validate", Project.class).getReturnType());
    }

    @Test
    void engine_is_final_with_one_immutable_rule_list_and_one_public_validate_method() throws Exception {
        assertTrue(Modifier.isFinal(ValidationEngine.class.getModifiers()));
        assertEquals(1, ValidationEngine.class.getDeclaredFields().length);
        assertTrue(Modifier.isFinal(ValidationEngine.class.getDeclaredFields()[0].getModifiers()));
        assertEquals(1, Arrays.stream(ValidationEngine.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).count());
        assertEquals(ValidationReport.class, ValidationEngine.class.getMethod("validate", Project.class).getReturnType());
    }

    @Test
    void package_has_no_builtin_rules_or_external_dependencies() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/validation");
        assertEquals(Set.of("ValidationSeverity.java", "ValidationIssue.java", "ValidationReport.java",
                        "ProjectValidationRule.java", "ValidationEngine.java"),
                Files.list(packagePath).filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                        .collect(java.util.stream.Collectors.toSet()));
        for (Path file : List.of(packagePath.resolve("ProjectValidationRule.java"),
                packagePath.resolve("ValidationEngine.java"))) {
            String source = Files.readString(file);
            for (String forbidden : List.of("ProjectReader", "Repository", "persistence", "importing", "schema",
                    "Jackson", "cli", "springframework", "jakarta.persistence", "ServiceLoader", "reflect",
                    "parallel", "Registry", "Trace", "UseCase")) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
