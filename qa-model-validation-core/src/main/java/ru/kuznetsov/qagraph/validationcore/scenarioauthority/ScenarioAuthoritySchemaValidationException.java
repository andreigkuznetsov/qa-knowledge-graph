package ru.kuznetsov.qagraph.validationcore.scenarioauthority;

/** Fail-closed inability to provide the pinned deterministic V1 validation primitive. */
public final class ScenarioAuthoritySchemaValidationException extends RuntimeException {
    public ScenarioAuthoritySchemaValidationException(String message) { super(message); }
    public ScenarioAuthoritySchemaValidationException(String message, Throwable cause) { super(message, cause); }
}
