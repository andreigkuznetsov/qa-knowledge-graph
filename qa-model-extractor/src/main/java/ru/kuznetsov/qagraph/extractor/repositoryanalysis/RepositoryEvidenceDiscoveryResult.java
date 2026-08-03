package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;

import java.util.List;
import java.util.Objects;

public record RepositoryEvidenceDiscoveryResult(
        List<EvidenceGraphProjection> operationEvidence,
        List<String> warnings
) {
    public RepositoryEvidenceDiscoveryResult {
        operationEvidence = List.copyOf(Objects.requireNonNull(operationEvidence, "operationEvidence"));
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}
