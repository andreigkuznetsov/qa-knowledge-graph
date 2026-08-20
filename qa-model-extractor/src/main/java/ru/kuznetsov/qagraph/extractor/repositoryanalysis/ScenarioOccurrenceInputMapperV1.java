package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.math.BigInteger; import java.util.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Extractor-only exact mapping; all validation, classification and fingerprinting remain downstream. */
public final class ScenarioOccurrenceInputMapperV1 {
    public NormalizedScenarioOccurrenceInputV1 map(ScenarioAuthorityNormalizedProcessingV1 handoff,
            ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding binding) {
        Objects.requireNonNull(handoff); Objects.requireNonNull(binding);
        if(!handoff.occurrences().contains(binding)) throw new IllegalArgumentException("occurrence is not part of the verified handoff");
        ParentCapturedMemberRef parent=binding.parentMemberRef(); NormalizedScenarioDeclarationOccurrence d=binding.declaration();
        RepositoryCaptureAttestation capture=handoff.captureAttestation(); var contracts=handoff.contracts();
        Objects.requireNonNull(parent); Objects.requireNonNull(d); Objects.requireNonNull(capture);
        var pm=new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(parent.parentSourceId(),parent.parentSnapshotId(),parent.parentContentFingerprint(),parent.normalizedRepositoryRelativePath(),parent.rawByteLength(),parent.rawMemberFingerprint());
        var mi=d.occurrenceIdentity().manifestOccurrenceIdentity();
        var oi=new NormalizedScenarioOccurrenceInputV1.ScenarioDeclarationOccurrenceIdentity(
                new NormalizedScenarioOccurrenceInputV1.ManifestOccurrenceIdentity(mi.parentSourceId(),mi.parentSnapshotId(),mi.parentContentFingerprint(),mi.normalizedRepositoryRelativePath(),mi.identityVersion()),d.occurrenceIdentity().structuralPath(),d.occurrenceIdentity().identityVersion());
        var identity=id(d.claimedScenarioIdentity()); List<NormalizedScenarioOccurrenceInputV1.NormalizedStep> given=new ArrayList<>(),when=new ArrayList<>(),then=new ArrayList<>();
        for(var s:d.steps()){var x=new NormalizedScenarioOccurrenceInputV1.NormalizedStep(id(s.identity().claimedScenarioIdentity()),StepSemanticFingerprintInput.Phase.valueOf(s.identity().phase().name()),BigInteger.valueOf(s.identity().ordinal()),s.identity().identityVersion(),s.exactAuthoredText(),contracts.stepSemanticCanonicalizationVersion());switch(s.identity().phase()){case GIVEN->given.add(x);case WHEN->when.add(x);case THEN->then.add(x);}}
        var op=d.operationReference();var opi=op.identity();var operation=new NormalizedScenarioOccurrenceInputV1.UnresolvedOperationReference(id(opi.claimedScenarioIdentity()),opi.role(),opi.identityVersion(),op.targetProfile(),op.method(),op.path(),contracts.operationReferenceSemanticCanonicalizationVersion());
        var rules=d.businessRuleReferences().stream().map(r->new NormalizedScenarioOccurrenceInputV1.UnresolvedBusinessRuleReference(BigInteger.valueOf(r.authoredArrayPosition()),id(r.identity().claimedScenarioIdentity()),r.identity().referencedAuthority(),r.identity().stableRuleKey(),r.identity().identityScheme(),r.identity().identityVersion(),contracts.businessRuleReferenceSemanticCanonicalizationVersion())).toList();
        return new NormalizedScenarioOccurrenceInputV1(contracts.sourceNormalizationVersion(),contracts.scenarioSemanticCanonicalizationVersion(),contracts.scenarioSemanticContractVersion(),oi,pm,capture,oi.structuralPath(),identity,d.title(),d.given(),given,d.when(),when,d.then(),then,operation,rules);
    }
    public ScenarioOccurrenceCompositionOutcomeV1 attempt(ScenarioAuthorityNormalizedProcessingV1 h,ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding b){return ScenarioOccurrenceCompositionAttemptV1.attemptScenarioOccurrenceCompositionV1(map(h,b));}
    private static ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity id(ClaimedScenarioIdentity i){return new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(i.authority(),i.scenarioKey(),i.identityScheme());}
}
