package ru.kuznetsov.qaip.simulation.error;

import ru.kuznetsov.qagraph.validationcore.model.QaModelValidationResult;

import java.util.List;
import java.util.Objects;

public class SimulationException extends IllegalArgumentException {
    private final SimulationErrorCode code;
    private final String taskId;
    private final String nodeId;
    private final QaModelValidationResult validation;

    public SimulationException(SimulationErrorCode code, String message) {
        this(code, message, null, null, null);
    }

    public SimulationException(
            SimulationErrorCode code,
            String message,
            String taskId,
            String nodeId,
            QaModelValidationResult validation
    ) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.taskId = taskId;
        this.nodeId = nodeId;
        this.validation = immutableValidation(validation);
    }

    public SimulationErrorCode code() { return code; }
    public String taskId() { return taskId; }
    public String nodeId() { return nodeId; }
    public QaModelValidationResult validation() { return validation; }

    private static QaModelValidationResult immutableValidation(
            QaModelValidationResult validation
    ) {
        if (validation == null) {
            return null;
        }
        return new QaModelValidationResult(
                validation.valid(),
                validation.schemaVersion(),
                validation.summary(),
                List.copyOf(validation.issues())
        );
    }
}
