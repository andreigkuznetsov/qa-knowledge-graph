package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScenarioIdentityValidator {
    private static final Comparator<String> CODE_POINT_ORDER = ScenarioIdentityValidator::compareCodePoints;
    private static final Comparator<Identity> IDENTITY_ORDER = Comparator
            .comparing(Identity::authority, CODE_POINT_ORDER)
            .thenComparing(Identity::scenarioKey, CODE_POINT_ORDER);
    private static final Comparator<ScenarioIdentityValidationResult.Declaration> DECLARATION_ORDER = Comparator
            .comparing((ScenarioIdentityValidationResult.Declaration value) ->
                    value.source().repositoryRelativePath(), CODE_POINT_ORDER)
            .thenComparingInt(value -> value.source().declarationIndex());

    public ScenarioIdentityValidationResult validate(ScenarioManifestSchemaValidationResult schemaResult) {
        Objects.requireNonNull(schemaResult, "schemaResult");
        Map<Identity, List<ScenarioIdentityValidationResult.Declaration>> declarations = new LinkedHashMap<>();
        List<String> excluded = new ArrayList<>();

        for (ScenarioManifestSchemaValidationResult.ValidatedMember member : schemaResult.members()) {
            String path = member.source().source().repositoryRelativePath();
            if (!member.structurallyAdmitted()) {
                excluded.add(path);
                continue;
            }

            JsonNode manifest = member.source().document();
            String authority = manifest.path("authority").asText();
            JsonNode scenarios = manifest.path("scenarios");
            for (int index = 0; index < scenarios.size(); index++) {
                JsonNode scenario = scenarios.get(index);
                String scenarioKey = scenario.path("scenarioKey").asText();
                Identity identity = new Identity(authority, scenarioKey);
                declarations.computeIfAbsent(identity, ignored -> new ArrayList<>())
                        .add(new ScenarioIdentityValidationResult.Declaration(
                                new ScenarioIdentityValidationResult.DeclarationSource(path, index),
                                scenario));
            }
        }

        List<ScenarioIdentityValidationResult.IdentityGroup> groups = declarations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(IDENTITY_ORDER))
                .map(entry -> new ScenarioIdentityValidationResult.IdentityGroup(
                        entry.getKey().authority(),
                        entry.getKey().scenarioKey(),
                        entry.getValue().stream().sorted(DECLARATION_ORDER).toList()))
                .toList();
        List<ScenarioIdentityValidationResult.DuplicateDiagnostic> diagnostics = groups.stream()
                .filter(group -> !group.unique())
                .map(ScenarioIdentityValidator::duplicateDiagnostic)
                .toList();
        excluded.sort(CODE_POINT_ORDER);
        return new ScenarioIdentityValidationResult(groups, diagnostics, excluded);
    }

    private static ScenarioIdentityValidationResult.DuplicateDiagnostic duplicateDiagnostic(
            ScenarioIdentityValidationResult.IdentityGroup group) {
        List<ScenarioIdentityValidationResult.DeclarationSource> sources = group.declarations().stream()
                .map(ScenarioIdentityValidationResult.Declaration::source)
                .toList();
        return new ScenarioIdentityValidationResult.DuplicateDiagnostic(
                ScenarioIdentityValidationResult.Code.DUPLICATE_SCENARIO_IDENTITY,
                group.authority(),
                group.scenarioKey(),
                sources,
                "Duplicate Scenario source identity " + group.authority() + ':' + group.scenarioKey()
                        + " has " + sources.size() + " declarations.");
    }

    private static int compareCodePoints(String left, String right) {
        var leftPoints = left.codePoints().iterator();
        var rightPoints = right.codePoints().iterator();
        while (leftPoints.hasNext() && rightPoints.hasNext()) {
            int comparison = Integer.compare(leftPoints.nextInt(), rightPoints.nextInt());
            if (comparison != 0) return comparison;
        }
        return Boolean.compare(leftPoints.hasNext(), rightPoints.hasNext());
    }

    private record Identity(String authority, String scenarioKey) {
    }
}
