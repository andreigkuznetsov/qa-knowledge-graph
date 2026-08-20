package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
/** Factory-controlled authoritative semantic availability of one admitted Manifest. */
public sealed interface ManifestSemanticCompositionOutcomeV1 {
    VerifiedAdmittedManifestV1 manifest(); List<ScenarioOccurrenceCompositionOutcomeV1> childOutcomes();
    enum UnavailableReason { SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE }
    final class Composed implements ManifestSemanticCompositionOutcomeV1 {
        private final VerifiedAdmittedManifestV1 manifest; private final List<ScenarioOccurrenceCompositionOutcomeV1> children;
        private final List<ScenarioSemanticFingerprint> scenarioFingerprints; private final ManifestSemanticFingerprint fingerprint;
        Composed(VerifiedAdmittedManifestV1 m,List<ScenarioOccurrenceCompositionOutcomeV1> c,List<ScenarioSemanticFingerprint> s,ManifestSemanticFingerprint f){
            manifest=Objects.requireNonNull(m);children=List.copyOf(c);scenarioFingerprints=List.copyOf(s);fingerprint=Objects.requireNonNull(f);}
        public VerifiedAdmittedManifestV1 manifest(){return manifest;} public List<ScenarioOccurrenceCompositionOutcomeV1> childOutcomes(){return children;}
        public List<ScenarioSemanticFingerprint> scenarioFingerprints(){return scenarioFingerprints;} public ManifestSemanticFingerprint fingerprint(){return fingerprint;}
    }
    final class Unavailable implements ManifestSemanticCompositionOutcomeV1 {
        private final VerifiedAdmittedManifestV1 manifest; private final List<ScenarioOccurrenceCompositionOutcomeV1> children; private final UnavailableReason reason;
        Unavailable(VerifiedAdmittedManifestV1 m,List<ScenarioOccurrenceCompositionOutcomeV1> c){manifest=Objects.requireNonNull(m);children=List.copyOf(c);reason=UnavailableReason.SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE;}
        public VerifiedAdmittedManifestV1 manifest(){return manifest;} public List<ScenarioOccurrenceCompositionOutcomeV1> childOutcomes(){return children;} public UnavailableReason reason(){return reason;}
    }
}
