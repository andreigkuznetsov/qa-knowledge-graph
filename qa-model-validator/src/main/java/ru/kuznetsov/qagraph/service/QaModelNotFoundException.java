package ru.kuznetsov.qagraph.service;

public class QaModelNotFoundException extends RuntimeException {

    public QaModelNotFoundException(String modelId) {
        super("QA model not found: " + modelId);
    }
}
