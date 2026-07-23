package ru.kuznetsov.qaip.evidence;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qaip.evidence.EvidenceTestFixtures.*;

class ImpactEvidenceAnalyzerTest {
 private final ImpactEvidenceAnalyzer analyzer=new ImpactEvidenceAnalyzer();
 @Test void directProofUsesExactAcceptedEvidenceAndWins() throws Exception{
  var accepted=verified("BR-1"); var m=manifest(List.of(resolved("one","BR-1"),resolved("two","BR-2")),List.of(relation("r-1","one","two"),relation("r-2","two","one")));
  var proof=assertInstanceOf(DirectChangeProof.class,done(analyzer.analyze(request(accepted,m,"one"))).proof().orElseThrow()); assertSame(accepted,proof.verifiedChangeSet()); assertEquals(0,proof.declarationIndex());
 }
 @Test void modifiedAndRemovedSubjectsAreDirectlyAffected() throws Exception{
  var m=manifest(List.of(resolved("s","BR-X")),List.of());
  for(var kind:List.of(ru.kuznetsov.qagraph.change.model.ChangeKind.MODIFIED,ru.kuznetsov.qagraph.change.model.ChangeKind.REMOVED)){
   var proof=(DirectChangeProof)done(analyzer.analyze(request(verifiedExisting("BR-X",kind),m,"s"))).proof().orElseThrow(); assertEquals(kind,proof.changeKind());
  }
 }
 @Test void traversesDependencyToDependentWithOrderedEvidence() throws Exception{
  var m=manifest(List.of(resolved("base","BR-BASE"),resolved("mid","BR-MID"),resolved("subject","BR-S")),List.of(relation("r-2","subject","mid"),relation("r-1","mid","base")));
  var proof=assertInstanceOf(RelationshipPathProof.class,done(analyzer.analyze(request(verified("BR-BASE"),m,"subject"))).proof().orElseThrow());
  assertEquals(List.of("r-1","r-2"),proof.steps().stream().map(s->s.evidence().datumId()).toList()); assertEquals("BR-BASE",proof.steps().getFirst().propagationFrom().value());
 }
 @Test void noPathIsUnknownAndNegativeVocabularyDoesNotExist() throws Exception{
  var m=manifest(List.of(resolved("c","BR-C"),resolved("s","BR-S")),List.of()); var result=done(analyzer.analyze(request(verified("BR-C"),m,"s")));
  assertEquals(ImpactClassification.UNKNOWN,result.classification()); assertEquals(List.of(UnknownReason.NO_QUALIFIED_IMPACT_PROOF),result.unknownReasons()); assertEquals(2,ImpactClassification.values().length);
 }
 @Test void unresolvedSubjectIsExplicitUnknown() throws Exception{
  var result=done(analyzer.analyze(request(verified("BR-C"),manifest(List.of(resolved("c","BR-C"),unresolved("s")),List.of()),"s"))); assertEquals(List.of(UnknownReason.UNRESOLVED_SUBJECT_IDENTITY),result.unknownReasons());
 }
 @Test void unqualifiedStructuralPathCannotBecomeProof() throws Exception{
  var bad=new RelationshipEvidence("r-bad",new EvidenceSnapshotRef("jira","other",H1),"s","c",ru.kuznetsov.qagraph.model.RelationshipType.DEPENDS_ON,"blocks",ImpactEvidenceVersions.NORMALIZATION,H2,"missing");
  var result=done(analyzer.analyze(request(verified("BR-C"),manifest(List.of(resolved("c","BR-C"),resolved("s","BR-S")),List.of(bad)),"s")));
  assertEquals(ImpactClassification.UNKNOWN,result.classification()); assertEquals(List.of(QualificationReason.SNAPSHOT_MISMATCH,QualificationReason.MISSING_PROVENANCE),result.rejectedEvidence().getFirst().reasons());
 }
 @Test void replayAndShortestPathTieBreakAreDeterministic() throws Exception{
  var v=verified("BR-C"); var a=List.of(resolved("c","BR-C"),resolved("a","BR-A"),resolved("b","BR-B"),resolved("s","BR-S"));
  var r=List.of(relation("z-to-a","a","c"),relation("z-a-s","s","a"),relation("a-to-b","b","c"),relation("a-b-s","s","b")); var first=manifest(a,r); var reversed=new ArrayList<>(r); Collections.reverse(reversed); var second=manifest(a.reversed(),reversed);
  assertEquals(first.manifestFingerprint(),second.manifestFingerprint()); assertEquals(analyzer.analyze(request(v,first,"s")),analyzer.analyze(request(v,second,"s")));
  var proof=(RelationshipPathProof)done(analyzer.analyze(request(v,first,"s"))).proof().orElseThrow(); assertEquals(List.of("z-to-a","z-a-s"),proof.steps().stream().map(x->x.evidence().datumId()).toList());
 }
 private ImpactConclusion done(ImpactEvidenceResult r){return assertInstanceOf(ImpactEvidenceCompleted.class,r).conclusion();}
}
