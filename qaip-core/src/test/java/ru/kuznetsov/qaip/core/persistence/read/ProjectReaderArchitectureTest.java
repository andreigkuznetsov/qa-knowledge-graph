package ru.kuznetsov.qaip.core.persistence.read;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.ProjectRepository;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.postgresql.PostgreSqlProjectReader;

import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectReaderArchitectureTest {
    @Test
    void dedicated_port_has_exactly_one_infrastructure_free_capability() throws Exception {
        assertTrue(Modifier.isPublic(ProjectReader.class.getModifiers()));
        assertEquals(1, ProjectReader.class.getDeclaredMethods().length);
        var method = ProjectReader.class.getDeclaredMethod("findById", String.class);
        assertEquals(Optional.class, method.getReturnType());
        assertTrue(method.getGenericReturnType().getTypeName().contains(Project.class.getName()));
        assertFalse(ProjectRepository.class.isAssignableFrom(ProjectReader.class));
        assertFalse(ProjectReader.class.isAssignableFrom(ProjectRepository.class));
        assertTrue(ProjectReader.class.isAssignableFrom(InMemoryProjectReader.class));
        assertTrue(ProjectReader.class.isAssignableFrom(PostgreSqlProjectReader.class));
    }
}
