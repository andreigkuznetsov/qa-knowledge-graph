package ru.kuznetsov.qaip.core.application.query.relationship;

import java.util.List;

public record RelationshipDetailsResult(
        List<RelationshipDetails> incoming,
        List<RelationshipDetails> outgoing) {

    public RelationshipDetailsResult {
        incoming = List.copyOf(incoming);
        outgoing = List.copyOf(outgoing);
    }
}
