package ru.kuznetsov.qaip.evidencegovernance.source;

import java.util.Objects;

/** Exact safely attributed authority derived from an authoritative parsed result. */
public final class ScenarioAuthorityAttributionV1 {
    public static final String ATTRIBUTION_CONTRACT_IDENTIFIER = "scenario-authority-attribution-v1";
    private final ScenarioAuthorityParsedJsonV1 parsedJson;
    private final String authority;

    ScenarioAuthorityAttributionV1(ScenarioAuthorityParsedJsonV1 parsedJson, String authority) {
        this.parsedJson = Objects.requireNonNull(parsedJson);
        this.authority = Objects.requireNonNull(authority);
    }

    public String attributionContractIdentifier() { return ATTRIBUTION_CONTRACT_IDENTIFIER; }
    public ScenarioAuthorityParsedJsonV1 parsedJson() { return parsedJson; }
    public String authority() { return authority; }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof ScenarioAuthorityAttributionV1 that
                && parsedJson.equals(that.parsedJson) && authority.equals(that.authority);
    }
    @Override public int hashCode() { return Objects.hash(parsedJson, authority); }
}
