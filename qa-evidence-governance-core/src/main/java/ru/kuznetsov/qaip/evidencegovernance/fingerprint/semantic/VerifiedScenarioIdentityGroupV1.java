package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
/** Immutable factory-only authoritative group; state is always derived by Evidence Governance. */
public final class VerifiedScenarioIdentityGroupV1 {
    public enum State { UNIQUE,DUPLICATE_EQUIVALENT,DUPLICATE_CONFLICTING,DUPLICATE_UNCLASSIFIED }
    private final ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity identity; private final State state;
    private final List<ScenarioOccurrenceCompositionOutcomeV1> occurrences; private final ScenarioIdentityGroupFingerprint fingerprint;
    VerifiedScenarioIdentityGroupV1(ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity i,State s,List<ScenarioOccurrenceCompositionOutcomeV1> o,ScenarioIdentityGroupFingerprint f){identity=Objects.requireNonNull(i);state=Objects.requireNonNull(s);occurrences=List.copyOf(o);fingerprint=Objects.requireNonNull(f);}
    public ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity claimedIdentity(){return identity;} public State state(){return state;}
    public List<ScenarioOccurrenceCompositionOutcomeV1> occurrences(){return occurrences;} public ScenarioIdentityGroupFingerprint fingerprint(){return fingerprint;}
}
