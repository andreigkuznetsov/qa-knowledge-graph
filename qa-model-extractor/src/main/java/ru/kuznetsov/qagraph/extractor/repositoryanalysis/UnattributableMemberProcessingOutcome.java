package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Objects;
import java.util.Optional;

/** Terminal member outcome that cannot safely claim a Scenario authority. */
public record UnattributableMemberProcessingOutcome(
        ParentCapturedMemberRef parentMemberRef,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome,
        ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome,
        Optional<String> structuralLocation
) implements ScenarioMemberProcessingOutcome, ScenarioSchemaAdmissionOutcome {
    public UnattributableMemberProcessingOutcome {
        Objects.requireNonNull(parentMemberRef, "parentMemberRef");
        requireContract(parserContractIdentifier, PARSER_CONTRACT_IDENTIFIER, "parserContractIdentifier");
        requireContract(attributionContractIdentifier, ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        Objects.requireNonNull(parseOutcome, "parseOutcome");
        Objects.requireNonNull(attributionOutcome, "attributionOutcome");
        if (attributionOutcome == AttributionOutcome.ATTRIBUTED) {
            throw new IllegalArgumentException("unattributable member cannot have ATTRIBUTED outcome");
        }
        if (parseOutcome == ParseOutcome.PARSED
                && attributionOutcome == AttributionOutcome.PARSE_UNATTRIBUTABLE) {
            throw new IllegalArgumentException("parsed member requires a specific attribution outcome");
        }
        if (parseOutcome != ParseOutcome.PARSED
                && attributionOutcome != AttributionOutcome.PARSE_UNATTRIBUTABLE) {
            throw new IllegalArgumentException("parse failure requires PARSE_UNATTRIBUTABLE outcome");
        }
        structuralLocation = ScenarioMemberProcessingOutcome.requireJsonPointer(structuralLocation);
    }

    private static void requireContract(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
