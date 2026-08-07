package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationdetails.ConventionalImplementationPathResolver;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailableReason;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Objects;

public final class DefaultOperationOverviewQuery implements OperationOverviewQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";

    private final ProjectReader projectReader;
    private final ConventionalImplementationPathResolver implementationResolver;

    public DefaultOperationOverviewQuery(ProjectReader projectReader) {
        this(projectReader, new ConventionalImplementationPathResolver());
    }

    public DefaultOperationOverviewQuery(
            ProjectReader projectReader,
            ConventionalImplementationPathResolver implementationResolver
    ) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.implementationResolver = Objects.requireNonNull(implementationResolver, "implementationResolver");
    }

    @Override
    public OperationOverviewQueryResult execute(String projectId, String operationId) {
        String requestedProjectId = new OperationOverviewProjectNotFound(projectId).projectId();
        String requestedOperationId = new OperationOverviewOperationNotFound(
                requestedProjectId, operationId).operationId();
        var project = Objects.requireNonNull(
                projectReader.findById(requestedProjectId), "project reader result");
        if (project.isEmpty()) return new OperationOverviewProjectNotFound(requestedProjectId);

        Project snapshot = project.orElseThrow();
        Node operation = snapshot.nodes().stream()
                .filter(node -> requestedOperationId.equals(node.id()))
                .filter(node -> BUSINESS_OPERATION.equals(node.type()))
                .findFirst()
                .orElse(null);
        if (operation == null) {
            return new OperationOverviewOperationNotFound(requestedProjectId, requestedOperationId);
        }

        Endpoint endpoint = Endpoint.from(operation);
        OperationOverviewImplementationSection implementation = implementation(
                implementationResolver.resolve(snapshot, requestedOperationId));
        return new OperationOverviewFound(
                new OperationOverviewIdentity(requestedProjectId, requestedOperationId,
                        endpoint.method(), endpoint.path(), operation.name()),
                implementation,
                new OperationOverviewEventPathNotApplicable(),
                new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.UNVERIFIED, 0, 0, List.of()));
    }

    private static OperationOverviewImplementationSection implementation(
            ConventionalImplementationPathResolver.Resolution resolution
    ) {
        if (resolution instanceof ConventionalImplementationPathResolver.Available available) {
            return new OperationOverviewImplementationAvailable(new OperationOverviewImplementation(
                    available.controller().name(), available.service().name(), available.repository().name()));
        }
        var unavailable = (ConventionalImplementationPathResolver.Unavailable) resolution;
        return unavailable.reason() == OperationDetailsUnavailableReason.INCOMPLETE_PATH
                ? new OperationOverviewImplementationIncomplete()
                : new OperationOverviewImplementationAmbiguous();
    }

    private record Endpoint(String method, String path) {
        private static Endpoint from(Node node) {
            String[] parts = Objects.requireNonNull(
                    node.name(), "business operation name").trim().split("\\s+", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                        "business operation name must contain HTTP method and path: " + node.id());
            }
            return new Endpoint(parts[0], parts[1]);
        }
    }
}
