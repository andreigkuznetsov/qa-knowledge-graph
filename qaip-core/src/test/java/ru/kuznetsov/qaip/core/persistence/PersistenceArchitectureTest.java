package ru.kuznetsov.qaip.core.persistence;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.persistence.DefaultPersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProject;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectAccepted;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFinding;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectFindingCode;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectRejected;
import ru.kuznetsov.qaip.core.application.persistence.PersistProjectResult;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;
import ru.kuznetsov.qaip.core.validation.ApplicationValidProjectDocument;

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

class PersistenceArchitectureTest {
    private static final List<Class<?>> API = List.of(
            ProjectRepository.class, ProjectInsertResult.class, ProjectInserted.class,
            ProjectAlreadyExists.class, ProjectPersistenceException.class,
            InMemoryProjectRepository.class, PersistProject.class, DefaultPersistProject.class,
            PersistProjectResult.class, PersistProjectAccepted.class, PersistProjectRejected.class,
            PersistProjectFinding.class, PersistProjectFindingCode.class);

    @Test
    void proof_requirement_and_atomic_repository_operation_are_explicit() throws Exception {
        Method execute = PersistProject.class.getDeclaredMethod("execute", ApplicationValidProjectDocument.class);
        assertEquals(PersistProjectResult.class, execute.getReturnType());
        assertEquals(1, PersistProject.class.getDeclaredMethods().length);
        assertFalse(List.of(PersistProject.class.getMethods()).stream().anyMatch(method ->
                List.of(method.getParameterTypes()).contains(Project.class)));

        Method insert = ProjectRepository.class.getDeclaredMethod("insertIfAbsent", Project.class);
        assertEquals(ProjectInsertResult.class, insert.getReturnType());
        assertEquals(1, ProjectRepository.class.getDeclaredMethods().length);
        assertFalse(List.of(ProjectRepository.class.getMethods()).stream().anyMatch(method ->
                List.of(method.getParameterTypes()).contains(ApplicationValidProjectDocument.class)));
    }

    @Test
    void public_api_exposes_no_forbidden_infrastructure_or_import_diagnostics() {
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
    void production_dependency_direction_is_enforced() throws IOException {
        assertSourceDirectoryExcludes("src/main/java/ru/kuznetsov/qaip/core/persistence",
                "core.application.persistence", "core.importing", "core.validation", "com.fasterxml",
                "com.networknt", "org.springframework", "jakarta.persistence", "org.hibernate");
        assertSourceTreeExcludes("src/main/java/ru/kuznetsov/qaip/core/persistence/memory",
                "core.application", "core.importing", "core.validation");
        assertSourceTreeExcludes("src/main/java/ru/kuznetsov/qaip/core/application/persistence",
                "JacksonProjectJsonParser", "NetworkntProjectSchemaValidator", "DefaultProjectBinder",
                "DefaultProjectApplicationValidator", "ProjectImportFinding", "ApplicationValidationFinding");
        for (String lowerLayer : List.of("domain", "importing", "validation", "application/importing")) {
            assertSourceTreeExcludes("src/main/java/ru/kuznetsov/qaip/core/" + lowerLayer,
                    "qaip.core.persistence", "qaip.core.application.persistence");
        }
    }

    private static void assertSourceTreeExcludes(String root, String... forbiddenValues) throws IOException {
        try (var files = Files.walk(Path.of(root))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : forbiddenValues) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }

    private static void assertSourceDirectoryExcludes(String root, String... forbiddenValues) throws IOException {
        try (var files = Files.list(Path.of(root))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : forbiddenValues) {
                    assertFalse(source.contains(forbidden), () -> file + " contains " + forbidden);
                }
            }
        }
    }

    private static void assertAllowed(Class<?> owner, Type signature) {
        if (signature == null) return;
        String name = signature.getTypeName();
        for (String forbidden : List.of("com.fasterxml", "com.networknt", "org.springframework",
                "jakarta.persistence", "org.hibernate", "JacksonProjectJsonParser",
                "NetworkntProjectSchemaValidator", "DefaultProjectBinder", "DefaultProjectApplicationValidator",
                "ProjectImportFinding", "ApplicationValidationFinding", "SchemaValidationFinding",
                "BindingFinding", "ProjectParseFinding", "java.lang.reflect")) {
            assertFalse(name.contains(forbidden), () -> owner.getName() + " exposes " + name);
        }
    }
}
