package ru.kuznetsov.qaip.evidencegovernance.source;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.scenarioauthority.ScenarioAuthorityManifestSchemaValidatorV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.kuznetsov.qaip.evidencegovernance.source.EvidenceGovernanceNormalizedManifestV1.*;

/** Sole authoritative deterministic Scenario Authority V1 source normalizer. */
public final class ScenarioAuthorityNormalizerV1 {
    private final ScenarioAuthorityManifestSchemaValidatorV1 validator =
            new ScenarioAuthorityManifestSchemaValidatorV1();

    public EvidenceGovernanceNormalizedManifestV1 normalizeStructurallyAdmitted(
            ScenarioAuthorityParsedJsonV1 parsed, ScenarioAuthorityAttributionV1 attribution,
            ScenarioAuthorityNormalizationContractsV1 contracts) {
        Objects.requireNonNull(parsed); Objects.requireNonNull(attribution); Objects.requireNonNull(contracts);
        if (attribution.parsedJson() != parsed)
            throw new IllegalArgumentException("attribution must belong to the exact parsed proof");
        JsonNode root = parsed.authoritativeDocument();
        if (!validator.validate(contracts.schemaContractIdentifier(), root).structurallyValid())
            throw new IllegalArgumentException("normalization requires pinned-schema-compatible parsed input");
        if (!attribution.authority().equals(root.path("authority").textValue()))
            throw new IllegalStateException("attribution contradicts parsed source");
        List<Scenario> scenarios = new ArrayList<>();
        JsonNode sourceScenarios = root.path("scenarios");
        for (int index = 0; index < sourceScenarios.size(); index++)
            scenarios.add(scenario(sourceScenarios.get(index), index, attribution.authority(),
                    root.path("scenarioIdentityScheme").textValue(), contracts));
        return new EvidenceGovernanceNormalizedManifestV1(contracts, attribution.authority(),
                root.path("format").textValue(), root.path("schemaVersion").textValue(),
                root.path("scenarioIdentityScheme").textValue(), scenarios);
    }

    private static Scenario scenario(JsonNode source, int index, String authority, String identityScheme,
                                     ScenarioAuthorityNormalizationContractsV1 c) {
        String key = source.path("scenarioKey").textValue();
        ClaimedScenarioIdentity identity = new ClaimedScenarioIdentity(authority, key, identityScheme);
        List<String> given = texts(source.path("given")), when = texts(source.path("when")),
                then = texts(source.path("then"));
        List<Step> steps = new ArrayList<>();
        addSteps(steps, identity, StepPhase.GIVEN, given, c);
        addSteps(steps, identity, StepPhase.WHEN, when, c);
        addSteps(steps, identity, StepPhase.THEN, then, c);
        JsonNode op = source.path("operationRef");
        OperationReference operation = new OperationReference(identity, c.operationRole(),
                c.operationDatumIdentityVersion(), op.path("identityScheme").textValue(),
                c.operationSemanticVersion(), op.path("method").textValue(), op.path("path").textValue());
        List<BusinessRuleReference> rules = new ArrayList<>();
        JsonNode sourceRules = source.path("ruleRefs");
        for (int position = 0; position < sourceRules.size(); position++) {
            JsonNode rule = sourceRules.get(position);
            rules.add(new BusinessRuleReference(position, identity, rule.path("authority").textValue(),
                    rule.path("stableRuleKey").textValue(), rule.path("identityScheme").textValue(),
                    c.businessRuleDatumIdentityVersion(), c.businessRuleSemanticVersion()));
        }
        return new Scenario(index, "/scenarios/" + index, c.scenarioDeclarationOccurrenceIdentityVersion(),
                identity, key, source.path("title").textValue(), c.scenarioSemanticVersion(), given, when, then,
                steps, operation, rules);
    }

    private static List<String> texts(JsonNode array) {
        List<String> result = new ArrayList<>(); array.forEach(value -> result.add(value.textValue())); return result;
    }
    private static void addSteps(List<Step> target, ClaimedScenarioIdentity owner, StepPhase phase,
                                 List<String> texts, ScenarioAuthorityNormalizationContractsV1 c) {
        for (int ordinal = 0; ordinal < texts.size(); ordinal++)
            target.add(new Step(owner, phase, ordinal, c.stepIdentityVersion(), texts.get(ordinal),
                    c.stepSemanticVersion()));
    }
}
