package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.math.BigInteger;
import java.util.List;

/** Approved finite support/integrity decision and authoritative composer binding. */
public final class ScenarioOccurrenceCompositionAttemptV1 {
    private ScenarioOccurrenceCompositionAttemptV1() {}
    public static ScenarioOccurrenceCompositionOutcomeV1 attemptScenarioOccurrenceCompositionV1(NormalizedScenarioOccurrenceInputV1 o) {
        validateOccurrence(o); // precedence stage 1: never classified unavailable
        if (!supported(o)) return new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(o,
                ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT);
        try {
            ScenarioSemanticCompositionRequest request = request(o);
            ScenarioSemanticCompositionResult result = ScenarioSemanticFingerprintComposer.compose(request);
            return new ScenarioOccurrenceCompositionOutcomeV1.Composed(o, request, result);
        } catch (IllegalArgumentException e) {
            return new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(o,
                    ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE);
        }
    }
    private static void validateOccurrence(NormalizedScenarioOccurrenceInputV1 o) {
        if (o == null) throw new NullPointerException("occurrence");
        var id=o.occurrenceIdentity(); var m=id.manifest(); var p=o.parentMember();
        if (!NormalizedScenarioOccurrenceInputV1.MANIFEST_OCCURRENCE_IDENTITY_VERSION.equals(m.identityVersion())
                || !NormalizedScenarioOccurrenceInputV1.DECLARATION_OCCURRENCE_IDENTITY_VERSION.equals(id.identityVersion())
                || !id.structuralPath().matches("/scenarios/(0|[1-9][0-9]*)") || !id.structuralPath().equals(o.structuralLocation()))
            throw new IllegalArgumentException("invalid occurrence identity or structural location");
        if (!m.parentSourceId().equals(p.parentSourceId()) || !m.parentSnapshotId().equals(p.parentSnapshotId())
                || !m.parentFingerprint().equals(p.parentContentFingerprint()) || !m.memberPath().equals(p.normalizedRepositoryRelativePath()))
            throw new IllegalArgumentException("occurrence/parent-member mismatch");
        var a=o.repositoryCaptureAttestation();
        if (!a.sourceId().equals(p.parentSourceId()) || !a.snapshotId().equals(p.parentSnapshotId())
                || !a.contentFingerprint().equals(p.parentContentFingerprint()) || !a.regularMembers().contains(p))
            throw new IllegalArgumentException("parent member is not verified by Repository Capture");
    }
    private static boolean supported(NormalizedScenarioOccurrenceInputV1 o) {
        boolean ok = eq(o.claimedIdentity().identitySchemeVersion(), ScenarioSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION)
                & eq(o.sourceNormalizationVersion(), ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION)
                & eq(o.scenarioSemanticCanonicalizationVersion(), ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER)
                & eq(o.scenarioSemanticContractVersion(), ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION);
        for (var s : concat(o.givenSteps(),o.whenSteps(),o.thenSteps())) ok &= eq(s.claimedIdentity().identitySchemeVersion(), StepSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION)
                & eq(s.identityVersion(),StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION) & eq(s.semanticCanonicalizationVersion(),StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        var op=o.operationReference(); ok &= eq(op.claimedIdentity().identitySchemeVersion(),HttpOperationReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION)
                & eq(op.role(),HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE) & eq(op.datumIdentityVersion(),HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION)
                & eq(op.targetProfile(),HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE) & eq(op.semanticCanonicalizationVersion(),HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        for(var r:o.businessRuleReferences()) ok &= eq(r.claimedIdentity().identitySchemeVersion(),BusinessRuleReferenceSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION)
                & eq(r.identityScheme(),BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_IDENTITY_SCHEME) & eq(r.datumIdentityVersion(),BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION)
                & eq(r.semanticCanonicalizationVersion(),BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        return ok;
    }
    private static ScenarioSemanticCompositionRequest request(NormalizedScenarioOccurrenceInputV1 o) {
        var given=steps(o,o.authoredGiven(),o.givenSteps(),StepSemanticFingerprintInput.Phase.GIVEN);
        var when=steps(o,o.authoredWhen(),o.whenSteps(),StepSemanticFingerprintInput.Phase.WHEN);
        var then=steps(o,o.authoredThen(),o.thenSteps(),StepSemanticFingerprintInput.Phase.THEN);
        var op=o.operationReference(); require(op.claimedIdentity().equals(o.claimedIdentity()));
        var opa=FingerprintOperationReferenceAttestation.create(new HttpOperationReferenceSemanticFingerprintInput(op.claimedIdentity().authority(),op.claimedIdentity().scenarioKey(),op.claimedIdentity().identitySchemeVersion(),op.role(),op.datumIdentityVersion(),op.targetProfile(),op.method(),op.path(),op.semanticCanonicalizationVersion()));
        var rules=new java.util.ArrayList<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference>(); int i=0;
        for(var r:o.businessRuleReferences()){require(r.authoredPosition().equals(BigInteger.valueOf(i++)) && r.claimedIdentity().equals(o.claimedIdentity()));
            rules.add(new ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference(r.authoredPosition(),FingerprintBusinessRuleReferenceAttestation.create(new BusinessRuleReferenceSemanticFingerprintInput(r.claimedIdentity().authority(),r.claimedIdentity().scenarioKey(),r.claimedIdentity().identitySchemeVersion(),r.referencedAuthority(),r.stableRuleKey(),r.identityScheme(),r.datumIdentityVersion(),r.semanticCanonicalizationVersion()))));}
        return new ScenarioSemanticCompositionRequest(o.claimedIdentity(),o.exactTitle(),given,when,then,opa,rules,o.scenarioSemanticCanonicalizationVersion(),o.sourceNormalizationVersion(),o.scenarioSemanticContractVersion());
    }
    private static List<FingerprintStepAttestation> steps(NormalizedScenarioOccurrenceInputV1 o,List<String> authored,List<NormalizedScenarioOccurrenceInputV1.NormalizedStep> values,StepSemanticFingerprintInput.Phase phase){
        require(authored.size()==values.size()); var out=new java.util.ArrayList<FingerprintStepAttestation>();
        for(int i=0;i<values.size();i++){var s=values.get(i);require(s.claimedIdentity().equals(o.claimedIdentity())&&s.phase()==phase&&s.ordinal().equals(BigInteger.valueOf(i))&&s.exactAuthoredText().equals(authored.get(i)));
            out.add(FingerprintStepAttestation.create(new StepSemanticFingerprintInput(s.claimedIdentity().authority(),s.claimedIdentity().scenarioKey(),s.claimedIdentity().identitySchemeVersion(),s.phase(),s.ordinal(),s.identityVersion(),s.exactAuthoredText(),s.semanticCanonicalizationVersion())));}
        return List.copyOf(out);
    }
    private static void require(boolean v){if(!v)throw new IllegalArgumentException("occurrence composition correspondence failure");}
    private static boolean eq(String a,String b){return b.equals(a);} @SafeVarargs private static <T> List<T> concat(List<T>...ls){return java.util.Arrays.stream(ls).flatMap(List::stream).toList();}
}
