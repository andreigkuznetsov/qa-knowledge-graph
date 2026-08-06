package ru.kuznetsov.qaip.explorer.adapter.runtime;

import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathAmbiguous;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathIncomplete;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathNotEventDriven;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathOperationNotFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathProjectNotFound;
import ru.kuznetsov.qaip.core.application.query.eventpath.EventPathQuery;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionAmbiguous;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionIncomplete;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionNotEventDriven;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionOperationNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionProjectNotFound;
import ru.kuznetsov.qaip.explorer.application.EventPathProjectionResult;
import ru.kuznetsov.qaip.explorer.application.GetEventPathService;

import java.util.Objects;

public final class RuntimeEventPathProjectionService implements GetEventPathService {
    private final EventPathQuery query;
    private final EventPathViewMapper mapper;

    public RuntimeEventPathProjectionService(EventPathQuery query, EventPathViewMapper mapper) {
        this.query = Objects.requireNonNull(query, "query");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public EventPathProjectionResult getEventPath(String repositoryId, String operationId) {
        var result = query.execute(repositoryId, operationId);
        return switch (result) {
            case EventPathFound found -> new EventPathProjectionFound(mapper.map(found));
            case EventPathProjectNotFound notFound ->
                    new EventPathProjectionProjectNotFound(notFound.projectId());
            case EventPathOperationNotFound notFound ->
                    new EventPathProjectionOperationNotFound(notFound.projectId(), notFound.operationId());
            case EventPathIncomplete incomplete ->
                    new EventPathProjectionIncomplete(incomplete.projectId(), incomplete.operationId());
            case EventPathAmbiguous ambiguous ->
                    new EventPathProjectionAmbiguous(ambiguous.projectId(), ambiguous.operationId());
            case EventPathNotEventDriven notEventDriven ->
                    new EventPathProjectionNotEventDriven(
                            notEventDriven.projectId(), notEventDriven.operationId());
        };
    }
}
