package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathStep;
import ru.kuznetsov.qaip.explorer.view.EventPathStepView;
import ru.kuznetsov.qaip.explorer.view.EventPathView;

import java.util.Objects;

public final class EventPathViewMapper {
    public EventPathView map(EventPathFound found) {
        var path = Objects.requireNonNull(found, "found").path();
        return new EventPathView(
                path.projectId(),
                path.operationId(),
                path.pathKind().name(),
                path.steps().stream().map(EventPathViewMapper::mapStep).toList());
    }

    private static EventPathStepView mapStep(EventPathStep step) {
        return new EventPathStepView(
                step.implementationRole().name(),
                step.name(),
                step.implementationType().name(),
                step.technology());
    }
}
