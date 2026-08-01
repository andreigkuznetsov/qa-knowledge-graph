package ru.kuznetsov.qaip.core.persistence.memory;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.domain.Project;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryProjectReaderTest {
    @Test
    void reads_exact_stored_instance_without_mutation() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        InMemoryProjectReader reader = new InMemoryProjectReader(repository);
        Project first = InMemoryProjectRepositoryTest.project("P-1", "First");
        Project second = InMemoryProjectRepositoryTest.project("P-2", "Second");
        repository.insertIfAbsent(first);
        repository.insertIfAbsent(second);

        assertSame(first, reader.findById("P-1").orElseThrow());
        assertSame(first, reader.findById("P-1").orElseThrow());
        assertSame(second, reader.findById("P-2").orElseThrow());
        assertTrue(reader.findById("p-1").isEmpty());
        assertTrue(reader.findById(" P-1 ").isEmpty());
    }

    @Test
    void validates_input_and_does_not_share_state_between_repositories() {
        InMemoryProjectRepository populated = new InMemoryProjectRepository();
        populated.insertIfAbsent(InMemoryProjectRepositoryTest.project("P-1", "First"));
        InMemoryProjectReader reader = new InMemoryProjectReader(populated);
        InMemoryProjectReader other = new InMemoryProjectReader(new InMemoryProjectRepository());

        assertThrows(NullPointerException.class, () -> new InMemoryProjectReader(null));
        assertThrows(NullPointerException.class, () -> reader.findById(null));
        assertThrows(IllegalArgumentException.class, () -> reader.findById(" \t"));
        assertTrue(reader.findById("missing").isEmpty());
        assertTrue(other.findById("P-1").isEmpty());
        assertEquals(0, java.lang.reflect.Modifier.isStatic(
                InMemoryProjectRepository.class.getDeclaredFields()[0].getModifiers()) ? 1 : 0);
    }
}
