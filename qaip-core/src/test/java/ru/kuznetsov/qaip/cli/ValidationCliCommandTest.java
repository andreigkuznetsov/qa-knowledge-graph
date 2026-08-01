package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.query.validation.*;
import ru.kuznetsov.qaip.core.persistence.ProjectPersistenceException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValidationCliCommandTest {
    @Test
    void invokes_once_and_returns_success_for_valid_warning_and_invalid_reports() {
        for (ValidationReportResult report : List.of(
                new ValidationReportResult(true, 0, 0, List.of()),
                new ValidationReportResult(true, 0, 1, List.of()),
                new ValidationReportResult(false, 1, 0, List.of()))) {
            AtomicInteger calls = new AtomicInteger();
            ValidationUseCase useCase = id -> {
                calls.incrementAndGet();
                assertEquals(" P ", id);
                return new ValidationCompleted(id, report);
            };
            QaipCliApplicationTest.Streams streams = new QaipCliApplicationTest.Streams();
            assertEquals(0, new ValidationCliCommand(useCase, new ValidationTextRenderer())
                    .execute(" P ", streams.out, streams.err));
            assertEquals(1, calls.get());
            assertEquals("", streams.stderr());
        }
    }

    @Test
    void maps_project_absence_and_failures_to_existing_safe_policy() {
        QaipCliApplicationTest.Streams missing = new QaipCliApplicationTest.Streams();
        assertEquals(3, command(id -> new ValidationProjectNotFound(id)).execute("P", missing.out, missing.err));
        assertEquals("Project not found: P" + System.lineSeparator(), missing.stdout());

        QaipCliApplicationTest.Streams persistence = new QaipCliApplicationTest.Streams();
        assertEquals(4, command(id -> { throw new ProjectPersistenceException("database unavailable"); })
                .execute("P", persistence.out, persistence.err));
        assertEquals("Validation failed: database unavailable" + System.lineSeparator(), persistence.stderr());

        QaipCliApplicationTest.Streams unexpected = new QaipCliApplicationTest.Streams();
        assertEquals(4, command(id -> { throw new IllegalStateException("secret"); })
                .execute("P", unexpected.out, unexpected.err));
        assertEquals("Validation failed." + System.lineSeparator(), unexpected.stderr());
        assertFalse(unexpected.stderr().contains("Exception"));
        assertFalse(unexpected.stderr().contains("secret"));
    }

    private static ValidationCliCommand command(ValidationUseCase useCase) {
        return new ValidationCliCommand(useCase, new ValidationTextRenderer());
    }
}
