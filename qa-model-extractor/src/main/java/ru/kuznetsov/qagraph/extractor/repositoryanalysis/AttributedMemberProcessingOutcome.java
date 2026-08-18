package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

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
        parsedSource = Objects.requireNonNull(parsedSource, "parsedSource").deepCopy();
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
