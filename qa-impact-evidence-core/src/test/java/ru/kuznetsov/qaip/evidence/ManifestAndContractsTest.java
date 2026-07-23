package ru.kuznetsov.qaip.evidence;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidence.EvidenceTestFixtures.*;

class ManifestAndContractsTest {
 @Test void canonicalFingerprintHasGoldenVectorAndCoversSemanticMutation(){
  var original=manifest(List.of(resolved("a","BR-A")),List.of());
  assertEquals("8d7802a43e9a8e64d173f73cc9678f49bbcd2ba22b8605c2b8234f1c0fa9e530",original.manifestFingerprint());
  var changed=FrozenEvidenceManifest.create("jira",SNAPSHOT,List.of(resolved("a","BR-B")),List.of(),List.of(provenance("p-1"))); assertNotEquals(original.manifestFingerprint(),changed.manifestFingerprint());
 }
 @Test void fingerprintMismatchIsFailureNotUnknown() throws Exception{
  var good=manifest(List.of(resolved("a","BR-A")),List.of());
  var bad=new FrozenEvidenceManifest(good.contractVersion(),good.sourceId(),good.snapshot(),good.normalizationVersion(),good.canonicalizationVersion(),H1,good.identityAssertions(),good.relationships(),good.provenance());
  var result=assertInstanceOf(ImpactEvidenceFailed.class,new ImpactEvidenceAnalyzer().analyze(request(verified("BR-X"),bad,"a"))); assertEquals(FailureCode.INTEGRITY_MISMATCH,result.failure().code());
 }
 @Test void unsupportedVersionAndDuplicateIdentityFail() throws Exception{
  var a=resolved("a","BR-A"); var unsigned=new FrozenEvidenceManifest("future","jira",SNAPSHOT,ImpactEvidenceVersions.NORMALIZATION,ImpactEvidenceVersions.CANONICAL,"x",List.of(a),List.of(),List.of(provenance("p-1")));
  assertEquals(FailureCode.UNSUPPORTED_VERSION,assertInstanceOf(ImpactEvidenceFailed.class,new ImpactEvidenceAnalyzer().analyze(request(verified("BR-X"),unsigned,"a"))).failure().code());
  var duplicate=FrozenEvidenceManifest.create("jira",SNAPSHOT,List.of(a,a),List.of(),List.of(provenance("p-1")));
  assertEquals(FailureCode.INVALID_MANIFEST,assertInstanceOf(ImpactEvidenceFailed.class,new ImpactEvidenceAnalyzer().analyze(request(verified("BR-X"),duplicate,"a"))).failure().code());
 }
 @Test void collectionsAreImmutableAndNullPolicyIsExplicit(){
  var m=manifest(List.of(resolved("a","BR-A")),List.of()); assertThrows(UnsupportedOperationException.class,()->m.identityAssertions().clear());
  assertEquals(FailureCode.INVALID_REQUEST,assertInstanceOf(ImpactEvidenceFailed.class,new ImpactEvidenceAnalyzer().analyze(null)).failure().code());
  assertThrows(NullPointerException.class,()->new SubjectArtifactRef(null));
 }
}
