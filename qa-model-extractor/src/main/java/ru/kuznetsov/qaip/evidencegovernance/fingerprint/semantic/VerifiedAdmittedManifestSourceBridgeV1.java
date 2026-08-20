package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
import ru.kuznetsov.qagraph.extractor.repositoryanalysis.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedManifestDatum;
/** The only bridge from the factory-controlled complete source handoff to an admitted Manifest proof. */
public final class VerifiedAdmittedManifestSourceBridgeV1 {
 private VerifiedAdmittedManifestSourceBridgeV1(){}
 public static VerifiedAdmittedManifestV1 fromCompleteHandoff(ScenarioAuthorityNormalizedProcessingV1 h,NormalizedManifestDatum m){
  Objects.requireNonNull(h);Objects.requireNonNull(m);if(!h.normalizationResult().memberOutcomes().contains(m))throw new IllegalArgumentException("Manifest is not in the complete verified handoff");
  var admission=m.sourceAdmission();if(admission.structuralAdmissionState()!=AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED)throw new IllegalArgumentException("Manifest is not structurally admitted");
  var p=ref(admission.parentMemberRef());var mi=m.occurrenceIdentity();var id=new NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity(mi.parentSourceId(),mi.parentSnapshotId(),mi.parentContentFingerprint(),mi.normalizedRepositoryRelativePath(),mi.identityVersion());
  var bindings=h.occurrences().stream().filter(b->b.declaration().occurrenceIdentity().manifestOccurrenceIdentity().equals(mi)).toList();
  if(bindings.size()!=m.scenarioDeclarations().size())throw new IllegalArgumentException("derived Scenario enumeration is incomplete");
  for(int i=0;i<bindings.size();i++)if(!bindings.get(i).declaration().equals(m.scenarioDeclarations().get(i)))throw new IllegalArgumentException("Scenario enumeration was substituted");
  var mapper=new ScenarioOccurrenceInputMapperV1();var children=bindings.stream().map(b->mapper.map(h,b)).toList();
  var c=h.contracts();var versions=List.of(VERSION(admission.parserContractIdentifier()),VERSION(admission.attributionContractIdentifier()),VERSION(admission.schemaContractIdentifier()),VERSION(c.sourceNormalizationVersion()),VERSION(c.scenarioSemanticCanonicalizationVersion()),VERSION(c.scenarioSemanticContractVersion()),VERSION(c.stepSemanticCanonicalizationVersion()),VERSION(c.operationReferenceSemanticCanonicalizationVersion()),VERSION(c.businessRuleReferenceSemanticCanonicalizationVersion()));
  return VerifiedAdmittedManifestV1.fromAuthoritativeSource(h.captureAttestation(),p,id,m.claimedAuthority(),admission,m,m.format(),m.schemaVersion(),m.scenarioIdentityScheme(),versions,children);
 }
 private static String VERSION(String x){return Objects.requireNonNull(x);}
 private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference ref(ParentCapturedMemberRef p){return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(p.parentSourceId(),p.parentSnapshotId(),p.parentContentFingerprint(),p.normalizedRepositoryRelativePath(),p.rawByteLength(),p.rawMemberFingerprint());}
}
