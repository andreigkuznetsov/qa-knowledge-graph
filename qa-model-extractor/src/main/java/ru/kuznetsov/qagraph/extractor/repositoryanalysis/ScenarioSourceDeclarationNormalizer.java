package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Converts schema-admitted JSON into immutable source-native declaration records. */
public final class ScenarioSourceDeclarationNormalizer {
    public ScenarioSourceNormalizationResult normalize(ScenarioSchemaAdmissionResult admissionResult) {
        Objects.requireNonNull(admissionResult, "admissionResult");
        List<ScenarioSourceNormalizationMemberOutcome> outcomes = new ArrayList<>();
        for (ScenarioSchemaAdmissionOutcome outcome : admissionResult.memberOutcomes()) {
            if (outcome instanceof AttributedMemberSchemaAdmissionOutcome attributed
                    && attributed.structuralAdmissionState()
                    == AttributedMemberSchemaAdmissionOutcome.StructuralAdmissionState.STRUCTURALLY_ADMITTED) {
                outcomes.add(normalizeManifest(attributed));
            } else {
                outcomes.add((ScenarioSourceNormalizationMemberOutcome) outcome);
            }
        }
        return new ScenarioSourceNormalizationResult(admissionResult, outcomes);
    }

    private static NormalizedManifestDatum normalizeManifest(AttributedMemberSchemaAdmissionOutcome admission) {
        JsonNode root = admission.parsedSource();
        ParentCapturedMemberRef parent = admission.parentMemberRef();
        ManifestOccurrenceIdentity manifestIdentity = new ManifestOccurrenceIdentity(
                parent.parentSourceId(), parent.parentSnapshotId(), parent.parentContentFingerprint(),
                parent.normalizedRepositoryRelativePath(), MANIFEST_OCCURRENCE_IDENTITY_VERSION);
        String claimedAuthority = admission.claimedAuthority();
        String identityScheme = root.path("scenarioIdentityScheme").textValue();
        List<NormalizedScenarioDeclarationOccurrence> scenarios = new ArrayList<>();
        JsonNode sourceScenarios = root.path("scenarios");
        for (int scenarioIndex = 0; scenarioIndex < sourceScenarios.size(); scenarioIndex++) {
            scenarios.add(normalizeScenario(sourceScenarios.get(scenarioIndex), scenarioIndex,
                    manifestIdentity, claimedAuthority, identityScheme));
        }
        return new NormalizedManifestDatum(admission, manifestIdentity, claimedAuthority,
                root.path("format").textValue(), root.path("schemaVersion").textValue(),
                identityScheme, scenarios);
    }

    private static NormalizedScenarioDeclarationOccurrence normalizeScenario(
            JsonNode source,
            int scenarioIndex,
            ManifestOccurrenceIdentity manifestIdentity,
            String authority,
            String identityScheme
    ) {
        String scenarioKey = source.path("scenarioKey").textValue();
        ClaimedScenarioIdentity claimedIdentity = new ClaimedScenarioIdentity(
                authority, scenarioKey, identityScheme);
        ScenarioDeclarationOccurrenceIdentity occurrenceIdentity =
                new ScenarioDeclarationOccurrenceIdentity(manifestIdentity,
                        "/scenarios/" + scenarioIndex, DECLARATION_OCCURRENCE_IDENTITY_VERSION);
        List<String> given = authoredTexts(source.path("given"));
        List<String> when = authoredTexts(source.path("when"));
        List<String> then = authoredTexts(source.path("then"));
        List<NormalizedScenarioStep> steps = new ArrayList<>();
        addSteps(steps, claimedIdentity, StepPhase.GIVEN, given);
        addSteps(steps, claimedIdentity, StepPhase.WHEN, when);
        addSteps(steps, claimedIdentity, StepPhase.THEN, then);

        JsonNode operation = source.path("operationRef");
        NormalizedOperationReferenceDatum operationReference = new NormalizedOperationReferenceDatum(
                new OperationReferenceDatumIdentity(claimedIdentity, OPERATION_REFERENCE_ROLE,
                        OPERATION_REFERENCE_DATUM_IDENTITY_VERSION),
                operation.path("identityScheme").textValue(),
                operation.path("method").textValue(), operation.path("path").textValue());

        List<NormalizedBusinessRuleReferenceDatum> rules = new ArrayList<>();
        JsonNode sourceRules = source.path("ruleRefs");
        for (int index = 0; index < sourceRules.size(); index++) {
            JsonNode rule = sourceRules.get(index);
            rules.add(new NormalizedBusinessRuleReferenceDatum(
                    new BusinessRuleReferenceDatumIdentity(claimedIdentity,
                            rule.path("authority").textValue(), rule.path("stableRuleKey").textValue(),
                            rule.path("identityScheme").textValue(),
                            BUSINESS_RULE_REFERENCE_DATUM_IDENTITY_VERSION), index));
        }
        return new NormalizedScenarioDeclarationOccurrence(occurrenceIdentity, claimedIdentity,
                scenarioKey, source.path("title").textValue(), given, when, then, steps,
                operationReference, rules);
    }

    private static List<String> authoredTexts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.textValue()));
        return values;
    }

    private static void addSteps(
            List<NormalizedScenarioStep> target,
            ClaimedScenarioIdentity claimedIdentity,
            StepPhase phase,
            List<String> texts
    ) {
        for (int ordinal = 0; ordinal < texts.size(); ordinal++) {
            target.add(new NormalizedScenarioStep(
                    new ScenarioStepIdentity(claimedIdentity, phase, ordinal, STEP_IDENTITY_VERSION),
                    texts.get(ordinal)));
        }
    }
}
