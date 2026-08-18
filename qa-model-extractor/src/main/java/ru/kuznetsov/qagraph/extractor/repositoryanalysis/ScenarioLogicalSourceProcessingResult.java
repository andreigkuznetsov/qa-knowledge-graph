package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

/** Immutable ordered terminal outcomes for every captured member in one parent candidate. */
public final class ScenarioLogicalSourceProcessingResult {
    private final ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity;
    private final List<ScenarioMemberProcessingOutcome> memberOutcomes;

    ScenarioLogicalSourceProcessingResult(
            ScenarioRepositoryCaptureSnapshotCandidate parent,
            List<ScenarioMemberProcessingOutcome> memberOutcomes
    ) {
        Objects.requireNonNull(parent, "parent");
        this.parentIdentity = parent.identity();
        this.memberOutcomes = List.copyOf(Objects.requireNonNull(memberOutcomes, "memberOutcomes"));
        if (this.memberOutcomes.size() != parent.members().size()) {
            throw new IllegalArgumentException("one outcome is required for every parent member");
        }
        for (int index = 0; index < this.memberOutcomes.size(); index++) {
            ScenarioMemberProcessingOutcome outcome = this.memberOutcomes.get(index);
            outcome.parentMemberRef().verifyAgainst(parent);
            String expectedPath = parent.members().get(index).repositoryRelativePath();
            if (!expectedPath.equals(outcome.parentMemberRef().normalizedRepositoryRelativePath())) {
                throw new IllegalArgumentException("member outcomes must retain exact parent order");
            }
        }
    }

    public ScenarioRepositoryCaptureSnapshotCandidate.SnapshotIdentity parentIdentity() {
        return parentIdentity;
    }

    public List<ScenarioMemberProcessingOutcome> memberOutcomes() {
        return memberOutcomes;
    }
}
