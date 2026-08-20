package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.Objects;
/** Internal typed signal for one explicitly approved finite V1 integrity rejection. */
final class ScenarioOccurrenceIntegrityExceptionV1 extends RuntimeException {
    private final ScenarioOccurrenceIntegrityRejectionV1 rejection;
    ScenarioOccurrenceIntegrityExceptionV1(ScenarioOccurrenceIntegrityRejectionV1 rejection){super(Objects.requireNonNull(rejection).name());this.rejection=rejection;}
    ScenarioOccurrenceIntegrityRejectionV1 rejection(){return rejection;}
}
