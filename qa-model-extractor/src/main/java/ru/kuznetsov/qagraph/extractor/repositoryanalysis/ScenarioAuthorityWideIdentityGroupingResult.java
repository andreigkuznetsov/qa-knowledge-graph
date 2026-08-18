package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;

/** Immutable authority-wide grouping derived from one unchanged normalization result. */
public final class ScenarioAuthorityWideIdentityGroupingResult {
    private final ScenarioSourceNormalizationResult normalizationResult;
    private final List<ScenarioIdentityGroup> identityGroups;

    ScenarioAuthorityWideIdentityGroupingResult(
            ScenarioSourceNormalizationResult normalizationResult,
            List<ScenarioIdentityGroup> identityGroups
    ) {
        this.normalizationResult = Objects.requireNonNull(normalizationResult, "normalizationResult");
        this.identityGroups = List.copyOf(Objects.requireNonNull(identityGroups, "identityGroups"));
    }

    /** The exact prior result; rejected and unattributable member outcomes remain unchanged within it. */
    public ScenarioSourceNormalizationResult normalizationResult() {
        return normalizationResult;
    }

    public List<ScenarioSourceNormalizationMemberOutcome> memberOutcomes() {
        return normalizationResult.memberOutcomes();
    }

    public List<ScenarioIdentityGroup> identityGroups() {
        return identityGroups;
    }
}
