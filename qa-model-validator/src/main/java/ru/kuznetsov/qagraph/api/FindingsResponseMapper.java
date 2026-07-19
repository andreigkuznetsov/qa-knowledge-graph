package ru.kuznetsov.qagraph.api;

import org.springframework.stereotype.Component;
import ru.kuznetsov.qaip.findings.model.Finding;
import ru.kuznetsov.qaip.findings.model.FindingsReport;
import ru.kuznetsov.qaip.findings.model.FindingsSummary;

@Component
public class FindingsResponseMapper {

    public RegisteredModelFindingsResponse map(
            String modelId,
            FindingsReport report
    ) {
        return new RegisteredModelFindingsResponse(
                modelId,
                report.analyzed(),
                report.schemaVersion(),
                map(report.summary()),
                report.findings().stream()
                        .map(this::map)
                        .toList(),
                report.validation()
        );
    }

    private FindingsSummaryResponse map(FindingsSummary summary) {
        return new FindingsSummaryResponse(
                summary.total(),
                summary.high(),
                summary.medium(),
                summary.low()
        );
    }

    private FindingResponse map(Finding finding) {
        return new FindingResponse(
                finding.code().name(),
                finding.severity().name(),
                finding.nodeId(),
                finding.nodeType(),
                finding.message(),
                finding.recommendation()
        );
    }
}
