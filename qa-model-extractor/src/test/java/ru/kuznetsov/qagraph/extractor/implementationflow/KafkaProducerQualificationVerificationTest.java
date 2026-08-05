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

        var result = new DefaultRepositoryAnalysisService().analyze(
                new RepositoryAnalysisRequest(repository, "order-events-kafka-tests"));

        assertEquals(RepositoryAnalysisStatus.COMPLETE, result.status());
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
        assertTrue(java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                .filter(node -> "MESSAGE_PRODUCER".equals(
                        node.path("technicalImplementation").path("implementationRole").asText()))
                .allMatch(node -> "Kafka".equals(
                        node.path("technicalImplementation").path("details").path("technology").asText())));

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
    }
}
