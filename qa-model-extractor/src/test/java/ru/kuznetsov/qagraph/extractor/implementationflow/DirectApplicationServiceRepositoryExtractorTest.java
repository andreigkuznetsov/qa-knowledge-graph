package ru.kuznetsov.qagraph.extractor.implementationflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectApplicationServiceRepositoryExtractorTest {
    @TempDir Path repository;
    private final DirectApplicationServiceRepositoryExtractor extractor =
            new DirectApplicationServiceRepositoryExtractor();

    @Test
    void extractsOneRepositoryAndAllResolvableInvocationsDeterministically() throws Exception {
        write("OrderService.java", service("OrderService", "repository.existsByCode(value); repository.save(value);"));
        write("OrderRepository.java", repositorySource());
        var service = serviceEvidence("OrderService", 7, 10);

        var first = extractor.extract(repository, List.of(service));
        var second = extractor.extract(repository, List.of(service));

        assertEquals(first, second);
        assertEquals(1, first.size());
        assertEquals("example.OrderRepository", first.getFirst().repositoryClass());
        assertEquals(List.of("existsByCode", "save"), first.getFirst().invocations().stream()
                .map(ApplicationServiceRepositoryEvidence.RepositoryInvocationEvidence::repositoryMethod).toList());
        assertEquals(DependencyInjectionKind.CONSTRUCTOR, first.getFirst().injectionKind());
    }

    @Test
    void multipleServicesReuseTheSameRepositoryDeclarationEvidence() throws Exception {
        write("FirstService.java", service("FirstService", "repository.existsByCode(value);"));
        write("SecondService.java", service("SecondService", "repository.save(value);"));
        write("OrderRepository.java", repositorySource());

        var result = extractor.extract(repository, List.of(
                serviceEvidence("FirstService", 7, 10), serviceEvidence("SecondService", 7, 10)));

        assertEquals(2, result.size());
        assertEquals(1, result.stream().map(ApplicationServiceRepositoryEvidence::repositoryClass).distinct().count());
        assertEquals(1, result.stream().map(ApplicationServiceRepositoryEvidence::repositoryDeclarationLocation)
                .distinct().count());
    }

    @Test
    void unresolvedAndMultipleRepositoryDeclarationsAreUnsupported() throws Exception {
        write("OrderService.java", """
                package example;
                import org.springframework.stereotype.Service;
                @Service class OrderService {
                    private final OrderRepository repository;
                    private final OtherRepository other;
                    OrderService(OrderRepository repository, OtherRepository other) {
                        this.repository = repository; this.other = other;
                    }
                    void process(String value) { repository.existsByCode(value); other.save(value); }
                }
                """);
        write("OrderRepository.java", repositorySource());
        write("OtherRepository.java", repositorySource().replace("OrderRepository", "OtherRepository"));

        assertTrue(extractor.extract(repository, List.of(serviceEvidence("OrderService", 9, 10))).isEmpty());

        write("OrderService.java", service("OrderService", "repository.missing(value);"));
        assertTrue(extractor.extract(repository, List.of(serviceEvidence("OrderService", 7, 10))).isEmpty());
    }

    private ConsumerApplicationServiceEvidence serviceEvidence(String name, int line, int column) {
        var consumer = new MessageConsumerEvidence("Kafka", "example.Listener", "listen", "orders", "TOPIC",
                "group", new SourceLocation("src/main/java/example/Listener.java", 1, 1),
                new SourceLocation("src/main/java/example/Listener.java", 1, 1));
        return new ConsumerApplicationServiceEvidence(
                consumer, "example." + name, "process", DependencyInjectionKind.CONSTRUCTOR,
                ImplementationInvocationKind.DIRECT_FIELD_METHOD_INVOCATION,
                new SourceLocation("src/main/java/example/Listener.java", 1, 1),
                new SourceLocation("src/main/java/example/" + name + ".java", line, column));
    }

    private static String service(String name, String calls) {
        return """
                package example;
                import org.springframework.stereotype.Service;
                @Service class %s {
                    private final OrderRepository repository;
                    %s(OrderRepository repository) { this.repository = repository; }
                    void process(String value) { %s }
                }
                """.formatted(name, name, calls);
    }

    private static String repositorySource() {
        return """
                package example;
                import org.springframework.data.repository.CrudRepository;
                interface OrderRepository extends CrudRepository<String, Long> {
                    boolean existsByCode(String code);
                }
                """;
    }

    private void write(String name, String source) throws Exception {
        Path file = repository.resolve("src/main/java/example").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source);
    }
}
