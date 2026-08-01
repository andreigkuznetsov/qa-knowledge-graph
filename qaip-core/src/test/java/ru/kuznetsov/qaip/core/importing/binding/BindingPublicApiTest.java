package ru.kuznetsov.qaip.core.importing.binding;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingPublicApiTest {
    private static final List<Class<?>> API_TYPES = List.of(
            ProjectBinder.class, DefaultProjectBinder.class, BindingResult.class,
            BindingSuccess.class, BindingFailure.class, BindingFinding.class,
            BindingException.class, BoundProjectDocument.class,
            Project.class, Metadata.class, Subject.class, Node.class,
            Relationship.class, EvidenceManifest.class, DeclaredChange.class);

    @Test
    void bound_proof_has_restricted_construction_and_public_project_access() throws Exception {
        Constructor<?> constructor = BoundProjectDocument.class.getDeclaredConstructor(Project.class);
        assertFalse(Modifier.isPublic(constructor.getModifiers()));
        assertEquals(Project.class, BoundProjectDocument.class.getMethod("project").getReturnType());
        assertTrue(Arrays.stream(BoundProjectDocument.class.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .noneMatch(method -> method.getReturnType() == BoundProjectDocument.class));
    }

    @Test
    void prompt_3_public_api_exposes_no_forbidden_infrastructure_types() {
        for (Class<?> type : API_TYPES) {
            assertAllowed(type, type.getGenericSuperclass());
            for (Type implemented : type.getGenericInterfaces()) assertAllowed(type, implemented);
            for (Class<?> permitted : type.getPermittedSubclasses() == null
                    ? new Class<?>[0] : type.getPermittedSubclasses()) assertAllowed(type, permitted);
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

    private static void assertAllowed(Class<?> owner, Type signature) {
        if (signature == null) return;
        String name = signature.getTypeName();
        for (String forbidden : List.of("com.fasterxml.jackson", "org.springframework", "jakarta.persistence",
                "org.hibernate", "com.networknt.schema", "JacksonProjectJsonParser",
                "NetworkntProjectSchemaValidator")) {
            assertFalse(name.contains(forbidden), () -> owner.getName() + " exposes " + name);
        }
    }
}
