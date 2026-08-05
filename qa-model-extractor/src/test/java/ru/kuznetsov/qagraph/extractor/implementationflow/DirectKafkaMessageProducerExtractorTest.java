package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceAssemblyRequest;
import ru.kuznetsov.qagraph.extractor.assembly.OperationEvidenceGraphAssembler;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SpringMvcRestOperationScanner;
import ru.kuznetsov.qagraph.model.ImplementationRole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectKafkaMessageProducerExtractorTest {
    @TempDir
    Path repository;

    private final SpringMvcRestOperationScanner scanner = new SpringMvcRestOperationScanner();
    private final DirectKafkaMessageProducerExtractor extractor = new DirectKafkaMessageProducerExtractor();

    @Test
    void extractsOneDirectConstructorOwnedKafkaProducerWithSourceLocation() throws Exception {
        writeController("FirstController", "/first", "publish", 1);
        RestOperationEvidence operation = operations().getFirst();

        MessageProducerEvidence producer = extractor.extract(repository, operation).getFirst();

        assertEquals("example.FirstController", producer.ownerClass());
        assertEquals("publish", producer.ownerMethod());
        assertEquals("kafkaTemplate", producer.producerField());
        assertEquals("send", producer.publishingMethod());
        assertEquals(DependencyInjectionKind.CONSTRUCTOR, producer.injectionKind());
        assertTrue(producer.publishingLocation().repositoryRelativePath().endsWith("FirstController.java"));
        assertTrue(producer.publishingLocation().line() > 0);
    }

    @Test
    void extractsMultipleProducersAcrossOperationsAndIsRepeatable() throws Exception {
        writeController("FirstController", "/first", "publish", 1);
        writeController("SecondController", "/second", "emit", 1);

        List<MessageProducerEvidence> first = extractAll();
        List<MessageProducerEvidence> second = extractAll();

        assertEquals(first, second);
        assertEquals(2, first.size());
        assertEquals(List.of("example.FirstController.publish", "example.SecondController.emit"), first.stream()
                .map(value -> value.ownerClass() + '.' + value.ownerMethod()).toList());
    }

    @Test
    void repeatedSendCallsProduceOneDeterministicallyIdentifiedCanonicalNode() throws Exception {
        writeController("FirstController", "/first", "publish", 2);
        RestOperationEvidence operation = operations().getFirst();
        List<MessageProducerEvidence> evidence = extractor.extract(repository, operation);
        var assembler = new OperationEvidenceGraphAssembler();
        var request = new OperationEvidenceAssemblyRequest(
                operation, List.of(), List.of(), new IntegrationTestEvidence(List.of(), List.of(), List.of()),
                List.of(), evidence);

        var first = assembler.assemble(request);
        var second = assembler.assemble(request);
        var producers = first.technicalImplementations().stream()
                .filter(value -> value.technicalImplementation().implementationRole()
                        == ImplementationRole.MESSAGE_PRODUCER)
                .toList();

        assertEquals(first, second);
        assertEquals(1, producers.size());
        assertTrue(producers.getFirst().id().startsWith("TI-MESSAGE-PRODUCER-"));
        assertEquals("MESSAGE", producers.getFirst().technicalImplementation().implementationType().name());
        assertEquals("Kafka", producers.getFirst().technicalImplementation().details().get("technology"));
        assertEquals(1, producers.getFirst().sourceReferences().size());
        assertTrue(producers.getFirst().sourceReferences().getFirst().location().value()
                .contains("FirstController.java:"));
        assertEquals(1, first.relationships().size(), "producer extraction adds no relationship");
    }

    private List<MessageProducerEvidence> extractAll() throws Exception {
        List<MessageProducerEvidence> result = new ArrayList<>();
        for (RestOperationEvidence operation : operations()) {
            result.addAll(extractor.extract(repository, operation));
        }
        return List.copyOf(result);
    }

    private List<RestOperationEvidence> operations() throws Exception {
        return scanner.scan(repository);
    }

    private void writeController(String className, String path, String method, int sends) throws Exception {
        Path source = repository.resolve("src/main/java/example/" + className + ".java");
        Files.createDirectories(source.getParent());
        String calls = "        kafkaTemplate.send(\"orders.created\", value);\n".repeat(sends);
        Files.writeString(source, """
                package example;

                import org.springframework.kafka.core.KafkaTemplate;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("%s")
                public class %s {
                    private final KafkaTemplate<String, Object> kafkaTemplate;

                    public %s(KafkaTemplate<String, Object> kafkaTemplate) {
                        this.kafkaTemplate = kafkaTemplate;
                    }

                    @PostMapping
                    public String %s(String value) {
                %s        return value;
                    }
                }
                """.formatted(path, className, className, method, calls));
    }
}
