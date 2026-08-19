package ru.kuznetsov.qaip.evidencegovernance.fingerprint.semantic;

import java.util.Objects;

/** The only parent variant admitted by the first provenance activity slice. */
public record SemanticProvenanceParentReference(
        String parentKind,
        AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference capturedMember
) {
    public static final String CAPTURED_MEMBER = "CAPTURED_MEMBER";

    public SemanticProvenanceParentReference {
        Objects.requireNonNull(parentKind, "parentKind");
        if (!CAPTURED_MEMBER.equals(parentKind)) {
            throw new IllegalArgumentException(
                    "DERIVE_ATTRIBUTED_MEMBER_OUTCOME accepts only CAPTURED_MEMBER parents");
        }
        Objects.requireNonNull(capturedMember, "capturedMember");
    }

    public static SemanticProvenanceParentReference capturedMember(
            AttributedMemberOutcomeFingerprintInput.ParentCapturedMemberReference capturedMember
    ) {
        return new SemanticProvenanceParentReference(CAPTURED_MEMBER, capturedMember);
    }
}
