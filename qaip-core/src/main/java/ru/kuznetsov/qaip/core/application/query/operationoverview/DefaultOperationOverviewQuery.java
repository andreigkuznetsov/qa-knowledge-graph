package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationdetails.ConventionalImplementationPathResolver;
import ru.kuznetsov.qaip.core.application.query.operationdetails.OperationDetailsUnavailableReason;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathAmbiguous;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathIncomplete;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathNotEventDriven;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQueryResult;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathResolver;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.persistence.read.ProjectReader;

import java.util.List;
import java.util.Objects;

public final class DefaultOperationOverviewQuery implements OperationOverviewQuery {
    private static final String BUSINESS_OPERATION = "BUSINESS_OPERATION";

    private final ProjectReader projectReader;
    private final ConventionalImplementationPathResolver implementationResolver;
    private final EventPathResolver eventPathResolver;

    public DefaultOperationOverviewQuery(ProjectReader projectReader) {
        this(projectReader, new ConventionalImplementationPathResolver(), new EventPathResolver());
    }

    public DefaultOperationOverviewQuery(
            ProjectReader projectReader,
            ConventionalImplementationPathResolver implementationResolver,
            EventPathResolver eventPathResolver
    ) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.implementationResolver = Objects.requireNonNull(implementationResolver, "implementationResolver");
        this.eventPathResolver = Objects.requireNonNull(eventPathResolver, "eventPathResolver");
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
        OperationOverviewEventPathSection eventPath = eventPath(
                eventPathResolver.resolve(snapshot, requestedProjectId, requestedOperationId));
        return new OperationOverviewFound(
                new OperationOverviewIdentity(requestedProjectId, requestedOperationId,
                        endpoint.method(), endpoint.path(), operation.name()),
                implementation,
                eventPath,
                new OperationOverviewVerificationAvailable(
                        OperationVerificationStatus.UNVERIFIED, 0, 0, List.of()));
    }

    private static OperationOverviewEventPathSection eventPath(EventPathQueryResult result) {
        if (result instanceof EventPathFound found) {
            return new OperationOverviewEventPathAvailable(found.path());
        }
        if (result instanceof EventPathNotEventDriven) {
            return new OperationOverviewEventPathNotApplicable();
        }
        if (result instanceof EventPathIncomplete) {
            return new OperationOverviewEventPathIncomplete();
        }
        if (result instanceof EventPathAmbiguous) {
            return new OperationOverviewEventPathAmbiguous();
        }
        throw new IllegalStateException("event path resolver returned an identity outcome for an existing operation");
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
