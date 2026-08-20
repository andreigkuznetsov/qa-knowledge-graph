package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioAuthoritySchemaDiagnosticMapperV1;
import ru.kuznetsov.qaip.evidencegovernance.diagnostic.ScenarioSchemaDiagnostic;

import java.util.List;

/** Extractor projection of the Evidence Governance-owned canonical diagnostic authority. */
public final class ScenarioSchemaDiagnosticAdapterV1 {
    public static final String CONTRACT_IDENTIFIER = ScenarioAuthoritySchemaDiagnosticMapperV1.CONTRACT_IDENTIFIER;
    public static final String RULE_PREFIX = ScenarioAuthoritySchemaDiagnosticMapperV1.RULE_PREFIX;
    public static final String SCHEMA_CONTENT_IDENTITY = ScenarioAuthoritySchemaDiagnosticMapperV1.SCHEMA_CONTENT_IDENTITY;
    public static final String V1_SCHEMA_MAPPING_BINDING =
            ScenarioAuthoritySchemaDiagnosticMapperV1.V1_SCHEMA_MAPPING_BINDING;

    private final ScenarioAuthoritySchemaDiagnosticMapperV1 delegate =
            new ScenarioAuthoritySchemaDiagnosticMapperV1();

    public List<ScenarioSchemaDiagnostic> validate(JsonNode document) {
        try { return delegate.validate(document); }
        catch (IllegalStateException exception) {
            throw new ScenarioSchemaDiagnosticMappingException(exception.getMessage(), exception);
        }
    }
}
