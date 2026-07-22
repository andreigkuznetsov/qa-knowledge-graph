package ru.kuznetsov.qagraph.change.base;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeSetResult;
import ru.kuznetsov.qagraph.change.validation.IntrinsicallyValidChange;
import java.util.List;
import java.util.Optional;
import ru.kuznetsov.qagraph.change.model.*;
import ru.kuznetsov.qagraph.change.validation.IntrinsicChangeValidator;
public final class BaseTestFixtures {
    private BaseTestFixtures() { }
    public static BaseVerifiedChange verified(IntrinsicallyValidChange candidate, Optional<ArtifactState> state) { return new BaseVerifiedChange(candidate, state); }
    public static BaseChangeSetResult result(CanonicalBaseModelEvidence evidence, IntrinsicChangeSetResult intrinsic, List<BaseVerifiedChange> verified) {
        return new BaseChangeSetResult(evidence.artifactIndex(), evidence, intrinsic, intrinsic.failedDeclarations(), intrinsic.ambiguities(), verified, List.of());
    }
    public static BaseChangeSetResult source(CanonicalBaseModelEvidence evidence) {
        try {
            NodeArtifactState state = new NodeArtifactState(CanonicalQaModelVersion.V0_1,
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"id\":\"FIXTURE-ONLY\",\"type\":\"CHECK\",\"name\":\"fixture\",\"check\":{\"checkType\":\"SQL\",\"assertion\":\"ok\"}}"));
            DeclaredChange declaration = new DeclaredChange(ArtifactCategory.NODE, state.identity(), ChangeKind.ADDED, CanonicalQaModelVersion.V0_1, Optional.empty(), Optional.of(state));
            IntrinsicChangeSetResult intrinsic = new IntrinsicChangeValidator().validate(new DeclaredChangeSet(List.of(declaration)));
            return result(evidence, intrinsic, List.of());
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
