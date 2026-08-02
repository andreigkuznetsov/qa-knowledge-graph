package ru.kuznetsov.qagraph.extractor.integrationtest;

import java.util.List;
import java.util.Objects;

public record IntegrationTestEvidence(
        List<TestImplementationEvidence> tests,
        List<HttpInteractionEvidence> httpInteractions,
        List<AssertionEvidence> assertions
) {
    public IntegrationTestEvidence {
        tests = List.copyOf(Objects.requireNonNull(tests, "tests"));
        httpInteractions = List.copyOf(Objects.requireNonNull(httpInteractions, "httpInteractions"));
        assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
    }
}
