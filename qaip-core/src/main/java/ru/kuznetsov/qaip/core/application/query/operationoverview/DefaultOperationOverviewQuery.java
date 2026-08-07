package ru.kuznetsov.qaip.core.application.query.operationoverview;

import ru.kuznetsov.qaip.core.application.query.operationtests.OperationVerificationStatus;
import ru.kuznetsov.qaip.core.application.query.operationtests.DefaultOperationTestsResolver;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsAmbiguous;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsFound;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsNoneQualified;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsQueryResult;
import ru.kuznetsov.qaip.core.application.query.operationtests.OperationTestsResolver;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationListProjector;
import ru.kuznetsov.qaip.core.application.query.operationlist.OperationIdentityResolver;
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
    private final OperationTestsResolver operationTestsResolver;
    private final OperationIdentityResolver identityResolver;

    public DefaultOperationOverviewQuery(ProjectReader projectReader) {
        this(projectReader, new ConventionalImplementationPathResolver(), new EventPathResolver(),
                new DefaultOperationTestsResolver(new OperationListProjector()), new OperationIdentityResolver());
    }

    public DefaultOperationOverviewQuery(
            ProjectReader projectReader,
            ConventionalImplementationPathResolver implementationResolver,
            EventPathResolver eventPathResolver,
            OperationTestsResolver operationTestsResolver
    ) {
        this(projectReader, implementationResolver, eventPathResolver,
                operationTestsResolver, new OperationIdentityResolver());
    }

    public DefaultOperationOverviewQuery(
            ProjectReader projectReader,
            ConventionalImplementationPathResolver implementationResolver,
            EventPathResolver eventPathResolver,
            OperationTestsResolver operationTestsResolver,
            OperationIdentityResolver identityResolver
    ) {
        this.projectReader = Objects.requireNonNull(projectReader, "projectReader");
        this.implementationResolver = Objects.requireNonNull(implementationResolver, "implementationResolver");
        this.eventPathResolver = Objects.requireNonNull(eventPathResolver, "eventPathResolver");
        this.operationTestsResolver = Objects.requireNonNull(operationTestsResolver, "operationTestsResolver");
        this.identityResolver = Objects.requireNonNull(identityResolver, "identityResolver");
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

        OperationIdentityResolver.ResolvedOperationIdentity identity = identityResolver.resolve(operation);
        OperationOverviewImplementationSection implementation = implementation(
                implementationResolver.resolve(snapshot, requestedOperationId));
        OperationOverviewEventPathSection eventPath = eventPath(
                eventPathResolver.resolve(snapshot, requestedProjectId, requestedOperationId));
        OperationOverviewVerificationSection verification = verification(
                operationTestsResolver.resolve(snapshot, requestedProjectId, requestedOperationId));
        return new OperationOverviewFound(
                new OperationOverviewIdentity(requestedProjectId, requestedOperationId,
                        identity.method(), identity.path(), identity.displayName()),
                implementation,
                eventPath,
                verification);
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

    private static OperationOverviewVerificationSection verification(OperationTestsQueryResult result) {
        if (result instanceof OperationTestsFound found) {
            return new OperationOverviewVerificationAvailable(found.verificationStatus(),
                    found.testCount(), found.checkCount(), found.tests());
        }
        if (result instanceof OperationTestsNoneQualified) {
            return new OperationOverviewVerificationAvailable(
                    OperationVerificationStatus.UNVERIFIED, 0, 0, List.of());
        }
        if (result instanceof OperationTestsAmbiguous) {
            return new OperationOverviewVerificationAmbiguous();
        }
        throw new IllegalStateException(
                "operation tests resolver returned an identity outcome for an existing operation");
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

}
