package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

/** Immutable schema outcomes for every processing outcome in exact parent order. */
public final class ScenarioSchemaAdmissionResult {
    private final ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity;
    private final List<ScenarioSchemaAdmissionOutcome> memberOutcomes;

    ScenarioSchemaAdmissionResult(
            ScenarioLogicalSourceProcessingResult processingResult,
            List<ScenarioSchemaAdmissionOutcome> memberOutcomes
    ) {
        Objects.requireNonNull(processingResult, "processingResult");
        this.parentIdentity = processingResult.parentIdentity();
        this.memberOutcomes = List.copyOf(Objects.requireNonNull(memberOutcomes, "memberOutcomes"));
        if (this.memberOutcomes.size() != processingResult.memberOutcomes().size()) {
            throw new IllegalArgumentException("schema admission must preserve every processing outcome");
        }
        for (int index = 0; index < this.memberOutcomes.size(); index++) {
            ScenarioMemberProcessingOutcome before = processingResult.memberOutcomes().get(index);
            ScenarioSchemaAdmissionOutcome after = this.memberOutcomes.get(index);
            if (!before.parentMemberRef().equals(after.parentMemberRef())) {
                throw new IllegalArgumentException("schema admission must preserve parent binding and order");
            }
            if (before instanceof UnattributableMemberProcessingOutcome && before != after) {
                throw new IllegalArgumentException("unattributable processing outcome must pass through unchanged");
            }
        }
    }

    public ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity() {
        return parentIdentity;
    }

    public List<ScenarioSchemaAdmissionOutcome> memberOutcomes() {
        return memberOutcomes;
    }
}
