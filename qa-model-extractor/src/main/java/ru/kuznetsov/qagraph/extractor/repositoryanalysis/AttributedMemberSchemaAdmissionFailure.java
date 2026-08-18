package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Objects;
import java.util.Optional;

/** Member-local schema mapping compatibility failure; never canonical rejected evidence. */
public record AttributedMemberSchemaAdmissionFailure(
        ParentCapturedMemberRef parentMemberRef,
        String claimedAuthority,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome,
        ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome,
        Optional<String> attributionStructuralLocation,
        String schemaContractIdentifier,
        String mappingContractIdentifier,
        String failureCode
) implements ScenarioSchemaAdmissionOutcome, ScenarioSourceNormalizationMemberOutcome {
    public AttributedMemberSchemaAdmissionFailure {
        Objects.requireNonNull(parentMemberRef, "parentMemberRef");
        claimedAuthority = requireNonBlank(claimedAuthority, "claimedAuthority");
        requireExact(parserContractIdentifier, ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                "parserContractIdentifier");
        requireExact(attributionContractIdentifier,
                ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        if (parseOutcome != ScenarioMemberProcessingOutcome.ParseOutcome.PARSED) {
            throw new IllegalArgumentException("schema mapping failure must retain PARSED outcome");
        }
        if (attributionOutcome != ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED) {
            throw new IllegalArgumentException("schema mapping failure must retain ATTRIBUTED outcome");
        }
        attributionStructuralLocation = ScenarioMemberProcessingOutcome.requireJsonPointer(
                attributionStructuralLocation);
        requireExact(schemaContractIdentifier, ScenarioManifestSchemaValidator.SCHEMA_CONTRACT_IDENTIFIER,
                "schemaContractIdentifier");
        requireExact(mappingContractIdentifier, ScenarioSchemaDiagnosticAdapterV1.CONTRACT_IDENTIFIER,
                "mappingContractIdentifier");
        requireExact(failureCode, ScenarioSchemaDiagnosticMappingException.FAILURE_CODE, "failureCode");
    }

    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private static void requireExact(String actual, String expected, String field) {
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
