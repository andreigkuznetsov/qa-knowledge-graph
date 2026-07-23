package ru.kuznetsov.qaip.evidence;

/** Explicitly unresolved identity with a stable source reason code. */
public record UnresolvedIdentity(String reasonCode) implements IdentityResolution {
    public UnresolvedIdentity { EvidenceSnapshotRef.requireText(reasonCode, "reasonCode"); }
}
