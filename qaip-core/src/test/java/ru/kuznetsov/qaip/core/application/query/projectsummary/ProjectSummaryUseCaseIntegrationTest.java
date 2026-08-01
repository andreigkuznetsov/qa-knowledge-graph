package ru.kuznetsov.qaip.core.application.query.projectsummary;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.persistence.ProjectAlreadyExists;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectReader;
import ru.kuznetsov.qaip.core.persistence.memory.InMemoryProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSummaryUseCaseIntegrationTest {
    @Test
    void real_in_memory_write_read_and_summary_flow_is_non_mutating_and_first_write_wins() {
        InMemoryProjectRepository repository = new InMemoryProjectRepository();
        var original = DefaultProjectSummaryUseCaseTest.project("P-1");
        var duplicate = DefaultProjectSummaryUseCaseTest.project("P-1");
        repository.insertIfAbsent(original);
        assertInstanceOf(ProjectAlreadyExists.class, repository.insertIfAbsent(duplicate));
        ProjectSummaryUseCase useCase = new DefaultProjectSummaryUseCase(
                new InMemoryProjectReader(repository), new ProjectSummaryMapper());

        ProjectSummaryFound first = assertInstanceOf(ProjectSummaryFound.class, useCase.execute("P-1"));
        ProjectSummaryFound repeated = assertInstanceOf(ProjectSummaryFound.class, useCase.execute("P-1"));
        assertEquals(new ProjectSummaryResult("P-1", "contract", "schema", 0, 0, 0, 0, 0), first.summary());
        assertEquals(first, repeated);
        assertInstanceOf(ProjectSummaryNotFound.class, useCase.execute("missing"));
        assertSame(original, new InMemoryProjectReader(repository).findById("P-1").orElseThrow());
    }
}
