package ru.kuznetsov.qaip.core.importing.schema;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.importing.parsing.NetworkntProjectSchemaValidator;
import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SchemaValidationPublicApiTest {
    @Test
    void publicPrompt2ApiExposesNeitherJacksonNorNetworknt() {
        for (Class<?> type : List.of(ProjectSchemaValidator.class, SchemaValidationResult.class,
                SchemaValidationAccepted.class, SchemaValidationRejected.class,
                SchemaValidationFinding.class, SchemaValidationFindingCode.class,
                JsonSchemaLocation.class, SchemaValidProjectDocument.class,
                NetworkntProjectSchemaValidator.class, ProjectSchemaValidationContractException.class)) {
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
        assertFalse(name.contains("com.fasterxml.jackson") || name.contains("com.networknt.schema"),
                () -> owner.getName() + " exposes " + name);
    }
}
