package ru.kuznetsov.qaip.core.importing.binding;

import java.util.List;

public record BindingFailure(List<BindingFinding> findings) implements BindingResult {
    public BindingFailure {
        findings = List.copyOf(findings);
        if (findings.isEmpty()) throw new IllegalArgumentException("findings must not be empty");
    }
}
