package ru.kuznetsov.qaip.core.application.importproject.result;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportResultContractsTest {
    @Test
    void completed_result_accepts_valid_and_zero_counts_and_preserves_exact_project_id() {
        assertEquals(new ImportCompletedResult(" P-1 ", 2, 3),
                new ImportCompletedResult(" P-1 ", 2, 3));
        assertEquals(0, new ImportCompletedResult("P-1", 0, 0).nodeCount());
        assertEquals(0, new ImportCompletedResult("P-1", 0, 0).relationshipCount());
        assertEquals(" P-1 ", new ImportCompletedResult(" P-1 ", 0, 0).projectId());
        assertThrows(IllegalArgumentException.class, () -> new ImportCompletedResult("P-1", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ImportCompletedResult("P-1", 0, -1));
    }

    @Test
    void rejection_allows_empty_findings_and_preserves_order_duplicates_and_source_independence() {
        ImportFindingResult first = finding("FIRST", "/first");
        ImportFindingResult duplicate = finding("DUPLICATE", null);
        List<ImportFindingResult> source = new ArrayList<>(List.of(first, duplicate, duplicate));
        ImportRejectedResult result = new ImportRejectedResult(" PARSING ", source);
        source.clear();

        assertEquals(" PARSING ", result.stage());
        assertEquals(List.of(first, duplicate, duplicate), result.findings());
        assertThrows(UnsupportedOperationException.class, () -> result.findings().clear());
        assertEquals(List.of(), new ImportRejectedResult("PARSING", List.of()).findings());
        assertThrows(NullPointerException.class,
                () -> new ImportRejectedResult("PARSING", java.util.Arrays.asList(first, null)));
    }

    @Test
    void finding_preserves_exact_text_and_accepts_null_optional_path() {
        ImportFindingResult finding = new ImportFindingResult(" CODE ", " ERROR ", " message ", " /path ");
        assertEquals(" CODE ", finding.code());
        assertEquals(" ERROR ", finding.severity());
        assertEquals(" message ", finding.message());
        assertEquals(" /path ", finding.path());
        assertNull(new ImportFindingResult("CODE", "ERROR", "message", null).path());
    }

    @Test
    void required_text_fields_reject_null_and_blank() {
        assertThrows(NullPointerException.class, () -> new ImportCompletedResult(null, 0, 0));
        assertThrows(NullPointerException.class, () -> new ImportRejectedResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ImportRejectedResult("PARSING", null));
        assertThrows(NullPointerException.class, () -> new ImportPersistenceRejectedResult(null, "message"));
        assertThrows(NullPointerException.class, () -> new ImportPersistenceRejectedResult("CODE", null));
        assertThrows(NullPointerException.class, () -> new ImportPersistenceFailedResult(null));
        assertThrows(NullPointerException.class, () -> new ImportFindingResult(null, "ERROR", "message", null));
        assertThrows(NullPointerException.class, () -> new ImportFindingResult("CODE", null, "message", null));
        assertThrows(NullPointerException.class, () -> new ImportFindingResult("CODE", "ERROR", null, null));

        for (String blank : List.of("", " ", "\t")) {
            assertThrows(IllegalArgumentException.class, () -> new ImportCompletedResult(blank, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> new ImportRejectedResult(blank, List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> new ImportPersistenceRejectedResult(blank, "message"));
            assertThrows(IllegalArgumentException.class,
                    () -> new ImportPersistenceRejectedResult("CODE", blank));
            assertThrows(IllegalArgumentException.class, () -> new ImportPersistenceFailedResult(blank));
            assertThrows(IllegalArgumentException.class,
                    () -> new ImportFindingResult(blank, "ERROR", "message", null));
            assertThrows(IllegalArgumentException.class,
                    () -> new ImportFindingResult("CODE", blank, "message", null));
            assertThrows(IllegalArgumentException.class,
                    () -> new ImportFindingResult("CODE", "ERROR", blank, null));
        }
    }

    @Test
    void persistence_results_preserve_exact_values_and_records_supply_value_semantics() {
        ImportPersistenceRejectedResult rejected = new ImportPersistenceRejectedResult(" CODE ", " message ");
        assertEquals(" CODE ", rejected.code());
        assertEquals(" message ", rejected.message());
        assertEquals(rejected, new ImportPersistenceRejectedResult(" CODE ", " message "));
        assertEquals(rejected.hashCode(), new ImportPersistenceRejectedResult(" CODE ", " message ").hashCode());
        assertNotEquals(rejected, new ImportPersistenceRejectedResult("OTHER", " message "));

        ImportPersistenceFailedResult failed = new ImportPersistenceFailedResult(" failure ");
        assertEquals(" failure ", failed.message());
        assertEquals(failed, new ImportPersistenceFailedResult(" failure "));
        assertEquals(failed.hashCode(), new ImportPersistenceFailedResult(" failure ").hashCode());
    }

    private static ImportFindingResult finding(String code, String path) {
        return new ImportFindingResult(code, "ERROR", "message", path);
    }
}
