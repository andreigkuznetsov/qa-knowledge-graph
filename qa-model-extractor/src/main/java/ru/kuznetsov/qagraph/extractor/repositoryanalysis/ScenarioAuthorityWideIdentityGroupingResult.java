package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.List;
import java.util.Objects;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.VerifiedScenarioIdentityGroupV1;

/** Immutable authority-wide grouping derived from one unchanged normalization result. */
public final class ScenarioAuthorityWideIdentityGroupingResult {
    private final ScenarioSourceNormalizationResult normalizationResult;
    private final List<ScenarioIdentityGroup> identityGroups;
    private final List<VerifiedScenarioIdentityGroupV1> verifiedIdentityGroups;

    ScenarioAuthorityWideIdentityGroupingResult(
            ScenarioSourceNormalizationResult normalizationResult,
            List<ScenarioIdentityGroup> identityGroups,
            List<VerifiedScenarioIdentityGroupV1> verifiedIdentityGroups
    ) {
        this.normalizationResult = Objects.requireNonNull(normalizationResult, "normalizationResult");
        this.identityGroups = List.copyOf(Objects.requireNonNull(identityGroups, "identityGroups"));
        this.verifiedIdentityGroups=List.copyOf(Objects.requireNonNull(verifiedIdentityGroups,"verifiedIdentityGroups"));
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

    /** Authoritative production groups; legacy identityGroups are presentation projections only. */
    public List<VerifiedScenarioIdentityGroupV1> verifiedIdentityGroups(){return verifiedIdentityGroups;}
}
