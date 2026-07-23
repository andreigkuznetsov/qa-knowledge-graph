package ru.kuznetsov.qaip.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kuznetsov.qagraph.change.aggregate.*;
import ru.kuznetsov.qagraph.change.base.*;
import ru.kuznetsov.qagraph.change.complete.*;
import ru.kuznetsov.qagraph.change.materialization.*;
import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.root.*;
import ru.kuznetsov.qagraph.change.validation.*;
import ru.kuznetsov.qagraph.change.verification.*;
import ru.kuznetsov.qagraph.model.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class EvidenceTestFixtures {
 static final String H1="1111111111111111111111111111111111111111111111111111111111111111", H2="2222222222222222222222222222222222222222222222222222222222222222";
 static final EvidenceSnapshotRef SNAPSHOT=new EvidenceSnapshotRef("jira","s-1",H1);
 static VerifiedChangeSet verified(String... ids) throws Exception {
  ObjectMapper mapper=new ObjectMapper();
  var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":[],\"relationships\":[]}"))).evidence();
  List<DeclaredChange> changes=new ArrayList<>();
  for(String id:ids){ var node=businessRule(mapper,id,"n"); changes.add(new DeclaredChange(ArtifactCategory.NODE,node.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(node))); }
  return run(base,changes);
 }
 static VerifiedChangeSet run(CanonicalBaseModelEvidence base,List<DeclaredChange> changes){
  var intrinsic=new IntrinsicChangeValidator().validate(new DeclaredChangeSet(changes));
  var baseResult=new BaseChangeVerifier(base).verify(intrinsic);
  var materialized=assertInstanceOf(ProposedModelMaterialized.class,new ProposedModelMaterializer().materialize(base.artifactIndex(),baseResult));
  var aggregate=assertInstanceOf(AggregateTransitionValid.class,new AggregateTransitionValidator().validate(materialized));
  var root=assertInstanceOf(ProposedRootReconstructed.class,new ProposedCanonicalRootReconstructor().reconstruct(base,aggregate));
  var complete=assertInstanceOf(CompleteProposedRootValid.class,new CompleteProposedRootValidator().validate(root));
  return assertInstanceOf(VerifiedChangeSet.class,new FinalChangeSetVerifier().verify(complete));
 }
 static VerifiedChangeSet verifiedExisting(String id, ChangeKind kind) throws Exception {
  ObjectMapper mapper=new ObjectMapper();
  String node=businessRuleJson(id,"old");
  var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":["+node+"],\"relationships\":[]}"))).evidence();
  ArtifactState before=((BaseArtifactFound)base.artifactIndex().lookup(ArtifactCategory.NODE,new CanonicalIdentity(id))).state();
  ArtifactState after=kind==ChangeKind.MODIFIED?businessRule(mapper,id,"new"):null;
  var change=new DeclaredChange(ArtifactCategory.NODE,new CanonicalIdentity(id),kind,CanonicalQaModelVersion.V0_1,Optional.of(before),Optional.ofNullable(after));
  return run(base,List.of(change));
 }
 static VerifiedChangeSet verifiedAdded(NodeArtifactState... nodes) throws Exception{
  ObjectMapper mapper=new ObjectMapper(); var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":[],\"relationships\":[]}"))).evidence();
  List<DeclaredChange> changes=Arrays.stream(nodes).map(n->new DeclaredChange(ArtifactCategory.NODE,n.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(n))).toList(); return run(base,changes);
 }
 static VerifiedChangeSet verifiedTransition(String id,boolean intoBusinessRule) throws Exception{
  ObjectMapper mapper=new ObjectMapper(); NodeArtifactState before=intoBusinessRule?check(mapper,id,"old"):businessRule(mapper,id,"old"); NodeArtifactState after=intoBusinessRule?businessRule(mapper,id,"new"):check(mapper,id,"new");
  String root="{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":["+before.snapshot()+"],\"relationships\":[]}"; var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree(root))).evidence();
  var change=new DeclaredChange(ArtifactCategory.NODE,before.identity(),ChangeKind.MODIFIED,CanonicalQaModelVersion.V0_1,Optional.of(before),Optional.of(after)); return run(base,List.of(change));
 }
 static VerifiedChangeSet verifiedWithRelationshipCollision() throws Exception{
  ObjectMapper mapper=new ObjectMapper(); var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":[],\"relationships\":[]}"))).evidence();
  NodeArtifactState first=businessRule(mapper,"BR-A","a"), second=businessRule(mapper,"BR-B","b");
  RelationshipArtifactState relationship=new RelationshipArtifactState(CanonicalQaModelVersion.V0_1,mapper.readTree("{\"id\":\"BR-A\",\"from\":\"BR-A\",\"type\":\"DEPENDS_ON\",\"to\":\"BR-B\"}"));
  List<DeclaredChange> changes=List.of(
    new DeclaredChange(ArtifactCategory.NODE,first.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(first)),
    new DeclaredChange(ArtifactCategory.NODE,second.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(second)),
    new DeclaredChange(ArtifactCategory.RELATIONSHIP,relationship.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(relationship)));
  return run(base,changes);
 }
 static ProvenanceRef provenance(String id){return new ProvenanceRef(id,"jira://issue/1",H2,"map",ImpactEvidenceVersions.NORMALIZATION);}
 static ArtifactIdentityAssertion resolved(String local,String canonical){return new ArtifactIdentityAssertion("a-"+local,SNAPSHOT,local,NodeType.BUSINESS_RULE,new ResolvedIdentity(new CanonicalIdentity(canonical)),H1,"p-1");}
 static ArtifactIdentityAssertion unresolved(String local){return new ArtifactIdentityAssertion("a-"+local,SNAPSHOT,local,NodeType.BUSINESS_RULE,new UnresolvedIdentity("NO_MAPPING"),H1,"p-1");}
 static RelationshipEvidence relation(String id,String dependent,String dependency){return new RelationshipEvidence(id,SNAPSHOT,dependent,dependency,RelationshipType.DEPENDS_ON,"blocks",ImpactEvidenceVersions.NORMALIZATION,H2,"p-1");}
 static FrozenEvidenceManifest manifest(List<ArtifactIdentityAssertion>a,List<RelationshipEvidence>r){return FrozenEvidenceManifest.create("jira",SNAPSHOT,a,r,List.of(provenance("p-1")));}
 static ImpactEvidenceRequest request(VerifiedChangeSet v,FrozenEvidenceManifest m,String s){return new ImpactEvidenceRequest(v,m,new SubjectArtifactRef(s),SliceAnalysisContext.supported());}
 static NodeArtifactState businessRule(ObjectMapper mapper,String id,String name) throws Exception{return new NodeArtifactState(CanonicalQaModelVersion.V0_1,mapper.readTree(businessRuleJson(id,name)));}
 static NodeArtifactState check(ObjectMapper mapper,String id,String name) throws Exception{return new NodeArtifactState(CanonicalQaModelVersion.V0_1,mapper.readTree("{\"id\":\""+id+"\",\"type\":\"CHECK\",\"name\":\""+name+"\",\"check\":{\"checkType\":\"SQL\",\"assertion\":\"ok\"}}"));}
 private static String businessRuleJson(String id,String name){return "{\"id\":\""+id+"\",\"type\":\"BUSINESS_RULE\",\"name\":\""+name+"\",\"rule\":{\"code\":\""+id+"\",\"ruleType\":\"BUSINESS_INVARIANT\",\"text\":\"rule\"}}";}
 private EvidenceTestFixtures(){}
}
