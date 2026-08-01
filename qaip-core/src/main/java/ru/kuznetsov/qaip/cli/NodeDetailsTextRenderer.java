package ru.kuznetsov.qaip.cli;

import ru.kuznetsov.qaip.core.application.query.nodedetails.NodeDetailsResult;

import java.util.Objects;

final class NodeDetailsTextRenderer {
    String renderFound(String projectId, NodeDetailsResult details) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(details, "details");
        return String.join(System.lineSeparator(),
                "Node Details",
                "Project ID: " + projectId,
                "Node ID: " + details.nodeId(),
                "Type: " + details.nodeType(),
                "Name: " + details.name(),
                "Description: " + optional(details.description()),
                "Status: " + optional(details.status()));
    }

    private static String optional(String value) {
        return value == null ? "not specified" : value;
    }
}
