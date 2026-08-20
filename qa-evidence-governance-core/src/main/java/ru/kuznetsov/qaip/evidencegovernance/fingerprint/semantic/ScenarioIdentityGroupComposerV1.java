package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalBinaryWriter;
import ru.kuznetsov.qaip.evidencegovernance.canonical.CanonicalSha256;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RawSourceMemberFingerprint;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.RepositoryCaptureFingerprint;
import java.math.BigInteger; import java.util.*;

/** Sole validator, state derivation, canonical encoder and fingerprinter for V1 groups. */
public final class ScenarioIdentityGroupComposerV1 {
    public static final String DOMAIN="QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_IDENTITY_GROUP\u0000V1";
    public static final String ENCODING_IDENTIFIER="scenario-authority-scenario-identity-group-c14n-v1";
    public static final String DIGEST_IDENTIFIER=CanonicalSha256.ALGORITHM_IDENTIFIER;
    public static final String DUPLICATE_OUTCOME_CONTRACT="scenario-authority-duplicate-outcomes-v1";
    private ScenarioIdentityGroupComposerV1(){}
    public static VerifiedScenarioIdentityGroupV1 compose(String duplicateContract,
            ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity identity,List<ScenarioOccurrenceCompositionOutcomeV1> occurrences){
        Objects.requireNonNull(identity); occurrences=List.copyOf(Objects.requireNonNull(occurrences));
        if(!DUPLICATE_OUTCOME_CONTRACT.equals(duplicateContract)||occurrences.isEmpty())throw new IllegalArgumentException("unsupported or empty group");
        String previous=null; BigInteger previousIndex=null;
        for(var outcome:occurrences){var o=outcome.occurrence(); if(!identity.equals(o.claimedIdentity()))throw new IllegalArgumentException("group/occurrence identity mismatch");
            // Re-run the sole attempt to reject substituted/fabricated outcome objects.
            var verified=ScenarioOccurrenceCompositionAttemptV1.attemptScenarioOccurrenceCompositionV1(o);
            if(!same(verified,outcome))throw new IllegalArgumentException("occurrence outcome substitution");
            String path=o.parentMember().normalizedRepositoryRelativePath(); BigInteger index=index(o.structuralLocation());
            if(previous!=null){int c=codePoints(previous,path); if(c>0||(c==0&&previousIndex.compareTo(index)>=0))throw new IllegalArgumentException("noncanonical or duplicate occurrence ordering key");}
            previous=path;previousIndex=index;
        }
        var state=derive(occurrences); if(state==VerifiedScenarioIdentityGroupV1.State.UNIQUE&&!(occurrences.getFirst() instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed))throw new IllegalArgumentException("singleton unavailable cannot form V1 group");
        byte[] bytes=encode(identity,state,occurrences); var fp=new ScenarioIdentityGroupFingerprint(ScenarioIdentityGroupFingerprint.VALUE_PREFIX+CanonicalSha256.lowercaseHexDigest(bytes));
        return new VerifiedScenarioIdentityGroupV1(identity,state,occurrences,fp);
    }
    public static byte[] canonicalBytes(VerifiedScenarioIdentityGroupV1 group){
        var rebuilt=compose(DUPLICATE_OUTCOME_CONTRACT,group.claimedIdentity(),group.occurrences());
        if(rebuilt.state()!=group.state()||!rebuilt.fingerprint().equals(group.fingerprint()))throw new IllegalArgumentException("fabricated group");
        return encode(group.claimedIdentity(),group.state(),group.occurrences());
    }
    private static VerifiedScenarioIdentityGroupV1.State derive(List<ScenarioOccurrenceCompositionOutcomeV1> o){if(o.size()==1)return VerifiedScenarioIdentityGroupV1.State.UNIQUE;
        if(o.stream().anyMatch(x->x instanceof ScenarioOccurrenceCompositionOutcomeV1.Unavailable))return VerifiedScenarioIdentityGroupV1.State.DUPLICATE_UNCLASSIFIED;
        long n=o.stream().map(x->((ScenarioOccurrenceCompositionOutcomeV1.Composed)x).fingerprint()).distinct().limit(2).count();return n==1?VerifiedScenarioIdentityGroupV1.State.DUPLICATE_EQUIVALENT:VerifiedScenarioIdentityGroupV1.State.DUPLICATE_CONFLICTING;}
    private static byte[] encode(ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity id,VerifiedScenarioIdentityGroupV1.State state,List<ScenarioOccurrenceCompositionOutcomeV1> os){var w=new CanonicalBinaryWriter().writeDomain(DOMAIN).writeText(ENCODING_IDENTIFIER).writeText(DIGEST_IDENTIFIER).writeText(DUPLICATE_OUTCOME_CONTRACT).writeText(id.authority()).writeText(id.scenarioKey()).writeText(id.identitySchemeVersion()).writeText(state.name());
        return w.writeOrderedCollection(os,(x,v)->occurrence(x,v)).toByteArray();}
    private static void occurrence(CanonicalBinaryWriter w,ScenarioOccurrenceCompositionOutcomeV1 out){var o=out.occurrence();var m=o.occurrenceIdentity().manifest();var p=o.parentMember();
        w.writeText(m.parentSourceId()).writeText(m.parentSnapshotId()).writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER).writeText(m.parentFingerprint().value()).writeText(m.memberPath()).writeText(m.identityVersion()).writeText(o.occurrenceIdentity().structuralPath()).writeText(o.occurrenceIdentity().identityVersion())
         .writeText(p.parentSourceId()).writeText(p.parentSnapshotId()).writeText(RepositoryCaptureFingerprint.VALUE_IDENTIFIER).writeText(p.parentContentFingerprint().value()).writeText(p.normalizedRepositoryRelativePath()).writeUnsigned64(p.rawByteLength()).writeText(RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER).writeText(p.rawMemberFingerprint().value()).writeText(o.structuralLocation());
        if(out instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed c){w.writePresent(x->x.writeText(ScenarioSemanticFingerprint.VALUE_IDENTIFIER).writeText(c.fingerprint().value())).writeAbsent();}
        else {var u=(ScenarioOccurrenceCompositionOutcomeV1.Unavailable)out;w.writeAbsent().writePresent(x->x.writeText(u.reason().name()));}}
    private static boolean same(ScenarioOccurrenceCompositionOutcomeV1 a,ScenarioOccurrenceCompositionOutcomeV1 b){if(a.getClass()!=b.getClass()||a.occurrence()!=b.occurrence())return false;if(a instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed x)return x.fingerprint().equals(((ScenarioOccurrenceCompositionOutcomeV1.Composed)b).fingerprint());return ((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)a).reason()==((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)b).reason();}
    private static BigInteger index(String p){return new BigInteger(p.substring(p.lastIndexOf('/')+1));}
    private static int codePoints(String a,String b){var x=a.codePoints().iterator();var y=b.codePoints().iterator();while(x.hasNext()&&y.hasNext()){int c=Integer.compare(x.nextInt(),y.nextInt());if(c!=0)return c;}return x.hasNext()?1:y.hasNext()?-1:0;}
}
