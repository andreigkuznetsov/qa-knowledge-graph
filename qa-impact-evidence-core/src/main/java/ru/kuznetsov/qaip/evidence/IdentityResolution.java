package ru.kuznetsov.qaip.evidence;

import ru.kuznetsov.qagraph.change.model.CanonicalIdentity;

/** Categorical source-local to canonical identity resolution. */
public sealed interface IdentityResolution permits ResolvedIdentity, UnresolvedIdentity { }
