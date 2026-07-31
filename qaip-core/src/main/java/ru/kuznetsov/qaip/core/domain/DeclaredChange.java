package ru.kuznetsov.qaip.core.domain;

import java.util.Map;

public record DeclaredChange(String artifactCategory, String canonicalIdentity,
                             String changeKind, String schemaVersion,
                             Map<String, Object> beforeState, Map<String, Object> afterState) {
    public DeclaredChange {
        beforeState = beforeState == null ? null : Metadata.immutableMap(beforeState);
        afterState = afterState == null ? null : Metadata.immutableMap(afterState);
    }
}
