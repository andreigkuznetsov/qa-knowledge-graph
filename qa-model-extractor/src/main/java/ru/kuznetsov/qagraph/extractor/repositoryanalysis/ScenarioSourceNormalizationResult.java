package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

/** Immutable source-native normalization outcome for every admitted input member, in parent order. */
public final class ScenarioSourceNormalizationResult {
    private final ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity;
    private final List<ScenarioSourceNormalizationMemberOutcome> memberOutcomes;

    ScenarioSourceNormalizationResult(
            ScenarioSchemaAdmissionResult admissionResult,
            List<ScenarioSourceNormalizationMemberOutcome> memberOutcomes
    ) {
        Objects.requireNonNull(admissionResult, "admissionResult");
        parentIdentity = admissionResult.parentIdentity();
        this.memberOutcomes = List.copyOf(Objects.requireNonNull(memberOutcomes, "memberOutcomes"));
        if (this.memberOutcomes.size() != admissionResult.memberOutcomes().size()) {
            throw new IllegalArgumentException("normalization must preserve every admission outcome");
        }
        for (int index = 0; index < this.memberOutcomes.size(); index++) {
            ScenarioSchemaAdmissionOutcome before = admissionResult.memberOutcomes().get(index);
            ScenarioSourceNormalizationMemberOutcome after = this.memberOutcomes.get(index);
            if (!before.parentMemberRef().equals(after.parentMemberRef())) {
                throw new IllegalArgumentException("normalization must preserve parent binding and order");
            }
            boolean admitted = before instanceof AttributedMemberSchemaAdmissionOutcome attributed
                    && attributed.structuralAdmissionState()
                    == AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED;
            if (admitted != (after instanceof ScenarioSourceNormalizedRecords.NormalizedManifestDatum)) {
                throw new IllegalArgumentException("only admitted attributed members may produce declarations");
            }
            if (!admitted && before != after) {
                throw new IllegalArgumentException("rejected and unattributable outcomes must pass through unchanged");
            }
        }
    }

    public ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity() {
        return parentIdentity;
    }

    public List<ScenarioSourceNormalizationMemberOutcome> memberOutcomes() {
        return memberOutcomes;
    }
}
