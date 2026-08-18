package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Groups admitted source declarations solely by their exact claimed Scenario identity. */
public final class ScenarioAuthorityWideIdentityGrouper {
    private static final Comparator<ClaimedScenarioIdentity> IDENTITY_ORDER = (left, right) -> {
        int authority = compareCodePoints(left.authority(), right.authority());
        if (authority != 0) return authority;
        int key = compareCodePoints(left.scenarioKey(), right.scenarioKey());
        if (key != 0) return key;
        return compareCodePoints(left.identityScheme(), right.identityScheme());
    };

    public ScenarioAuthorityWideIdentityGroupingResult group(ScenarioSourceNormalizationResult normalizationResult) {
        Objects.requireNonNull(normalizationResult, "normalizationResult");
        Map<ClaimedScenarioIdentity, List<ScenarioIdentityGroup.Occurrence>> grouped = new LinkedHashMap<>();
        for (ScenarioSourceNormalizationMemberOutcome member : normalizationResult.memberOutcomes()) {
            if (!(member instanceof NormalizedManifestDatum manifest)) continue;
            for (NormalizedScenarioDeclarationOccurrence declaration : manifest.scenarioDeclarations()) {
                grouped.computeIfAbsent(declaration.claimedScenarioIdentity(), ignored -> new ArrayList<>())
                        .add(new ScenarioIdentityGroup.Occurrence(manifest.parentMemberRef(), declaration));
            }
        }

        List<ScenarioIdentityGroup> groups = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(IDENTITY_ORDER))
                .map(entry -> new ScenarioIdentityGroup(entry.getKey(),
                        entry.getValue().size() == 1
                                ? ScenarioIdentityGroup.State.UNIQUE
                                : ScenarioIdentityGroup.State.DUPLICATE_UNCLASSIFIED,
                        entry.getValue()))
                .toList();
        return new ScenarioAuthorityWideIdentityGroupingResult(normalizationResult, groups);
    }

    private static int compareCodePoints(String left, String right) {
        var leftPoints = left.codePoints().iterator();
        var rightPoints = right.codePoints().iterator();
        while (leftPoints.hasNext() && rightPoints.hasNext()) {
            int comparison = Integer.compare(leftPoints.nextInt(), rightPoints.nextInt());
            if (comparison != 0) return comparison;
        }
        if (leftPoints.hasNext()) return 1;
        if (rightPoints.hasNext()) return -1;
        return 0;
    }
}
