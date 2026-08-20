package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
/** Finite input to the authoritative Manifest semantic attempt. */
public record NormalizedManifestSemanticCompositionInputV1(String inputVersion,String outcomeContractVersion,
        String attemptVersion,String outcomeVocabularyVersion,String unavailableReasonVocabularyVersion,
        VerifiedAdmittedManifestV1 manifest,List<ScenarioOccurrenceCompositionOutcomeV1> candidateChildOutcomes){
    public static final String INPUT_VERSION="scenario-authority-normalized-manifest-semantic-composition-input-v1";
    public static final String OUTCOME_CONTRACT_VERSION="scenario-authority-manifest-semantic-outcome-v1";
    public static final String ATTEMPT_VERSION="scenario-authority-manifest-semantic-composition-attempt-v1";
    public static final String OUTCOME_VOCABULARY_VERSION="scenario-authority-manifest-semantic-outcome-vocabulary-v1";
    public static final String REASON_VOCABULARY_VERSION="scenario-authority-manifest-semantic-unavailable-reason-v1";
    public NormalizedManifestSemanticCompositionInputV1 { Objects.requireNonNull(inputVersion);Objects.requireNonNull(outcomeContractVersion);
        Objects.requireNonNull(attemptVersion);Objects.requireNonNull(outcomeVocabularyVersion);Objects.requireNonNull(unavailableReasonVocabularyVersion);
        Objects.requireNonNull(manifest);candidateChildOutcomes=List.copyOf(Objects.requireNonNull(candidateChildOutcomes)); }
    public static NormalizedManifestSemanticCompositionInputV1 selectedV1(VerifiedAdmittedManifestV1 m,List<ScenarioOccurrenceCompositionOutcomeV1> c){
        return new NormalizedManifestSemanticCompositionInputV1(INPUT_VERSION,OUTCOME_CONTRACT_VERSION,ATTEMPT_VERSION,OUTCOME_VOCABULARY_VERSION,REASON_VOCABULARY_VERSION,m,c);}
}
