package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectImplementationFlowExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectKafkaMessageProducerExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectKafkaMessageDestinationExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectKafkaMessageConsumerExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectConsumerApplicationServiceExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectApplicationServiceRepositoryExtractor;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidenceExtractor;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.extractor.rest.binding.ControllerRequestModelBindingExtractor;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidenceExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class RepositoryEvidenceDiscovery {
    private static final Comparator<EvidenceGraphProjection> STABLE_ORDER = Comparator
            .comparing((EvidenceGraphProjection graph) -> graph.businessOperation().name())
            .thenComparing(graph -> graph.businessOperation().id());

    private final SpringMvcRestOperationScanner operationScanner;
    private final ControllerRequestModelBindingExtractor requestBindingExtractor;
    private final BeanValidationEvidenceExtractor validationExtractor;
    private final DirectImplementationFlowExtractor implementationFlowExtractor;
    private final DirectKafkaMessageProducerExtractor messageProducerExtractor;
    private final DirectKafkaMessageDestinationExtractor messageDestinationExtractor;
    private final DirectKafkaMessageConsumerExtractor messageConsumerExtractor;
    private final DirectConsumerApplicationServiceExtractor consumerApplicationServiceExtractor;
    private final DirectApplicationServiceRepositoryExtractor applicationServiceRepositoryExtractor;
    private final IntegrationTestEvidenceExtractor testEvidenceExtractor;
    private final OperationEvidenceGraphAssembler assembler;

    public RepositoryEvidenceDiscovery() {
        this(new SpringMvcRestOperationScanner(),
                new ControllerRequestModelBindingExtractor(),
                new BeanValidationEvidenceExtractor(),
                new DirectImplementationFlowExtractor(),
                new DirectKafkaMessageProducerExtractor(),
                new DirectKafkaMessageDestinationExtractor(),
                new DirectKafkaMessageConsumerExtractor(),
                new DirectConsumerApplicationServiceExtractor(),
                new DirectApplicationServiceRepositoryExtractor(),
                new IntegrationTestEvidenceExtractor(),
                new OperationEvidenceGraphAssembler());
    }

    RepositoryEvidenceDiscovery(
            SpringMvcRestOperationScanner operationScanner,
            ControllerRequestModelBindingExtractor requestBindingExtractor,
            BeanValidationEvidenceExtractor validationExtractor,
            DirectImplementationFlowExtractor implementationFlowExtractor,
            DirectKafkaMessageProducerExtractor messageProducerExtractor,
            DirectKafkaMessageDestinationExtractor messageDestinationExtractor,
            DirectKafkaMessageConsumerExtractor messageConsumerExtractor,
            DirectConsumerApplicationServiceExtractor consumerApplicationServiceExtractor,
            DirectApplicationServiceRepositoryExtractor applicationServiceRepositoryExtractor,
            IntegrationTestEvidenceExtractor testEvidenceExtractor,
            OperationEvidenceGraphAssembler assembler
    ) {
        this.operationScanner = Objects.requireNonNull(operationScanner, "operationScanner");
        this.requestBindingExtractor = Objects.requireNonNull(requestBindingExtractor, "requestBindingExtractor");
        this.validationExtractor = Objects.requireNonNull(validationExtractor, "validationExtractor");
        this.implementationFlowExtractor = Objects.requireNonNull(
                implementationFlowExtractor, "implementationFlowExtractor");
        this.messageProducerExtractor = Objects.requireNonNull(messageProducerExtractor, "messageProducerExtractor");
        this.messageDestinationExtractor = Objects.requireNonNull(
                messageDestinationExtractor, "messageDestinationExtractor");
        this.messageConsumerExtractor = Objects.requireNonNull(messageConsumerExtractor, "messageConsumerExtractor");
        this.consumerApplicationServiceExtractor = Objects.requireNonNull(
                consumerApplicationServiceExtractor, "consumerApplicationServiceExtractor");
        this.applicationServiceRepositoryExtractor = Objects.requireNonNull(
                applicationServiceRepositoryExtractor, "applicationServiceRepositoryExtractor");
        this.testEvidenceExtractor = Objects.requireNonNull(testEvidenceExtractor, "testEvidenceExtractor");
        this.assembler = Objects.requireNonNull(assembler, "assembler");
    }

    RepositoryEvidenceDiscovery(
            SpringMvcRestOperationScanner operationScanner,
            ControllerRequestModelBindingExtractor requestBindingExtractor,
            BeanValidationEvidenceExtractor validationExtractor,
            DirectImplementationFlowExtractor implementationFlowExtractor,
            IntegrationTestEvidenceExtractor testEvidenceExtractor,
            OperationEvidenceGraphAssembler assembler
    ) {
        this(operationScanner, requestBindingExtractor, validationExtractor, implementationFlowExtractor,
                new DirectKafkaMessageProducerExtractor(), new DirectKafkaMessageDestinationExtractor(),
                new DirectKafkaMessageConsumerExtractor(), new DirectConsumerApplicationServiceExtractor(),
                new DirectApplicationServiceRepositoryExtractor(), testEvidenceExtractor, assembler);
    }

    public RepositoryEvidenceDiscoveryResult discover(RepositoryAnalysisRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        Path repositoryRoot = request.repositoryRoot();
        var operations = operationScanner.scan(repositoryRoot);
        if (operations.isEmpty()) return new RepositoryEvidenceDiscoveryResult(List.of(), List.of());

        var validationEvidence = validationExtractor.extract(repositoryRoot);
        var testEvidence = testEvidenceExtractor.extract(repositoryRoot);
        var messageConsumers = messageConsumerExtractor.extract(repositoryRoot);
        var consumerApplicationServices = consumerApplicationServiceExtractor.extract(
                repositoryRoot, messageConsumers);
        var applicationServiceRepositories = applicationServiceRepositoryExtractor.extract(
                repositoryRoot, consumerApplicationServices);
        List<EvidenceGraphProjection> projections = new ArrayList<>();

        for (var operation : operations) {
            var bindings = requestBindingExtractor.extract(repositoryRoot, operation);
            var implementationFlows = implementationFlowExtractor.extract(repositoryRoot, operation)
                    .map(List::of)
                    .orElseGet(List::of);
            var messageProducers = messageProducerExtractor.extract(repositoryRoot, operation);
            var messageDestinations = new ArrayList<ru.kuznetsov.qagraph.extractor.implementationflow.MessageDestinationEvidence>();
            for (var producer : messageProducers) {
                messageDestinationExtractor.extract(repositoryRoot, producer).ifPresent(messageDestinations::add);
            }
            projections.add(assembler.assemble(new OperationEvidenceAssemblyRequest(
                    operation, bindings, validationEvidence, testEvidence, implementationFlows,
                    messageProducers, messageDestinations, messageConsumers, consumerApplicationServices,
                    applicationServiceRepositories)));
        }

        projections.sort(STABLE_ORDER);
        return new RepositoryEvidenceDiscoveryResult(projections, List.of());
    }
}
