package ru.kuznetsov.qaip.core.application.query.eventpath;

public sealed interface EventPathQueryResult permits EventPathFound, EventPathProjectNotFound,
        EventPathOperationNotFound, EventPathIncomplete, EventPathAmbiguous, EventPathNotEventDriven { }
