package ru.kuznetsov.qaip.evidence;

import org.junit.jupiter.api.Test;
import ru.kuznetsov.qagraph.model.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidence.EvidenceTestFixtures.*;

class QualificationAndTraversalTest {
 private final ImpactEvidenceAnalyzer analyzer=new ImpactEvidenceAnalyzer();
 @Test void reportsAllStableQualificationReasons() throws Exception{
  var wrongType=new ArtifactIdentityAssertion("a-s",SNAPSHOT,"s",NodeType.CHECK,new ResolvedIdentity(new ru.kuznetsov.qagraph.change.model.CanonicalIdentity("BR-S")),H1,"p-1");
  var relation=new RelationshipEvidence("bad",SNAPSHOT,"s","c",RelationshipType.RELATED_TO,"related","old-normalizer",H2,"missing");
  var conclusion=done(analyzer.analyze(request(verified("BR-C"),manifest(List.of(resolved("c","BR-C"),wrongType,resolved("subject","BR-SUBJECT")),List.of(relation)),"subject")));
  assertEquals(List.of(QualificationReason.MISSING_PROVENANCE,QualificationReason.WRONG_RELATIONSHIP_TYPE,QualificationReason.WRONG_SOURCE_ENDPOINT_TYPE,QualificationReason.UNSUPPORTED_NORMALIZATION),conclusion.rejectedEvidence().getFirst().reasons());
 }
 @Test void unresolvedEndpointNeverEntersProof() throws Exception{
  var conclusion=done(analyzer.analyze(request(verified("BR-C"),manifest(List.of(resolved("c","BR-C"),unresolved("s"),resolved("subject","BR-S")),List.of(relation("edge","s","c"))),"subject")));
  assertEquals(ImpactClassification.UNKNOWN,conclusion.classification()); assertEquals(QualificationReason.UNRESOLVED_SOURCE_ENDPOINT,conclusion.rejectedEvidence().getFirst().reasons().getFirst());
 }
 @Test void cyclesTerminateAndShortestQualifiedPathWins() throws Exception{
  var m=manifest(List.of(resolved("c","BR-C"),resolved("a","BR-A"),resolved("s","BR-S")),List.of(relation("c-a","a","c"),relation("a-c","c","a"),relation("a-s","s","a")));
  var proof=(RelationshipPathProof)done(analyzer.analyze(request(verified("BR-C"),m,"s"))).proof().orElseThrow(); assertEquals(2,proof.steps().size());
 }
 @Test void declarationOrderedRootsSelectDeterministicProof() throws Exception{
  var m=manifest(List.of(resolved("one","BR-1"),resolved("two","BR-2"),resolved("s","BR-S")),List.of(relation("from-2","s","two"),relation("from-1","s","one")));
  var proof=(RelationshipPathProof)done(analyzer.analyze(request(verified("BR-2","BR-1"),m,"s"))).proof().orElseThrow(); assertEquals("BR-2",proof.changedIdentity().value());
 }
 @Test void brokenEndpointIsManifestFailure() throws Exception{
  var m=manifest(List.of(resolved("c","BR-C")),List.of(relation("broken","missing","c")));
  assertEquals(FailureCode.INVALID_MANIFEST,assertInstanceOf(ImpactEvidenceFailed.class,analyzer.analyze(request(verified("BR-C"),m,"c"))).failure().code());
 }
 private ImpactConclusion done(ImpactEvidenceResult r){return assertInstanceOf(ImpactEvidenceCompleted.class,r).conclusion();}
}
