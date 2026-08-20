package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;
import java.util.List; import java.util.Objects;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.source.*;
/** Factory-controlled admitted Manifest proof; construction is package-bound to the positive verifier. */
public final class VerifiedAdmittedManifestV1 {
 public static final String VERSION="scenario-authority-verified-admitted-manifest-v1";
 private final RepositoryCaptureAttestation capture; private final AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember;
 private final NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity; private final String claimedAuthority;
 private final EvidenceGovernanceNormalizedManifestV1 exactNormalizedManifest; private final String format,schemaVersion,scenarioIdentityScheme;
 private final List<String> sourceContractIdentifiers; private final List<NormalizedScenarioOccurrenceInputV1> authoredScenarios;
 private final ScenarioAuthorityParsedJsonV1 parserProof; private final ScenarioAuthorityAttributionV1 attributionProof;
 private final List<ScenarioSchemaDiagnostic> structuralDiagnostics;
 static VerifiedAdmittedManifestV1 fromVerifier(RepositoryCaptureAttestation c,
   AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference p,
   NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity o, ScenarioAuthorityParsedJsonV1 parsed,
   ScenarioAuthorityAttributionV1 attribution, EvidenceGovernanceNormalizedManifestV1 normalized,
   List<String> ids,List<NormalizedScenarioOccurrenceInputV1> children){
  validate(c,p,o,attribution.authority(),ids,children);
  return new VerifiedAdmittedManifestV1(c,p,o,attribution.authority(),normalized,normalized.format(),
    normalized.schemaVersion(),normalized.scenarioIdentityScheme(),List.copyOf(ids),List.copyOf(children),
    parsed,attribution,List.of());
 }
 private VerifiedAdmittedManifestV1(RepositoryCaptureAttestation c,AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference p,
   NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity o,String a,EvidenceGovernanceNormalizedManifestV1 normalized,String f,String s,String scheme,List<String> ids,List<NormalizedScenarioOccurrenceInputV1> children,
   ScenarioAuthorityParsedJsonV1 parsed,ScenarioAuthorityAttributionV1 attribution,List<ScenarioSchemaDiagnostic> diagnostics){
  capture=c;parentMember=p;occurrenceIdentity=o;claimedAuthority=a;exactNormalizedManifest=normalized;
  format=f;schemaVersion=s;scenarioIdentityScheme=scheme;sourceContractIdentifiers=ids;authoredScenarios=children;
  parserProof=parsed;attributionProof=attribution;structuralDiagnostics=List.copyOf(diagnostics);}
 private static void validate(RepositoryCaptureAttestation c,AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference p,
   NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity o,String a,List<String> ids,List<NormalizedScenarioOccurrenceInputV1> supplied){
  Objects.requireNonNull(c);Objects.requireNonNull(p);Objects.requireNonNull(o);text(a);
  var versions=List.copyOf(Objects.requireNonNull(ids));if(versions.stream().anyMatch(x->x==null||x.isEmpty()))throw new IllegalArgumentException("source identifiers must be complete");
  var children=List.copyOf(Objects.requireNonNull(supplied));
  if(!c.sourceId().equals(p.parentSourceId())||!c.snapshotId().equals(p.parentSnapshotId())||!c.contentFingerprint().equals(p.parentContentFingerprint())||!c.regularMembers().contains(p))throw new IllegalArgumentException("parent is not capture-attested");
  if(!o.parentSourceId().equals(p.parentSourceId())||!o.parentSnapshotId().equals(p.parentSnapshotId())||!o.parentFingerprint().equals(p.parentContentFingerprint())||!o.memberPath().equals(p.normalizedRepositoryRelativePath())||!NormalizedScenarioOccurrenceInputV1.MANIFEST_OCCURRENCE_IDENTITY_VERSION.equals(o.identityVersion()))throw new IllegalArgumentException("Manifest occurrence mismatch");
  for(int i=0;i<children.size();i++){var x=children.get(i);if(!sameCapture(x.repositoryCaptureAttestation(),c)||!x.parentMember().equals(p)||!x.occurrenceIdentity().manifest().equals(o)||!x.claimedIdentity().authority().equals(a)||!x.structuralLocation().equals("/scenarios/"+i)||!x.occurrenceIdentity().structuralPath().equals(x.structuralLocation()))throw new IllegalArgumentException("authored Scenario mismatch");}
 }
 public RepositoryCaptureAttestation capture(){return capture;} public AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference parentMember(){return parentMember;}
 public NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity occurrenceIdentity(){return occurrenceIdentity;} public String claimedAuthority(){return claimedAuthority;}
 public String format(){return format;}public String schemaVersion(){return schemaVersion;}public String scenarioIdentityScheme(){return scenarioIdentityScheme;}
 public List<String> sourceContractIdentifiers(){return sourceContractIdentifiers;}public List<NormalizedScenarioOccurrenceInputV1> authoredScenarios(){return authoredScenarios;}
 public ScenarioAuthorityParsedJsonV1 parserProof(){return parserProof;} public ScenarioAuthorityAttributionV1 attributionProof(){return attributionProof;}
 public EvidenceGovernanceNormalizedManifestV1 normalizedManifest(){return exactNormalizedManifest;}
 public List<ScenarioSchemaDiagnostic> structuralDiagnostics(){return structuralDiagnostics;}
 private static boolean sameCapture(RepositoryCaptureAttestation left,RepositoryCaptureAttestation right){
  return left.sourceId().equals(right.sourceId())&&left.snapshotId().equals(right.snapshotId())
    &&left.contentFingerprint().equals(right.contentFingerprint())
    &&left.fingerprintInput().equals(right.fingerprintInput())
    &&left.regularMembers().equals(right.regularMembers())
    &&left.unsupportedMatchingEntries().equals(right.unsupportedMatchingEntries());
 }
 private static void text(String x){if(x==null||x.isEmpty())throw new IllegalArgumentException("text must not be empty");}
}
