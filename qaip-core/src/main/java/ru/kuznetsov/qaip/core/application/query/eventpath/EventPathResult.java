package ru.kuznetsov.qaip.core.application.query.eventpath;

import java.util.List;
import java.util.Objects;

public record EventPathResult(
        String projectId,
        String operationId,
        EventPathKind pathKind,
        List<EventPathStep> steps
) {
    private static final List<EventPathImplementationRole> EVENT_DRIVEN_ROLES = List.of(
            EventPathImplementationRole.REST_CONTROLLER,
            EventPathImplementationRole.MESSAGE_PRODUCER,
            EventPathImplementationRole.MESSAGE_DESTINATION,
            EventPathImplementationRole.MESSAGE_CONSUMER,
            EventPathImplementationRole.APPLICATION_SERVICE,
            EventPathImplementationRole.REPOSITORY);

    public EventPathResult {
        projectId = EventPathProjectNotFound.requireId(projectId, "projectId");
        operationId = EventPathProjectNotFound.requireId(operationId, "operationId");
        Objects.requireNonNull(pathKind, "pathKind");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        List<EventPathImplementationRole> actualRoles = steps.stream()
                .map(EventPathStep::implementationRole).toList();
        if (!EVENT_DRIVEN_ROLES.equals(actualRoles)) {
            throw new IllegalArgumentException("steps must contain the complete ordered event-driven role sequence");
        }
    }
}
