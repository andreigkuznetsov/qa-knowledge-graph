package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.integrationtest.HttpInteractionEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.IntegrationTestEvidence;
import ru.kuznetsov.qagraph.extractor.integrationtest.TestImplementationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OperationTestQualification {
    private static final Comparator<HttpInteractionEvidence> SOURCE_ORDER = Comparator
            .comparing(HttpInteractionEvidence::repositoryRelativePath)
            .thenComparingInt(HttpInteractionEvidence::line)
            .thenComparingInt(HttpInteractionEvidence::column)
            .thenComparing(HttpInteractionEvidence::sourceExpression);

    private OperationTestQualification() {
    }

    public static IntegrationTestEvidence withoutAmbiguousOperationMatches(
            List<RestOperationEvidence> operations, IntegrationTestEvidence evidence) {
        List<HttpInteractionEvidence> interactions = evidence.httpInteractions().stream()
                .filter(interaction -> operations.stream()
                        .filter(operation -> matches(interaction, operation))
                        .count() <= 1)
                .toList();
        return new IntegrationTestEvidence(evidence.tests(), interactions, evidence.assertions());
    }

    static Optional<HttpInteractionEvidence> qualifyingInteraction(
            TestImplementationEvidence test,
            RestOperationEvidence operation,
            IntegrationTestEvidence evidence) {
        Map<EndpointKey, HttpInteractionEvidence> distinctInteractions = new LinkedHashMap<>();
        evidence.httpInteractions().stream()
                .filter(interaction -> sameTest(interaction, test))
                .sorted(SOURCE_ORDER)
                .forEach(interaction -> distinctInteractions.putIfAbsent(EndpointKey.of(interaction), interaction));
        if (distinctInteractions.size() != 1) return Optional.empty();
        HttpInteractionEvidence interaction = distinctInteractions.values().iterator().next();
        return matches(interaction, operation) ? Optional.of(interaction) : Optional.empty();
    }

    private static boolean matches(HttpInteractionEvidence interaction, RestOperationEvidence operation) {
        return interaction.httpMethod().name().equals(operation.httpMethod().name())
                && normalizePath(interaction.endpointPath()).equals(normalizePath(operation.endpointPath()));
    }

    private static boolean sameTest(HttpInteractionEvidence interaction, TestImplementationEvidence test) {
        return interaction.owningTestClass().equals(test.testClass())
                && interaction.owningTestMethod().equals(test.testMethod());
    }

    private static String normalizePath(String path) {
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record EndpointKey(String method, String path) {
        private static EndpointKey of(HttpInteractionEvidence interaction) {
            return new EndpointKey(interaction.httpMethod().name(), normalizePath(interaction.endpointPath()));
        }
    }
}
