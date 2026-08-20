package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Factory-controlled result of one authoritative occurrence attempt. */
public sealed interface ScenarioOccurrenceCompositionOutcomeV1 {
    NormalizedScenarioOccurrenceInputV1 occurrence();
    enum UnavailableReason { UNSUPPORTED_SEMANTIC_CONTRACT, SCENARIO_COMPOSITION_INTEGRITY_FAILURE }
    final class Composed implements ScenarioOccurrenceCompositionOutcomeV1 {
        private final NormalizedScenarioOccurrenceInputV1 occurrence; private final ScenarioSemanticCompositionRequest request;
        private final ScenarioSemanticCompositionResult result;
        Composed(NormalizedScenarioOccurrenceInputV1 occurrence, ScenarioSemanticCompositionRequest request, ScenarioSemanticCompositionResult result) {
            this.occurrence=Objects.requireNonNull(occurrence); this.request=Objects.requireNonNull(request); this.result=Objects.requireNonNull(result); }
        public NormalizedScenarioOccurrenceInputV1 occurrence(){return occurrence;} public ScenarioSemanticCompositionRequest request(){return request;}
        public ScenarioSemanticCompositionResult composition(){return result;} public ScenarioSemanticFingerprint fingerprint(){return result.fingerprint();}
    }
    final class Unavailable implements ScenarioOccurrenceCompositionOutcomeV1 {
        private final NormalizedScenarioOccurrenceInputV1 occurrence; private final UnavailableReason reason;
        Unavailable(NormalizedScenarioOccurrenceInputV1 occurrence, UnavailableReason reason){this.occurrence=Objects.requireNonNull(occurrence);this.reason=Objects.requireNonNull(reason);}
        public NormalizedScenarioOccurrenceInputV1 occurrence(){return occurrence;} public UnavailableReason reason(){return reason;}
    }
}
