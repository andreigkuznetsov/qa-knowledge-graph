package ru.kuznetsov.qaip.core.application.query.validation;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.validation.ValidationEngine;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUseCaseArchitectureTest {
    @Test
    void sealed_api_has_exactly_two_scalar_query_outcomes_without_internal_leakage() {
        assertTrue(ValidationQueryResult.class.isSealed());
        assertEquals(Set.of(ValidationCompleted.class, ValidationProjectNotFound.class),
                Set.of(ValidationQueryResult.class.getPermittedSubclasses()));
        for (Class<?> type : ValidationQueryResult.class.getPermittedSubclasses()) assertTrue(type.isRecord());
        String api = ValidationUseCase.class.toGenericString() + Arrays.toString(ValidationUseCase.class.getMethods())
                + Arrays.toString(ValidationQueryResult.class.getPermittedSubclasses());
        for (String forbidden : List.of("core.domain", "ValidationReport", "ValidationIssue", "Optional",
                "persistence", "ProjectReader")) assertFalse(api.contains(forbidden));
    }

    @Test
    void implementation_is_final_injected_stateless_and_exposes_one_public_execute_method() throws Exception {
        assertTrue(Modifier.isFinal(DefaultValidationUseCase.class.getModifiers()));
        assertEquals(3, DefaultValidationUseCase.class.getDeclaredFields().length);
        assertTrue(Arrays.stream(DefaultValidationUseCase.class.getDeclaredFields())
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertEquals(List.of(ProjectReader.class, ValidationEngine.class, ValidationReportMapper.class),
                List.of(DefaultValidationUseCase.class.getConstructors()[0].getParameterTypes()));
        assertEquals(1, Arrays.stream(DefaultValidationUseCase.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).count());
    }

    @Test
    void source_is_orchestration_only_without_rules_algorithms_mapping_or_delivery() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/ru/kuznetsov/qaip/core/application/query/validation/DefaultValidationUseCase.java"));
        for (String forbidden : List.of("IsolatedNode", "ScenarioWithoutTest", "new ValidationEngine",
                "new ValidationReportMapper", ".nodes()", ".relationships()", "new ValidationIssueResult",
                "errorCount()", "warningCount()", "isValid()", "InMemory", "Repository", "cli", "Renderer",
                "ServiceLoader", "reflect", "catch (")) {
            assertFalse(source.contains(forbidden), () -> "use case contains " + forbidden);
        }
    }
}
