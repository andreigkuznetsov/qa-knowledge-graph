package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.*;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.AdmittedManifestVerificationRejectionV1.Code.*;

class AdmittedManifestVerifierV1Test {
    private final AdmittedManifestVerifierV1 verifier=new AdmittedManifestVerifierV1();

    @Test void verifiesExactBytesAndDerivesCompleteOrderedProof(){
        String scenarios=scenario("same","First")+","+scenario("same","Second"); Fixture f=fixture(manifest(scenarios));
        VerifiedAdmittedManifestV1 proof=verifier.verifyAdmittedManifestV1(f.request());
        assertSame(f.capture,proof.capture()); assertEquals("Orders/V1",proof.claimedAuthority());
        assertEquals("manifests/orders.json",proof.occurrenceIdentity().memberPath());
        assertEquals(List.of("/scenarios/0","/scenarios/1"),proof.authoredScenarios().stream()
                .map(NormalizedScenarioOccurrenceInputV1::structuralLocation).toList());
        assertEquals(List.of("same","same"),proof.authoredScenarios().stream()
                .map(x->x.claimedIdentity().scenarioKey()).toList());
        assertEquals("Given First",proof.authoredScenarios().get(0).givenSteps().get(0).exactAuthoredText());
        assertSame(proof.parserProof(),proof.attributionProof().parsedJson());
        assertEquals(proof,proof); assertEquals(proof.authoredScenarios(),
                verifier.verifyAdmittedManifestV1(f.request()).authoredScenarios());
    }

    @Test void emptyScenarioCollectionIsAdmitted(){
        assertTrue(verifier.verifyAdmittedManifestV1(fixture(manifest("")).request()).authoredScenarios().isEmpty());
    }

    @Test void requestDefensivelyCopiesBytes(){
        Fixture f=fixture(manifest(scenario("one","One"))); byte[] caller=f.bytes.clone();
        var request=new AdmittedManifestVerificationRequestV1(f.capture,f.parent,caller,
                AdmittedManifestVerificationRequestV1.selectedV1Identifiers()); caller[0]^=1;
        assertEquals("Orders/V1",verifier.verifyAdmittedManifestV1(request).claimedAuthority());
        byte[] exposed=request.exactRawBytes();exposed[0]^=1;
        assertEquals("Orders/V1",verifier.verifyAdmittedManifestV1(request).claimedAuthority());
    }

    @Test void unsupportedContractAlwaysWins(){
        Fixture f=fixture("not json"); var ids=new ArrayList<>(AdmittedManifestVerificationRequestV1.selectedV1Identifiers());
        ids.set(3,"future-parser"); assertCode(UNSUPPORTED_VERIFICATION_CONTRACT,
                new AdmittedManifestVerificationRequestV1(f.capture,f.parent,new byte[]{1},ids));
    }

    @Test void everySelectedIdentifierIsIndependentlyFailClosedBeforeBytes(){
        Fixture f=fixture("not json"); List<String> supported=ids(); assertEquals(23,supported.size());
        for(int index=0;index<supported.size();index++){
            var changed=new ArrayList<>(supported);changed.set(index,supported.get(index)+"-unsupported");
            var rejection=assertThrows(AdmittedManifestVerificationRejectionV1.class,()->verifier.verifyAdmittedManifestV1(
                    new AdmittedManifestVerificationRequestV1(f.capture,f.parent,new byte[]{(byte)0xff},changed)),
                    "selection index "+index+" must be independently rejected");
            assertEquals(UNSUPPORTED_VERIFICATION_CONTRACT,rejection.code(),"selection index "+index);
        }
    }

