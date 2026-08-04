package ru.kuznetsov.qaip.explorer.view;

import java.util.Objects;

public record ImplementationPathView(
        String controllerName,
        String serviceName,
        String repositoryName
) {
    public ImplementationPathView {
        Objects.requireNonNull(controllerName, "controllerName");
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(repositoryName, "repositoryName");
    }
}
