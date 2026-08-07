package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationOverviewView(
        OperationOverviewIdentityView identity,
        OperationOverviewImplementationSectionView implementation,
        OperationOverviewEventPathSectionView eventPath,
        OperationOverviewVerificationSectionView verification
) {
    public OperationOverviewView {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(eventPath, "eventPath");
        Objects.requireNonNull(verification, "verification");
    }
}
