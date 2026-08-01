package ru.kuznetsov.qaip.cli;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportCompletedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportFindingResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceFailedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportRejectedResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportTextRendererTest {
    private static final String NL = System.lineSeparator();
    private final ImportTextRenderer renderer = new ImportTextRenderer();

    @Test
    void completed_output_is_exact_and_has_no_trailing_newline() {
        String rendered = renderer.render(new ImportCompletedResult("P-1", 2, 1));

        assertEquals(String.join(NL,
                "Import completed",
                "Project ID: P-1",
                "Nodes: 2",
                "Relationships: 1"), rendered);
        assertFalse(rendered.endsWith(NL));
    }

    @Test
    void rejected_output_preserves_order_duplicates_and_optional_path() {
        ImportFindingResult first = new ImportFindingResult(
                "FIRST", "ERROR", "first message", null);
        ImportFindingResult duplicate = new ImportFindingResult(
                "DUPLICATE", "WARNING", "duplicate message", "/nodes/1");

        String rendered = renderer.render(new ImportRejectedResult(
                "APPLICATION_VALIDATION", List.of(first, duplicate, duplicate)));

        assertEquals(String.join(NL,
                "Import rejected",
                "Stage: APPLICATION_VALIDATION",
                "",
                "Findings:",
                "[ERROR] FIRST | first message",
                "[WARNING] DUPLICATE | duplicate message | path=/nodes/1",
                "[WARNING] DUPLICATE | duplicate message | path=/nodes/1"), rendered);
    }

    @Test
    void empty_rejection_renders_none() {
        assertEquals(String.join(NL,
                "Import rejected",
                "Stage: PARSING",
                "",
                "Findings:",
                "(none)"), renderer.render(new ImportRejectedResult("PARSING", List.of())));
    }

    @Test
    void persistence_rejection_output_is_exact() {
        assertEquals(String.join(NL,
                "Import rejected",
                "Code: PROJECT_ALREADY_EXISTS",
                "Message: Project already exists."),
                renderer.render(new ImportPersistenceRejectedResult(
                        "PROJECT_ALREADY_EXISTS", "Project already exists.")));
    }

    @Test
    void persistence_failure_output_is_exact() {
        assertEquals(String.join(NL,
                "Import failed",
                "Message: Project persistence failed."),
                renderer.render(new ImportPersistenceFailedResult("Project persistence failed.")));
    }

    @Test
    void exact_unicode_and_whitespace_are_preserved_without_record_to_string_leakage() {
        var result = new ImportRejectedResult(" STAGE ", List.of(
                new ImportFindingResult(" КОД ", " ERROR ", " сообщение 🌍 ", " /путь ")));

        String rendered = renderer.render(result);

        assertEquals(String.join(NL,
                "Import rejected",
                "Stage:  STAGE ",
                "",
                "Findings:",
                "[ ERROR ]  КОД  |  сообщение 🌍  | path= /путь "), rendered);
        assertFalse(rendered.contains("ImportRejectedResult["));
        assertFalse(rendered.contains("ImportFindingResult["));
    }

    @Test
    void repeated_rendering_is_deterministic_and_null_is_rejected() {
        var result = new ImportCompletedResult("P-1", 0, 0);
        assertEquals(renderer.render(result), renderer.render(result));
        assertThrows(NullPointerException.class, () -> renderer.render(null));
    }
}
