package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.Optional;

/** One immutable terminal ADR-014 processing outcome for one captured parent member. */
public sealed interface ScenarioMemberProcessingOutcome
        permits AttributedMemberProcessingOutcome, UnattributableMemberProcessingOutcome {

    String PARSER_CONTRACT_IDENTIFIER = ScenarioManifestJsonParser.PARSER_CONTRACT_IDENTIFIER;
    String ATTRIBUTION_CONTRACT_IDENTIFIER = "scenario-authority-attribution-v1";
    String STRUCTURAL_LOCATION_CONTRACT_IDENTIFIER = "rfc-6901-json-pointer-v1";

    ParentCapturedMemberRef parentMemberRef();

    String parserContractIdentifier();

    String attributionContractIdentifier();

    ParseOutcome parseOutcome();

    AttributionOutcome attributionOutcome();

    Optional<String> structuralLocation();

    enum ParseOutcome {
        PARSED,
        INVALID_UTF8,
        MALFORMED_JSON,
        DUPLICATE_JSON_MEMBER,
        TRAILING_JSON_CONTENT
    }

    enum AttributionOutcome {
        ATTRIBUTED,
        NON_OBJECT_ROOT,
        MISSING_AUTHORITY,
        AUTHORITY_NOT_STRING,
        INVALID_AUTHORITY,
        PARSE_UNATTRIBUTABLE
    }

    static Optional<String> requireJsonPointer(Optional<String> value) {
        if (value == null) throw new NullPointerException("structuralLocation");
        value.ifPresent(pointer -> {
            if (!pointer.isEmpty() && !pointer.startsWith("/")) {
                throw new IllegalArgumentException("structuralLocation must be an RFC 6901 JSON Pointer");
            }
        });
        return value;
    }
}
