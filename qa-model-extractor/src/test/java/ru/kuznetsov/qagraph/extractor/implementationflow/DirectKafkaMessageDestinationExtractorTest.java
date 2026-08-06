package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.assembly.ProjectEvidenceGraphAggregator;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.model.ImplementationRole;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectKafkaMessageDestinationExtractorTest {
    @TempDir
    Path repository;

    private final SpringMvcRestOperationScanner scanner = new SpringMvcRestOperationScanner();
    private final DirectKafkaMessageProducerExtractor producerExtractor = new DirectKafkaMessageProducerExtractor();
    private final DirectKafkaMessageDestinationExtractor destinationExtractor =
            new DirectKafkaMessageDestinationExtractor();

    @Test
    void resolvesDirectLiteralAndBuildsCanonicalDestinationRelationship() throws Exception {
        writeController("LiteralController", "/literal", "\"orders.created\"");
        RestOperationEvidence operation = operations().getFirst();
        MessageProducerEvidence producer = producer(operation);
        MessageDestinationEvidence destination = destination(producer);
        var graph = assemble(operation, producer, destination);
        var node = graph.technicalImplementations().stream()
                .filter(value -> value.technicalImplementation().implementationRole()
                        == ImplementationRole.MESSAGE_DESTINATION).findFirst().orElseThrow();
        var relationship = graph.relationships().stream()
                .filter(value -> value.type() == RelationshipType.PUBLISHES_TO).findFirst().orElseThrow();

        assertEquals("orders.created", destination.destinationName());
        assertEquals("TOPIC", destination.destinationKind());
        assertEquals("Kafka", destination.technology());
        assertEquals("orders.created", node.name());
        assertEquals("Kafka", node.technicalImplementation().details().get("technology"));
        assertEquals("TOPIC", node.technicalImplementation().details().get("destinationKind"));
        assertEquals(1, relationship.sourceReferences().size());
        assertTrue(relationship.sourceReferences().getFirst().location().value().contains("LiteralController.java:"));
    }

    @Test
    void resolvesStaticConstantWithDeclarationLevelIdentity() throws Exception {
        writeController("ConstantController", "/constant", "TOPIC",
                "    private static final String TOPIC = \"orders.constant\";\n");

        MessageDestinationEvidence destination = destination(producer(operations().getFirst()));
        var node = assemble(operations().getFirst(), producer(operations().getFirst()), destination)
                .technicalImplementations().stream()
                .filter(value -> value.technicalImplementation().implementationRole()
                        == ImplementationRole.MESSAGE_DESTINATION).findFirst().orElseThrow();

        assertEquals("orders.constant", destination.destinationName());
        assertTrue(destination.declarationLocation().repositoryRelativePath().endsWith("ConstantController.java"));
        assertTrue(node.id().startsWith("TI-MESSAGE-DESTINATION-"));
    }

    @Test
    void resolvesSupportedConfigurationBackedAccessor() throws Exception {
        writeTopicNames();
        writeController("ConfiguredController", "/configured", "topicNames.ordersCreated()",
                "    private final TopicNames topicNames;\n",
                ", TopicNames topicNames", "        this.topicNames = topicNames;\n");
        write("src/main/resources/application.yml", """
                app:
                  kafka:
                    topics:
                      orders-created: orders.created
                """);

        MessageDestinationEvidence destination = destination(producer(operations().getFirst()));

        assertEquals("orders.created", destination.destinationName());
        assertEquals("src/main/resources/application.yml", destination.declarationLocation().repositoryRelativePath());
        assertEquals(4, destination.declarationLocation().line());
    }

    @Test
    void ignoresUnresolvedDynamicDestination() throws Exception {
        writeController("DynamicController", "/dynamic", "topic(value)",
                "    private String topic(String value) { return value + \".created\"; }\n");

        assertTrue(destinationExtractor.extract(repository, producer(operations().getFirst())).isEmpty());
    }

    @Test
    void twoProducersShareOneStableDestinationAndRepeatedAggregationIsDeterministic() throws Exception {
        writeController("FirstController", "/first", "\"orders.shared\"");
        writeController("SecondController", "/second", "\"orders.shared\"");
        List<ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection> graphs = new ArrayList<>();
        for (RestOperationEvidence operation : operations()) {
            MessageProducerEvidence producer = producer(operation);
            graphs.add(assemble(operation, producer, destination(producer)));
        }
        var aggregator = new ProjectEvidenceGraphAggregator();
        var first = aggregator.aggregate(graphs);
        var second = aggregator.aggregate(graphs);

        assertEquals(first, second);
        assertEquals(1, first.technicalImplementations().stream()
                .filter(value -> value.technicalImplementation().implementationRole()
                        == ImplementationRole.MESSAGE_DESTINATION).count());
        assertEquals(2, first.technicalImplementations().stream()
                .filter(value -> value.technicalImplementation().implementationRole()
                        == ImplementationRole.MESSAGE_PRODUCER).count());
        assertEquals(2, first.relationships().stream()
                .filter(value -> value.type() == RelationshipType.PUBLISHES_TO).count());
    }

    private ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection assemble(
            RestOperationEvidence operation,
            MessageProducerEvidence producer,
            MessageDestinationEvidence destination) {
        return new OperationEvidenceGraphAssembler().assemble(new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), new IntegrationTestEvidence(List.of(), List.of(), List.of()),
                List.of(), List.of(producer), List.of(destination)));
    }

    private MessageProducerEvidence producer(RestOperationEvidence operation) throws Exception {
        return producerExtractor.extract(repository, operation).getFirst();
    }

    private MessageDestinationEvidence destination(MessageProducerEvidence producer) throws Exception {
        return destinationExtractor.extract(repository, producer).orElseThrow();
    }

    private List<RestOperationEvidence> operations() throws Exception {
        return scanner.scan(repository);
    }

    private void writeTopicNames() throws Exception {
        write("src/main/java/example/TopicNames.java", """
                package example;
                import org.springframework.beans.factory.annotation.Value;
                public class TopicNames {
                    @Value("${app.kafka.topics.orders-created}")
                    private String ordersCreated;
                    public String ordersCreated() { return ordersCreated; }
                }
                """);
    }

    private void writeController(String className, String path, String destination) throws Exception {
        writeController(className, path, destination, "");
    }

    private void writeController(String className, String path, String destination, String member) throws Exception {
        writeController(className, path, destination, member, "", "");
    }

    private void writeController(
            String className, String path, String destination, String member,
            String constructorParameter, String constructorStatement) throws Exception {
        write("src/main/java/example/" + className + ".java", """
                package example;
                import org.springframework.kafka.core.KafkaTemplate;
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping("%s")
                public class %s {
                    private final KafkaTemplate<String, Object> kafkaTemplate;
                %s
                    public %s(KafkaTemplate<String, Object> kafkaTemplate%s) {
                        this.kafkaTemplate = kafkaTemplate;
                %s    }
                    @PostMapping
                    public String publish(String value) {
                        kafkaTemplate.send(%s, value);
                        return value;
                    }
                }
                """.formatted(path, className, member, className, constructorParameter,
                constructorStatement, destination));
    }

    private void write(String relative, String content) throws Exception {
        Path file = repository.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
