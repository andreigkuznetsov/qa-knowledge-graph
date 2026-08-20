package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityAttributionV1;
import ru.kuznetsov.qaip.evidencegovernance.source.ScenarioAuthorityParsedJsonV1;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Schema admission result for one safely attributed parsed member. */
public record AttributedMemberSchemaAdmissionOutcome(
        ParentCapturedMemberRef parentMemberRef,
        String claimedAuthority,
        String parserContractIdentifier,
        String attributionContractIdentifier,
        ScenarioMemberProcessingOutcome.ParseOutcome parseOutcome,
        ScenarioMemberProcessingOutcome.AttributionOutcome attributionOutcome,
        Optional<String> attributionStructuralLocation,
        String schemaContractIdentifier,
        StructuralAdmissionState structuralAdmissionState,
        List<ScenarioSchemaDiagnostic> schemaDiagnostics,
        ScenarioAuthorityParsedJsonV1 authoritativeParsedJson,
        ScenarioAuthorityAttributionV1 authoritativeAttribution,
        JsonNode parsedSource
) implements ScenarioSchemaAdmissionOutcome, ScenarioSourceNormalizationMemberOutcome {
    public AttributedMemberSchemaAdmissionOutcome {
        Objects.requireNonNull(parentMemberRef, "parentMemberRef");
        claimedAuthority = requireNonBlank(claimedAuthority, "claimedAuthority");
        requireContract(parserContractIdentifier, ScenarioMemberProcessingOutcome.PARSER_CONTRACT_IDENTIFIER,
                "parserContractIdentifier");
        requireContract(attributionContractIdentifier,
                ScenarioMemberProcessingOutcome.ATTRIBUTION_CONTRACT_IDENTIFIER,
                "attributionContractIdentifier");
        if (parseOutcome != ScenarioMemberProcessingOutcome.ParseOutcome.PARSED) {
            throw new IllegalArgumentException("schema-admitted member must retain PARSED outcome");
        }
        if (attributionOutcome != ScenarioMemberProcessingOutcome.AttributionOutcome.ATTRIBUTED) {
            throw new IllegalArgumentException("schema-admitted member must retain ATTRIBUTED outcome");
        }
        attributionStructuralLocation = ScenarioMemberProcessingOutcome.requireJsonPointer(
                attributionStructuralLocation);
        requireContract(schemaContractIdentifier,
                ScenarioManifestSchemaValidator.SCHEMA_CONTRACT_IDENTIFIER,
                "schemaContractIdentifier");
        Objects.requireNonNull(structuralAdmissionState, "structuralAdmissionState");
        schemaDiagnostics = ScenarioSchemaDiagnostic.canonicalCollection(schemaDiagnostics);
        Objects.requireNonNull(authoritativeParsedJson, "authoritativeParsedJson");
        Objects.requireNonNull(authoritativeAttribution, "authoritativeAttribution");
        if (authoritativeAttribution.parsedJson() != authoritativeParsedJson)
            throw new IllegalArgumentException("attribution must bind the exact parsed proof");
        if (!claimedAuthority.equals(authoritativeAttribution.authority()))
            throw new IllegalArgumentException("claimedAuthority must project authoritative attribution");
        if ((structuralAdmissionState == StructuralAdmissionState.STRUCTURALLY_ADMITTED)
                != schemaDiagnostics.isEmpty()) {
            throw new IllegalArgumentException(
                    "STRUCTURALLY_ADMITTED requires no diagnostics and STRUCTURALLY_REJECTED requires diagnostics");
        }
        parsedSource = Objects.requireNonNull(parsedSource, "parsedSource").deepCopy();
        if (!parsedSource.equals(authoritativeParsedJson.document()))
            throw new IllegalArgumentException("parsedSource must project the authoritative parsed proof");
    }

    @Override
    public JsonNode parsedSource() {
        return parsedSource.deepCopy();
    }

    public enum StructuralAdmissionState {
        STRUCTURALLY_ADMITTED,
        STRUCTURALLY_REJECTED
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
