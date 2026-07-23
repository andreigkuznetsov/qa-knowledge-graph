package ru.kuznetsov.qaip.evidence;

/** Outcome algebra separating valid conclusions from analysis failures. */
public sealed interface ImpactEvidenceResult permits ImpactEvidenceCompleted, ImpactEvidenceFailed { }
