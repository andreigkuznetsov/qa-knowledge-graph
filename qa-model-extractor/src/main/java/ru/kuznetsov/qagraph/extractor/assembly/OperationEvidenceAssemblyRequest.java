package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.binding.RequestModelBindingEvidence;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;

import java.util.List;
import java.util.Objects;

public record OperationEvidenceAssemblyRequest(
        RestOperationEvidence operation,
        List<RequestModelBindingEvidence> requestModelBindings,
        List<BeanValidationEvidence> validationEvidence,
        IntegrationTestEvidence integrationTestEvidence
) {
    public OperationEvidenceAssemblyRequest {
        Objects.requireNonNull(operation, "operation");
        requestModelBindings = List.copyOf(Objects.requireNonNull(requestModelBindings, "requestModelBindings"));
        validationEvidence = List.copyOf(Objects.requireNonNull(validationEvidence, "validationEvidence"));
        Objects.requireNonNull(integrationTestEvidence, "integrationTestEvidence");
    }
}
