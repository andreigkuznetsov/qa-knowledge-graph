package ru.kuznetsov.qaip.core.application.query.relationship;

import ru.kuznetsov.qaip.core.domain.Relationship;

import java.util.List;

public record ProjectRelationships(List<Relationship> incoming, List<Relationship> outgoing) {
    public ProjectRelationships {
        incoming = List.copyOf(incoming);
        outgoing = List.copyOf(outgoing);
    }
}