    @Test void everyCaptureOrRawSubstitutionHasOneCode(){
        Fixture f=fixture(manifest(scenario("one","One"))); byte[] changed=f.bytes.clone();changed[changed.length-1]^=1;
        Fixture foreign=fixture(manifest(scenario("other","Other")));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(
                f.capture,foreign.parent,foreign.bytes,ids()));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,f.parent,changed,ids()));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,
                parent(f,1,f.parent.normalizedRepositoryRelativePath(),f.parent.rawByteLength(),f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,
                parent(f,0,"foreign.json",f.parent.rawByteLength(),f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,
                parent(f,0,f.parent.normalizedRepositoryRelativePath(),BigInteger.ZERO,f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,
                parent(f,0,f.parent.normalizedRepositoryRelativePath(),f.parent.rawByteLength(),RawSourceMemberFingerprint.calculate(new byte[]{9})),f.bytes,ids()));
    }

    @Test void parserCausesAreExhaustivelyRetained(){
        List<byte[]> vectors=List.of(new byte[]{(byte)0xff},"{".getBytes(StandardCharsets.UTF_8),
                "{\"x\":1,\"x\":2}".getBytes(StandardCharsets.UTF_8),"{} {}".getBytes(StandardCharsets.UTF_8));
        for(byte[] bytes:vectors){Fixture f=fixture(bytes);var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));assertEquals(PARSE_REJECTED,x.code());assertNotNull(x.parserCause());}
    }

    @Test void attributionPrecedesSchemaAndRetainsCauseAndLocation(){
        Fixture f=fixture("{}");var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));
        assertEquals(AUTHORITY_ATTRIBUTION_UNAVAILABLE,x.code());assertNotNull(x.attributionCause());
        assertEquals("/authority",x.attributionLocation());
    }

    @Test void everyAttributionCauseIsRetainedExactly(){
        record Vector(String json,ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code cause,String location){}
        var vectors=List.of(
                new Vector("[]",ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code.NON_OBJECT_ROOT,""),
                new Vector("{}",ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code.MISSING_AUTHORITY,"/authority"),
                new Vector("{\"authority\":7}",ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code.AUTHORITY_NOT_STRING,"/authority"),
                new Vector("{\"authority\":\"not valid!\"}",ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionRejectionV1.Code.INVALID_AUTHORITY,"/authority"));
        for(Vector vector:vectors){Fixture f=fixture(vector.json);var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));assertEquals(AUTHORITY_ATTRIBUTION_UNAVAILABLE,x.code());
            assertEquals(vector.cause,x.attributionCause());assertEquals(vector.location,x.attributionLocation());}
    }

    @Test void captureMismatchPrecedesParseRejection(){
        Fixture f=fixture(manifest(""));assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
                new AdmittedManifestVerificationRequestV1(f.capture,f.parent,new byte[]{(byte)0xff},ids()));
    }
    @Test void parseRejectionPrecedesUnavailableAttribution(){
        Fixture f=fixture("{");var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));assertEquals(PARSE_REJECTED,x.code());
    }
    @Test void attributionRejectionPrecedesStructuralSchemaRejection(){
        Fixture f=fixture("{}");var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));assertEquals(AUTHORITY_ATTRIBUTION_UNAVAILABLE,x.code());
        assertTrue(x.canonicalDiagnostics().isEmpty());
    }
    @Test void schemaRejectionProducesNoDownstreamProof(){
        Fixture f=fixture("{\"authority\":\"Orders/V1\"}");
        assertEquals(STRUCTURAL_SCHEMA_REJECTED,assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request())).code());
    }

    @Test void rejectsForeignRepositoryCapture(){Fixture a=fixture(manifest("")),b=fixture(manifest(scenario("b","B")));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(b.capture,a.parent,a.bytes,ids()));}
    @Test void rejectsForeignCapturedMember(){Fixture a=fixture(manifest("")),b=fixture(manifest(scenario("b","B")));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(a.capture,b.parent,a.bytes,ids()));}
    @Test void rejectsCaptureAWithCaptureBExactBytes(){Fixture a=fixture(manifest("")),b=fixture(manifest(scenario("b","B")));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(a.capture,a.parent,b.bytes,ids()));}
    @Test void rejectsChangedNormalizedPath(){Fixture f=fixture(manifest(""));assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
        new AdmittedManifestVerificationRequestV1(f.capture,parent(f,0,"other.json",f.parent.rawByteLength(),f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));}
    @Test void rejectsChangedOrderedPosition(){Fixture f=fixture(manifest(""));assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
        new AdmittedManifestVerificationRequestV1(f.capture,parent(f,1,f.parent.normalizedRepositoryRelativePath(),f.parent.rawByteLength(),f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));}
    @Test void rejectsChangedBytesWithCorrectMetadata(){Fixture f=fixture(manifest(""));byte[] changed=f.bytes.clone();changed[0]^=1;
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(f.capture,f.parent,changed,ids()));}
    @Test void rejectsChangedRawByteLength(){Fixture f=fixture(manifest(""));assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
        new AdmittedManifestVerificationRequestV1(f.capture,parent(f,0,f.parent.normalizedRepositoryRelativePath(),BigInteger.ZERO,f.parent.rawSourceMemberFingerprint()),f.bytes,ids()));}
    @Test void rejectsChangedRawFingerprint(){Fixture f=fixture(manifest(""));assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,
        new AdmittedManifestVerificationRequestV1(f.capture,parent(f,0,f.parent.normalizedRepositoryRelativePath(),f.parent.rawByteLength(),RawSourceMemberFingerprint.calculate(new byte[]{9})),f.bytes,ids()));}
    @Test void rejectsFullyCoordinatedCandidateRelabeling(){Fixture authority=fixture(manifest("")),candidate=fixture(manifest(scenario("b","B")));
        assertCode(CAPTURE_MEMBER_RAW_BYTES_MISMATCH,new AdmittedManifestVerificationRequestV1(authority.capture,candidate.parent,candidate.bytes,ids()));}

    @Test void schemaRejectionRetainsCanonicalDiagnostics(){
        Fixture f=fixture("{\"authority\":\"Orders/V1\"}");var x=assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(f.request()));
        assertEquals(STRUCTURAL_SCHEMA_REJECTED,x.code());
        var expected=List.of("format","scenarioIdentityScheme","scenarios","schemaVersion").stream()
                .map(property->ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic.v1("","required",
                        ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic.RULE_PREFIX+"/required",
                        List.of(new ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic.TextParameter("missingProperty",property))))
                .sorted(ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic.canonicalOrder()).toList();
        assertEquals(expected,x.canonicalDiagnostics());
    }

    @Test void requestAndProofExposeNoCallerSuppliedDerivedAuthority(){
        var names=Arrays.stream(AdmittedManifestVerificationRequestV1.class.getMethods()).map(m->m.getName().toLowerCase()).toList();
        assertTrue(names.stream().noneMatch(n->n.contains("normalized")||n.contains("authority")||n.contains("occurrence")||n.contains("scenario")||n.contains("admission")));
        assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredConstructors()).allMatch(c->Modifier.isPrivate(c.getModifiers())));
        assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredMethods()).filter(m->Modifier.isPublic(m.getModifiers()))
                .noneMatch(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==VerifiedAdmittedManifestV1.class));
    }

    @Test void equivalentReconstructedCaptureIsAcceptedWithoutReferenceIdentity(){
        Fixture f=fixture(manifest(scenario("one","One")));var proof=verifier.verifyAdmittedManifestV1(f.request());
        var equivalent=RepositoryCaptureAttestation.verified(f.capture.sourceId(),f.capture.snapshotId(),
                f.capture.fingerprintInput(),f.capture.contentFingerprint(),f.capture.regularMembers(),f.capture.unsupportedMatchingEntries());
        assertNotSame(f.capture,equivalent);
        var child=withCapture(proof.authoredScenarios().getFirst(),equivalent);
        assertDoesNotThrow(()->VerifiedAdmittedManifestV1.fromVerifier(proof.capture(),proof.parentMember(),
                proof.occurrenceIdentity(),proof.parserProof(),proof.attributionProof(),proof.normalizedManifest(),
                proof.sourceContractIdentifiers(),List.of(child)));
        Fixture changed=fixture(manifest(scenario("other","Other")));
        assertThrows(IllegalArgumentException.class,()->VerifiedAdmittedManifestV1.fromVerifier(proof.capture(),proof.parentMember(),
                proof.occurrenceIdentity(),proof.parserProof(),proof.attributionProof(),proof.normalizedManifest(),proof.sourceContractIdentifiers(),
                List.of(withCapture(proof.authoredScenarios().getFirst(),changed.capture))));
    }

    @Test void staticProofMintingBoundaryHasOneNewVerifierCallSite() throws Exception {
        assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredConstructors())
                .allMatch(c->Modifier.isPrivate(c.getModifiers())));
        assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredMethods())
                .filter(m->m.getReturnType()==VerifiedAdmittedManifestV1.class)
                .noneMatch(m->Modifier.isPublic(m.getModifiers())||Modifier.isProtected(m.getModifiers())));
        Path root=repositoryRoot();String verifierSource=Files.readString(root.resolve(
                "qa-evidence-governance-core/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/AdmittedManifestVerifierV1.java"));
        assertTrue(verifierSource.contains("VerifiedAdmittedManifestV1.fromVerifier("));
        assertFalse(Files.exists(root.resolve(
                "qa-model-extractor/src/main/java/ru/kuznetsov/qaip/evidencegovernance/fingerprint/semantic/VerifiedAdmittedManifestSourceBridgeV1.java")));
    }

    private static NormalizedScenarioOccurrenceInputV1 withCapture(NormalizedScenarioOccurrenceInputV1 x,RepositoryCaptureAttestation capture){
        return new NormalizedScenarioOccurrenceInputV1(x.sourceNormalizationVersion(),x.scenarioSemanticCanonicalizationVersion(),
                x.scenarioSemanticContractVersion(),x.occurrenceIdentity(),x.parentMember(),capture,x.structuralLocation(),
                x.claimedIdentity(),x.exactTitle(),x.authoredGiven(),x.givenSteps(),x.authoredWhen(),x.whenSteps(),
                x.authoredThen(),x.thenSteps(),x.operationReference(),x.businessRuleReferences());
    }
    private static Path repositoryRoot(){Path current=Path.of("").toAbsolutePath();while(current!=null&&!Files.exists(current.resolve("settings.gradle")))current=current.getParent();return current;}

    private void assertCode(AdmittedManifestVerificationRejectionV1.Code code,AdmittedManifestVerificationRequestV1 request){
        assertEquals(code,assertThrows(AdmittedManifestVerificationRejectionV1.class,
                ()->verifier.verifyAdmittedManifestV1(request)).code());
    }
    private static List<String> ids(){return AdmittedManifestVerificationRequestV1.selectedV1Identifiers();}
    private static AdmittedManifestParentMemberReferenceV1 parent(Fixture f,int position,String path,BigInteger length,RawSourceMemberFingerprint raw){
        return new AdmittedManifestParentMemberReferenceV1("repo","snap",f.capture.contentFingerprint(),path,position,length,raw);
    }
    private static Fixture fixture(String text){return fixture(text.getBytes(StandardCharsets.UTF_8));}
    private static Fixture fixture(byte[] bytes){
        var raw=RawSourceMemberFingerprint.calculate(bytes);var member=new RepositoryCaptureFingerprintInput.CapturedMember("manifests/orders.json",bytes.length,raw);
        var input=new RepositoryCaptureFingerprintInput("repo","source-v1","profile-v1","discovery-v1","path-v1","order-v1",RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER,List.of(member),List.of());
        var fp=RepositoryCaptureFingerprintEncoder.fingerprint(input);
        var ref=new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference("repo","snap",fp,"manifests/orders.json",bytes.length,raw);
        var capture=RepositoryCaptureAttestation.verified("repo","snap",input,fp,List.of(ref),List.of());
        var parent=new AdmittedManifestParentMemberReferenceV1("repo","snap",fp,"manifests/orders.json",0,BigInteger.valueOf(bytes.length),raw);
        return new Fixture(bytes.clone(),capture,parent);
    }
    private static String manifest(String scenarios){return "{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\",\"authority\":\"Orders/V1\",\"scenarioIdentityScheme\":\"qaip-scenario-identity-v1\",\"scenarios\":["+scenarios+"]}";}
    private static String scenario(String key,String title){return "{\"scenarioKey\":\""+key+"\",\"title\":\""+title+"\",\"given\":[\"Given "+title+"\"],\"when\":[\"When "+title+"\"],\"then\":[\"Then "+title+"\"],\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\",\"method\":\"GET\",\"path\":\"/orders\"},\"ruleRefs\":[]}";}
    private record Fixture(byte[] bytes,RepositoryCaptureAttestation capture,AdmittedManifestParentMemberReferenceV1 parent){
        AdmittedManifestVerificationRequestV1 request(){return new AdmittedManifestVerificationRequestV1(capture,parent,bytes,ids());}
    }
}
