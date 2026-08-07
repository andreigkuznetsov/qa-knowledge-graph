package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.operationoverview.*;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.OperationOverviewProjectionResult;
import ru.kuznetsov.qaip.explorer.view.*;

import java.util.Objects;

public final class OperationOverviewViewMapper {
    private final EventPathViewMapper eventPathMapper;
    private final OperationTestsViewMapper operationTestsMapper;

    public OperationOverviewViewMapper(
            EventPathViewMapper eventPathMapper,
            OperationTestsViewMapper operationTestsMapper
    ) {
        this.eventPathMapper = Objects.requireNonNull(eventPathMapper, "eventPathMapper");
        this.operationTestsMapper = Objects.requireNonNull(operationTestsMapper, "operationTestsMapper");
    }

    public OperationOverviewProjectionResult map(OperationOverviewQueryResult result) {
        Objects.requireNonNull(result, "result");
        return switch (result) {
            case OperationOverviewFound found -> new OperationOverviewProjectionFound(mapFound(found));
            case OperationOverviewProjectNotFound notFound ->
                    new OperationOverviewProjectionProjectNotFound(notFound.projectId());
            case OperationOverviewOperationNotFound notFound ->
                    new OperationOverviewProjectionOperationNotFound(notFound.projectId(), notFound.operationId());
        };
    }

    private OperationOverviewView mapFound(OperationOverviewFound found) {
        var identity = found.identity();
        return new OperationOverviewView(
                new OperationOverviewIdentityView(identity.projectId(), identity.operationId(),
                        identity.method(), identity.path(), identity.displayName()),
                mapImplementation(found.implementation()),
                mapEventPath(found.eventPath()),
                mapVerification(identity, found.verification()));
    }

    private static OperationOverviewImplementationSectionView mapImplementation(
            OperationOverviewImplementationSection section
    ) {
        return switch (section) {
            case OperationOverviewImplementationAvailable available -> {
                var implementation = available.implementation();
                yield new OperationOverviewImplementationAvailableView(
                        ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState.AVAILABLE,
                        new ImplementationPathView(implementation.controllerName(), implementation.serviceName(),
                                implementation.repositoryName()));
            }
            case OperationOverviewImplementationIncomplete ignored ->
                    new OperationOverviewImplementationIncompleteView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState.INCOMPLETE);
            case OperationOverviewImplementationAmbiguous ignored ->
                    new OperationOverviewImplementationAmbiguousView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewImplementationState.AMBIGUOUS);
        };
    }

    private OperationOverviewEventPathSectionView mapEventPath(OperationOverviewEventPathSection section) {
        return switch (section) {
            case OperationOverviewEventPathAvailable available -> new OperationOverviewEventPathAvailableView(
                    ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState.AVAILABLE,
                    eventPathMapper.mapPath(available.path()));
            case OperationOverviewEventPathNotApplicable ignored ->
                    new OperationOverviewEventPathNotApplicableView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState.NOT_APPLICABLE);
            case OperationOverviewEventPathIncomplete ignored ->
                    new OperationOverviewEventPathIncompleteView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState.INCOMPLETE);
            case OperationOverviewEventPathAmbiguous ignored ->
                    new OperationOverviewEventPathAmbiguousView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewEventPathState.AMBIGUOUS);
        };
    }

    private OperationOverviewVerificationSectionView mapVerification(
            OperationOverviewIdentity identity,
            OperationOverviewVerificationSection section
    ) {
        return switch (section) {
            case OperationOverviewVerificationAvailable available -> new OperationOverviewVerificationAvailableView(
                    ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationState.AVAILABLE,
                    operationTestsMapper.map(identity.projectId(), identity.operationId(),
                            available.verificationStatus(), available.testCount(), available.checkCount(),
                            available.tests()));
            case OperationOverviewVerificationAmbiguous ignored ->
                    new OperationOverviewVerificationAmbiguousView(
                            ru.kuznetsov.qaip.explorer.view.OperationOverviewVerificationState.AMBIGUOUS);
        };
    }
}
