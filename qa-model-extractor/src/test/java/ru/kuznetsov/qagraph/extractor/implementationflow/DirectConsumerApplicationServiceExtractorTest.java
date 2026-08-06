package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectConsumerApplicationServiceExtractorTest {
    @TempDir Path repository;
    private final DirectKafkaMessageConsumerExtractor consumers = new DirectKafkaMessageConsumerExtractor();
    private final DirectConsumerApplicationServiceExtractor extractor =
            new DirectConsumerApplicationServiceExtractor();

    @Test
    void extractsOneDirectInjectedServiceDeterministically() throws Exception {
        write("Listener.java", listener("service.process(value);"));
        write("OrderService.java", service("void process(String value) { }"));

        var consumerEvidence = consumers.extract(repository);
        var first = extractor.extract(repository, consumerEvidence);
        var second = extractor.extract(repository, consumerEvidence);

        assertEquals(first, second);
        assertEquals(1, first.size());
        assertEquals("example.OrderService", first.getFirst().serviceClass());
        assertEquals("process", first.getFirst().serviceMethod());
        assertEquals(DependencyInjectionKind.CONSTRUCTOR, first.getFirst().injectionKind());
        assertTrue(first.getFirst().invocationLocation().repositoryRelativePath().endsWith("Listener.java"));
        assertTrue(first.getFirst().serviceMethodLocation().repositoryRelativePath().endsWith("OrderService.java"));
    }

    @Test
    void multipleQualifiedServicesAreUnsupported() throws Exception {
        write("Listener.java", listener("service.process(value); other.process(value);"));
        write("OrderService.java", service("void process(String value) { }"));
        write("OtherService.java", """
                package example;
                import org.springframework.stereotype.Service;
                @Service class OtherService { void process(String value) { } }
                """);

        assertTrue(extractor.extract(repository, consumers.extract(repository)).isEmpty());
    }

    @Test
    void helperMediatedAndUnresolvedCallsAreUnsupported() throws Exception {
        write("Listener.java", """
                package example;
                import org.springframework.kafka.annotation.KafkaListener;
                class Listener {
                    private final OrderService service;
                    Listener(OrderService service) { this.service = service; }
                    @KafkaListener(topics = "orders", groupId = "group")
                    void listen(String value) { helper(value); }
                    private void helper(String value) { service.process(value); }
                }
                """);
        write("OrderService.java", service("void process(String value) { }"));

        assertTrue(extractor.extract(repository, consumers.extract(repository)).isEmpty());
    }

    private static String listener(String body) {
        return """
                package example;
                import org.springframework.kafka.annotation.KafkaListener;
                class Listener {
                    private final OrderService service;
                    private final OtherService other;
                    Listener(OrderService service, OtherService other) {
                        this.service = service; this.other = other;
                    }
                    @KafkaListener(topics = "orders", groupId = "group")
                    void listen(String value) { %s }
                }
                """.formatted(body);
    }

    private static String service(String method) {
        return """
                package example;
                import org.springframework.stereotype.Service;
                @Service class OrderService { %s }
                """.formatted(method);
    }

    private void write(String name, String source) throws Exception {
        Path file = repository.resolve("src/main/java/example").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
