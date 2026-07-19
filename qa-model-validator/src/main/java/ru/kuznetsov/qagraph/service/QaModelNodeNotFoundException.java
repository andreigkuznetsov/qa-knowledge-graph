package ru.kuznetsov.qagraph.service;

public class QaModelNodeNotFoundException extends RuntimeException {

    private final String nodeId;

    public QaModelNodeNotFoundException(String nodeId) {
        super("QA model node not found: " + nodeId);
        this.nodeId = nodeId;
    }

    public String nodeId() {
        return nodeId;
    }
}
