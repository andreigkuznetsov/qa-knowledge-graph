package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import java.math.BigInteger;
import java.util.*;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

/** Data-driven normative V1 support, integrity, authority, state, and version corpus. */
class ScenarioIdentityGroupV1ConformanceTest {
    private static final ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity FOREIGN =
            new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity("foreign","checkout","qaip-scenario-identity-v1");

    @TestFactory Collection<DynamicTest> everySupportTableRowClassifiesUnsupported() {
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var base=withRule(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"base"));
        Map<String,UnaryOperator<NormalizedScenarioOccurrenceInputV1>> vectors=new LinkedHashMap<>();
        vectors.put("scenario identity scheme",o->identity(o,"future-scenario-identity-v2"));
        vectors.put("source normalization",o->copy(o,"future-source-v2",o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),o.operationReference(),o.businessRuleReferences(),o.authoredGiven()));
        vectors.put("scenario canonicalization",o->copy(o,o.sourceNormalizationVersion(),"future-scenario-c14n-v2",o.scenarioSemanticContractVersion(),o.givenSteps(),o.operationReference(),o.businessRuleReferences(),o.authoredGiven()));
        vectors.put("scenario contract",o->copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),"future-scenario-contract-v2",o.givenSteps(),o.operationReference(),o.businessRuleReferences(),o.authoredGiven()));
        vectors.put("step identity",o->step(o,"future-step-identity-v2",o.givenSteps().getFirst().semanticCanonicalizationVersion()));
        vectors.put("step semantic",o->step(o,o.givenSteps().getFirst().identityVersion(),"future-step-c14n-v2"));
        vectors.put("operation role",o->operation(o,"FUTURE_ROLE",o.operationReference().datumIdentityVersion(),o.operationReference().targetProfile(),o.operationReference().semanticCanonicalizationVersion()));
        vectors.put("operation datum",o->operation(o,o.operationReference().role(),"future-operation-datum-v2",o.operationReference().targetProfile(),o.operationReference().semanticCanonicalizationVersion()));
        vectors.put("operation profile",o->operation(o,o.operationReference().role(),o.operationReference().datumIdentityVersion(),"future-operation-profile-v2",o.operationReference().semanticCanonicalizationVersion()));
        vectors.put("operation semantic",o->operation(o,o.operationReference().role(),o.operationReference().datumIdentityVersion(),o.operationReference().targetProfile(),"future-operation-c14n-v2"));
        vectors.put("rule datum",o->rule(o,o.businessRuleReferences().getFirst().identityScheme(),"future-rule-datum-v2",o.businessRuleReferences().getFirst().semanticCanonicalizationVersion()));
        vectors.put("rule identity scheme",o->rule(o,"future-rule-identity-v2",o.businessRuleReferences().getFirst().datumIdentityVersion(),o.businessRuleReferences().getFirst().semanticCanonicalizationVersion()));
        vectors.put("rule semantic",o->rule(o,o.businessRuleReferences().getFirst().identityScheme(),o.businessRuleReferences().getFirst().datumIdentityVersion(),"future-rule-c14n-v2"));
        return vectors.entrySet().stream().map(e->DynamicTest.dynamicTest(e.getKey(),()->assertUnsupported(e.getValue().apply(base)))).toList();
    }

    @Test void multipleUnsupportedRowsAndIntegrityDefectHaveStablePrecedence(){
        var base=ScenarioIdentityGroupComposerV1Test.input(ScenarioIdentityGroupComposerV1Test.fixture(),"a.json",2,"base");
        var changed=step(copy(base,"future-source-v2","future-c14n-v2",base.scenarioSemanticContractVersion(),base.givenSteps(),base.operationReference(),base.businessRuleReferences(),List.of("mismatch")),"future-step-v2","future-step-c14n-v2");
        assertUnsupported(changed);
    }

    @TestFactory Collection<DynamicTest> closedIntegrityTaxonomyIsIndependentlyAddressable(){
        return Arrays.stream(ScenarioOccurrenceIntegrityRejectionV1.values()).map(value->DynamicTest.dynamicTest(value.name(),()->{
            var signal=new ScenarioOccurrenceIntegrityExceptionV1(value);assertSame(value,signal.rejection());
            assertFalse(value.name().contains("OTHER"));assertFalse(value.name().contains("UNKNOWN"));
        })).toList();
    }

    @Test void reachableCorrespondenceDefectsClassifyIntegrityUnavailable(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var base=withRule(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"base"));
        List<NormalizedScenarioOccurrenceInputV1> defects=List.of(
                copy(base,base.sourceNormalizationVersion(),base.scenarioSemanticCanonicalizationVersion(),base.scenarioSemanticContractVersion(),List.of(),base.operationReference(),base.businessRuleReferences(),base.authoredGiven()),
                stepValue(base,FOREIGN,StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,"ready"),
                stepValue(base,base.claimedIdentity(),StepSemanticFingerprintInput.Phase.WHEN,BigInteger.ZERO,"ready"),
                stepValue(base,base.claimedIdentity(),StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ONE,"ready"),
                stepValue(base,base.claimedIdentity(),StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,"changed"),
                operationIdentity(base,FOREIGN,"POST","/pay"), operationIdentity(base,base.claimedIdentity(),"","/pay"),
                rulePosition(base,BigInteger.ONE,base.claimedIdentity(),"policy","one"),
                rulePosition(base,BigInteger.ZERO,FOREIGN,"policy","one"),
                rulePosition(base,BigInteger.ZERO,base.claimedIdentity(),"","one"));
        defects.forEach(this::assertIntegrity);
    }

    @Test void unavailableAndAllDerivedStatesAreAuthoritative(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var a=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"same"));
        var equal=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",10,"same"));
        var conflict=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",2,"different"));
        var unsupported=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",10,"x","future-source"));
        var integrity=ScenarioIdentityGroupComposerV1Test.attempt(stepValue(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",10,"x"),FOREIGN,StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,"ready"));
        assertEquals(VerifiedScenarioIdentityGroupV1.State.UNIQUE,ScenarioIdentityGroupComposerV1Test.compose(a).state());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT,ScenarioIdentityGroupComposerV1Test.compose(a,equal).state());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING,ScenarioIdentityGroupComposerV1Test.compose(a,conflict).state());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,ScenarioIdentityGroupComposerV1Test.compose(a,unsupported).state());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,ScenarioIdentityGroupComposerV1Test.compose(a,integrity).state());
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1Test.compose(unsupported));
    }

    @Test void groupVersionsAndOrderingAreRejected(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var a=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"a"));var b=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",10,"b"));
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1.compose("future-duplicate-v2",a.occurrence().claimedIdentity(),List.of(a)));
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1Test.compose(b,a));
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1Test.compose(a,a));
    }

    @Test void relocationIndexAndUnicodeOrderingAreNormative(){
        var first=ScenarioIdentityGroupComposerV1Test.fixture("a.json","b.json");
        var atA=(ScenarioOccurrenceCompositionOutcomeV1.Composed)ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(first,"a.json",2,"same"));
        var atB=(ScenarioOccurrenceCompositionOutcomeV1.Composed)ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(first,"b.json",2,"same"));
        var at10=(ScenarioOccurrenceCompositionOutcomeV1.Composed)ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(first,"a.json",10,"same"));
        assertEquals(atA.fingerprint(),atB.fingerprint());assertEquals(atA.fingerprint(),at10.fingerprint());
        assertNotEquals(ScenarioIdentityGroupComposerV1Test.compose(atA).fingerprint(),ScenarioIdentityGroupComposerV1Test.compose(atB).fingerprint());
        assertNotEquals(ScenarioIdentityGroupComposerV1Test.compose(atA).fingerprint(),ScenarioIdentityGroupComposerV1Test.compose(at10).fingerprint());
        var unicode=ScenarioIdentityGroupComposerV1Test.fixture("\uE000.json","😀.json");
        var bmp=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(unicode,"\uE000.json",2,"same"));
        var supplementary=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(unicode,"😀.json",2,"same"));
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT,ScenarioIdentityGroupComposerV1Test.compose(bmp,supplementary).state());
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1Test.compose(supplementary,bmp));
    }

    @Test void postOutcomeProofSubstitutionVectors(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var valid=(ScenarioOccurrenceCompositionOutcomeV1.Composed)ScenarioIdentityGroupComposerV1Test.attempt(withRule(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"valid")));
        var request=valid.request();var step=FingerprintStepAttestation.create(new StepSemanticFingerprintInput(request.parentIdentity().authority(),request.parentIdentity().scenarioKey(),request.parentIdentity().identitySchemeVersion(),StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION,"substituted",StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        var stepRequest=new ScenarioSemanticCompositionRequest(request.parentIdentity(),request.exactTitle(),List.of(step),request.whenSteps(),request.thenSteps(),request.operationReference(),request.businessRuleReferences(),request.scenarioSemanticCanonicalizationVersion(),request.sourceNormalizationVersion(),request.scenarioSemanticContractVersion());
        assertProofRejected(valid,stepRequest,valid.composition());
        var op=FingerprintOperationReferenceAttestation.create(new HttpOperationReferenceSemanticFingerprintInput(request.parentIdentity().authority(),request.parentIdentity().scenarioKey(),request.parentIdentity().identitySchemeVersion(),HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE,HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION,HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,"PUT","/other",HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER));
        assertProofRejected(valid,new ScenarioSemanticCompositionRequest(request.parentIdentity(),request.exactTitle(),request.givenSteps(),request.whenSteps(),request.thenSteps(),op,request.businessRuleReferences(),request.scenarioSemanticCanonicalizationVersion(),request.sourceNormalizationVersion(),request.scenarioSemanticContractVersion()),valid.composition());
        assertProofRejected(valid,new ScenarioSemanticCompositionRequest(request.parentIdentity(),request.exactTitle(),request.givenSteps(),request.whenSteps(),request.thenSteps(),request.operationReference(),List.of(),request.scenarioSemanticCanonicalizationVersion(),request.sourceNormalizationVersion(),request.scenarioSemanticContractVersion()),valid.composition());
        var foreign=new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity("foreign","checkout","qaip-scenario-identity-v1");
        var foreignRequest=new ScenarioSemanticCompositionRequest(foreign,request.exactTitle(),request.givenSteps(),request.whenSteps(),request.thenSteps(),request.operationReference(),request.businessRuleReferences(),request.scenarioSemanticCanonicalizationVersion(),request.sourceNormalizationVersion(),request.scenarioSemanticContractVersion());
        assertProofRejected(valid,foreignRequest,valid.composition());
    }

    @Test void normativeStateAndUnavailableGoldenFingerprints(){var f=ScenarioIdentityGroupComposerV1Test.fixture();var a=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"same"));var eq=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",10,"same"));var conflict=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",2,"different"));var unsupported=ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",10,"x","future-source"));var integrity=ScenarioIdentityGroupComposerV1Test.attempt(stepValue(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",10,"x"),FOREIGN,StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,"ready"));assertEquals(List.of(
            "scenario-authority-scenario-identity-group-v1:ecc9d4918f490468319a714431932880780c7720ea91b6705b3e3642f46e4b69",
            "scenario-authority-scenario-identity-group-v1:a8d8e0388c0debe02f8bc7d004da7c45e58264b378e1894b736bee19c2a4894f",
            "scenario-authority-scenario-identity-group-v1:b03d18cca36c57bdfa4a04b7203dd2a11b2de2edf4277a36b48c53ad2227309c",
            "scenario-authority-scenario-identity-group-v1:bc84dc6b9a24263c1f65737ebb3d12b605702da5559b81720112c1af14975d49",
            "scenario-authority-scenario-identity-group-v1:99173f02e686d77818fe5fa5fd67f10a2758a9abea6544aac0b26ed23af93c62"),List.of(ScenarioIdentityGroupComposerV1Test.compose(a).fingerprint().value(),ScenarioIdentityGroupComposerV1Test.compose(a,eq).fingerprint().value(),ScenarioIdentityGroupComposerV1Test.compose(a,conflict).fingerprint().value(),ScenarioIdentityGroupComposerV1Test.compose(a,unsupported).fingerprint().value(),ScenarioIdentityGroupComposerV1Test.compose(a,integrity).fingerprint().value()));}

    private void assertUnsupported(NormalizedScenarioOccurrenceInputV1 o){var result=ScenarioIdentityGroupComposerV1Test.attempt(o);assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)result).reason());}
    private void assertIntegrity(NormalizedScenarioOccurrenceInputV1 o){var result=ScenarioIdentityGroupComposerV1Test.attempt(o);assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE,((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)result).reason());}

    private static NormalizedScenarioOccurrenceInputV1 copy(NormalizedScenarioOccurrenceInputV1 o,String source,String c14n,String contract,List<NormalizedScenarioOccurrenceInputV1.NormalizedStep> given,NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference op,List<NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference> rules,List<String> authored){return new NormalizedScenarioOccurrenceInputV1(source,c14n,contract,o.occurrenceIdentity(),o.parentMember(),o.repositoryCaptureAttestation(),o.structuralLocation(),o.claimedIdentity(),o.exactTitle(),authored,given,o.authoredWhen(),o.whenSteps(),o.authoredThen(),o.thenSteps(),op,rules);}
    private static NormalizedScenarioOccurrenceInputV1 identity(NormalizedScenarioOccurrenceInputV1 o,String scheme){var id=new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(o.claimedIdentity().authority(),o.claimedIdentity().scenarioKey(),scheme);return new NormalizedScenarioOccurrenceInputV1(o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.occurrenceIdentity(),o.parentMember(),o.repositoryCaptureAttestation(),o.structuralLocation(),id,o.exactTitle(),o.authoredGiven(),o.givenSteps(),o.authoredWhen(),o.whenSteps(),o.authoredThen(),o.thenSteps(),o.operationReference(),o.businessRuleReferences());}
    private static NormalizedScenarioOccurrenceInputV1 step(NormalizedScenarioOccurrenceInputV1 o,String identity,String semantic){var s=o.givenSteps().getFirst();return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),List.of(new NormalizedScenarioOccurrenceInputV1.NormalizedStep(s.claimedIdentity(),s.phase(),s.ordinal(),identity,s.exactAuthoredText(),semantic)),o.operationReference(),o.businessRuleReferences(),o.authoredGiven());}
    private static NormalizedScenarioOccurrenceInputV1 stepValue(NormalizedScenarioOccurrenceInputV1 o,ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity id,StepSemanticFingerprintInput.Phase phase,BigInteger ordinal,String text){var s=o.givenSteps().getFirst();return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),List.of(new NormalizedScenarioOccurrenceInputV1.NormalizedStep(id,phase,ordinal,s.identityVersion(),text,s.semanticCanonicalizationVersion())),o.operationReference(),o.businessRuleReferences(),o.authoredGiven());}
    private static NormalizedScenarioOccurrenceInputV1 operation(NormalizedScenarioOccurrenceInputV1 o,String role,String datum,String profile,String semantic){var x=o.operationReference();var op=new NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference(x.claimedIdentity(),role,datum,profile,x.method(),x.path(),semantic);return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),op,o.businessRuleReferences(),o.authoredGiven());}
    private static NormalizedScenarioOccurrenceInputV1 operationIdentity(NormalizedScenarioOccurrenceInputV1 o,ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity id,String method,String path){var x=o.operationReference();var op=new NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference(id,x.role(),x.datumIdentityVersion(),x.targetProfile(),method,path,x.semanticCanonicalizationVersion());return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),op,o.businessRuleReferences(),o.authoredGiven());}
    static NormalizedScenarioOccurrenceInputV1 withRule(NormalizedScenarioOccurrenceInputV1 o){var r=new NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference(BigInteger.ZERO,o.claimedIdentity(),"policy","one",BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_IDENTITY_SCHEME,BusinessRuleReferenceSemanticFingerprintEncoder.BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION,BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),o.operationReference(),List.of(r),o.authoredGiven());}
    private static void assertProofRejected(ScenarioOccurrenceCompositionOutcomeV1.Composed valid,ScenarioSemanticCompositionRequest request,ScenarioSemanticCompositionResult result){var fabricated=new ScenarioOccurrenceCompositionOutcomeV1.Composed(valid.occurrence(),request,result);assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1Test.compose(fabricated));}
    private static NormalizedScenarioOccurrenceInputV1 rule(NormalizedScenarioOccurrenceInputV1 o,String scheme,String datum,String semantic){var x=o.businessRuleReferences().getFirst();var r=new NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference(x.authoredPosition(),x.claimedIdentity(),x.referencedAuthority(),x.stableRuleKey(),scheme,datum,semantic);return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),o.operationReference(),List.of(r),o.authoredGiven());}
    private static NormalizedScenarioOccurrenceInputV1 rulePosition(NormalizedScenarioOccurrenceInputV1 o,BigInteger position,ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity id,String authority,String key){var x=o.businessRuleReferences().getFirst();var r=new NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference(position,id,authority,key,x.identityScheme(),x.datumIdentityVersion(),x.semanticCanonicalizationVersion());return copy(o,o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.givenSteps(),o.operationReference(),List.of(r),o.authoredGiven());}
}
