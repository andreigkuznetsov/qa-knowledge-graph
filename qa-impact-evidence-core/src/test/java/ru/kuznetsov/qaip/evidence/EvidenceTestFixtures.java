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
  for(String id:ids){ var node=new NodeArtifactState(CanonicalQaModelVersion.V0_1,mapper.readTree("{\"id\":\""+id+"\",\"type\":\"CHECK\",\"name\":\"n\",\"check\":{\"checkType\":\"SQL\",\"assertion\":\"ok\"}}")); changes.add(new DeclaredChange(ArtifactCategory.NODE,node.identity(),ChangeKind.ADDED,CanonicalQaModelVersion.V0_1,Optional.empty(),Optional.of(node))); }
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
  String node="{\"id\":\""+id+"\",\"type\":\"CHECK\",\"name\":\"old\",\"check\":{\"checkType\":\"SQL\",\"assertion\":\"ok\"}}";
  var base=((CanonicalBaseEvidenceExtracted)new CanonicalBaseEvidenceExtractor().extract(mapper.readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":["+node+"],\"relationships\":[]}"))).evidence();
  ArtifactState before=((BaseArtifactFound)base.artifactIndex().lookup(ArtifactCategory.NODE,new CanonicalIdentity(id))).state();
  ArtifactState after=kind==ChangeKind.MODIFIED?new NodeArtifactState(CanonicalQaModelVersion.V0_1,mapper.readTree(node.replace("old","new"))):null;
  var change=new DeclaredChange(ArtifactCategory.NODE,new CanonicalIdentity(id),kind,CanonicalQaModelVersion.V0_1,Optional.of(before),Optional.ofNullable(after));
  var intrinsic=new IntrinsicChangeValidator().validate(new DeclaredChangeSet(List.of(change))); var baseResult=new BaseChangeVerifier(base).verify(intrinsic);
  var materialized=assertInstanceOf(ProposedModelMaterialized.class,new ProposedModelMaterializer().materialize(base.artifactIndex(),baseResult)); var aggregate=assertInstanceOf(AggregateTransitionValid.class,new AggregateTransitionValidator().validate(materialized));
  var root=assertInstanceOf(ProposedRootReconstructed.class,new ProposedCanonicalRootReconstructor().reconstruct(base,aggregate)); var complete=assertInstanceOf(CompleteProposedRootValid.class,new CompleteProposedRootValidator().validate(root)); return assertInstanceOf(VerifiedChangeSet.class,new FinalChangeSetVerifier().verify(complete));
 }
 static ProvenanceRef provenance(String id){return new ProvenanceRef(id,"jira://issue/1",H2,"map",ImpactEvidenceVersions.NORMALIZATION);}
 static ArtifactIdentityAssertion resolved(String local,String canonical){return new ArtifactIdentityAssertion("a-"+local,SNAPSHOT,local,NodeType.BUSINESS_RULE,new ResolvedIdentity(new CanonicalIdentity(canonical)),H1,"p-1");}
 static ArtifactIdentityAssertion unresolved(String local){return new ArtifactIdentityAssertion("a-"+local,SNAPSHOT,local,NodeType.BUSINESS_RULE,new UnresolvedIdentity("NO_MAPPING"),H1,"p-1");}
 static RelationshipEvidence relation(String id,String dependent,String dependency){return new RelationshipEvidence(id,SNAPSHOT,dependent,dependency,RelationshipType.DEPENDS_ON,"blocks",ImpactEvidenceVersions.NORMALIZATION,H2,"p-1");}
 static FrozenEvidenceManifest manifest(List<ArtifactIdentityAssertion>a,List<RelationshipEvidence>r){return FrozenEvidenceManifest.create("jira",SNAPSHOT,a,r,List.of(provenance("p-1")));}
 static ImpactEvidenceRequest request(VerifiedChangeSet v,FrozenEvidenceManifest m,String s){return new ImpactEvidenceRequest(v,m,new SubjectArtifactRef(s),SliceAnalysisContext.supported());}
 private EvidenceTestFixtures(){}
}
