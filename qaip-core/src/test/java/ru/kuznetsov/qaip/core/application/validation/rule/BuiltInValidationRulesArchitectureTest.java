package ru.kuznetsov.qaip.core.application.validation.rule;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.ProjectValidationRule;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInValidationRulesArchitectureTest {
    @Test
    void rules_are_public_final_stateless_and_expose_only_contract_methods() {
        for (Class<?> type : List.of(IsolatedNodeValidationRule.class, ScenarioWithoutTestValidationRule.class)) {
            assertTrue(Modifier.isPublic(type.getModifiers()));
            assertTrue(Modifier.isFinal(type.getModifiers()));
            assertTrue(ProjectValidationRule.class.isAssignableFrom(type));
            assertEquals(0, type.getDeclaredFields().length);
            assertEquals(List.of("ruleId", "validate"), Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .map(method -> method.getName()).sorted().toList());
        }
    }

    @Test
    void rules_have_only_domain_validation_model_and_standard_library_dependencies() throws Exception {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/validation/rule");
        for (Path file : Files.list(packagePath).toList()) {
            String source = Files.readString(file);
            for (String forbidden : List.of("ProjectReader", "Repository", "persistence", "importing", "schema",
                    "Jackson", "cli", "springframework", "jakarta.persistence", "ServiceLoader", "reflect",
                    "Registry", "ValidationEngine", "parallel", "Cache")) {
                assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
            }
        }
    }
}
