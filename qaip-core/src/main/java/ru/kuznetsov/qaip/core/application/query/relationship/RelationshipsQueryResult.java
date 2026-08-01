package ru.kuznetsov.qaip.core.application.query.relationship;

public sealed interface RelationshipsQueryResult
        permits RelationshipsFound, RelationshipsProjectNotFound, RelationshipsNodeNotFound {
}
