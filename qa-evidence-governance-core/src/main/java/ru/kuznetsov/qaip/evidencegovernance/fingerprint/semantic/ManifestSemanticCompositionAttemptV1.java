package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.ArrayList; import java.util.List;
/** Approved precedence, proof revalidation, and authoritative Manifest outcome decision. */
public final class ManifestSemanticCompositionAttemptV1 {
    private ManifestSemanticCompositionAttemptV1(){}
    public static ManifestSemanticCompositionOutcomeV1 attemptManifestSemanticCompositionV1(NormalizedManifestSemanticCompositionInputV1 input){
        validateAdmission(input); // stage 1
        validateSupport(input); // stage 2
        List<ScenarioOccurrenceCompositionOutcomeV1> children=validateChildren(input); // stage 3
        if(children.stream().anyMatch(ScenarioOccurrenceCompositionOutcomeV1.Unavailable.class::isInstance))
            return new ManifestSemanticCompositionOutcomeV1.Unavailable(input.manifest(),children); // stage 4
        var composed=new ArrayList<ScenarioOccurrenceCompositionOutcomeV1.Composed>(children.size());
        var fps=new ArrayList<ScenarioSemanticFingerprint>(children.size());
        for(var child:children){var c=(ScenarioOccurrenceCompositionOutcomeV1.Composed)child;composed.add(c);fps.add(c.fingerprint());}
        ManifestSemanticFingerprint fingerprint=ManifestSemanticFingerprintComposerV1.composeManifestSemanticFingerprintV1(input.manifest(),composed); // stage 5
        return new ManifestSemanticCompositionOutcomeV1.Composed(input.manifest(),children,fps,fingerprint); // stage 6
    }
    private static void validateAdmission(NormalizedManifestSemanticCompositionInputV1 input){
        if(input==null)throw new NullPointerException("input");var m=input.manifest();
        if(!m.capture().regularMembers().contains(m.parentMember()))throw new IllegalArgumentException("Manifest parent is not capture-attested");
    }
    private static void validateSupport(NormalizedManifestSemanticCompositionInputV1 i){
        boolean ok=NormalizedManifestSemanticCompositionInputV1.INPUT_VERSION.equals(i.inputVersion())
                &NormalizedManifestSemanticCompositionInputV1.OUTCOME_CONTRACT_VERSION.equals(i.outcomeContractVersion())
                &NormalizedManifestSemanticCompositionInputV1.ATTEMPT_VERSION.equals(i.attemptVersion())
                &NormalizedManifestSemanticCompositionInputV1.OUTCOME_VOCABULARY_VERSION.equals(i.outcomeVocabularyVersion())
                &NormalizedManifestSemanticCompositionInputV1.REASON_VOCABULARY_VERSION.equals(i.unavailableReasonVocabularyVersion())
                &ManifestSemanticFingerprintEncoder.MANIFEST_FORMAT_IDENTIFIER.equals(i.manifest().format())
                &ManifestSemanticFingerprintEncoder.SCHEMA_VERSION.equals(i.manifest().schemaVersion())
                &ManifestSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION.equals(i.manifest().scenarioIdentityScheme());
        if(!ok)throw new ManifestCompositionRejectionV1(ManifestCompositionRejectionV1.Code.UNSUPPORTED_MANIFEST_CONTRACT);
    }
    private static List<ScenarioOccurrenceCompositionOutcomeV1> validateChildren(NormalizedManifestSemanticCompositionInputV1 i){
        var expected=i.manifest().authoredScenarios();var supplied=i.candidateChildOutcomes();
        if(expected.size()!=supplied.size())throw new ManifestCompositionRejectionV1(ManifestCompositionRejectionV1.Code.SCENARIO_COUNT_MISMATCH);
        for(int x=0;x<expected.size();x++){
            var child=supplied.get(x);
            if(!child.occurrence().equals(expected.get(x)))throw new ManifestCompositionRejectionV1(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION);
            ScenarioOccurrenceCompositionAttemptV1.revalidateProof(child);
        }
        return List.copyOf(supplied);
    }
}
