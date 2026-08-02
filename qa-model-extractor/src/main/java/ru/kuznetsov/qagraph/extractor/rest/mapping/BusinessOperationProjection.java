package ru.kuznetsov.qagraph.extractor.rest.mapping;

import ru.kuznetsov.qagraph.model.NodeType;

import java.util.List;
import java.util.Objects;

public record BusinessOperationProjection(
        String id,
        NodeType type,
        String name,
        String description,
        List<SourceReferenceProjection> sourceReferences,
        OperationProjection operation
) {
    public BusinessOperationProjection {
        requireNonBlank(id, "id");
        if (type != NodeType.BUSINESS_OPERATION) {
            throw new IllegalArgumentException("type must be BUSINESS_OPERATION");
        }
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        sourceReferences = List.copyOf(Objects.requireNonNull(sourceReferences, "sourceReferences"));
        Objects.requireNonNull(operation, "operation");
    }

    public record OperationProjection(
            String code,
            String domain,
            String businessOutcome
    ) {
        public OperationProjection {
            requireNonBlank(code, "code");
            requireNonBlank(domain, "domain");
        }
    }

    public record SourceReferenceProjection(
            String sourceId,
            SourceReferenceLocation location,
            String text,
            double confidence,
            EvidenceType evidenceType
    ) {
        public SourceReferenceProjection {
            requireNonBlank(sourceId, "sourceId");
            Objects.requireNonNull(location, "location");
            requireNonBlank(text, "text");
            if (confidence < 0 || confidence > 1) {
                throw new IllegalArgumentException("confidence must be between 0 and 1");
            }
            Objects.requireNonNull(evidenceType, "evidenceType");
        }
    }

    public record SourceReferenceLocation(LocationType type, String value) {
        public SourceReferenceLocation {
            Objects.requireNonNull(type, "type");
            requireNonBlank(value, "value");
        }
    }

    public enum LocationType {
        OTHER
    }

    public enum EvidenceType {
        OBSERVED
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
