package ru.kuznetsov.qaip.impact.service;

public class ImpactAnalysisException extends IllegalArgumentException {

    private final ImpactAnalysisErrorCode code;

    public ImpactAnalysisException(
            ImpactAnalysisErrorCode code,
            String message
    ) {
        super(message);
        this.code = code;
    }

    public ImpactAnalysisErrorCode code() {
        return code;
    }
}
