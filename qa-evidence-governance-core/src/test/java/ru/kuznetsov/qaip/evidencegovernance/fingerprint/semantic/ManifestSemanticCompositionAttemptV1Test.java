package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import java.math.BigInteger; import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ManifestSemanticCompositionAttemptV1Test {
    @Test void composedOneAndMultiplePreserveOrderAndExistingBytes(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var a=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"one");
        var b=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",1,"two");
        var one=attempt(verified(f,List.of(a)),List.of(attemptChild(a)));
        var many=attempt(verified(f,List.of(a,b)),List.of(attemptChild(a),attemptChild(b)));
        var c1=assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,one);
        var c2=assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,many);
        assertEquals(ManifestSemanticFingerprintEncoder.fingerprint(ManifestSemanticFingerprintComposerV1.input(c1.manifest(),c1.scenarioFingerprints())),c1.fingerprint());
        assertEquals(List.of(((ScenarioOccurrenceCompositionOutcomeV1.Composed)attemptChild(a)).fingerprint(),
                ((ScenarioOccurrenceCompositionOutcomeV1.Composed)attemptChild(b)).fingerprint()),c2.scenarioFingerprints());
        assertNotEquals(c1.fingerprint(),c2.fingerprint());
    }

    @Test void unavailableCoversUnsupportedIntegrityMixedAndPositions(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var composed=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"ok");
        var unsupported=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",1,"u","future-source");
        var integrity=integrityInput(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"i"));
        var result=attempt(verified(f,List.of(composed,unsupported,integrity)),List.of(attemptChild(composed),attemptChild(unsupported),attemptChild(integrity)));
        var unavailable=assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,result);
        assertEquals(ManifestSemanticCompositionOutcomeV1.UnavailableReason.SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE,unavailable.reason());
        assertEquals(3,unavailable.childOutcomes().size());
        for(var order:List.of(List.of(unsupported,composed,integrity),List.of(composed,unsupported,integrity),List.of(composed,integrity,unsupported))){
            var normalized=reindex(order);var outcomes=normalized.stream().map(ManifestSemanticCompositionAttemptV1Test::attemptChild).toList();
            assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Unavailable.class,attempt(verified(f,normalized),outcomes));
        }
    }

    @Test void closureRejectsMissingExtraDuplicateReorderedAndForeign(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json","b.json");
        var a=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"a");var b=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",1,"b");
        var m=verified(f,List.of(a,b));var oa=attemptChild(a);var ob=attemptChild(b);
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_COUNT_MISMATCH,()->attempt(m,List.of(oa)));
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_COUNT_MISMATCH,()->attempt(m,List.of(oa,ob,oa)));
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION,()->attempt(m,List.of(oa,oa)));
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION,()->attempt(m,List.of(ob,oa)));
        var foreign=ScenarioIdentityGroupComposerV1Test.input(f,"b.json",0,"x");
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION,()->attempt(m,List.of(attemptChild(foreign),ob)));
        var otherFixture=ScenarioIdentityGroupComposerV1Test.fixture("a.json");
        var crossCapture=ScenarioIdentityGroupComposerV1Test.input(otherFixture,"a.json",0,"x");
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_DECLARATION_SUBSTITUTION,()->attempt(m,List.of(attemptChild(crossCapture),ob)));
    }

    @Test void revalidationRejectsFabricatedComposedUnavailableAndFingerprintSubstitution(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");var a=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"a");
        var real=(ScenarioOccurrenceCompositionOutcomeV1.Composed)attemptChild(a);
        var other=(ScenarioOccurrenceCompositionOutcomeV1.Composed)attemptChild(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"other"));
        var bad=new ScenarioOccurrenceCompositionOutcomeV1.Composed(a,real.request(),other.composition());
        assertThrows(IllegalArgumentException.class,()->attempt(verified(f,List.of(a)),List.of(bad)));
        var unsupported=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"u","future-source");
        var wrong=new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(unsupported,ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE);
        assertThrows(IllegalArgumentException.class,()->attempt(verified(f,List.of(unsupported)),List.of(wrong)));
    }

    @Test void finiteComposerTaxonomyAndUnsupportedInputDoNotBecomeUnavailable(){
        assertEquals(3,ManifestCompositionRejectionV1.Code.values().length);
        assertEquals(9,ManifestCompositionRejectionV1.HistoricalCode.values().length);
        assertEquals(9,Arrays.stream(ManifestCompositionRejectionV1.HistoricalCode.values()).map(ManifestCompositionRejectionV1::disposition).count());
        assertTrue(Arrays.stream(ManifestCompositionRejectionV1.Code.values()).noneMatch(x->x.name().contains("OTHER")||x.name().contains("UNKNOWN")));
        var f=ScenarioIdentityGroupComposerV1Test.fixture("a.json");var a=ScenarioIdentityGroupComposerV1Test.input(f,"a.json",0,"a");var m=verified(f,List.of(a));
        assertCode(ManifestCompositionRejectionV1.Code.SCENARIO_COUNT_MISMATCH,()->ManifestSemanticFingerprintComposerV1.composeManifestSemanticFingerprintV1(m,List.of()));
        var future=new NormalizedManifestSemanticCompositionInputV1("future",NormalizedManifestSemanticCompositionInputV1.OUTCOME_CONTRACT_VERSION,
                NormalizedManifestSemanticCompositionInputV1.ATTEMPT_VERSION,NormalizedManifestSemanticCompositionInputV1.OUTCOME_VOCABULARY_VERSION,
                NormalizedManifestSemanticCompositionInputV1.REASON_VOCABULARY_VERSION,m,List.of(attemptChild(a)));
        assertCode(ManifestCompositionRejectionV1.Code.UNSUPPORTED_MANIFEST_CONTRACT,()->ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(future));
        assertThrows(NullPointerException.class,()->ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(null));
        assertThrows(IllegalArgumentException.class,()->ManifestSemanticFingerprintComposerV1.composeManifestSemanticFingerprintV1(m,explodingList(new IllegalArgumentException("unexpected"))));
        assertThrows(IllegalStateException.class,()->ManifestSemanticFingerprintComposerV1.composeManifestSemanticFingerprintV1(m,explodingList(new IllegalStateException("unexpected"))));
    }

    private static ManifestSemanticCompositionOutcomeV1 attempt(VerifiedAdmittedManifestV1 m,List<ScenarioOccurrenceCompositionOutcomeV1> o){return attempt(m,o,true);}
    private static ManifestSemanticCompositionOutcomeV1 attempt(VerifiedAdmittedManifestV1 m,List<ScenarioOccurrenceCompositionOutcomeV1> o,boolean ignored){return ManifestSemanticCompositionAttemptV1.attemptManifestSemanticCompositionV1(NormalizedManifestSemanticCompositionInputV1.selectedV1(m,o));}
    private static ScenarioOccurrenceCompositionOutcomeV1 attemptChild(NormalizedScenarioOccurrenceInputV1 o){return ScenarioOccurrenceCompositionAttemptV1.attemptScenarioOccurrenceCompositionV1(o);}
    private static VerifiedAdmittedManifestV1 verified(ScenarioIdentityGroupComposerV1Test.Fixture f,List<NormalizedScenarioOccurrenceInputV1> c){var p=c.getFirst().parentMember();var mi=c.getFirst().occurrenceIdentity().manifest();
        try{var constructor=VerifiedAdmittedManifestV1.class.getDeclaredConstructors()[0];constructor.setAccessible(true);
            return (VerifiedAdmittedManifestV1)constructor.newInstance(f.att(),p,mi,"orders",null,
                    ManifestSemanticFingerprintEncoder.MANIFEST_FORMAT_IDENTIFIER,ManifestSemanticFingerprintEncoder.SCHEMA_VERSION,
                    ManifestSemanticFingerprintEncoder.SCENARIO_IDENTITY_SCHEME_VERSION,List.of("test-only"),List.copyOf(c),
                    null,null,List.of());
        }catch(ReflectiveOperationException exception){throw new AssertionError(exception);}}
    private static NormalizedScenarioOccurrenceInputV1 integrityInput(NormalizedScenarioOccurrenceInputV1 b){var bad=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(b.claimedIdentity(),StepSemanticFingerprintInput.Phase.WHEN,BigInteger.valueOf(7),StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION,"different",StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        return new NormalizedScenarioOccurrenceInputV1(b.sourceNormalizationVersion(),b.scenarioSemanticCanonicalizationVersion(),b.scenarioSemanticContractVersion(),b.occurrenceIdentity(),b.parentMember(),b.repositoryCaptureAttestation(),b.structuralLocation(),b.claimedIdentity(),b.exactTitle(),b.authoredGiven(),List.of(bad),b.authoredWhen(),b.whenSteps(),b.authoredThen(),b.thenSteps(),b.operationReference(),b.businessRuleReferences());}
    private static List<NormalizedScenarioOccurrenceInputV1> reindex(List<NormalizedScenarioOccurrenceInputV1> source){var result=new ArrayList<NormalizedScenarioOccurrenceInputV1>();for(int i=0;i<source.size();i++){var b=source.get(i);var oi=new NormalizedScenarioOccurrenceInputV1.ScenarioDeclarationOccurrenceIdentity(b.occurrenceIdentity().manifest(),"/scenarios/"+i,b.occurrenceIdentity().identityVersion());result.add(new NormalizedScenarioOccurrenceInputV1(b.sourceNormalizationVersion(),b.scenarioSemanticCanonicalizationVersion(),b.scenarioSemanticContractVersion(),oi,b.parentMember(),b.repositoryCaptureAttestation(),oi.structuralPath(),b.claimedIdentity(),b.exactTitle(),b.authoredGiven(),b.givenSteps(),b.authoredWhen(),b.whenSteps(),b.authoredThen(),b.thenSteps(),b.operationReference(),b.businessRuleReferences()));}return result;}
    private static List<ScenarioOccurrenceCompositionOutcomeV1.Composed> explodingList(RuntimeException failure){return new AbstractList<>(){@Override public ScenarioOccurrenceCompositionOutcomeV1.Composed get(int index){throw failure;}@Override public int size(){throw failure;}@Override public Object[] toArray(){throw failure;}@Override public <T>T[] toArray(T[] target){throw failure;}};}
    private static void assertCode(ManifestCompositionRejectionV1.Code c,org.junit.jupiter.api.function.Executable x){assertEquals(c,assertThrows(ManifestCompositionRejectionV1.class,x).code());}
}
