package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.DefaultRepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisRequest;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaProducerQualificationVerificationTest {
    @Test
    void orderCommandCreateOrderProducesOneMessageProducerAndPreservesRestNode() {
        String configured = System.getenv("ORDER_EVENTS_KAFKA_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "ORDER_EVENTS_KAFKA_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/main/java")),
                "Kafka qualification production sources are unavailable");

        var analysisService = new DefaultRepositoryAnalysisService();
        var analysisRequest = new RepositoryAnalysisRequest(repository, "order-events-kafka-tests");
        var result = analysisService.analyze(analysisRequest);
        var repeated = analysisService.analyze(analysisRequest);

        assertEquals(RepositoryAnalysisStatus.COMPLETE, result.status());
        assertEquals(result.canonicalProjectJson(), repeated.canonicalProjectJson());
        var nodes = result.canonicalProjectJson().path("baseModel").path("nodes");
        long producers = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_PRODUCER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.controller.OrderCommandController.createOrder"
                        .equals(node.path("name").asText()))
                .count();
        long restNodes = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "API".equals(node.path("technicalImplementation").path("implementationType").asText()))
                .filter(node -> "com.example.kafkaorders.controller.OrderCommandController.createOrder"
                        .equals(node.path("name").asText()))
                .count();

        assertEquals(1, producers);
        assertEquals(1, restNodes);
        var eventController = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "REST_CONTROLLER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.controller.OrderCommandController.createOrder"
                        .equals(node.path("name").asText())).findFirst().orElseThrow();
        var producer = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_PRODUCER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.controller.OrderCommandController.createOrder"
                        .equals(node.path("name").asText())).findFirst().orElseThrow();
        assertEquals(3, producer.path("technicalImplementation").path("details").size());
        assertTrue(producer.path("technicalImplementation").path("details").has("ownerClass"));
        assertTrue(producer.path("technicalImplementation").path("details").has("ownerMethod"));
        assertTrue(producer.path("technicalImplementation").path("details").has("technology"));
        assertTrue(java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_PRODUCER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .allMatch(node -> "Kafka".equals(
                        node.path("technicalImplementation").path("details").path("technology").asText())));

        var controllerUsesProducer = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "USES".equals(relationship.path("type").asText()))
                .filter(relationship -> eventController.path("id").asText()
                        .equals(relationship.path("from").asText()))
                .filter(relationship -> producer.path("id").asText()
                        .equals(relationship.path("to").asText())).toList();
        assertEquals(1, controllerUsesProducer.size());
        assertEquals(1, controllerUsesProducer.getFirst().path("sourceReferences").size());
        assertTrue(controllerUsesProducer.getFirst().at("/sourceReferences/0/text").asText()
                .contains("kafkaTemplate.send"));
        assertTrue(!producer.at("/sourceReferences/0/text").asText().contains("send"));

        var destinations = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_DESTINATION".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "orders.created".equals(node.path("name").asText()))
                .toList();
        var publications = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "PUBLISHES_TO".equals(relationship.path("type").asText()))
                .toList();
        assertEquals(1, destinations.size());
        assertEquals(1, publications.size());
        assertEquals(1, publications.getFirst().path("sourceReferences").size());

        var consumers = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_CONSUMER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.listener.OrderCreatedListener.listen"
                        .equals(node.path("name").asText()))
                .toList();
        var consumptions = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "CONSUMES_FROM".equals(relationship.path("type").asText()))
                .toList();
        assertEquals(1, consumers.size());
        assertEquals("order-processing-group", consumers.getFirst().path("technicalImplementation")
                .path("details").path("consumerGroup").asText());
        assertEquals(1, consumptions.size());
        assertEquals(publications.getFirst().path("to").asText(), consumptions.getFirst().path("to").asText());
        assertEquals(consumers.getFirst().path("id").asText(), consumptions.getFirst().path("from").asText());
        assertEquals(1, consumptions.getFirst().path("sourceReferences").size());

        var services = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "APPLICATION_SERVICE".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.service.OrderProcessingService.process"
                        .equals(node.path("name").asText())).toList();
        assertEquals(1, services.size());
        var consumerUsesService = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "USES".equals(relationship.path("type").asText()))
                .filter(relationship -> consumers.getFirst().path("id").asText()
                        .equals(relationship.path("from").asText()))
                .filter(relationship -> services.getFirst().path("id").asText()
                        .equals(relationship.path("to").asText())).toList();
        assertEquals(1, consumerUsesService.size());
        assertEquals(1, consumerUsesService.getFirst().path("sourceReferences").size());

        var repositories = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "REPOSITORY".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .filter(node -> "com.example.kafkaorders.repository.ProcessedOrderRepository"
                        .equals(node.path("name").asText())).toList();
        assertEquals(1, repositories.size());
        var serviceUsesRepository = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "USES".equals(relationship.path("type").asText()))
                .filter(relationship -> services.getFirst().path("id").asText()
                        .equals(relationship.path("from").asText()))
                .filter(relationship -> repositories.getFirst().path("id").asText()
                        .equals(relationship.path("to").asText())).toList();
        assertEquals(1, serviceUsesRepository.size());
        assertEquals(5, serviceUsesRepository.getFirst().path("sourceReferences").size());

        var createOrder = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "BUSINESS_OPERATION".equals(node.path("type").asText()))
                .filter(node -> "POST /api/orders".equals(node.path("name").asText()))
                .findFirst().orElseThrow();
        var controllerImplementation = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "IMPLEMENTED_BY".equals(relationship.path("type").asText()))
                .filter(relationship -> createOrder.path("id").asText()
                        .equals(relationship.path("from").asText()))
                .map(relationship -> relationship.path("to").asText())
                .findFirst().orElseThrow();
        var qualifiedTestIds = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "USES".equals(relationship.path("type").asText()))
                .filter(relationship -> controllerImplementation.equals(relationship.path("to").asText()))
                .map(relationship -> relationship.path("from").asText())
                .collect(java.util.stream.Collectors.toSet());
        long qualifiedChecks = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "HAS_CHECK".equals(relationship.path("type").asText()))
                .filter(relationship -> qualifiedTestIds.contains(relationship.path("from").asText()))
                .count();

        assertEquals(2, qualifiedTestIds.size());
        assertEquals(16, qualifiedChecks);
        var homeOperation = java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "BUSINESS_OPERATION".equals(node.path("type").asText()))
                .filter(node -> "GET /".equals(node.path("name").asText()))
                .findFirst().orElseThrow();
        var homeImplementation = java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "IMPLEMENTED_BY".equals(relationship.path("type").asText()))
                .filter(relationship -> homeOperation.path("id").asText()
                        .equals(relationship.path("from").asText()))
                .map(relationship -> relationship.path("to").asText())
                .findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(
                        result.canonicalProjectJson().at("/baseModel/relationships").spliterator(), false)
                .filter(relationship -> "USES".equals(relationship.path("type").asText()))
                .noneMatch(relationship -> homeImplementation.equals(relationship.path("to").asText())));
    }
}
