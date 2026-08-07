package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record OperationOverviewVerificationAvailableView(
        OperationOverviewVerificationState state,
        OperationTestsView verification
) implements OperationOverviewVerificationSectionView {
    public OperationOverviewVerificationAvailableView {
        if (state != OperationOverviewVerificationState.AVAILABLE) {
            throw new IllegalArgumentException("state must be AVAILABLE");
        }
        Objects.requireNonNull(verification, "verification");
    }
}
