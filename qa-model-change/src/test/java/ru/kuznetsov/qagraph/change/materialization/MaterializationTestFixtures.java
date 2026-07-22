package ru.kuznetsov.qagraph.change.materialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kuznetsov.qagraph.change.base.BaseChangeSetResult;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseEvidenceExtracted;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseEvidenceExtractor;
import ru.kuznetsov.qagraph.change.root.CanonicalBaseModelEvidence;
import java.util.List;
import java.util.Optional;

/** Stage-local fixtures; never part of the production API. */
public final class MaterializationTestFixtures {
    private MaterializationTestFixtures() { }
    public static ProposedModelMaterialized materialized(ProposedArtifactModel model) {
        try {
            var root = new ObjectMapper().readTree("{\"schemaVersion\":\"0.1\",\"project\":{\"id\":\"P-1\",\"name\":\"P\"},\"sources\":[],\"nodes\":[],\"relationships\":[]}");
            CanonicalBaseModelEvidence evidence = ((CanonicalBaseEvidenceExtracted) new CanonicalBaseEvidenceExtractor().extract(root)).evidence();
            return materialized(model, evidence);
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    public static ProposedModelMaterialized materialized(ProposedArtifactModel model, CanonicalBaseModelEvidence evidence) {
        BaseChangeSetResult source = ru.kuznetsov.qagraph.change.base.BaseTestFixtures.source(evidence);
        return new ProposedModelMaterialized(model, evidence, source);
    }
}
