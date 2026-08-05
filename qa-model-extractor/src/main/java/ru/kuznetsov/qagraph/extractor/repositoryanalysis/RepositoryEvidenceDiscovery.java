package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectImplementationFlowExtractor;
import ru.kuznetsov.qagraph.extractor.implementationflow.DirectKafkaMessageProducerExtractor;
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
    private final IntegrationTestEvidenceExtractor testEvidenceExtractor;
    private final OperationEvidenceGraphAssembler assembler;

    public RepositoryEvidenceDiscovery() {
        this(new SpringMvcRestOperationScanner(),
                new ControllerRequestModelBindingExtractor(),
                new BeanValidationEvidenceExtractor(),
                new DirectImplementationFlowExtractor(),
                new DirectKafkaMessageProducerExtractor(),
                new IntegrationTestEvidenceExtractor(),
                new OperationEvidenceGraphAssembler());
    }

    RepositoryEvidenceDiscovery(
            SpringMvcRestOperationScanner operationScanner,
            ControllerRequestModelBindingExtractor requestBindingExtractor,
            BeanValidationEvidenceExtractor validationExtractor,
            DirectImplementationFlowExtractor implementationFlowExtractor,
            DirectKafkaMessageProducerExtractor messageProducerExtractor,
            IntegrationTestEvidenceExtractor testEvidenceExtractor,
            OperationEvidenceGraphAssembler assembler
    ) {
        this.operationScanner = Objects.requireNonNull(operationScanner, "operationScanner");
        this.requestBindingExtractor = Objects.requireNonNull(requestBindingExtractor, "requestBindingExtractor");
        this.validationExtractor = Objects.requireNonNull(validationExtractor, "validationExtractor");
        this.implementationFlowExtractor = Objects.requireNonNull(
                implementationFlowExtractor, "implementationFlowExtractor");
        this.messageProducerExtractor = Objects.requireNonNull(messageProducerExtractor, "messageProducerExtractor");
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
                new DirectKafkaMessageProducerExtractor(), testEvidenceExtractor, assembler);
    }

    public RepositoryEvidenceDiscoveryResult discover(RepositoryAnalysisRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        Path repositoryRoot = request.repositoryRoot();
        var operations = operationScanner.scan(repositoryRoot);
        if (operations.isEmpty()) return new RepositoryEvidenceDiscoveryResult(List.of(), List.of());

        var validationEvidence = validationExtractor.extract(repositoryRoot);
        var testEvidence = testEvidenceExtractor.extract(repositoryRoot);
        List<EvidenceGraphProjection> projections = new ArrayList<>();

        for (var operation : operations) {
            var bindings = requestBindingExtractor.extract(repositoryRoot, operation);
            var implementationFlows = implementationFlowExtractor.extract(repositoryRoot, operation)
                    .map(List::of)
                    .orElseGet(List::of);
            var messageProducers = messageProducerExtractor.extract(repositoryRoot, operation);
            projections.add(assembler.assemble(new OperationEvidenceAssemblyRequest(
                    operation, bindings, validationEvidence, testEvidence, implementationFlows, messageProducers)));
        }

        projections.sort(STABLE_ORDER);
        return new RepositoryEvidenceDiscoveryResult(projections, List.of());
    }
}
