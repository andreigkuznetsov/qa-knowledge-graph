package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.importproject.result.ImportCompletedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportFindingResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceFailedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportPersistenceRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportRejectedResult;
import ru.kuznetsov.qaip.core.application.importproject.result.ImportResult;

import java.util.Objects;

public final class ImportTextRenderer {
    public String render(ImportResult result) {
        Objects.requireNonNull(result, "result");
        if (result instanceof ImportCompletedResult completed) {
            return String.join(System.lineSeparator(),
                    "Import completed",
                    "Project ID: " + completed.projectId(),
                    "Nodes: " + completed.nodeCount(),
                    "Relationships: " + completed.relationshipCount());
        }
        if (result instanceof ImportRejectedResult rejected) {
            return String.join(System.lineSeparator(),
                    "Import rejected",
                    "Stage: " + rejected.stage(),
                    "",
                    "Findings:",
                    renderFindings(rejected));
        }
        if (result instanceof ImportPersistenceRejectedResult rejected) {
            return String.join(System.lineSeparator(),
                    "Import rejected",
                    "Code: " + rejected.code(),
                    "Message: " + rejected.message());
        }
        ImportPersistenceFailedResult failed = (ImportPersistenceFailedResult) result;
        return String.join(System.lineSeparator(),
                "Import failed",
                "Message: " + failed.message());
    }

    private static String renderFindings(ImportRejectedResult rejected) {
        if (rejected.findings().isEmpty()) return "(none)";
        return String.join(System.lineSeparator(), rejected.findings().stream()
                .map(ImportTextRenderer::renderFinding)
                .toList());
    }

    private static String renderFinding(ImportFindingResult finding) {
        String line = "[" + finding.severity() + "] " + finding.code() + " | " + finding.message();
        if (finding.path() != null) line += " | path=" + finding.path();
        return line;
    }
}
