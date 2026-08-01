package ru.kuznetsov.qaip.core.persistence;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectPersistenceContractsTest {
    @Test
    void repository_results_require_non_blank_ids_and_have_value_semantics() {
        for (String invalid : new String[] {"", " ", "\t"}) {
            assertThrows(IllegalArgumentException.class, () -> new ProjectInserted(invalid));
            assertThrows(IllegalArgumentException.class, () -> new ProjectAlreadyExists(invalid));
        }
        assertThrows(NullPointerException.class, () -> new ProjectInserted(null));
        assertThrows(NullPointerException.class, () -> new ProjectAlreadyExists(null));
        assertEquals(new ProjectInserted("P-1"), new ProjectInserted("P-1"));
        assertEquals(new ProjectInserted("P-1").hashCode(), new ProjectInserted("P-1").hashCode());
        assertEquals(new ProjectAlreadyExists("P-1"), new ProjectAlreadyExists("P-1"));
        assertNotEquals(new ProjectInserted("P-1"), new ProjectAlreadyExists("P-1"));
    }

    @Test
    void persistence_exception_preserves_message_and_cause() {
        RuntimeException cause = new RuntimeException("storage");
        ProjectPersistenceException exception = new ProjectPersistenceException("failed", cause);
        assertEquals("failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("failed", new ProjectPersistenceException("failed").getMessage());
    }

    @Test
    void repository_results_expose_only_immutable_project_identity() {
        for (Class<?> resultType : new Class<?>[] {ProjectInserted.class, ProjectAlreadyExists.class}) {
            assertEquals(1, resultType.getRecordComponents().length);
            assertEquals(String.class, resultType.getRecordComponents()[0].getType());
            assertEquals("projectId", resultType.getRecordComponents()[0].getName());
            assertEquals(0, Arrays.stream(resultType.getRecordComponents())
                    .map(RecordComponent::getType)
                    .filter(ru.kuznetsov.qaip.core.domain.Project.class::equals)
                    .count());
        }
    }
}
