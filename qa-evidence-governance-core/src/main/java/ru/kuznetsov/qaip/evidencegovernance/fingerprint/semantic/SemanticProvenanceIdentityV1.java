package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** Structured V1 provenance identity; the output fingerprint is intentionally absent. */
public record SemanticProvenanceIdentityV1(
        String identityContractVersion,
        String activityKind,
        String outputKind,
        AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference outputDatumIdentity
) {
    public static final String IDENTITY_CONTRACT_VERSION =
            "scenario-authority-semantic-provenance-identity-v1";
    public static final String ACTIVITY_KIND = "DERIVE_ATTRIBUTED_MEMBER_OUTCOME";
    public static final String OUTPUT_KIND = "ATTRIBUTED_MEMBER_OUTCOME";

    public SemanticProvenanceIdentityV1 {
        requireExact(identityContractVersion, IDENTITY_CONTRACT_VERSION, "identityContractVersion");
        requireExact(activityKind, ACTIVITY_KIND, "activityKind");
        requireExact(outputKind, OUTPUT_KIND, "outputKind");
        Objects.requireNonNull(outputDatumIdentity, "outputDatumIdentity");
    }

    public static SemanticProvenanceIdentityV1 forAttributedMember(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference identity
    ) {
        return new SemanticProvenanceIdentityV1(
                IDENTITY_CONTRACT_VERSION, ACTIVITY_KIND, OUTPUT_KIND, identity);
    }

    private static void requireExact(String actual, String expected, String field) {
        Objects.requireNonNull(actual, field);
        if (!expected.equals(actual)) throw new IllegalArgumentException(field + " must be " + expected);
    }
}
