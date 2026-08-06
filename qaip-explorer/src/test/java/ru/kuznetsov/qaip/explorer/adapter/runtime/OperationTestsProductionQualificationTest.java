package ru.kuznetsov.qaip.explorer.adapter.runtime;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.DefaultRepositoryAnalysisService;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisRequest;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationTestsProjectionNoneQualified;
import ru.kuznetsov.qaip.runtime.QaipRuntimeFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class OperationTestsProductionQualificationTest {
    @Test
    void projects_two_tests_and_sixteen_owned_checks_for_create_order() {
        String configured = System.getenv("ORDER_EVENTS_KAFKA_REPOSITORY");
        Assumptions.assumeTrue(configured != null && !configured.isBlank(),
                "ORDER_EVENTS_KAFKA_REPOSITORY is not configured");
        Path repository = Path.of(configured).toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/main/java")),
                "Kafka qualification production sources are unavailable");

        var analysis = new DefaultRepositoryAnalysisService().analyze(
                new RepositoryAnalysisRequest(repository, "order-events-kafka-tests"));
        String projectId = analysis.canonicalProjectJson().at("/baseModel/project/id").asText();
        String postOperationId = operationId(analysis, "POST /api/orders");
        String getOperationId = operationId(analysis, "GET /");
        var runtime = QaipRuntimeFactory.inMemory();
        assertThat(runtime.importProjectUseCase().execute(
                new RawProjectJson(analysis.canonicalProjectJson().toString())))
                .isInstanceOf(ImportProjectCompleted.class);
        var service = new RuntimeOperationTestsProjectionService(
                runtime.operationTestsQuery(), new OperationTestsViewMapper());

        var first = service.getOperationTests(projectId, postOperationId);
        var second = service.getOperationTests(projectId, postOperationId);
        var found = (OperationTestsProjectionFound) first;

        assertThat(second).isEqualTo(first);
        assertThat(found.operationTests().testCount()).isEqualTo(2);
        assertThat(found.operationTests().checkCount()).isEqualTo(16);
        assertThat(found.operationTests().verificationStatus().name()).isEqualTo("VERIFIED");
        assertThat(found.operationTests().tests()).allSatisfy(test -> {
            assertThat(test.checkCount()).isEqualTo(test.checks().size());
            assertThat(test.checks()).isNotEmpty();
        });
        assertThat(service.getOperationTests(projectId, getOperationId))
                .isInstanceOf(OperationTestsProjectionNoneQualified.class);
    }

    private static String operationId(
            ru.kuznetsov.qagraph.extractor.repositoryanalysis.RepositoryAnalysisResult analysis,
            String name) {
        return StreamSupport.stream(
                        analysis.canonicalProjectJson().at("/baseModel/nodes").spliterator(), false)
                .filter(node -> "BUSINESS_OPERATION".equals(node.path("type").asText()))
                .filter(node -> name.equals(node.path("name").asText()))
                .map(node -> node.path("id").asText())
                .findFirst().orElseThrow();
    }
}
