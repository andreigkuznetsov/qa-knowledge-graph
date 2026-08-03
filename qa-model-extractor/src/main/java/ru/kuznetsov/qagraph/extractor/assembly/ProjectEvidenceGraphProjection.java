package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;

import java.util.List;
import java.util.Objects;

public record ProjectEvidenceGraphProjection(
        List<BusinessOperationProjection> businessOperations,
        List<EvidenceGraphProjection.BusinessRuleProjection> businessRules,
        List<EvidenceGraphProjection.TechnicalImplementationProjection> technicalImplementations,
        List<EvidenceGraphProjection.TestImplementationProjection> testImplementations,
        List<EvidenceGraphProjection.CheckProjection> checks,
        List<EvidenceGraphProjection.RelationshipProjection> relationships
) {
    public ProjectEvidenceGraphProjection {
        businessOperations = List.copyOf(Objects.requireNonNull(businessOperations, "businessOperations"));
        businessRules = List.copyOf(Objects.requireNonNull(businessRules, "businessRules"));
        technicalImplementations = List.copyOf(Objects.requireNonNull(
                technicalImplementations, "technicalImplementations"));
        testImplementations = List.copyOf(Objects.requireNonNull(testImplementations, "testImplementations"));
        checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships"));
    }
}
