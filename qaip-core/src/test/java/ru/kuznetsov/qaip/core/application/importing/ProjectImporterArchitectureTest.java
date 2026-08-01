package ru.kuznetsov.qaip.core.application.importing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProjectImporterArchitectureTest {
    private static final List<Class<?>> API = List.of(
            ProjectImporter.class, DefaultProjectImporter.class, ProjectImportResult.class,
            ProjectImportSuccess.class, ProjectImportFailure.class, ProjectImportFinding.class,
            ProjectImportStage.class, ProjectImportSeverity.class);

    @Test
    void project_importer_is_the_single_public_use_case_operation() {
        List<Method> operations = List.of(ProjectImporter.class.getDeclaredMethods()).stream()
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers())).toList();
        assertEquals(1, operations.size());
        assertEquals("importProject", operations.getFirst().getName());
    }

    @Test
    void public_api_exposes_no_forbidden_implementation_or_infrastructure_types() {
        for (Class<?> type : API) {
            assertAllowed(type, type.getGenericSuperclass());
            for (Type implemented : type.getGenericInterfaces()) assertAllowed(type, implemented);
            for (Method method : type.getMethods()) {
                assertAllowed(type, method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) assertAllowed(type, parameter);
            }
            for (Constructor<?> constructor : type.getConstructors()) {
                for (Type parameter : constructor.getGenericParameterTypes()) assertAllowed(type, parameter);
            }
            for (Field field : type.getFields()) assertAllowed(type, field.getGenericType());
            for (RecordComponent component : type.getRecordComponents() == null
                    ? new RecordComponent[0] : type.getRecordComponents()) assertAllowed(type, component.getGenericType());
        }
    }

    @Test
    void importer_has_no_rules_frameworks_persistence_or_reflection() throws IOException {
        Path packagePath = Path.of("src/main/java/ru/kuznetsov/qaip/core/application/importing");
        try (var files = Files.walk(packagePath)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : List.of("com.fasterxml.jackson", "com.networknt", "org.springframework",
                        "jakarta.persistence", "org.hibernate", "java.lang.reflect", "DefaultProjectBinder",
                        "DefaultProjectApplicationValidator", "JacksonProjectJsonParser",
                        "NetworkntProjectSchemaValidator", "RELATIONSHIP_NOT_ALLOWED", "DUPLICATE_NODE_ID")) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }

    @Test
    void frozen_boundaries_and_domain_do_not_depend_on_orchestrator() throws IOException {
        for (String relative : List.of("domain", "importing", "validation")) {
            Path root = Path.of("src/main/java/ru/kuznetsov/qaip/core/" + relative);
            try (var files = Files.walk(root)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertFalse(Files.readString(file).contains("qaip.core.application.importing"),
                            () -> file + " depends on import orchestrator");
                }
            }
        }
    }

    private static void assertAllowed(Class<?> owner, Type signature) {
        if (signature == null) return;
        String name = signature.getTypeName();
        for (String forbidden : List.of("com.fasterxml.jackson", "com.networknt", "org.springframework",
                "jakarta.persistence", "org.hibernate", "JacksonProjectJsonParser",
                "NetworkntProjectSchemaValidator", "DefaultProjectBinder", "DefaultProjectApplicationValidator",
                "ProjectParseFinding", "SchemaValidationFinding", "BindingFinding", "ApplicationValidationFinding")) {
            assertFalse(name.contains(forbidden), () -> owner.getName() + " exposes " + name);
        }
    }
}
