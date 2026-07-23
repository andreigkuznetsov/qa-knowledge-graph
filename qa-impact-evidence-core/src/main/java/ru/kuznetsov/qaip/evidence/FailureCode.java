package ru.kuznetsov.qaip.evidence;

/** Failures that prevent a conclusion from being formed. */
public enum FailureCode {
    INVALID_REQUEST, INVALID_MANIFEST, UNSUPPORTED_VERSION, INTEGRITY_MISMATCH,
    INCOMPATIBLE_CHANGE_DOMAIN, CHANGE_MANIFEST_MISMATCH
}
