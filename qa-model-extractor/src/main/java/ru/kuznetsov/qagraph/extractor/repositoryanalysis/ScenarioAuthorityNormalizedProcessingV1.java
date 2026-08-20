package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;
import java.util.*;
import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Immutable, capture-verified handoff from source normalization to Evidence Governance. */
public final class ScenarioAuthorityNormalizedProcessingV1 {
    private final ScenarioSourceNormalizationResult normalization;
    private final RepositoryCaptureAttestation captureAttestation;
    private final ContractIdentifiers contracts;
    private final List<OccurrenceBinding> occurrences;
    private final List<ScenarioManifestStableCaptureResult.CapturedMember> capturedMembers;

    private ScenarioAuthorityNormalizedProcessingV1(ScenarioSourceNormalizationResult normalization,
            RepositoryCaptureAttestation captureAttestation, ContractIdentifiers contracts,
            List<OccurrenceBinding> occurrences, List<ScenarioManifestStableCaptureResult.CapturedMember> capturedMembers) {
        this.normalization=normalization; this.captureAttestation=captureAttestation;
        this.contracts=contracts; this.occurrences=occurrences;this.capturedMembers=List.copyOf(capturedMembers);
    }

    public static ScenarioAuthorityNormalizedProcessingV1 verified(
            ScenarioRepositoryCaptureSnapshotCandidate parent, ScenarioSourceNormalizationResult normalization,
            ContractIdentifiers contracts) {
        return verified(parent, normalization, contracts, exactOccurrences(normalization));
    }

    /** Explicit form used at trust boundaries; supplied occurrence membership and order must be exact. */
    public static ScenarioAuthorityNormalizedProcessingV1 verified(
            ScenarioRepositoryCaptureSnapshotCandidate parent, ScenarioSourceNormalizationResult normalization,
            ContractIdentifiers contracts, List<OccurrenceBinding> suppliedOccurrences) {
        Objects.requireNonNull(parent,"parent"); Objects.requireNonNull(normalization,"normalization");
        Objects.requireNonNull(contracts,"contracts"); suppliedOccurrences=List.copyOf(Objects.requireNonNull(suppliedOccurrences,"suppliedOccurrences"));
        if(!parent.identity().equals(normalization.parentIdentity())) throw new IllegalArgumentException("normalization belongs to another Repository Capture");
        List<AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference> refs=parent.members().stream()
                .map(m->reference(new ParentCapturedMemberRef(parent.sourceId(),parent.snapshotId(),parent.contentFingerprint(),m.repositoryRelativePath(),m.rawByteLength(),m.rawMemberFingerprint()))).toList();
        var input=parent.repositoryCaptureFingerprintInput();
        var attestation=RepositoryCaptureAttestation.verified(parent.sourceId(),parent.snapshotId(),input,parent.contentFingerprint(),refs,input.unsupportedMatchingEntries());
        List<OccurrenceBinding> exact=exactOccurrences(normalization);
        if(!exact.equals(suppliedOccurrences)) throw new IllegalArgumentException("normalized occurrence membership and order must be exact");
        for(var outcome:normalization.memberOutcomes()) {
            var p=outcome.parentMemberRef(); var ref=reference(p);
            if(!refs.contains(ref)) throw new IllegalArgumentException("normalized member is not in the verified capture");
        }
        return new ScenarioAuthorityNormalizedProcessingV1(normalization,attestation,contracts,List.copyOf(exact),parent.members());
    }

    public ScenarioSourceNormalizationResult normalizationResult(){return normalization;}
    public RepositoryCaptureAttestation captureAttestation(){return captureAttestation;}
    public ContractIdentifiers contracts(){return contracts;}
    public List<OccurrenceBinding> occurrences(){return occurrences;}
    /** Defensive exact-byte handoff from the completed stable capture for one fully corresponding member. */
    public byte[] exactCapturedBytes(ParentCapturedMemberRef selected){
        Objects.requireNonNull(selected);var expected=reference(selected);
        int index=captureAttestation.regularMembers().indexOf(expected);
        if(index<0)throw new IllegalArgumentException("selected member is not capture-attested");
        var captured=capturedMembers.get(index);
        if(!captured.repositoryRelativePath().equals(selected.normalizedRepositoryRelativePath())
                ||captured.rawByteLength()!=selected.rawByteLength()
                ||!captured.rawMemberFingerprint().equals(selected.rawMemberFingerprint()))
            throw new IllegalArgumentException("selected member has no exact capture-owned bytes");
        return captured.bytes();
    }

    public record OccurrenceBinding(ParentCapturedMemberRef parentMemberRef,
            NormalizedScenarioDeclarationOccurrence declaration) {
        public OccurrenceBinding { Objects.requireNonNull(parentMemberRef); Objects.requireNonNull(declaration);
            var m=declaration.occurrenceIdentity().manifestOccurrenceIdentity();
            if(!parentMemberRef.parentSourceId().equals(m.parentSourceId())||!parentMemberRef.parentSnapshotId().equals(m.parentSnapshotId())
                    ||!parentMemberRef.parentContentFingerprint().equals(m.parentContentFingerprint())||!parentMemberRef.normalizedRepositoryRelativePath().equals(m.normalizedRepositoryRelativePath()))
                throw new IllegalArgumentException("occurrence does not match parent member"); }
    }

    /** Exact processing-selected identifiers; values are retained, never validated or defaulted here. */
    public record ContractIdentifiers(String sourceNormalizationVersion,String scenarioSemanticCanonicalizationVersion,
            String scenarioSemanticContractVersion,String stepSemanticCanonicalizationVersion,
            String operationReferenceSemanticCanonicalizationVersion,String businessRuleReferenceSemanticCanonicalizationVersion) {
        public ContractIdentifiers { Objects.requireNonNull(sourceNormalizationVersion);Objects.requireNonNull(scenarioSemanticCanonicalizationVersion);
            Objects.requireNonNull(scenarioSemanticContractVersion);Objects.requireNonNull(stepSemanticCanonicalizationVersion);
            Objects.requireNonNull(operationReferenceSemanticCanonicalizationVersion);Objects.requireNonNull(businessRuleReferenceSemanticCanonicalizationVersion); }
        public static ContractIdentifiers selectedV1(){return new ContractIdentifiers(ScenarioSemanticFingerprintEncoder.SOURCE_NORMALIZATION_VERSION,
                ScenarioSemanticFingerprintEncoder.ENCODING_IDENTIFIER,ScenarioSemanticFingerprintEncoder.SCENARIO_SEMANTIC_CONTRACT_VERSION,
                StepSemanticFingerprintEncoder.ENCODING_IDENTIFIER,HttpOperationReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER,
                BusinessRuleReferenceSemanticFingerprintEncoder.ENCODING_IDENTIFIER);}
    }
    private static List<OccurrenceBinding> exactOccurrences(ScenarioSourceNormalizationResult n){var result=new ArrayList<OccurrenceBinding>();
        for(var member:n.memberOutcomes())if(member instanceof NormalizedManifestDatum m)for(var d:m.scenarioDeclarations())result.add(new OccurrenceBinding(m.parentMemberRef(),d));return List.copyOf(result);}
    private static AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference reference(ParentCapturedMemberRef p){return new AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference(p.parentSourceId(),p.parentSnapshotId(),p.parentContentFingerprint(),p.normalizedRepositoryRelativePath(),p.rawByteLength(),p.rawMemberFingerprint());}
}
