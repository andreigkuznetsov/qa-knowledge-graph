package ru.kuznetsov.qagraph.extractor.rest.mapping;

import ru.kuznetsov.qagraph.extractor.rest.RestOperationEvidence;
import ru.kuznetsov.qagraph.extractor.rest.SourceLocation;
import ru.kuznetsov.qagraph.model.NodeType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class RestOperationEvidenceMapper {
    public BusinessOperationProjection map(RestOperationEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence");

        String normalizedPath = normalizePath(evidence.endpointPath());
        String canonicalOperation = evidence.httpMethod().name() + " " + normalizedPath;
        String identity = sha256(canonicalOperation);
        String qualifiedController = qualifiedController(evidence);

        return new BusinessOperationProjection(
                "BO-REST-" + identity,
                NodeType.BUSINESS_OPERATION,
                canonicalOperation,
                "Spring MVC REST operation " + canonicalOperation + " handled by "
                        + qualifiedController + "." + evidence.controllerMethod() + ".",
                List.of(sourceReference(evidence, qualifiedController)),
                new BusinessOperationProjection.OperationProjection(
                        "REST-" + identity,
                        evidence.javaPackage().isBlank() ? evidence.controllerClass() : evidence.javaPackage(),
                        null));
    }

    private static BusinessOperationProjection.SourceReferenceProjection sourceReference(
            RestOperationEvidence evidence, String qualifiedController) {
        SourceLocation location = evidence.sourceLocation();
        String locationValue = location == null
                ? qualifiedController + "." + evidence.controllerMethod()
                : location.repositoryRelativePath() + ":" + location.line() + ":" + location.column()
                + "#" + qualifiedController + "." + evidence.controllerMethod();
        return new BusinessOperationProjection.SourceReferenceProjection(
                "SRC-JAVA-" + sha256(qualifiedController),
                new BusinessOperationProjection.SourceReferenceLocation(
                        BusinessOperationProjection.LocationType.OTHER, locationValue),
                "Controller method " + qualifiedController + "." + evidence.controllerMethod(),
                1.0,
                BusinessOperationProjection.EvidenceType.OBSERVED);
    }

    private static String qualifiedController(RestOperationEvidence evidence) {
        return evidence.javaPackage().isBlank()
                ? evidence.controllerClass()
                : evidence.javaPackage() + "." + evidence.controllerClass();
    }

    private static String normalizePath(String path) {
        String normalized = path.trim().replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
