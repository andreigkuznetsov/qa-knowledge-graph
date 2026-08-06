package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionCategory;
import ru.kuznetsov.qagraph.extractor.integrationtest.AssertionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.HttpInteractionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestStyle;
import ru.kuznetsov.qagraph.extractor.integrationtest.TestImplementationEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ImplementationFlowEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageProducerEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageDestinationEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.MessageConsumerEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ConsumerApplicationServiceEvidence;
import ru.kuznetsov.qagraph.extractor.implementationflow.ApplicationServiceRepositoryEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.extractor.rest.mapping.RestOperationEvidenceMapper;
import ru.kuznetsov.qagraph.extractor.validation.BeanValidationEvidence;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.ImplementationRole;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class OperationEvidenceGraphAssembler {
    private final RestOperationEvidenceMapper operationMapper = new RestOperationEvidenceMapper();

    public EvidenceGraphProjection assemble(OperationEvidenceAssemblyRequest request) {
        Objects.requireNonNull(request, "request");
        BusinessOperationProjection operation = operationMapper.map(request.operation());
        EvidenceGraphProjection.TechnicalImplementationProjection implementation =
                technicalImplementation(request.operation(), operation);
        List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations =
                implementationFlow(request, implementation);
        implementations = withMessageProducers(request, implementations);
        implementations = withMessageDestinations(request, implementations);
        implementations = withMessageConsumers(request, implementations);
        implementations = withConsumerApplicationServices(request, implementations);
        implementations = withApplicationServiceRepositories(request, implementations);

        Map<String, EvidenceGraphProjection.BusinessRuleProjection> rules = new TreeMap<>();
        Set<String> boundRequestModelTypes = request.requestModelBindings().stream()
                .filter(binding -> binding.controllerClass().equals(qualifiedController(request.operation())))
                .filter(binding -> binding.controllerMethod().equals(request.operation().controllerMethod()))
                .map(binding -> binding.resolvedModelType())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (BeanValidationEvidence evidence : request.validationEvidence()) {
            if (!boundRequestModelTypes.contains(evidence.owningJavaType())) continue;
            var projection = businessRule(evidence);
            rules.putIfAbsent(projection.id(), projection);
        }

        Map<String, EvidenceGraphProjection.TestImplementationProjection> tests = new TreeMap<>();
        Map<String, EvidenceGraphProjection.CheckProjection> checks = new TreeMap<>();
        List<EvidenceGraphProjection.RelationshipProjection> relationships = new ArrayList<>();
        relationships.add(relationship(
                operation.id(), RelationshipType.IMPLEMENTED_BY, implementation.id()));
        for (MessageProducerEvidence producer : request.messageProducers()) {
            if (!producer.operation().equals(request.operation())
                    || !producer.ownerClass().equals(qualifiedController(request.operation()))
                    || !producer.ownerMethod().equals(request.operation().controllerMethod())) continue;
            var producerNode = messageProducerImplementation(producer);
            relationships.add(controllerUsesProducerRelationship(
                    producer, implementation.id(), producerNode.id()));
        }
        if (hasSynchronousFlow(implementations)) {
            relationships.add(relationship(
                    implementations.get(0).id(), RelationshipType.USES, implementations.get(1).id()));
            relationships.add(relationship(
                    implementations.get(1).id(), RelationshipType.USES, implementations.get(2).id()));
        }
        for (MessageDestinationEvidence destination : request.messageDestinations()) {
            var producerNode = messageProducerImplementation(destination.producer());
            var destinationNode = messageDestinationImplementation(destination);
            relationships.add(publishingRelationship(destination, producerNode.id(), destinationNode.id()));
        }
        for (MessageConsumerEvidence consumer : request.messageConsumers()) {
            var consumerNode = messageConsumerImplementation(consumer);
            var destinationNode = messageDestinationImplementation(consumer);
            relationships.add(consumingRelationship(consumer, consumerNode.id(), destinationNode.id()));
        }
        for (ConsumerApplicationServiceEvidence service : request.consumerApplicationServices()) {
            var consumerNode = messageConsumerImplementation(service.consumer());
            var serviceNode = applicationServiceImplementation(
                    service.serviceClass(), service.serviceMethod(), service.serviceMethodLocation());
            relationships.add(consumerUsesServiceRelationship(service, consumerNode.id(), serviceNode.id()));
        }
        for (ApplicationServiceRepositoryEvidence repository : request.applicationServiceRepositories()) {
            var serviceNode = applicationServiceImplementation(
                    repository.applicationService().serviceClass(),
                    repository.applicationService().serviceMethod(),
                    repository.applicationService().serviceMethodLocation());
            var repositoryNode = repositoryImplementation(
                    repository.repositoryClass(), repository.repositoryDeclarationLocation());
            relationships.add(serviceUsesRepositoryRelationship(
                    repository, serviceNode.id(), repositoryNode.id()));
        }
        for (var rule : rules.values()) {
            relationships.add(relationship(operation.id(), RelationshipType.GOVERNED_BY, rule.id()));
        }

        addTestsAndChecks(request, implementation, tests, checks, relationships);
        relationships.sort(Comparator.comparing(EvidenceGraphProjection.RelationshipProjection::id));

        EvidenceGraphProjection graph = new EvidenceGraphProjection(
                operation,
                List.copyOf(rules.values()),
                implementations,
                List.copyOf(tests.values()),
                List.copyOf(checks.values()),
                relationships);
        verifyGraph(graph);
        return graph;
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> implementationFlow(
            OperationEvidenceAssemblyRequest request,
            EvidenceGraphProjection.TechnicalImplementationProjection controller) {
        List<ImplementationFlowEvidence> matching = request.implementationFlows().stream()
                .filter(flow -> flow.operation().equals(request.operation()))
                .filter(flow -> flow.controllerClass().equals(qualifiedController(request.operation())))
                .filter(flow -> flow.controllerMethod().equals(request.operation().controllerMethod()))
                .toList();
        if (matching.size() != 1) return List.of(controller);
        ImplementationFlowEvidence flow = matching.getFirst();
        return List.of(controller, serviceImplementation(flow), repositoryImplementation(flow));
    }

    private static boolean hasSynchronousFlow(
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        return implementations.size() >= 3
                && "SERVICE".equals(implementations.get(1).technicalImplementation().details().get("flowStage"))
                && "REPOSITORY".equals(implementations.get(2).technicalImplementation().details().get("flowStage"));
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> withMessageProducers(
            OperationEvidenceAssemblyRequest request,
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> producers = new TreeMap<>();
        request.messageProducers().stream()
                .filter(evidence -> evidence.operation().equals(request.operation()))
                .filter(evidence -> evidence.ownerClass().equals(qualifiedController(request.operation())))
                .filter(evidence -> evidence.ownerMethod().equals(request.operation().controllerMethod()))
                .map(OperationEvidenceGraphAssembler::messageProducerImplementation)
                .forEach(producer -> producers.putIfAbsent(producer.id(), producer));
        if (producers.isEmpty()) return implementations;
        List<EvidenceGraphProjection.TechnicalImplementationProjection> result = new ArrayList<>(implementations);
        result.addAll(producers.values());
        return List.copyOf(result);
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> withMessageDestinations(
            OperationEvidenceAssemblyRequest request,
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> destinations = new TreeMap<>();
        request.messageDestinations().stream()
                .filter(evidence -> evidence.producer().operation().equals(request.operation()))
                .map(OperationEvidenceGraphAssembler::messageDestinationImplementation)
                .forEach(destination -> destinations.putIfAbsent(destination.id(), destination));
        if (destinations.isEmpty()) return implementations;
        List<EvidenceGraphProjection.TechnicalImplementationProjection> result = new ArrayList<>(implementations);
        result.addAll(destinations.values());
        return List.copyOf(result);
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> withMessageConsumers(
            OperationEvidenceAssemblyRequest request,
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> additions = new TreeMap<>();
        request.messageConsumers().forEach(consumer -> {
            var destination = messageDestinationImplementation(consumer);
            additions.putIfAbsent(destination.id(), destination);
            var consumerNode = messageConsumerImplementation(consumer);
            additions.putIfAbsent(consumerNode.id(), consumerNode);
        });
        if (additions.isEmpty()) return implementations;
        List<EvidenceGraphProjection.TechnicalImplementationProjection> result = new ArrayList<>(implementations);
        Set<String> existing = implementations.stream()
                .map(EvidenceGraphProjection.TechnicalImplementationProjection::id)
                .collect(java.util.stream.Collectors.toSet());
        additions.values().stream().filter(value -> !existing.contains(value.id())).forEach(result::add);
        return List.copyOf(result);
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> withConsumerApplicationServices(
            OperationEvidenceAssemblyRequest request,
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> additions = new TreeMap<>();
        request.consumerApplicationServices().stream()
                .map(value -> applicationServiceImplementation(
                        value.serviceClass(), value.serviceMethod(), value.serviceMethodLocation()))
                .forEach(value -> additions.putIfAbsent(value.id(), value));
        List<EvidenceGraphProjection.TechnicalImplementationProjection> result = new ArrayList<>(implementations);
        Set<String> existing = implementations.stream()
                .map(EvidenceGraphProjection.TechnicalImplementationProjection::id)
                .collect(java.util.stream.Collectors.toSet());
        additions.values().stream().filter(value -> !existing.contains(value.id())).forEach(result::add);
        return List.copyOf(result);
    }

    private static List<EvidenceGraphProjection.TechnicalImplementationProjection> withApplicationServiceRepositories(
            OperationEvidenceAssemblyRequest request,
            List<EvidenceGraphProjection.TechnicalImplementationProjection> implementations) {
        Map<String, EvidenceGraphProjection.TechnicalImplementationProjection> additions = new TreeMap<>();
        request.applicationServiceRepositories().stream()
                .map(value -> repositoryImplementation(
                        value.repositoryClass(), value.repositoryDeclarationLocation()))
                .forEach(value -> additions.putIfAbsent(value.id(), value));
        List<EvidenceGraphProjection.TechnicalImplementationProjection> result = new ArrayList<>(implementations);
        Set<String> existing = implementations.stream()
                .map(EvidenceGraphProjection.TechnicalImplementationProjection::id)
                .collect(java.util.stream.Collectors.toSet());
        additions.values().stream().filter(value -> !existing.contains(value.id())).forEach(result::add);
        return List.copyOf(result);
    }

    private static void addTestsAndChecks(
            OperationEvidenceAssemblyRequest request,
            EvidenceGraphProjection.TechnicalImplementationProjection implementation,
            Map<String, EvidenceGraphProjection.TestImplementationProjection> tests,
            Map<String, EvidenceGraphProjection.CheckProjection> checks,
            List<EvidenceGraphProjection.RelationshipProjection> relationships) {
        IntegrationTestEvidence testEvidence = request.integrationTestEvidence();
        for (TestImplementationEvidence test : testEvidence.tests()) {
            List<HttpInteractionEvidence> interactions = testEvidence.httpInteractions().stream()
                    .filter(interaction -> sameTest(interaction, test))
                    .toList();
            if (interactions.size() != 1 || !matchesOperation(interactions.getFirst(), request.operation())) {
                continue;
            }

            var testProjection = testImplementation(test);
            if (tests.putIfAbsent(testProjection.id(), testProjection) != null) continue;
            relationships.add(relationship(
                    testProjection.id(), RelationshipType.USES, implementation.id()));

            for (AssertionEvidence assertion : testEvidence.assertions()) {
                if (!sameTest(assertion, test)) continue;
                var check = check(assertion, testProjection.id());
                if (checks.putIfAbsent(check.id(), check) == null) {
                    relationships.add(relationship(
                            testProjection.id(), RelationshipType.HAS_CHECK, check.id()));
                }
            }
        }
    }

    private static EvidenceGraphProjection.BusinessRuleProjection businessRule(
            BeanValidationEvidence evidence) {
        String canonical = "VALIDATION|" + evidence.owningJavaType() + '|' + evidence.memberName()
                + '|' + evidence.annotationType() + '|' + evidence.declaredAttributes()
                + '|' + sourceLocation(evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String identity = sha256(canonical);
        String annotation = simpleName(evidence.annotationType());
        String target = evidence.owningJavaType() + '.' + evidence.memberName();
        String rendered = renderAnnotation(evidence);
        return new EvidenceGraphProjection.BusinessRuleProjection(
                "BR-VALIDATION-" + identity,
                NodeType.BUSINESS_RULE,
                "@" + annotation + " on " + target,
                "Declared Bean Validation constraint " + rendered + " on " + target + ".",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.owningJavaType()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        sourceLocation(evidence.repositoryRelativePath(), evidence.line(), evidence.column())
                                + "#" + target,
                        "Bean Validation annotation " + rendered)),
                new EvidenceGraphProjection.RuleProjection(
                        "VALIDATION-" + identity,
                        EvidenceGraphProjection.RuleType.VALIDATION_RULE,
                        rendered + " is declared on " + target + ".",
                        null));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection technicalImplementation(
            RestOperationEvidence evidence, BusinessOperationProjection operation) {
        String controller = qualifiedController(evidence);
        SourceLocation location = evidence.sourceLocation();
        String locationValue = location == null
                ? controller + '.' + evidence.controllerMethod()
                : sourceLocation(location.repositoryRelativePath(), location.line(), location.column())
                + "#" + controller + '.' + evidence.controllerMethod();
        String identity = sha256("CONTROLLER|" + locationValue);
        var operationSource = operation.sourceReferences().getFirst();
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-REST-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                controller + '.' + evidence.controllerMethod(),
                "Spring MVC controller method implementing " + operation.name() + ".",
                List.of(sourceReference(
                        operationSource.sourceId(),
                        EvidenceGraphProjection.LocationType.OTHER,
                        locationValue,
                        "Controller method " + controller + '.' + evidence.controllerMethod())),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.API,
                        ImplementationRole.REST_CONTROLLER,
                        evidence.javaPackage().isBlank() ? evidence.controllerClass() : evidence.javaPackage(),
                        Map.of(
                                "controllerClass", controller,
                                "controllerMethod", evidence.controllerMethod(),
                                "endpoint", operation.name())));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection serviceImplementation(
            ImplementationFlowEvidence flow) {
        return applicationServiceImplementation(flow.serviceClass(), flow.serviceMethod(), flow.serviceMethodLocation());
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection applicationServiceImplementation(
            String serviceClass, String serviceMethod, SourceLocation declarationLocation) {
        String location = sourceLocation(declarationLocation.repositoryRelativePath(),
                declarationLocation.line(), declarationLocation.column());
        String identity = sha256("SERVICE|" + serviceClass + '#' + serviceMethod + '|' + location);
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-SERVICE-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                serviceClass + '.' + serviceMethod,
                "Directly invoked service method in the implementation flow.",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(serviceClass),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + serviceClass + '.' + serviceMethod,
                        "Service method " + serviceClass + '.' + serviceMethod)),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.OTHER,
                        ImplementationRole.APPLICATION_SERVICE,
                        packageName(serviceClass),
                        Map.of(
                                "flowStage", "SERVICE",
                                "serviceClass", serviceClass,
                                "serviceMethod", serviceMethod)));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection repositoryImplementation(
            ImplementationFlowEvidence flow) {
        return repositoryImplementation(flow.repositoryClass(), flow.repositoryDeclarationLocation());
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection repositoryImplementation(
            String repositoryClass, SourceLocation declarationLocation) {
        String location = sourceLocation(declarationLocation.repositoryRelativePath(),
                declarationLocation.line(), declarationLocation.column());
        String identity = sha256("REPOSITORY|" + repositoryClass + '|' + location);
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-REPOSITORY-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                repositoryClass,
                "Directly invoked repository in the implementation flow.",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(repositoryClass),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + repositoryClass,
                        "Repository " + repositoryClass)),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.DATABASE,
                        ImplementationRole.REPOSITORY,
                        packageName(repositoryClass),
                        Map.of("flowStage", "REPOSITORY", "repositoryClass", repositoryClass)));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection messageProducerImplementation(
            MessageProducerEvidence evidence) {
        SourceLocation declaration = evidence.operation().sourceLocation();
        String declarationLocation = declaration == null
                ? evidence.ownerClass() + '.' + evidence.ownerMethod()
                : sourceLocation(declaration.repositoryRelativePath(), declaration.line(), declaration.column());
        String invocationLocation = sourceLocation(evidence.publishingLocation().repositoryRelativePath(),
                evidence.publishingLocation().line(), evidence.publishingLocation().column());
        String owner = evidence.ownerClass() + '.' + evidence.ownerMethod();
        String identity = sha256("MESSAGE_PRODUCER|" + owner + '|' + invocationLocation);
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-MESSAGE-PRODUCER-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                owner,
                "Kafka producer responsibility declared by " + owner + ".",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.ownerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        declarationLocation + "#" + owner,
                        "Producer-owning controller method " + owner)),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.MESSAGE,
                        ImplementationRole.MESSAGE_PRODUCER,
                        packageName(evidence.ownerClass()),
                        Map.of(
                                "technology", "Kafka",
                                "ownerClass", evidence.ownerClass(),
                                "ownerMethod", evidence.ownerMethod())));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection messageDestinationImplementation(
            MessageDestinationEvidence evidence) {
        String identity = sha256("MESSAGE_DESTINATION|" + evidence.technology() + '|'
                + evidence.destinationKind() + '|' + evidence.destinationName());
        String location = sourceLocation(evidence.declarationLocation().repositoryRelativePath(),
                evidence.declarationLocation().line(), evidence.declarationLocation().column());
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-MESSAGE-DESTINATION-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                evidence.destinationName(),
                "Kafka topic " + evidence.destinationName() + ".",
                List.of(sourceReference(
                        "SRC-CONFIG-" + sha256(evidence.declarationLocation().repositoryRelativePath()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location,
                        "Kafka topic declaration " + evidence.destinationName())),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.MESSAGE,
                        ImplementationRole.MESSAGE_DESTINATION,
                        "Kafka",
                        Map.of(
                                "technology", evidence.technology(),
                                "destinationName", evidence.destinationName(),
                                "destinationKind", evidence.destinationKind())));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection messageDestinationImplementation(
            MessageConsumerEvidence evidence) {
        return messageDestinationImplementation(
                evidence.technology(), evidence.destinationName(), evidence.destinationKind(),
                evidence.destinationDeclarationLocation());
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection messageDestinationImplementation(
            String technology, String destinationName, String destinationKind, SourceLocation declarationLocation) {
        String identity = sha256("MESSAGE_DESTINATION|" + technology + '|' + destinationKind + '|' + destinationName);
        String location = sourceLocation(declarationLocation.repositoryRelativePath(),
                declarationLocation.line(), declarationLocation.column());
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-MESSAGE-DESTINATION-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                destinationName,
                "Kafka topic " + destinationName + ".",
                List.of(sourceReference(
                        "SRC-CONFIG-" + sha256(declarationLocation.repositoryRelativePath()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location,
                        "Kafka topic declaration " + destinationName)),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.MESSAGE,
                        ImplementationRole.MESSAGE_DESTINATION,
                        "Kafka",
                        Map.of("technology", technology, "destinationName", destinationName,
                                "destinationKind", destinationKind)));
    }

    private static EvidenceGraphProjection.TechnicalImplementationProjection messageConsumerImplementation(
            MessageConsumerEvidence evidence) {
        String group = evidence.consumerGroup() == null ? "" : evidence.consumerGroup();
        String identity = sha256("MESSAGE_CONSUMER|" + evidence.technology() + '|' + evidence.listenerClass()
                + '#' + evidence.listenerMethod() + '|' + evidence.destinationName() + '|' + group);
        String location = sourceLocation(evidence.listenerLocation().repositoryRelativePath(),
                evidence.listenerLocation().line(), evidence.listenerLocation().column());
        Map<String, String> details = new TreeMap<>();
        details.put("technology", evidence.technology());
        details.put("listenerClass", evidence.listenerClass());
        details.put("listenerMethod", evidence.listenerMethod());
        details.put("destinationName", evidence.destinationName());
        if (evidence.consumerGroup() != null) details.put("consumerGroup", evidence.consumerGroup());
        return new EvidenceGraphProjection.TechnicalImplementationProjection(
                "TI-MESSAGE-CONSUMER-" + identity,
                NodeType.TECHNICAL_IMPLEMENTATION,
                evidence.listenerClass() + '.' + evidence.listenerMethod(),
                "Spring Kafka listener consuming from " + evidence.destinationName() + ".",
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.listenerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + evidence.listenerClass() + '.' + evidence.listenerMethod(),
                        "KafkaListener subscription for " + evidence.destinationName())),
                new EvidenceGraphProjection.TechnicalProjection(
                        EvidenceGraphProjection.ImplementationType.MESSAGE,
                        ImplementationRole.MESSAGE_CONSUMER,
                        packageName(evidence.listenerClass()),
                        details));
    }

    private static EvidenceGraphProjection.RelationshipProjection consumingRelationship(
            MessageConsumerEvidence evidence, String consumerId, String destinationId) {
        String location = sourceLocation(evidence.listenerLocation().repositoryRelativePath(),
                evidence.listenerLocation().line(), evidence.listenerLocation().column());
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(consumerId + '|' + RelationshipType.CONSUMES_FROM + '|' + destinationId),
                consumerId,
                RelationshipType.CONSUMES_FROM,
                destinationId,
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.listenerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + evidence.listenerClass() + '.' + evidence.listenerMethod(),
                        "KafkaListener subscription to " + evidence.destinationName())));
    }

    private static EvidenceGraphProjection.RelationshipProjection controllerUsesProducerRelationship(
            MessageProducerEvidence evidence, String controllerId, String producerId) {
        String location = sourceLocation(evidence.publishingLocation().repositoryRelativePath(),
                evidence.publishingLocation().line(), evidence.publishingLocation().column());
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(controllerId + '|' + RelationshipType.USES + '|' + producerId),
                controllerId, RelationshipType.USES, producerId,
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.ownerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + evidence.ownerClass() + '.' + evidence.ownerMethod(),
                        "Direct " + evidence.producerField() + '.' + evidence.publishingMethod()
                                + " invocation via " + evidence.injectionKind().name())));
    }

    private static EvidenceGraphProjection.RelationshipProjection consumerUsesServiceRelationship(
            ConsumerApplicationServiceEvidence evidence, String consumerId, String serviceId) {
        String location = sourceLocation(evidence.invocationLocation().repositoryRelativePath(),
                evidence.invocationLocation().line(), evidence.invocationLocation().column());
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(consumerId + '|' + RelationshipType.USES + '|' + serviceId),
                consumerId, RelationshipType.USES, serviceId,
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.consumer().listenerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + evidence.consumer().listenerClass() + '.'
                                + evidence.consumer().listenerMethod(),
                        "Direct " + evidence.serviceClass() + '.' + evidence.serviceMethod() + " invocation")));
    }

    private static EvidenceGraphProjection.RelationshipProjection serviceUsesRepositoryRelationship(
            ApplicationServiceRepositoryEvidence evidence, String serviceId, String repositoryId) {
        List<EvidenceGraphProjection.SourceReferenceProjection> references = evidence.invocations().stream()
                .map(invocation -> {
                    String location = sourceLocation(invocation.invocationLocation().repositoryRelativePath(),
                            invocation.invocationLocation().line(), invocation.invocationLocation().column());
                    return sourceReference(
                            "SRC-JAVA-" + sha256(evidence.applicationService().serviceClass()),
                            EvidenceGraphProjection.LocationType.OTHER,
                            location + "#" + evidence.applicationService().serviceClass() + '.'
                                    + evidence.applicationService().serviceMethod(),
                            "Direct " + evidence.repositoryClass() + '.' + invocation.repositoryMethod()
                                    + " invocation");
                }).toList();
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(serviceId + '|' + RelationshipType.USES + '|' + repositoryId),
                serviceId, RelationshipType.USES, repositoryId, references);
    }

    private static EvidenceGraphProjection.RelationshipProjection publishingRelationship(
            MessageDestinationEvidence evidence, String producerId, String destinationId) {
        String location = sourceLocation(evidence.producer().publishingLocation().repositoryRelativePath(),
                evidence.producer().publishingLocation().line(), evidence.producer().publishingLocation().column());
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(producerId + '|' + RelationshipType.PUBLISHES_TO + '|' + destinationId),
                producerId,
                RelationshipType.PUBLISHES_TO,
                destinationId,
                List.of(sourceReference(
                        "SRC-JAVA-" + sha256(evidence.producer().ownerClass()),
                        EvidenceGraphProjection.LocationType.OTHER,
                        location + "#" + evidence.producer().ownerClass() + '.' + evidence.producer().ownerMethod(),
                        "KafkaTemplate.send invocation publishing to " + evidence.destinationName())));
    }

    private static EvidenceGraphProjection.TestImplementationProjection testImplementation(
            TestImplementationEvidence evidence) {
        String location = sourceLocation(
                evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String canonical = "TEST|" + evidence.testClass() + '#' + evidence.testMethod() + '|' + location;
        String identity = sha256(canonical);
        String name = evidence.displayName() == null
                ? evidence.testClass() + '.' + evidence.testMethod()
                : evidence.displayName();
        return new EvidenceGraphProjection.TestImplementationProjection(
                "TEST-AUTO-" + identity,
                NodeType.TEST_IMPLEMENTATION,
                name,
                testDescription(evidence),
                List.of(sourceReference(
                        "SRC-TEST-" + sha256(evidence.testClass()),
                        EvidenceGraphProjection.LocationType.TEST_CASE,
                        location + "#" + evidence.testClass() + '.' + evidence.testMethod(),
                        "JUnit 5 test method " + evidence.testClass() + '.' + evidence.testMethod())),
                new EvidenceGraphProjection.TestProjection(
                        "TEST-" + identity,
                        EvidenceGraphProjection.ExecutionType.AUTOMATED,
                        List.of(),
                        List.of()));
    }

    private static EvidenceGraphProjection.CheckProjection check(
            AssertionEvidence evidence, String testId) {
        String location = sourceLocation(
                evidence.repositoryRelativePath(), evidence.line(), evidence.column());
        String canonical = "CHECK|" + testId + '|' + evidence.category() + '|'
                + evidence.expression() + '|' + location;
        String identity = sha256(canonical);
        EvidenceGraphProjection.CheckType checkType = evidence.category() == AssertionCategory.PERSISTENCE_DATABASE
                ? EvidenceGraphProjection.CheckType.SQL
                : EvidenceGraphProjection.CheckType.API;
        return new EvidenceGraphProjection.CheckProjection(
                "CHECK-AUTO-" + identity,
                NodeType.CHECK,
                evidence.category() + " assertion",
                "Source assertion from " + evidence.owningTestClass()
                        + '.' + evidence.owningTestMethod() + ".",
                List.of(sourceReference(
                        "SRC-TEST-" + sha256(evidence.owningTestClass()),
                        EvidenceGraphProjection.LocationType.TEST_CASE,
                        location + "#" + evidence.owningTestClass() + '.' + evidence.owningTestMethod(),
                        "Assertion expression " + evidence.expression())),
                new EvidenceGraphProjection.CheckContentProjection(
                        checkType,
                        evidence.expression(),
                        Map.of("category", evidence.category().name())));
    }

    private static EvidenceGraphProjection.RelationshipProjection relationship(
            String from, RelationshipType type, String to) {
        return new EvidenceGraphProjection.RelationshipProjection(
                "REL-" + sha256(from + '|' + type.name() + '|' + to), from, type, to);
    }

    private static EvidenceGraphProjection.SourceReferenceProjection sourceReference(
            String sourceId,
            EvidenceGraphProjection.LocationType locationType,
            String location,
            String text) {
        return new EvidenceGraphProjection.SourceReferenceProjection(
                sourceId,
                new EvidenceGraphProjection.SourceLocationProjection(locationType, location),
                text,
                1.0,
                EvidenceGraphProjection.EvidenceType.OBSERVED);
    }

    private static boolean matchesOperation(
            HttpInteractionEvidence interaction, RestOperationEvidence operation) {
        return interaction.httpMethod().name().equals(operation.httpMethod().name())
                && normalizePath(interaction.endpointPath()).equals(normalizePath(operation.endpointPath()));
    }

    private static boolean sameTest(
            HttpInteractionEvidence interaction, TestImplementationEvidence test) {
        return interaction.owningTestClass().equals(test.testClass())
                && interaction.owningTestMethod().equals(test.testMethod());
    }

    private static boolean sameTest(AssertionEvidence assertion, TestImplementationEvidence test) {
        return assertion.owningTestClass().equals(test.testClass())
                && assertion.owningTestMethod().equals(test.testMethod());
    }

    private static String renderAnnotation(BeanValidationEvidence evidence) {
        if (evidence.declaredAttributes().isEmpty()) return "@" + simpleName(evidence.annotationType());
        String attributes = evidence.declaredAttributes().entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        return "@" + simpleName(evidence.annotationType()) + "(" + attributes + ")";
    }

    private static String qualifiedController(RestOperationEvidence evidence) {
        return evidence.javaPackage().isBlank()
                ? evidence.controllerClass()
                : evidence.javaPackage() + '.' + evidence.controllerClass();
    }

    private static String sourceLocation(String path, int line, int column) {
        return path + ':' + line + ':' + column;
    }

    private static String normalizePath(String path) {
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private static String testDescription(TestImplementationEvidence evidence) {
        String style = switch (evidence.testStyle()) {
            case REST_ASSURED -> "REST Assured";
            case MOCK_MVC -> "MockMvc";
            case TEST_REST_TEMPLATE -> "TestRestTemplate";
            case MIXED -> "REST Assured and MockMvc";
            case DIRECT_ASSERTION -> "direct assertion-library";
        };
        return "Automated JUnit 5 " + style + " test method "
                + evidence.testClass() + '.' + evidence.testMethod() + ".";
    }

    private static String packageName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(0, separator);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void verifyGraph(EvidenceGraphProjection graph) {
        Set<String> nodeIds = new HashSet<>();
        addUnique(nodeIds, graph.businessOperation().id());
        graph.businessRules().forEach(node -> addUnique(nodeIds, node.id()));
        graph.technicalImplementations().forEach(node -> addUnique(nodeIds, node.id()));
        graph.testImplementations().forEach(node -> addUnique(nodeIds, node.id()));
        graph.checks().forEach(node -> addUnique(nodeIds, node.id()));

        Set<String> relationshipIds = new HashSet<>();
        Set<String> triples = new HashSet<>();
        for (var relationship : graph.relationships()) {
            if (!nodeIds.contains(relationship.from()) || !nodeIds.contains(relationship.to())) {
                throw new IllegalStateException("Relationship has an unknown endpoint");
            }
            addUnique(relationshipIds, relationship.id());
            addUnique(triples, relationship.from() + '|' + relationship.type() + '|' + relationship.to());
        }
    }

    private static void addUnique(Set<String> values, String value) {
        if (!values.add(value)) throw new IllegalStateException("Duplicate graph identity " + value);
    }
}
