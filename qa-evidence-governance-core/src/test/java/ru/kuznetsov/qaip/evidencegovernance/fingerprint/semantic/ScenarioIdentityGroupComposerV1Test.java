package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.*;
import java.math.BigInteger; import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioIdentityGroupComposerV1Test {
    private static final ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity ID =
            new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity("orders","checkout","qaip-scenario-identity-v1");

    @Test void goldenUniqueAndExactCanonicalRecalculation() {
        var f=fixture(); var outcome=attempt(input(f,"a.json",2,"pay")); assertInstanceOf(ScenarioOccurrenceCompositionOutcomeV1.Composed.class,outcome);
        var group=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,ID,List.of(outcome));
        assertEquals(VerifiedScenarioIdentityGroupV1.State.UNIQUE,group.state());
        assertEquals(group.fingerprint(),new ScenarioIdentityGroupFingerprint(ScenarioIdentityGroupFingerprint.VALUE_PREFIX+
                ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256.lowercaseHexDigest(ScenarioIdentityGroupComposerV1.canonicalBytes(group))));
        assertEquals("scenario-authority-scenario-identity-group-v1:6192583c168aad9cf73a19d4c663164580c802048a5f95fd0a9c63ba7b3af8a4",group.fingerprint().value());
        assertEquals("AAAAAAAAADJRQUlQAFNDRU5BUklPX0FVVEhPUklUWV9TQ0VOQVJJT19JREVOVElUWV9HUk9VUABWMQAAAAAAAAAyc2NlbmFyaW8tYXV0aG9yaXR5LXNjZW5hcmlvLWlkZW50aXR5LWdyb3VwLWMxNG4tdjEAAAAAAAAACnNoYS0yNTYtdjEAAAAAAAAAKHNjZW5hcmlvLWF1dGhvcml0eS1kdXBsaWNhdGUtb3V0Y29tZXMtdjEAAAAAAAAABm9yZGVycwAAAAAAAAAIY2hlY2tvdXQAAAAAAAAAGXFhaXAtc2NlbmFyaW8taWRlbnRpdHktdjEAAAAAAAAABlVOSVFVRQAAAAAAAAABAAAAAAAAAARyZXBvAAAAAAAAAARzbmFwAAAAAAAAAChzY2VuYXJpby1hdXRob3JpdHktcmVwb3NpdG9yeS1jYXB0dXJlLXYxAAAAAAAAAGlzY2VuYXJpby1hdXRob3JpdHktcmVwb3NpdG9yeS1jYXB0dXJlLXYxOmE0YTllOTY1ODczZTg3NTRmNGJiYmIxNGQ0NzU4MWUwZjU3OGY0MjMxNGZlZjhjMmRjZmRhZTNiNzU4MDNjYzgAAAAAAAAABmEuanNvbgAAAAAAAAAtcWFpcC1zY2VuYXJpby1tYW5pZmVzdC1vY2N1cnJlbmNlLWlkZW50aXR5LXYxAAAAAAAAAAwvc2NlbmFyaW9zLzIAAAAAAAAAMHFhaXAtc2NlbmFyaW8tZGVjbGFyYXRpb24tb2NjdXJyZW5jZS1pZGVudGl0eS12MQAAAAAAAAAEcmVwbwAAAAAAAAAEc25hcAAAAAAAAAAoc2NlbmFyaW8tYXV0aG9yaXR5LXJlcG9zaXRvcnktY2FwdHVyZS12MQAAAAAAAABpc2NlbmFyaW8tYXV0aG9yaXR5LXJlcG9zaXRvcnktY2FwdHVyZS12MTphNGE5ZTk2NTg3M2U4NzU0ZjRiYmJiMTRkNDc1ODFlMGY1NzhmNDIzMTRmZWY4YzJkY2ZkYWUzYjc1ODAzY2M4AAAAAAAAAAZhLmpzb24AAAAAAAAAAQAAAAAAAAAKc2hhLTI1Ni12MQAAAAAAAABLc2hhLTI1Ni12MTo0YmY1MTIyZjM0NDU1NGM1M2JkZTJlYmI4Y2QyYjdlM2QxNjAwYWQ2MzFjMzg1YTVkN2NjZTIzYzc3ODU0NTlhAAAAAAAAAAwvc2NlbmFyaW9zLzIBAAAAAAAAACdzY2VuYXJpby1hdXRob3JpdHktc2NlbmFyaW8tc2VtYW50aWMtdjEAAAAAAAAAaHNjZW5hcmlvLWF1dGhvcml0eS1zY2VuYXJpby1zZW1hbnRpYy12MTo2NWM0NTlhZWNmOGNkYmY2ZTBjODQyYjVkZTIwOGMxOWM5YWQ1NGVlYjIwNzQwYWYwNjFkMjJlZGVjZjc4MGE4AA==",Base64.getEncoder().encodeToString(ScenarioIdentityGroupComposerV1.canonicalBytes(group)));
    }
    @Test void derivesEveryDuplicateStateAndNumericOrder() {
        var f=fixture();var a=attempt(input(f,"a.json",2,"pay"));var equal=attempt(input(f,"a.json",10,"pay"));
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT,compose(a,equal).state());
        var conflict=attempt(input(f,"b.json",2,"ship"));assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING,compose(a,conflict).state());
        var unsupported=input(f,"b.json",2,"ship","future-source-normalization");var unavailable=attempt(unsupported);
        assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)unavailable).reason());
        assertEquals(VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED,compose(a,unavailable).state());
        assertThrows(IllegalArgumentException.class,()->compose(equal,a));
    }
    @Test void validationPrecedenceAndAntiSubstitution() {
        var f=fixture();var unsupported=input(f,"a.json",2,"wrong","future");
        assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,
                ((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)attempt(unsupported)).reason());
        var a=attempt(input(f,"a.json",2,"same"));var b=attempt(input(f,"a.json",10,"different"));
        // same identity and semantics still cannot substitute: outcome is bound by object identity to its occurrence.
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,ID,List.of(a,new ScenarioOccurrenceCompositionOutcomeV1.Composed(b.occurrence(),((ScenarioOccurrenceCompositionOutcomeV1.Composed)b).request(),((ScenarioOccurrenceCompositionOutcomeV1.Composed)a).composition()))));
        var badParent=input(f,"a.json",2,"x");var foreign=new NormalizedScenarioOccurrenceInputV1(badParent.sourceNormalizationVersion(),badParent.scenarioSemanticCanonicalizationVersion(),badParent.scenarioSemanticContractVersion(),badParent.occurrenceIdentity(),f.refs.get(1),badParent.repositoryCaptureAttestation(),badParent.structuralLocation(),badParent.claimedIdentity(),badParent.exactTitle(),badParent.authoredGiven(),badParent.givenSteps(),badParent.authoredWhen(),badParent.whenSteps(),badParent.authoredThen(),badParent.thenSteps(),badParent.operationReference(),badParent.businessRuleReferences());
        assertThrows(IllegalArgumentException.class,()->attempt(foreign));
    }
    @Test void rejectsUnsupportedGroupSchemeEvenWhenEveryOccurrenceIsUnavailable() {
        var f=fixture();var unavailable=attempt(input(f,"a.json",2,"x","future-source"));
        var future=new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity("orders","checkout","future-identity-v2");
        assertThrows(IllegalArgumentException.class,()->ScenarioIdentityGroupComposerV1.compose(
                ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,future,List.of(unavailable,unavailable)));
    }
    @Test void revalidatesCompleteComposedAndUnavailableProofs() {
        var f=fixture();var first=(ScenarioOccurrenceCompositionOutcomeV1.Composed)attempt(input(f,"a.json",2,"one"));
        var second=(ScenarioOccurrenceCompositionOutcomeV1.Composed)attempt(input(f,"a.json",10,"two"));
        var substitutedRequest=new ScenarioOccurrenceCompositionOutcomeV1.Composed(first.occurrence(),second.request(),first.composition());
        assertThrows(IllegalArgumentException.class,()->compose(substitutedRequest));
        var substitutedScenarioProof=new ScenarioOccurrenceCompositionOutcomeV1.Composed(first.occurrence(),first.request(),second.composition());
        assertThrows(IllegalArgumentException.class,()->compose(substitutedScenarioProof));
        var unsupported=input(f,"b.json",2,"x","future-source");
        var fabricatedReason=new ScenarioOccurrenceCompositionOutcomeV1.Unavailable(unsupported,
                ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.SCENARIO_COMPOSITION_INTEGRITY_FAILURE);
        assertThrows(IllegalArgumentException.class,()->compose(fabricatedReason,
                attempt(input(f,"a.json",2,"valid"))));
    }
    @Test void unsupportedPrecedesFiniteIntegrityAndUnexpectedIdentityFailuresPropagate() {
        var f=fixture();var base=input(f,"a.json",2,"x","future-source");
        var mismatchedStep=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(ID,StepSemanticFingerprintInput.Phase.WHEN,
                BigInteger.valueOf(7),StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION,"different",StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        var combined=new NormalizedScenarioOccurrenceInputV1(base.sourceNormalizationVersion(),base.scenarioSemanticCanonicalizationVersion(),base.scenarioSemanticContractVersion(),base.occurrenceIdentity(),base.parentMember(),base.repositoryCaptureAttestation(),base.structuralLocation(),base.claimedIdentity(),base.exactTitle(),base.authoredGiven(),List.of(mismatchedStep),base.authoredWhen(),base.whenSteps(),base.authoredThen(),base.thenSteps(),base.operationReference(),base.businessRuleReferences());
        assertEquals(ScenarioOccurrenceCompositionOutcomeV1.UnavailableReason.UNSUPPORTED_SEMANTIC_CONTRACT,
                ((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)attempt(combined)).reason());
        var badLocation=new NormalizedScenarioOccurrenceInputV1(base.sourceNormalizationVersion(),base.scenarioSemanticCanonicalizationVersion(),base.scenarioSemanticContractVersion(),base.occurrenceIdentity(),base.parentMember(),base.repositoryCaptureAttestation(),"/scenarios/99",base.claimedIdentity(),base.exactTitle(),base.authoredGiven(),base.givenSteps(),base.authoredWhen(),base.whenSteps(),base.authoredThen(),base.thenSteps(),base.operationReference(),base.businessRuleReferences());
        assertThrows(IllegalArgumentException.class,()->attempt(badLocation));
    }
    @Test void finiteTaxonomyHasNoGenericEscapeCategory() {
        assertEquals(22,ScenarioOccurrenceIntegrityRejectionV1.values().length);
        assertTrue(Arrays.stream(ScenarioOccurrenceIntegrityRejectionV1.values()).noneMatch(v->v.name().contains("UNKNOWN")||v.name().contains("OTHER")));
    }
    private static VerifiedScenarioIdentityGroupV1 compose(ScenarioOccurrenceCompositionOutcomeV1...o){return ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,ID,List.of(o));}
    private static ScenarioOccurrenceCompositionOutcomeV1 attempt(NormalizedScenarioOccurrenceInputV1 o){return ScenarioOccurrenceCompositionAttemptV1.attemptScenarioOccurrenceCompositionV1(o);}
    private static NormalizedScenarioOccurrenceInputV1 input(Fixture f,String path,int index,String title){return input(f,path,index,title,ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION);}
    private static NormalizedScenarioOccurrenceInputV1 input(Fixture f,String path,int index,String title,String sourceVersion){var p=f.refs.stream().filter(x->x.normalizedRepositoryRelativePath().equals(path)).findFirst().orElseThrow();
        var m=new NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity("repo","snap",f.fp,path,NormalizedScenarioOccurrenceInputV1.MANIFEST_OCCURRENCE_IDENTITY_VERSION);var oi=new NormalizedScenarioOccurrenceInputV1.ScenarioDeclarationOccurrenceIdentity(m,"/scenarios/"+index,NormalizedScenarioOccurrenceInputV1.DECLARATION_OCCURRENCE_IDENTITY_VERSION);
        var step=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(ID,StepSemanticFingerprintInput.Phase.GIVEN,BigInteger.ZERO,StepSemanticFingerprintEncoder.STEP_IDENTITY_SCHEME_VERSION,"ready",StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        var op=new NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference(ID,HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_ROLE,HttpOperationReferenceSemanticFingerprintEncoder.OPERATION_REFERENCE_DATUM_IDENTITY_VERSION,HttpOperationReferenceSemanticFingerprintEncoder.TARGET_PROFILE,"POST","/pay",HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);
        return new NormalizedScenarioOccurrenceInputV1(sourceVersion,ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER,ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION,oi,p,f.att,"/scenarios/"+index,ID,title,List.of("ready"),List.of(step),List.of(),List.of(),List.of(),List.of(),op,List.of());}
    private static Fixture fixture(){var ra=RawSourceMemberFingerprint.calculate(new byte[]{1});var rb=RawSourceMemberFingerprint.calculate(new byte[]{2});var members=List.of(new RepositoryCaptureFingerprintInput.CapturedMember("a.json",1,ra),new RepositoryCaptureFingerprintInput.CapturedMember("b.json",1,rb));var in=new RepositoryCaptureFingerprintInput("repo","source-v1","profile","discovery-v1","path-v1","order-v1",RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,members,List.of());var fp=RepositoryCaptureFingerprintEncoder.fingerprint(in);var refs=List.of(new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference("repo","snap",fp,"a.json",1,ra),new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference("repo","snap",fp,"b.json",1,rb));return new Fixture(fp,refs,RepositoryCaptureAttestation.verified("repo","snap",in,fp,refs,List.of()));}
    private record Fixture(RepositoryCaptureFingerprint fp,List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> refs,RepositoryCaptureAttestation att){}
}
