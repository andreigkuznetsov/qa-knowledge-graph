package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.core.application.importproject.ImportProjectCompleted;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathImplementationRole;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathNotEventDriven;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailable;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailableReason;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewEventPathAvailable;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewEventPathNotApplicable;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewFound;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewImplementationIncomplete;
import ru.kuznetsov.qaip.core.application.query.operationoverview.OperationOverviewVerificationAvailable;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsNoneQualified;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.importing.parsing.RawProjectJson;
import ru.kuznetsov.qaip.runtime.QaipRuntimeFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OperationOverviewProductionQualificationTest {
    @Test
    void qualifies_unified_overviews_and_specialized_query_parity_on_real_repository() {
        Path repository = qualificationRepository();
        Assumptions.assumeTrue(Files.isDirectory(repository.resolve("src/main/java")),
                "order-events-kafka-tests production sources are unavailable");

        var analysis = new DefaultRepositoryAnalysisService().analyze(
                new RepositoryAnalysisRequest(repository, "operation-overview-qualification"));
        String projectId = analysis.canonicalProjectJson().at("/baseModel/project/id").asText();
        String postOperationId = operationId(analysis, "POST /api/orders");
        String getOperationId = operationId(analysis, "GET /");
        var runtime = QaipRuntimeFactory.inMemory();
        assertInstanceOf(ImportProjectCompleted.class, runtime.importProjectUseCase().execute(
                new RawProjectJson(analysis.canonicalProjectJson().toString())));

        OperationOverviewFound post = assertInstanceOf(OperationOverviewFound.class,
                runtime.operationOverviewQuery().execute(projectId, postOperationId));
        assertEquals("POST", post.identity().method());
        assertEquals("/api/orders", post.identity().path());
        assertInstanceOf(OperationOverviewImplementationIncomplete.class, post.implementation());
        OperationOverviewEventPathAvailable postPath = assertInstanceOf(
                OperationOverviewEventPathAvailable.class, post.eventPath());
        assertEquals(List.of(
                        EventPathImplementationRole.REST_CONTROLLER,
                        EventPathImplementationRole.MESSAGE_PRODUCER,
                        EventPathImplementationRole.MESSAGE_DESTINATION,
                        EventPathImplementationRole.MESSAGE_CONSUMER,
                        EventPathImplementationRole.APPLICATION_SERVICE,
                        EventPathImplementationRole.REPOSITORY),
                postPath.path().steps().stream().map(step -> step.implementationRole()).toList());
        OperationOverviewVerificationAvailable postVerification = assertInstanceOf(
                OperationOverviewVerificationAvailable.class, post.verification());
        assertEquals(OperationVerificationStatus.VERIFIED, postVerification.verificationStatus());
        assertEquals(2, postVerification.testCount());
        assertEquals(16, postVerification.checkCount());

        OperationDetailsUnavailable details = assertInstanceOf(OperationDetailsUnavailable.class,
                runtime.operationDetailsQuery().execute(projectId, postOperationId));
        assertEquals(OperationDetailsUnavailableReason.INCOMPLETE_PATH, details.reason());
        EventPathFound specializedPath = assertInstanceOf(EventPathFound.class,
                runtime.eventPathQuery().execute(projectId, postOperationId));
        assertEquals(specializedPath.path(), postPath.path());
        OperationTestsFound specializedTests = assertInstanceOf(OperationTestsFound.class,
                runtime.operationTestsQuery().execute(projectId, postOperationId));
        assertEquals(specializedTests.verificationStatus(), postVerification.verificationStatus());
        assertEquals(specializedTests.testCount(), postVerification.testCount());
        assertEquals(specializedTests.checkCount(), postVerification.checkCount());
        assertEquals(specializedTests.tests(), postVerification.tests());

        OperationOverviewFound get = assertInstanceOf(OperationOverviewFound.class,
                runtime.operationOverviewQuery().execute(projectId, getOperationId));
        assertEquals("GET", get.identity().method());
        assertEquals("/", get.identity().path());
        assertInstanceOf(OperationOverviewEventPathNotApplicable.class, get.eventPath());
        OperationOverviewVerificationAvailable getVerification = assertInstanceOf(
                OperationOverviewVerificationAvailable.class, get.verification());
        assertEquals(OperationVerificationStatus.UNVERIFIED, getVerification.verificationStatus());
        assertEquals(0, getVerification.testCount());
        assertEquals(0, getVerification.checkCount());
        assertEquals(List.of(), getVerification.tests());
        assertInstanceOf(EventPathNotEventDriven.class,
                runtime.eventPathQuery().execute(projectId, getOperationId));
        assertInstanceOf(OperationTestsNoneQualified.class,
                runtime.operationTestsQuery().execute(projectId, getOperationId));
    }

    private static Path qualificationRepository() {
        String configured = System.getenv("ORDER_EVENTS_KAFKA_REPOSITORY");
        String location = configured == null || configured.isBlank()
                ? "D:/git/order-events-kafka-tests"
                : configured;
        return Path.of(location).toAbsolutePath().normalize();
    }

    private static String operationId(RepositoryAnalysisResult analysis, String name) {
        return StreamSupport.stream(
                        analysis.canonicalProjectJson().at("/baseModel/nodes").spliterator(), false)
                .filter(node -> "BUSINESS_OPERATION".equals(node.path("type").asText()))
                .filter(node -> name.equals(node.path("name").asText()))
                .map(node -> node.path("id").asText())
                .findFirst()
                .orElseThrow();
    }
}
