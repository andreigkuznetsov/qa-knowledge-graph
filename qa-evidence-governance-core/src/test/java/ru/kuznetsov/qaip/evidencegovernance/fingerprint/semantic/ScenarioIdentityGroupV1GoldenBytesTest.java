package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Literal complete-byte and final-fingerprint goldens for every normative V1 group variant. */
class ScenarioIdentityGroupV1GoldenBytesTest {
    private static final String ROOT="/golden/scenario-identity-group-v1/";
    private static final byte[] DOMAIN_BYTES=Base64.getDecoder().decode("AAAAAAAAADJRQUlQAFNDRU5BUklPX0FVVEhPUklUWV9TQ0VOQVJJT19JREVOVElUWV9HUk9VUABWMQ==");
    @Test void literalResourcesLockAllElevenCompleteCanonicalSequences() throws Exception {
        for(var e:vectors().entrySet()){
            byte[] actual=ScenarioIdentityGroupComposerV1.canonicalBytes(e.getValue());
            assertArrayEquals(resourceBytes(e.getKey()+".canonical.b64"),actual,e.getKey());
            assertEquals(resourceText(e.getKey()+".fingerprint.txt"),e.getValue().fingerprint().value(),e.getKey());
            assertArrayEquals(DOMAIN_BYTES,Arrays.copyOf(actual,DOMAIN_BYTES.length),e.getKey()+" domain");
        }
    }
    static Map<String,VerifiedScenarioIdentityGroupV1> vectors(){
        var f=ScenarioIdentityGroupComposerV1Test.fixture();var result=new LinkedHashMap<String,VerifiedScenarioIdentityGroupV1>();
        var a2=composed(f,"a.json",2,"same");var a10=composed(f,"a.json",10,"same");var b2=composed(f,"b.json",2,"different");
        var unsupportedA=unavailable(ScenarioIdentityGroupComposerV1Test.input(f,"a.json",2,"u","future-source"));
        var unsupportedB=unavailable(ScenarioIdentityGroupComposerV1Test.input(f,"b.json",10,"u","future-source"));
        var integrityA=integrity(f,"a.json",2);var integrityB=integrity(f,"b.json",10);
        result.put("unique",ScenarioIdentityGroupComposerV1Test.compose(a2));
        result.put("duplicate-equivalent",ScenarioIdentityGroupComposerV1Test.compose(a2,a10));
        result.put("duplicate-conflicting",ScenarioIdentityGroupComposerV1Test.compose(a2,b2));
        result.put("duplicate-unclassified-unsupported",ScenarioIdentityGroupComposerV1Test.compose(unsupportedA,unsupportedB));
        result.put("duplicate-unclassified-integrity",ScenarioIdentityGroupComposerV1Test.compose(integrityA,integrityB));
        result.put("mixed-composed-unavailable",ScenarioIdentityGroupComposerV1Test.compose(a2,unsupportedB));
        result.put("multiple-unavailable",ScenarioIdentityGroupComposerV1Test.compose(unsupportedA,integrityB));
        result.put("relocation",ScenarioIdentityGroupComposerV1Test.compose(composed(f,"b.json",2,"same")));
        result.put("changed-index",ScenarioIdentityGroupComposerV1Test.compose(composed(f,"a.json",10,"same")));
        result.put("numeric-2-10",ScenarioIdentityGroupComposerV1Test.compose(a2,a10));
        var u=ScenarioIdentityGroupComposerV1Test.fixture("\uE000.json","😀.json");
        result.put("unicode-order",ScenarioIdentityGroupComposerV1Test.compose(composed(u,"\uE000.json",2,"same"),composed(u,"😀.json",2,"same")));
        return result;
    }
    private static ScenarioOccurrenceCompositionOutcomeV1.Composed composed(ScenarioIdentityGroupComposerV1Test.Fixture f,String p,int i,String title){return (ScenarioOccurrenceCompositionOutcomeV1.Composed)ScenarioIdentityGroupComposerV1Test.attempt(ScenarioIdentityGroupComposerV1Test.input(f,p,i,title));}
    private static ScenarioOccurrenceCompositionOutcomeV1.Unavailable unavailable(NormalizedScenarioOccurrenceInputV1 o){return (ScenarioOccurrenceCompositionOutcomeV1.Unavailable)ScenarioIdentityGroupComposerV1Test.attempt(o);}
    private static ScenarioOccurrenceCompositionOutcomeV1.Unavailable integrity(ScenarioIdentityGroupComposerV1Test.Fixture f,String p,int i){var o=ScenarioIdentityGroupComposerV1Test.input(f,p,i,"i");var s=o.givenSteps().getFirst();var bad=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(s.claimedIdentity(),StepSemanticFingerprintInput.Phase.WHEN,s.ordinal(),s.identityVersion(),s.exactAuthoredText(),s.semanticCanonicalizationVersion());var changed=new NormalizedScenarioOccurrenceInputV1(o.sourceNormalizationVersion(),o.scenarioSemanticCanonicalizationVersion(),o.scenarioSemanticContractVersion(),o.occurrenceIdentity(),o.parentMember(),o.repositoryCaptureAttestation(),o.structuralLocation(),o.claimedIdentity(),o.exactTitle(),o.authoredGiven(),List.of(bad),o.authoredWhen(),o.whenSteps(),o.authoredThen(),o.thenSteps(),o.operationReference(),o.businessRuleReferences());return unavailable(changed);}
    private static byte[] resourceBytes(String name)throws Exception{return Base64.getMimeDecoder().decode(resourceText(name));}
    private static String resourceText(String name)throws Exception{try(InputStream in=ScenarioIdentityGroupV1GoldenBytesTest.class.getResourceAsStream(ROOT+name)){assertNotNull(in,name);return new String(in.readAllBytes(),StandardCharsets.US_ASCII).trim();}}
}
