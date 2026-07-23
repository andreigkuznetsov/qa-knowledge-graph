package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;
import java.util.Objects;

/** Exactly resolved canonical identity. */
public record ResolvedIdentity(CanonicalIdentity identity) implements IdentityResolution {
    public ResolvedIdentity { Objects.requireNonNull(identity, "identity must not be null"); }
}
