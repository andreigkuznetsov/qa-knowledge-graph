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
    private final ScenarioNormalizedSemanticFingerprinter semanticFingerprinter =
            new ScenarioNormalizedSemanticFingerprinter();
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
                ScenarioNormalizedSemanticFingerprinter.Result semantic =
                        semanticFingerprinter.fingerprint(declaration);
                grouped.computeIfAbsent(declaration.claimedScenarioIdentity(), ignored -> new ArrayList<>())
                        .add(new ScenarioIdentityGroup.Occurrence(manifest.parentMemberRef(), declaration,
                                semantic.fingerprint(), semantic.unavailableReason()));
            }
        }

        List<ScenarioIdentityGroup> groups = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(IDENTITY_ORDER))
                .map(entry -> {
                    List<ScenarioIdentityGroup.Occurrence> occurrences = entry.getValue().stream()
                            .sorted(ScenarioAuthorityWideIdentityGrouper::compareOccurrences)
                            .toList();
                    return new ScenarioIdentityGroup(entry.getKey(), classify(occurrences), occurrences);
                })
                .toList();
        return new ScenarioAuthorityWideIdentityGroupingResult(normalizationResult, groups);
    }

    private static ScenarioIdentityGroup.State classify(List<ScenarioIdentityGroup.Occurrence> occurrences) {
        if (occurrences.size() == 1) return ScenarioIdentityGroup.State.UNIQUE;
        if (occurrences.stream().anyMatch(value -> !value.hasComparableSemanticFingerprint())) {
            return ScenarioIdentityGroup.State.DUPLICATE_UNCLASSIFIED;
        }
        long distinct = occurrences.stream().map(ScenarioIdentityGroup.Occurrence::semanticFingerprint)
                .distinct().limit(2).count();
        return distinct == 1
                ? ScenarioIdentityGroup.State.DUPLICATE_EQUIVALENT
                : ScenarioIdentityGroup.State.DUPLICATE_CONFLICTING;
    }

    private static int compareOccurrences(
            ScenarioIdentityGroup.Occurrence left,
            ScenarioIdentityGroup.Occurrence right
    ) {
        int path = compareCodePoints(left.parentMemberRef().normalizedRepositoryRelativePath(),
                right.parentMemberRef().normalizedRepositoryRelativePath());
        if (path != 0) return path;
        return Integer.compare(scenarioIndex(left), scenarioIndex(right));
    }

    private static int scenarioIndex(ScenarioIdentityGroup.Occurrence occurrence) {
        String location = occurrence.structuralLocation();
        return Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
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
