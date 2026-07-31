package ru.kuznetsov.qaip.core.validation;

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

import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationValidationArchitectureTest {
    private static final List<Class<?>> API = List.of(
            ProjectApplicationValidator.class, DefaultProjectApplicationValidator.class,
            ApplicationValidationResult.class, ApplicationValidationSuccess.class,
            ApplicationValidationFailure.class, ApplicationValidProjectDocument.class,
            ApplicationValidationFinding.class, ApplicationValidationSeverity.class,
            ApplicationValidationException.class);

    @Test
    void public_api_exposes_no_forbidden_infrastructure() {
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
    void implementation_has_no_forbidden_boundary_dependencies_or_reflection() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java/ru/kuznetsov/qaip/core/validation"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : List.of("com.fasterxml.jackson", "org.springframework", "jakarta.persistence",
                        "org.hibernate", "com.networknt", "JacksonProjectJsonParser", "NetworkntProjectSchemaValidator",
                        "DefaultProjectBinder", "java.lang.reflect", "getDeclaredMethod", "setAccessible")) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }

    @Test
    void domain_has_no_dependency_on_application_validation() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java/ru/kuznetsov/qaip/core/domain"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("qaip.core.validation"),
                        () -> file + " depends on application validation");
            }
        }
    }

    private static void assertAllowed(Class<?> owner, Type signature) {
        if (signature == null) return;
        String name = signature.getTypeName();
        for (String forbidden : List.of("com.fasterxml.jackson", "org.springframework", "jakarta.persistence",
                "org.hibernate", "com.networknt", "JacksonProjectJsonParser",
                "NetworkntProjectSchemaValidator", "DefaultProjectBinder")) {
            assertFalse(name.contains(forbidden), () -> owner.getName() + " exposes " + name);
        }
    }
}
