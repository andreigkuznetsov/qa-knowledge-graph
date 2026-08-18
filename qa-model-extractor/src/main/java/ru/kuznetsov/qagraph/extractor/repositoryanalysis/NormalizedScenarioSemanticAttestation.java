package ru.kuznetsov.qagraph.extractor.repositoryanalysis;

import ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic.ScenarioSemanticFingerprint;

import java.util.Objects;

import static ru.kuznetsov.qagraph.extractor.repositoryanalysis.ScenarioSourceNormalizedRecords.NormalizedScenarioDeclarationOccurrence;

/** Factory-only binding of one exact normalized Scenario occurrence to its composed fingerprint. */
public final class NormalizedScenarioSemanticAttestation {
    private final NormalizedScenarioDeclarationOccurrence declaration;
    private final ScenarioSemanticFingerprint fingerprint;

    private NormalizedScenarioSemanticAttestation(
            NormalizedScenarioDeclarationOccurrence declaration,
            ScenarioSemanticFingerprint fingerprint
    ) {
        this.declaration = Objects.requireNonNull(declaration, "declaration");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public static NormalizedScenarioSemanticAttestation create(
            NormalizedScenarioDeclarationOccurrence declaration
    ) {
        Objects.requireNonNull(declaration, "declaration");
        ScenarioNormalizedSemanticFingerprinter.Result result =
                new ScenarioNormalizedSemanticFingerprinter().fingerprint(declaration);
        if (result.fingerprint() == null) {
            throw new ManifestSemanticCompositionException(
                    ManifestSemanticCompositionException.Code.SCENARIO_SEMANTIC_UNAVAILABLE);
        }
        return new NormalizedScenarioSemanticAttestation(declaration, result.fingerprint());
    }

    public NormalizedScenarioDeclarationOccurrence declaration() {
        return declaration;
    }

    public ScenarioSemanticFingerprint fingerprint() {
        return fingerprint;
    }

    boolean isAuthoritativelyBound() {
        ScenarioNormalizedSemanticFingerprinter.Result result =
                new ScenarioNormalizedSemanticFingerprinter().fingerprint(declaration);
        return fingerprint.equals(result.fingerprint());
    }
}
