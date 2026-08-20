package ru.kuznetsov.qaip.evidencegovernance.source;

import java.util.List;
import java.util.Objects;

/** Immutable, source-native Scenario Authority Manifest V1 normalization result. */
public final class EvidenceGovernanceNormalizedManifestV1 {
    private final ScenarioAuthorityNormalizationContractsV1 contracts;
    private final String claimedAuthority;
    private final String format;
    private final String schemaVersion;
    private final String scenarioIdentityScheme;
    private final List<Scenario> authoredScenarios;

    EvidenceGovernanceNormalizedManifestV1(ScenarioAuthorityNormalizationContractsV1 contracts,
            String claimedAuthority, String format, String schemaVersion, String scenarioIdentityScheme,
            List<Scenario> authoredScenarios) {
        this.contracts = Objects.requireNonNull(contracts); text(claimedAuthority); text(format); text(schemaVersion);
        text(scenarioIdentityScheme); this.claimedAuthority = claimedAuthority; this.format = format;
        this.schemaVersion = schemaVersion; this.scenarioIdentityScheme = scenarioIdentityScheme;
        this.authoredScenarios = List.copyOf(Objects.requireNonNull(authoredScenarios));
    }
    public ScenarioAuthorityNormalizationContractsV1 contracts() { return contracts; }
    public String claimedAuthority() { return claimedAuthority; }
    public String format() { return format; }
    public String schemaVersion() { return schemaVersion; }
    public String scenarioIdentityScheme() { return scenarioIdentityScheme; }
    public List<Scenario> authoredScenarios() { return authoredScenarios; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvidenceGovernanceNormalizedManifestV1 that)) return false;
        return contracts.equals(that.contracts) && claimedAuthority.equals(that.claimedAuthority)
                && format.equals(that.format) && schemaVersion.equals(that.schemaVersion)
                && scenarioIdentityScheme.equals(that.scenarioIdentityScheme)
                && authoredScenarios.equals(that.authoredScenarios);
    }
    @Override public int hashCode() { return Objects.hash(contracts, claimedAuthority, format, schemaVersion,
            scenarioIdentityScheme, authoredScenarios); }

    public record ClaimedScenarioIdentity(String authority, String scenarioKey, String identityScheme) {
        public ClaimedScenarioIdentity { text(authority); text(scenarioKey); text(identityScheme); }
    }
    public enum StepPhase { GIVEN, WHEN, THEN }
    public record Step(ClaimedScenarioIdentity owner, StepPhase phase, int ordinal,
                       String identityVersion, String exactAuthoredText, String semanticVersion) {
        public Step { Objects.requireNonNull(owner); Objects.requireNonNull(phase); nonnegative(ordinal);
            text(identityVersion); Objects.requireNonNull(exactAuthoredText); text(semanticVersion); }
    }
    public record OperationReference(ClaimedScenarioIdentity owner, String role, String datumIdentityVersion,
                                     String targetProfile, String semanticVersion, String method, String path) {
        public OperationReference { Objects.requireNonNull(owner); text(role); text(datumIdentityVersion);
            text(targetProfile); text(semanticVersion); text(method); text(path); }
    }
    public record BusinessRuleReference(int authoredPosition, ClaimedScenarioIdentity owner,
                                        String referencedAuthority, String stableRuleKey, String identityScheme,
                                        String datumIdentityVersion, String semanticVersion) {
        public BusinessRuleReference { nonnegative(authoredPosition); Objects.requireNonNull(owner);
            text(referencedAuthority); text(stableRuleKey); text(identityScheme); text(datumIdentityVersion);
            text(semanticVersion); }
    }
    public record Scenario(int authoredIndex, String structuralPath, String declarationOccurrenceIdentityVersion,
                           ClaimedScenarioIdentity claimedIdentity, String scenarioKey, String title,
                           String scenarioSemanticVersion, List<String> given, List<String> when, List<String> then,
                           List<Step> steps, OperationReference operationReference,
                           List<BusinessRuleReference> businessRuleReferences) {
        public Scenario { nonnegative(authoredIndex); text(structuralPath); text(declarationOccurrenceIdentityVersion);
            Objects.requireNonNull(claimedIdentity); text(scenarioKey); text(title); text(scenarioSemanticVersion);
            given = strings(given); when = strings(when); then = strings(then);
            steps = List.copyOf(Objects.requireNonNull(steps)); Objects.requireNonNull(operationReference);
            businessRuleReferences = List.copyOf(Objects.requireNonNull(businessRuleReferences)); }
    }
    private static List<String> strings(List<String> values) {
        List<String> result = List.copyOf(Objects.requireNonNull(values)); result.forEach(Objects::requireNonNull);
        return result;
    }
    private static void nonnegative(int value) { if (value < 0) throw new IllegalArgumentException("negative index"); }
    private static void text(String value) { if (value == null || value.isEmpty()) throw new IllegalArgumentException("text required"); }
}
