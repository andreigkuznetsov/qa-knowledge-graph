package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.DefaultRepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisRequest;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectKafkaMessageConsumerExtractorTest {
    @TempDir
    Path repository;

    private final DirectKafkaMessageConsumerExtractor extractor = new DirectKafkaMessageConsumerExtractor();

    @Test
    void resolvesLiteralListenerAndStaticGroupWithStableDeclarationIdentity() throws Exception {
        writeListener("LiteralListener", "listen", "\"orders.created\"", "\"orders-group\"", "");

        MessageConsumerEvidence first = extractor.extract(repository).getFirst();
        MessageConsumerEvidence second = extractor.extract(repository).getFirst();

        assertEquals(first, second);
        assertEquals("Kafka", first.technology());
        assertEquals("example.LiteralListener", first.listenerClass());
        assertEquals("listen", first.listenerMethod());
        assertEquals("orders.created", first.destinationName());
        assertEquals("TOPIC", first.destinationKind());
        assertEquals("orders-group", first.consumerGroup());
        assertTrue(first.listenerLocation().repositoryRelativePath().endsWith("LiteralListener.java"));
    }

    @Test
    void resolvesStaticTopicAndGroupConstants() throws Exception {
        writeListener("ConstantListener", "receive", "TOPIC", "GROUP", """
                    private static final String TOPIC = "orders.constant";
                    private static final String GROUP = "constant-group";
                """);

        MessageConsumerEvidence evidence = extractor.extract(repository).getFirst();

        assertEquals("orders.constant", evidence.destinationName());
        assertEquals("constant-group", evidence.consumerGroup());
        assertTrue(evidence.destinationDeclarationLocation().repositoryRelativePath()
                .endsWith("ConstantListener.java"));
    }

    @Test
    void resolvesConfigurationBackedTopicAndConsumerGroup() throws Exception {
        writeListener("ConfiguredListener", "listen", "\"${app.kafka.topics.orders-created}\"",
                "\"${spring.kafka.consumer.group-id}\"", "");
        write("src/main/resources/application.yml", """
                spring:
                  kafka:
                    consumer:
                      group-id: order-processing-group
                app:
                  kafka:
                    topics:
                      orders-created: orders.created
                """);

        MessageConsumerEvidence evidence = extractor.extract(repository).getFirst();

        assertEquals("orders.created", evidence.destinationName());
        assertEquals("order-processing-group", evidence.consumerGroup());
        assertEquals("src/main/resources/application.yml",
                evidence.destinationDeclarationLocation().repositoryRelativePath());
    }

    @Test
    void extractsMultipleConsumersDeterministicallyAndIgnoresDynamicSubscription() throws Exception {
        writeListener("FirstListener", "listen", "\"orders.shared\"", "\"first\"", "");
        writeListener("SecondListener", "listen", "\"orders.shared\"", "\"second\"", "");
        writeListener("DynamicListener", "listen", "\"#{topicProvider.topic()}\"", "\"dynamic\"", "");

        List<MessageConsumerEvidence> first = extractor.extract(repository);
        List<MessageConsumerEvidence> second = extractor.extract(repository);

        assertEquals(first, second);
        assertEquals(2, first.size());
        assertEquals(List.of("example.FirstListener", "example.SecondListener"),
                first.stream().map(MessageConsumerEvidence::listenerClass).toList());
    }

    @Test
    void producerAndConsumersReuseDestinationAndSerializeDirectedRelationships() throws Exception {
        writeController();
        writeListener("FirstListener", "listen", "\"orders.shared\"", "\"first\"", "");
        writeListener("SecondListener", "listen", "\"orders.shared\"", "\"second\"", "");

        var first = new DefaultRepositoryAnalysisService().analyze(
                new RepositoryAnalysisRequest(repository, "shared-topic"));
        var second = new DefaultRepositoryAnalysisService().analyze(
                new RepositoryAnalysisRequest(repository, "shared-topic"));

        assertEquals(RepositoryAnalysisStatus.COMPLETE, first.status());
        assertEquals(first.canonicalProjectJson(), second.canonicalProjectJson());
        var nodes = first.canonicalProjectJson().at("/baseModel/nodes");
        var relationships = first.canonicalProjectJson().at("/baseModel/relationships");
        assertEquals(1, values(nodes, "implementationRole", "MESSAGE_PRODUCER"));
        assertEquals(1, values(nodes, "implementationRole", "MESSAGE_DESTINATION"));
        assertEquals(2, values(nodes, "implementationRole", "MESSAGE_CONSUMER"));
        assertEquals(1, values(relationships, "type", "PUBLISHES_TO"));
        assertEquals(2, values(relationships, "type", "CONSUMES_FROM"));
        java.util.stream.StreamSupport.stream(relationships.spliterator(), false)
                .filter(value -> "CONSUMES_FROM".equals(value.path("type").asText()))
                .forEach(value -> assertEquals(1, value.path("sourceReferences").size()));
    }

    private static long values(com.fasterxml.jackson.databind.JsonNode values, String field, String expected) {
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .filter(value -> expected.equals(value.path(field).asText())
                        || expected.equals(value.path("technicalImplementation").path(field).asText()))
                .count();
    }

    private void writeListener(
            String className, String method, String topic, String group, String members) throws Exception {
        write("src/main/java/example/" + className + ".java", """
                package example;
                import org.springframework.kafka.annotation.KafkaListener;
                public class %s {
                %s
                    @KafkaListener(topics = %s, groupId = %s)
                    public void %s(String event) { }
                }
                """.formatted(className, members, topic, group, method));
    }

    private void writeController() throws Exception {
        write("src/main/java/example/OrderController.java", """
                package example;
                import org.springframework.kafka.core.KafkaTemplate;
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping("/orders")
                public class OrderController {
                    private final KafkaTemplate<String, Object> kafkaTemplate;
                    public OrderController(KafkaTemplate<String, Object> kafkaTemplate) {
                        this.kafkaTemplate = kafkaTemplate;
                    }
                    @PostMapping
                    public String create(String value) {
                        kafkaTemplate.send("orders.shared", value);
                        return value;
                    }
                }
                """);
    }

    private void write(String relative, String content) throws Exception {
        Path file = repository.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
