package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.*;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.*;

/** Groups admitted source declarations solely by their exact claimed Scenario identity. */
public final class ScenarioAuthorityWideIdentityGrouper {
    private final ScenarioOccurrenceInputMapperV1 mapper = new ScenarioOccurrenceInputMapperV1();
    private static final Comparator<ClaimedScenarioIdentity> IDENTITY_ORDER = (left, right) -> {
        int authority = compareCodePoints(left.authority(), right.authority());
        if (authority != 0) return authority;
        int key = compareCodePoints(left.scenarioKey(), right.scenarioKey());
        if (key != 0) return key;
        return compareCodePoints(left.identityScheme(), right.identityScheme());
    };

    public ScenarioAuthorityWideIdentityGroupingResult group(ScenarioAuthorityNormalizedProcessingV1 handoff) {
        Objects.requireNonNull(handoff, "handoff");
        Map<ClaimedScenarioIdentity, List<ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding>> grouped = new LinkedHashMap<>();
        for(var binding:handoff.occurrences()) grouped.computeIfAbsent(binding.declaration().claimedScenarioIdentity(),ignored->new ArrayList<>()).add(binding);

        List<VerifiedScenarioIdentityGroupV1> verified = new ArrayList<>();
        List<ScenarioIdentityGroup> groups = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(IDENTITY_ORDER))
                .map(entry -> {
                    var bindings=entry.getValue().stream().sorted(ScenarioAuthorityWideIdentityGrouper::compareBindings).toList();
                    var outcomes=bindings.stream().map(b->mapper.attempt(handoff,b)).toList();
                    var sourceIdentity=entry.getKey();
                    var identity=new ScenarioSemanticCompositionRequest.ClaimedScenarioIdentity(sourceIdentity.authority(),sourceIdentity.scenarioKey(),sourceIdentity.identityScheme());
                    var authoritative=ScenarioIdentityGroupComposerV1.compose(ScenarioIdentityGroupComposerV1.DUPLICATE_OUTCOME_CONTRACT,identity,outcomes);
                    verified.add(authoritative);
                    List<ScenarioIdentityGroup.Occurrence> occurrences=new ArrayList<>();
                    for(int i=0;i<bindings.size();i++) occurrences.add(presentation(bindings.get(i),outcomes.get(i)));
                    return new ScenarioIdentityGroup(sourceIdentity,ScenarioIdentityGroup.State.valueOf(authoritative.state().name()),occurrences);
                })
                .toList();
        return new ScenarioAuthorityWideIdentityGroupingResult(handoff.normalizationResult(), groups, verified);
    }

    private static ScenarioIdentityGroup.Occurrence presentation(ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding b,ScenarioOccurrenceCompositionOutcomeV1 o){
        if(o instanceof ScenarioOccurrenceCompositionOutcomeV1.Composed c)return new ScenarioIdentityGroup.Occurrence(b.parentMemberRef(),b.declaration(),c.fingerprint(),null);
        var reason=((ScenarioOccurrenceCompositionOutcomeV1.Unavailable)o).reason();return new ScenarioIdentityGroup.Occurrence(b.parentMemberRef(),b.declaration(),null,ScenarioNormalizedSemanticFingerprinter.UnavailableReason.valueOf(reason.name()));}

    private static int compareBindings(ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding left,ScenarioAuthorityNormalizedProcessingV1.OccurrenceBinding right){
        int path=compareCodePoints(left.parentMemberRef().normalizedRepositoryRelativePath(),right.parentMemberRef().normalizedRepositoryRelativePath());if(path!=0)return path;
        return new java.math.BigInteger(left.declaration().occurrenceIdentity().structuralPath().substring("/scenarios/".length())).compareTo(new java.math.BigInteger(right.declaration().occurrenceIdentity().structuralPath().substring("/scenarios/".length())));}

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
