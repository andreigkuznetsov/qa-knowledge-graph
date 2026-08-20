package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;

/** Approved finite support/integrity decision and authoritative composer binding. */
public final class ScenarioOccurrenceCompositionAttemptV1 {
    private ScenarioOccurrenceCompositionAttemptV1() {}
    public static ScenarioOccurrenceCompositionOutcomeV1 attemptScenarioOccurrenceCompositionV1(NormalizedScenarioOccurrenceInputV1 o) {
        validateOccurrence(o); // precedence stage 1: never classified unavailable
        if (!supported(o)) return new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(o,
                ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT);
        try {
            ScenarioSemanticCompositionRequest request = ScenarioOccurrenceCorrespondenceValidatorV1.validateAndConstruct(o);
            ScenarioSemanticCompositionResult result;
            try {
                result = ScenarioSemanticFingerprintComposer.compose(request);
            } catch (ScenarioSemanticCompositionException exception) {
                throw mapComposerRejection(exception);
            }
            return new ScenarioOccurrenceCompositionOutcomeV1.Composed(o, request, result);
        } catch (ScenarioOccurrenceIntegrityExceptionV1 rejection) {
            return new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(o,
                    ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE);
        }
    }

    /** Recomputes and compares the complete proof; used before any outcome enters a group. */
    static void revalidateProof(ScenarioOccurrenceCompositionOutcomeV1 claimed) {
        var verified=attemptScenarioOccurrenceCompositionV1(claimed.occurrence());
        if(claimed instanceof ScenarioOccurrenceCompositionOutcomeV1.Unavailable unavailable){
            if(!(verified instanceof ScenarioOccurrenceCompositionOutcomeV1.Unavailable v)||v.reason()!=unavailable.reason())
                throw new IllegalArgumentException("unavailable outcome is not the authoritative attempt result");
            return;
        }
        if(!(verified instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed v))throw new IllegalArgumentException("composed outcome is not authoritative");
        var c=(ScenarioOccurrenceCompositionOutcomeV1.Composed)claimed;
        if(!sameRequest(c.request(),v.request())||!c.composition().equals(v.composition()))
            throw new IllegalArgumentException("composed request, leaf attestations, or Scenario proof was substituted");
    }

    private static boolean sameRequest(ScenarioSemanticCompositionRequest a,ScenarioSemanticCompositionRequest b){
        return a.parentIdentity().equals(b.parentIdentity())&&a.exactTitle().equals(b.exactTitle())
                &&sameSteps(a.givenSteps(),b.givenSteps())&&sameSteps(a.whenSteps(),b.whenSteps())&&sameSteps(a.thenSteps(),b.thenSteps())
                &&a.operationReference().input().equals(b.operationReference().input())&&a.operationReference().fingerprint().equals(b.operationReference().fingerprint())
                &&sameRules(a.businessRuleReferences(),b.businessRuleReferences())
                &&a.scenarioSemanticCanonicalizationVersion().equals(b.scenarioSemanticCanonicalizationVersion())
                &&a.sourceNormalizationVersion().equals(b.sourceNormalizationVersion())&&a.scenarioSemanticContractVersion().equals(b.scenarioSemanticContractVersion());
    }
    private static boolean sameSteps(List<FingerprintStepAttestation>a,List<FingerprintStepAttestation>b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++)if(!a.get(i).input().equals(b.get(i).input())||!a.get(i).fingerprint().equals(b.get(i).fingerprint()))return false;return true;}
    private static boolean sameRules(List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference>a,List<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference>b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++){var x=a.get(i);var y=b.get(i);if(!x.authoredPosition().equals(y.authoredPosition())||!x.attestation().input().equals(y.attestation().input())||!x.attestation().fingerprint().equals(y.attestation().fingerprint()))return false;}return true;}
    private static ScenarioOccurrenceIntegrityExceptionV1 mapComposerRejection(ScenarioSemanticCompositionException e){var reason=switch(e.code()){
        case STEP_ATTESTATION_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.STEP_ATTESTATION_INPUT_FINGERPRINT_MISMATCH;
        case STEP_SCENARIO_IDENTITY_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.STEP_IDENTITY_MISMATCH;
        case STEP_PHASE_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.STEP_PHASE_MISMATCH;
        case STEP_ORDINAL_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.STEP_ORDINAL_MISMATCH;
        case OPERATION_REFERENCE_ATTESTATION_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.OPERATION_ATTESTATION_INPUT_FINGERPRINT_MISMATCH;
        case OPERATION_REFERENCE_SCENARIO_IDENTITY_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.OPERATION_OWNERSHIP_MISMATCH;
        case BUSINESS_RULE_REFERENCE_ATTESTATION_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_ATTESTATION_INPUT_FINGERPRINT_MISMATCH;
        case BUSINESS_RULE_REFERENCE_SCENARIO_IDENTITY_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_OWNERSHIP_MISMATCH;
        case BUSINESS_RULE_REFERENCE_POSITION_MISMATCH->ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_ORDER_POSITION_MISMATCH;
        case UNSUPPORTED_CONTRACT->throw e;};return new ScenarioOccurrenceIntegrityExceptionV1(reason);}
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
    private static boolean eq(String a,String b){return b.equals(a);} @SafeVarargs private static <T> List<T> concat(List<T>...ls){return java.util.Arrays.stream(ls).flatMap(List::stream).toList();}
}
