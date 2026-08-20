package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.source.EvidenceGovernanceNormalizedManifestV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityNormalizationContractsV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityNormalizerV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Extractor compatibility projection over the authoritative Evidence Governance normalizer. */
public final class ScenarioSourceDeclarationNormalizer {
    private final ScenarioAuthorityNormalizerV1 delegate = new ScenarioAuthorityNormalizerV1();

    public ScenarioSourceNormalizationResult normalize(ScenarioSchemaAdmissionResult admissionResult) {
        Objects.requireNonNull(admissionResult, "admissionResult");
        List<ScenarioSourceNormalizationMemberOutcome> outcomes = new ArrayList<>();
        for (ScenarioSchemaAdmissionOutcome outcome : admissionResult.memberOutcomes()) {
            if (outcome instanceof AttributedMemberSchemaAdmissionOutcome admitted
                    && admitted.structuralAdmissionState()
                    == AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED) {
                outcomes.add(project(admitted, delegate.normalizeStructurallyAdmitted(
                        admitted.authoritativeParsedJson(), admitted.authoritativeAttribution(),
                        ScenarioAuthorityNormalizationContractsV1.selectedV1())));
            } else outcomes.add((ScenarioSourceNormalizationMemberOutcome) outcome);
        }
        return new ScenarioSourceNormalizationResult(admissionResult, outcomes);
    }

    private static NormalizedManifestDatum project(AttributedMemberSchemaAdmissionOutcome admission,
                                                    EvidenceGovernanceNormalizedManifestV1 source) {
        ParentCapturedMemberRef parent = admission.parentMemberRef();
        ManifestOccurrenceIdentity manifestIdentity = new ManifestOccurrenceIdentity(parent.parentSourceId(),
                parent.parentSnapshotId(), parent.parentContentFingerprint(),
                parent.normalizedRepositoryRelativePath(), source.contracts().manifestOccurrenceIdentityVersion());
        List<NormalizedScenarioDeclarationOccurrence> scenarios = source.authoredScenarios().stream()
                .map(value -> scenario(manifestIdentity, value)).toList();
        return new NormalizedManifestDatum(admission, manifestIdentity, source.claimedAuthority(), source.format(),
                source.schemaVersion(), source.scenarioIdentityScheme(), scenarios);
    }

    private static NormalizedScenarioDeclarationOccurrence scenario(
            ManifestOccurrenceIdentity manifestIdentity, EvidenceGovernanceNormalizedManifestV1.Scenario source) {
        ClaimedScenarioIdentity identity = identity(source.claimedIdentity());
        ScenarioDeclarationOccurrenceIdentity occurrence = new ScenarioDeclarationOccurrenceIdentity(
                manifestIdentity, source.structuralPath(), source.declarationOccurrenceIdentityVersion());
        List<NormalizedScenarioStep> steps = source.steps().stream().map(value -> new NormalizedScenarioStep(
                new ScenarioStepIdentity(identity, StepPhase.valueOf(value.phase().name()), value.ordinal(),
                        value.identityVersion()), value.exactAuthoredText())).toList();
        var op = source.operationReference();
        NormalizedOperationReferenceDatum operation = new NormalizedOperationReferenceDatum(
                new OperationReferenceDatumIdentity(identity, op.role(), op.datumIdentityVersion()),
                op.targetProfile(), op.method(), op.path());
        List<NormalizedBusinessRuleReferenceDatum> rules = source.businessRuleReferences().stream()
                .map(value -> new NormalizedBusinessRuleReferenceDatum(
                        new BusinessRuleReferenceDatumIdentity(identity, value.referencedAuthority(),
                                value.stableRuleKey(), value.identityScheme(), value.datumIdentityVersion()),
                        value.authoredPosition())).toList();
        return new NormalizedScenarioDeclarationOccurrence(occurrence, identity, source.scenarioKey(), source.title(),
                source.given(), source.when(), source.then(), steps, operation, rules);
    }

    private static ClaimedScenarioIdentity identity(
            EvidenceGovernanceNormalizedManifestV1.ClaimedScenarioIdentity source) {
        return new ClaimedScenarioIdentity(source.authority(), source.scenarioKey(), source.identityScheme());
    }
}
