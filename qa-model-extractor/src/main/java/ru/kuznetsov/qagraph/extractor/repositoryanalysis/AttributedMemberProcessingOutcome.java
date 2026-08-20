package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityParsedJsonV1;

import java.util.Objects;
import java.util.Optional;

/** Safely parsed member attributed to exactly one claimed Scenario authority. */
public record AttributedMemberProcessingOutcome(
        ParentCapturedMemberRef parentMemberRef,
        String claimedAuthority,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome,
        ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome,
        Optional<String> structuralLocation,
        ScenarioAuthorityParsedJsonV1 authoritativeParsedJson,
        ScenarioAuthorityAttributionV1 authoritativeAttribution,
        JsonNode parsedSource
) implements ScenarioMemberProcessingOutcome {
    public AttributedMemberProcessingOutcome {
        Objects.requireNonNull(parentMemberRef, "parentMemberRef");
        claimedAuthority = requireNonBlank(claimedAuthority, "claimedAuthority");
        requireContract(parserContractIdentifier, PARSER_CONTRACT_IDENTIFIER, "parserContractIdentifier");
        requireContract(attributionContractIdentifier, ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        if (parseOutcome != ParseOutcome.PARSED) {
            throw new IllegalArgumentException("attributed member must have PARSED outcome");
        }
        if (attributionOutcome != AttributionOutcome.ATTRIBUTED) {
            throw new IllegalArgumentException("attributed member must have ATTRIBUTED outcome");
        }
        structuralLocation = ScenarioMemberProcessingOutcome.requireJsonPointer(structuralLocation);
        Objects.requireNonNull(authoritativeParsedJson, "authoritativeParsedJson");
        Objects.requireNonNull(authoritativeAttribution, "authoritativeAttribution");
        if (authoritativeAttribution.parsedJson() != authoritativeParsedJson)
            throw new IllegalArgumentException("attribution must bind the exact parsed proof");
        if (!claimedAuthority.equals(authoritativeAttribution.authority()))
            throw new IllegalArgumentException("claimedAuthority must project authoritative attribution");
        parsedSource = Objects.requireNonNull(parsedSource, "parsedSource").deepCopy();
        if (!parsedSource.equals(authoritativeParsedJson.document()))
            throw new IllegalArgumentException("parsedSource must project the authoritative parsed proof");
    }

    @Override
    public JsonNode parsedSource() {
        return parsedSource.deepCopy();
    }

    private static void requireContract(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
