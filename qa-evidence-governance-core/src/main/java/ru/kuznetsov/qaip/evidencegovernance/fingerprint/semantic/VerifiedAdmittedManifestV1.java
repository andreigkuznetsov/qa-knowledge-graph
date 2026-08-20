package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
/** Factory-controlled admitted Manifest proof; construction is package-bound to the source bridge. */
public final class VerifiedAdmittedManifestV1 {
 public static final String VERSION="scenario-authority-verified-admitted-manifest-v1";
 private final RepositoryCaptureAttestation capture; private final AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember;
 private final NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity; private final String claimedAuthority;
 private final Object authoritativeAdmissionResult; private final Object exactNormalizedManifest; private final String format,schemaVersion,scenarioIdentityScheme;
 private final List<String> sourceContractIdentifiers; private final List<NormalizedScenarioOccurrenceInputV1> authoredScenarios;
 private VerifiedAdmittedManifestV1(RepositoryCaptureAttestation c,AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference p,
   NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity o,String a,Object admission,Object normalized,String f,String s,String scheme,List<String> ids,List<NormalizedScenarioOccurrenceInputV1> children){
  capture=c;parentMember=p;occurrenceIdentity=o;claimedAuthority=a;authoritativeAdmissionResult=admission;exactNormalizedManifest=normalized;format=f;schemaVersion=s;scenarioIdentityScheme=scheme;sourceContractIdentifiers=ids;authoredScenarios=children;}
 static VerifiedAdmittedManifestV1 fromAuthoritativeSource(RepositoryCaptureAttestation c,AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference p,
   NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity o,String a,Object admission,Object normalized,String f,String s,String scheme,List<String> ids,List<NormalizedScenarioOccurrenceInputV1> supplied){
  Objects.requireNonNull(c);Objects.requireNonNull(p);Objects.requireNonNull(o);text(a);Objects.requireNonNull(admission);Objects.requireNonNull(normalized);text(f);text(s);text(scheme);
  var versions=List.copyOf(Objects.requireNonNull(ids));if(versions.stream().anyMatch(x->x==null||x.isEmpty()))throw new IllegalArgumentException("source identifiers must be complete");
  var children=List.copyOf(Objects.requireNonNull(supplied));
  if(!c.sourceId().equals(p.parentSourceId())||!c.snapshotId().equals(p.parentSnapshotId())||!c.contentFingerprint().equals(p.parentContentFingerprint())||!c.regularMembers().contains(p))throw new IllegalArgumentException("parent is not capture-attested");
  if(!o.parentSourceId().equals(p.parentSourceId())||!o.parentSnapshotId().equals(p.parentSnapshotId())||!o.parentFingerprint().equals(p.parentContentFingerprint())||!o.memberPath().equals(p.normalizedRepositoryRelativePath())||!NormalizedScenarioOccurrenceInputV1.MANIFEST_OCCURRENCE_IDENTITY_VERSION.equals(o.identityVersion()))throw new IllegalArgumentException("Manifest occurrence mismatch");
  for(int i=0;i<children.size();i++){var x=children.get(i);if(x.repositoryCaptureAttestation()!=c||!x.parentMember().equals(p)||!x.occurrenceIdentity().manifest().equals(o)||!x.claimedIdentity().authority().equals(a)||!x.structuralLocation().equals("/scenarios/"+i)||!x.occurrenceIdentity().structuralPath().equals(x.structuralLocation()))throw new IllegalArgumentException("authored Scenario mismatch");}
  return new VerifiedAdmittedManifestV1(c,p,o,a,admission,normalized,f,s,scheme,versions,children);
 }
 public RepositoryCaptureAttestation capture(){return capture;} public AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember(){return parentMember;}
 public NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity(){return occurrenceIdentity;} public String claimedAuthority(){return claimedAuthority;}
 public String format(){return format;}public String schemaVersion(){return schemaVersion;}public String scenarioIdentityScheme(){return scenarioIdentityScheme;}
 public List<String> sourceContractIdentifiers(){return sourceContractIdentifiers;}public List<NormalizedScenarioOccurrenceInputV1> authoredScenarios(){return authoredScenarios;}
 Object authoritativeAdmissionResult(){return authoritativeAdmissionResult;}Object exactNormalizedManifest(){return exactNormalizedManifest;}
 private static void text(String x){if(x==null||x.isEmpty())throw new IllegalArgumentException("text must not be empty");}
}
