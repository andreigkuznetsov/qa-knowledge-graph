package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Explicit finite V1 occurrence-to-composition correspondence validator. */
public final class ScenarioOccurrenceCorrespondenceValidatorV1 {
    private ScenarioOccurrenceCorrespondenceValidatorV1() {}

    public static ScenarioSemanticCompositionRequest validateAndConstruct(NormalizedScenarioOccurrenceInputV1 o) {
        var given=steps(o,o.authoredGiven(),o.givenSteps(),StepSemanticFingerprintInput.Phase.GIVEN);
        var when=steps(o,o.authoredWhen(),o.whenSteps(),StepSemanticFingerprintInput.Phase.WHEN);
        var then=steps(o,o.authoredThen(),o.thenSteps(),StepSemanticFingerprintInput.Phase.THEN);
        var op=o.operationReference();
        reject(!op.claimedIdentity().equals(o.claimedIdentity()),ScenarioOccurrenceIntegrityRejectionV1.OPERATION_OWNERSHIP_MISMATCH);
        reject(op.method().isEmpty()||op.path().isEmpty(),ScenarioOccurrenceIntegrityRejectionV1.OPERATION_METHOD_PATH_MISMATCH);
        var operation=FingerprintOperationReferenceAttestation.create(new HttpOperationReferenceSemanticFingerprintInput(
                op.claimedIdentity().authority(),op.claimedIdentity().scenarioKey(),op.claimedIdentity().identitySchemeVersion(),
                op.role(),op.datumIdentityVersion(),op.targetProfile(),op.method(),op.path(),op.semanticCanonicalizationVersion()));
        var rules=new ArrayList<ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference>();
        for(int i=0;i<o.businessRuleReferences().size();i++){
            var r=o.businessRuleReferences().get(i);
            reject(!r.authoredPosition().equals(BigInteger.valueOf(i)),ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_ORDER_POSITION_MISMATCH);
            reject(!r.claimedIdentity().equals(o.claimedIdentity()),ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_OWNERSHIP_MISMATCH);
            reject(r.referencedAuthority().isEmpty()||r.stableRuleKey().isEmpty(),ScenarioOccurrenceIntegrityRejectionV1.BUSINESS_RULE_AUTHORITY_KEY_SCHEME_MISMATCH);
            var attestation=FingerprintBusinessRuleReferenceAttestation.create(new BusinessRuleReferenceSemanticFingerprintInput(
                    r.claimedIdentity().authority(),r.claimedIdentity().scenarioKey(),r.claimedIdentity().identitySchemeVersion(),
                    r.referencedAuthority(),r.stableRuleKey(),r.identityScheme(),r.datumIdentityVersion(),r.semanticCanonicalizationVersion()));
            rules.add(new ScenarioSemanticCompositionRequest.PositionedBusinessRuleReference(r.authoredPosition(),attestation));
        }
        return new ScenarioSemanticCompositionRequest(o.claimedIdentity(),o.exactTitle(),given,when,then,operation,rules,
                o.scenarioSemanticCanonicalizationVersion(),o.sourceNormalizationVersion(),o.scenarioSemanticContractVersion());
    }

    private static List<FingerprintStepAttestation> steps(NormalizedScenarioOccurrenceInputV1 o,List<String> authored,
            List<NormalizedScenarioOccurrenceInputV1.NormalizedStep> values,StepSemanticFingerprintInput.Phase phase){
        reject(authored.size()!=values.size(),ScenarioOccurrenceIntegrityRejectionV1.STEP_COUNT_MISMATCH);
        var result=new ArrayList<FingerprintStepAttestation>();
        for(int i=0;i<values.size();i++){
            var s=values.get(i);
            reject(!s.claimedIdentity().equals(o.claimedIdentity()),ScenarioOccurrenceIntegrityRejectionV1.STEP_IDENTITY_MISMATCH);
            reject(s.phase()!=phase,ScenarioOccurrenceIntegrityRejectionV1.STEP_PHASE_MISMATCH);
            reject(!s.ordinal().equals(BigInteger.valueOf(i)),ScenarioOccurrenceIntegrityRejectionV1.STEP_ORDINAL_MISMATCH);
            reject(!s.exactAuthoredText().equals(authored.get(i)),ScenarioOccurrenceIntegrityRejectionV1.STEP_TEXT_MISMATCH);
            result.add(FingerprintStepAttestation.create(new StepSemanticFingerprintInput(s.claimedIdentity().authority(),
                    s.claimedIdentity().scenarioKey(),s.claimedIdentity().identitySchemeVersion(),s.phase(),s.ordinal(),
                    s.identityVersion(),s.exactAuthoredText(),s.semanticCanonicalizationVersion())));
        }
        return List.copyOf(result);
    }
    private static void reject(boolean condition,ScenarioOccurrenceIntegrityRejectionV1 reason){if(condition)throw new ScenarioOccurrenceIntegrityExceptionV1(reason);}
}
