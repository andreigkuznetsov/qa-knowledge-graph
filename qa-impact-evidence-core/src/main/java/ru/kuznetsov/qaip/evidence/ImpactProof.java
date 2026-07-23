package ru.kuznetsov.qaip.evidence;

/** Auditable positive proof selected by deterministic precedence. */
public sealed interface ImpactProof permits DirectChangeProof, RelationshipPathProof { }
