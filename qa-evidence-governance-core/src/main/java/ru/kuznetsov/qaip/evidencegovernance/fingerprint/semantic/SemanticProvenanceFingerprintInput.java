package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.List;
import java.util.Objects;

/** Complete immutable input for the V1 attributed-member derivation activity. */
public record SemanticProvenanceFingerprintInput(
        String semanticCanonicalizationVersion,
        SemanticProvenanceIdentityV1 provenanceIdentity,
        String activityKind,
        String activityVersion,
        SemanticProvenanceOutputReference outputReference,
        List<SemanticProvenanceParentReference> parents,
        String derivationOutcome
) {
    public static final String ACTIVITY_VERSION =
            "scenario-authority-derive-attributed-member-outcome-v1";
    public static final String DERIVATION_OUTCOME = "DERIVED";

    public SemanticProvenanceFingerprintInput {
        requireExact(semanticCanonicalizationVersion,
                SemanticProvenanceFingerprintEncoder.ENCODING_IDENTIFIER,
                "semanticCanonicalizationVersion");
        Objects.requireNonNull(provenanceIdentity, "provenanceIdentity");
        requireExact(activityKind, SemanticProvenanceIdentityV1.ACTIVITY_KIND, "activityKind");
        requireExact(activityVersion, ACTIVITY_VERSION, "activityVersion");
        Objects.requireNonNull(outputReference, "outputReference");
        requireExact(outputReference.outputKind(), SemanticProvenanceIdentityV1.OUTPUT_KIND,
                "outputReference.outputKind");
        parents = List.copyOf(Objects.requireNonNull(parents, "parents"));
        if (parents.size() != 1) {
            throw new IllegalArgumentException(
                    "DERIVE_ATTRIBUTED_MEMBER_OUTCOME requires exactly one CAPTURED_MEMBER parent");
        }
        SemanticProvenanceParentReference parent = parents.getFirst();
        if (!provenanceIdentity.outputDatumIdentity().equals(outputReference.outputIdentity())
                || !provenanceIdentity.outputDatumIdentity().equals(parent.capturedMember())) {
            throw new IllegalArgumentException(
                    "provenance identity, output identity, and captured-member parent must match exactly");
        }
        requireExact(provenanceIdentity.activityKind(), activityKind, "provenanceIdentity.activityKind");
        requireExact(provenanceIdentity.outputKind(), outputReference.outputKind(),
                "provenanceIdentity.outputKind");
        requireExact(derivationOutcome, DERIVATION_OUTCOME, "derivationOutcome");
    }

    public static SemanticProvenanceFingerprintInput deriveAttributedMemberOutcome(
            SemanticProvenanceOutputReference outputReference
    ) {
        Objects.requireNonNull(outputReference, "outputReference");
        var identity = SemanticProvenanceIdentityV1.forAttributedMember(outputReference.outputIdentity());
        return new SemanticProvenanceFingerprintInput(
                SemanticProvenanceFingerprintEncoder.ENCODING_IDENTIFIER,
                identity,
                SemanticProvenanceIdentityV1.ACTIVITY_KIND,
                ACTIVITY_VERSION,
                outputReference,
                List.of(SemanticProvenanceParentReference.capturedMember(outputReference.outputIdentity())),
                DERIVATION_OUTCOME);
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
