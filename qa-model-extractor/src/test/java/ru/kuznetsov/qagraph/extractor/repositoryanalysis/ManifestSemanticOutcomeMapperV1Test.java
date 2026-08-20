package ru.kuznetsov.qagraph.extractor.repositoryanalysis;
import org.junit.jupiter.api.Test;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.*;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.lang.reflect.Modifier; import java.nio.charset.StandardCharsets; import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedManifestDatum;

class ManifestSemanticOutcomeMapperV1Test {
 private static final ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers CONTRACT=new ScenarioRepositoryCaptureSnapshotCandidate.ContractIdentifiers(
  "qaip-source-snapshot-contract-v1","qaip-scenario-authority-repository-json-v1","scenario-authority-repository-discovery-v1","scenario-authority-repository-path-v1","unicode-code-point-order-v1",RawSourceMemberFingerprint.ALGORITHM_IDENTIFIER);

 @Test void completeHandoffIsTheOnlyPublicConstructionPathAndDerivesExactChildren(){
  var h=handoff("capture:a","one","orders","A","B");var m=(NormalizedManifestDatum)h.normalizationResult().memberOutcomes().getFirst();
  var verified=new ManifestSemanticOutcomeMapperV1().mapVerifiedManifest(h,m);
  assertEquals(2,verified.authoredScenarios().size());assertEquals(List.of("/scenarios/0","/scenarios/1"),verified.authoredScenarios().stream().map(NormalizedScenarioOccurrenceInputV1::structuralLocation).toList());
  assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredConstructors()).noneMatch(c->Modifier.isPublic(c.getModifiers())));
  assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getMethods()).noneMatch(method->method.getReturnType()==VerifiedAdmittedManifestV1.class));
  assertTrue(Arrays.stream(VerifiedAdmittedManifestV1.class.getDeclaredClasses()).noneMatch(c->c.getSimpleName().contains("CompleteSourceProcessingEvidence")||c.getSimpleName().contains("StructuralAdmissionProof")));
  assertInstanceOf(ManifestSemanticCompositionOutcomeV1.Composed.class,new ManifestSemanticOutcomeMapperV1().attempt(h,m));
 }

 @Test void crossCaptureAndCoordinatedManifestRelabelingAreRejected(){
  var a=handoff("capture:a","one","orders","A");var b=handoff("capture:b","two","orders","A");
  var manifestB=(NormalizedManifestDatum)b.normalizationResult().memberOutcomes().getFirst();
  assertThrows(IllegalArgumentException.class,()->new ManifestSemanticOutcomeMapperV1().mapVerifiedManifest(a,manifestB));
 }

 @Test void noPublicNakedInputHasherOrLegacyManifestAuthorityRemains() throws Exception {
  assertTrue(Arrays.stream(ManifestSemanticFingerprintComposerV1.class.getMethods()).noneMatch(m->Arrays.asList(m.getParameterTypes()).contains(ManifestSemanticFingerprintInput.class)));
  assertThrows(ClassNotFoundException.class,()->Class.forName("ru.kuznetsov.qagraph.extractor.repositoryanalysis.ManifestSemanticFingerprintComposer"));
  assertThrows(ClassNotFoundException.class,()->Class.forName("ru.kuznetsov.qagraph.extractor.repositoryanalysis.NormalizedScenarioSemanticAttestation"));
 }

 private static ScenarioAuthorityNormalizedProcessingV1 handoff(String snapshot,String name,String authority,String...keys){
  String scenarios=String.join(",",Arrays.stream(keys).map(ManifestSemanticOutcomeMapperV1Test::scenario).toList());String json="{\"format\":\"qaip-scenario-authority-manifest-v1\",\"schemaVersion\":\"1.0\",\"authority\":\""+authority+"\",\"scenarioIdentityScheme\":\"qaip-scenario-identity-v1\",\"scenarios\":["+scenarios+"]}";
  byte[] bytes=json.getBytes(StandardCharsets.UTF_8);var member=new ScenarioManifestStableCaptureResult.CapturedMember(".qaip/scenarios/"+name+".scenario.json",bytes,bytes.length,RawSourceMemberFingerprint.calculate(bytes));
  var capture=new ScenarioManifestStableCaptureResult.Completed(List.of(member),List.of(),RepositoryCaptureFingerprintEncoder.MUTATION_DETECTION_VERSION);
  var parent=ScenarioRepositoryCaptureSnapshotCandidate.create(capture,"repository:test",snapshot,CONTRACT,ScenarioRepositoryCaptureSnapshotCandidate.CaptureProvenance.empty());
  var normalization=new ScenarioSourceDeclarationNormalizer().normalize(new ScenarioLogicalSourceSchemaAdmission().admit(new ScenarioLogicalSourceMemberProcessor().process(parent)));
  return ScenarioAuthorityNormalizedProcessingV1.verified(parent,normalization,ScenarioAuthorityNormalizedProcessingV1.ContractIdentifiers.selectedV1());
 }
 private static String scenario(String k){return "{\"scenarioKey\":\""+k+"\",\"title\":\"Title "+k+"\",\"given\":[\"ready\"],\"when\":[\"act\"],\"then\":[\"done\"],\"operationRef\":{\"identityScheme\":\"qaip-http-operation-reference-v1\",\"method\":\"POST\",\"path\":\"/api/"+k.toLowerCase()+"\"},\"ruleRefs\":[]}";}
}
