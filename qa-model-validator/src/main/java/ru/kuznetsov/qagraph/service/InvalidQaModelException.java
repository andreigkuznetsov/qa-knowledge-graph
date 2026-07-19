package ru.kuznetsov.qagraph.service;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

public class InvalidQaModelException extends RuntimeException {

    private final QaModelValidationResult validationResult;

    public InvalidQaModelException(
            QaModelValidationResult validationResult
    ) {
        super("QA model is invalid");
        this.validationResult = validationResult;
    }

    public QaModelValidationResult validationResult() {
        return validationResult;
    }
}
