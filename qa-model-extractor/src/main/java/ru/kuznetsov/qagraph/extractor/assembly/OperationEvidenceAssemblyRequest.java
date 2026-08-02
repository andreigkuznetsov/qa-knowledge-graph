package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record OperationEvidenceAssemblyRequest(
        RestOperationEvidence operation,
        Set<String> boundRequestModelTypes,
        List<BeanValidationEvidence> validationEvidence,
        IntegrationTestEvidence integrationTestEvidence
) {
    public OperationEvidenceAssemblyRequest {
        Objects.requireNonNull(operation, "operation");
        boundRequestModelTypes = Collections.unmodifiableSet(new TreeSet<>(
                Objects.requireNonNull(boundRequestModelTypes, "boundRequestModelTypes")));
        validationEvidence = List.copyOf(Objects.requireNonNull(validationEvidence, "validationEvidence"));
        Objects.requireNonNull(integrationTestEvidence, "integrationTestEvidence");
    }
}
