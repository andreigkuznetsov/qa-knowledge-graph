package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ImplementationFlowEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageProducerEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageDestinationEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageConsumerEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ConsumerApplicationServiceEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ApplicationServiceRepositoryEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.binding.RequestModelBindingEvidence;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;

import java.util.List;
import java.util.Objects;

public record OperationEvidenceAssemblyRequest(
        RestOperationEvidence operation,
        List<RequestModelBindingEvidence> requestModelBindings,
        List<BeanValidationEvidence> validationEvidence,
        IntegrationTestEvidence integrationTestEvidence,
        List<ImplementationFlowEvidence> implementationFlows,
        List<MessageProducerEvidence> messageProducers,
        List<MessageDestinationEvidence> messageDestinations,
        List<MessageConsumerEvidence> messageConsumers,
        List<ConsumerApplicationServiceEvidence> consumerApplicationServices,
        List<ApplicationServiceRepositoryEvidence> applicationServiceRepositories
) {
    public OperationEvidenceAssemblyRequest {
        Objects.requireNonNull(operation, "operation");
        requestModelBindings = List.copyOf(Objects.requireNonNull(requestModelBindings, "requestModelBindings"));
        validationEvidence = List.copyOf(Objects.requireNonNull(validationEvidence, "validationEvidence"));
        Objects.requireNonNull(integrationTestEvidence, "integrationTestEvidence");
        implementationFlows = List.copyOf(Objects.requireNonNull(implementationFlows, "implementationFlows"));
        messageProducers = List.copyOf(Objects.requireNonNull(messageProducers, "messageProducers"));
        messageDestinations = List.copyOf(Objects.requireNonNull(messageDestinations, "messageDestinations"));
        messageConsumers = List.copyOf(Objects.requireNonNull(messageConsumers, "messageConsumers"));
        consumerApplicationServices = List.copyOf(Objects.requireNonNull(
                consumerApplicationServices, "consumerApplicationServices"));
        applicationServiceRepositories = List.copyOf(Objects.requireNonNull(
                applicationServiceRepositories, "applicationServiceRepositories"));
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation,
            List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence,
            IntegrationTestEvidence integrationTestEvidence) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation,
            List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence,
            IntegrationTestEvidence integrationTestEvidence,
            List<ImplementationFlowEvidence> implementationFlows) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence,
                implementationFlows, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation,
            List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence,
            IntegrationTestEvidence integrationTestEvidence,
            List<ImplementationFlowEvidence> implementationFlows,
            List<MessageProducerEvidence> messageProducers) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence,
                implementationFlows, messageProducers, List.of(), List.of(), List.of(), List.of());
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation,
            List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence,
            IntegrationTestEvidence integrationTestEvidence,
            List<ImplementationFlowEvidence> implementationFlows,
            List<MessageProducerEvidence> messageProducers,
            List<MessageDestinationEvidence> messageDestinations) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence,
                implementationFlows, messageProducers, messageDestinations, List.of(), List.of(), List.of());
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation, List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence, IntegrationTestEvidence integrationTestEvidence,
            List<ImplementationFlowEvidence> implementationFlows, List<MessageProducerEvidence> messageProducers,
            List<MessageDestinationEvidence> messageDestinations, List<MessageConsumerEvidence> messageConsumers) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence, implementationFlows,
                messageProducers, messageDestinations, messageConsumers, List.of(), List.of());
    }

    public OperationEvidenceAssemblyRequest(
            RestOperationEvidence operation, List<RequestModelBindingEvidence> requestModelBindings,
            List<BeanValidationEvidence> validationEvidence, IntegrationTestEvidence integrationTestEvidence,
            List<ImplementationFlowEvidence> implementationFlows, List<MessageProducerEvidence> messageProducers,
            List<MessageDestinationEvidence> messageDestinations, List<MessageConsumerEvidence> messageConsumers,
            List<ConsumerApplicationServiceEvidence> consumerApplicationServices) {
        this(operation, requestModelBindings, validationEvidence, integrationTestEvidence, implementationFlows,
                messageProducers, messageDestinations, messageConsumers, consumerApplicationServices, List.of());
    }
}
