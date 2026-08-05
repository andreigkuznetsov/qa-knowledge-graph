package ru.kuznetsov.qagraph.extractor.assembly;

import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;
import ru.kuznetsov.qagraph.model.NodeType;
import ru.kuznetsov.qagraph.model.ImplementationRole;
import ru.kuznetsov.qagraph.model.RelationshipType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record EvidenceGraphProjection(
        BusinessOperationProjection businessOperation,
        List<BusinessRuleProjection> businessRules,
        List<TechnicalImplementationProjection> technicalImplementations,
        List<TestImplementationProjection> testImplementations,
        List<CheckProjection> checks,
        List<RelationshipProjection> relationships
) {
    public EvidenceGraphProjection {
        Objects.requireNonNull(businessOperation, "businessOperation");
        businessRules = List.copyOf(Objects.requireNonNull(businessRules, "businessRules"));
        technicalImplementations = List.copyOf(Objects.requireNonNull(
                technicalImplementations, "technicalImplementations"));
        testImplementations = List.copyOf(Objects.requireNonNull(testImplementations, "testImplementations"));
        checks = List.copyOf(Objects.requireNonNull(checks, "checks"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships"));
    }

    public record BusinessRuleProjection(
            String id,
            NodeType type,
            String name,
            String description,
            List<SourceReferenceProjection> sourceReferences,
            RuleProjection rule
    ) {
        public BusinessRuleProjection {
            requireType(type, NodeType.BUSINESS_RULE);
            requireNodeFields(id, name, description, sourceReferences);
            sourceReferences = List.copyOf(sourceReferences);
            Objects.requireNonNull(rule, "rule");
        }
    }

    public record RuleProjection(String code, RuleType ruleType, String text, String expression) {
        public RuleProjection {
            requireNonBlank(code, "code");
            Objects.requireNonNull(ruleType, "ruleType");
            requireNonBlank(text, "text");
        }
    }

    public enum RuleType {
        VALIDATION_RULE
    }

    public record TechnicalImplementationProjection(
            String id,
            NodeType type,
            String name,
            String description,
            List<SourceReferenceProjection> sourceReferences,
            TechnicalProjection technicalImplementation
    ) {
        public TechnicalImplementationProjection {
            requireType(type, NodeType.TECHNICAL_IMPLEMENTATION);
            requireNodeFields(id, name, description, sourceReferences);
            sourceReferences = List.copyOf(sourceReferences);
            Objects.requireNonNull(technicalImplementation, "technicalImplementation");
        }
    }

    public record TechnicalProjection(
            ImplementationType implementationType,
            ImplementationRole implementationRole,
            String system,
            Map<String, String> details
    ) {
        public TechnicalProjection {
            Objects.requireNonNull(implementationType, "implementationType");
            requireNonBlank(system, "system");
            details = immutableSortedMap(details);
        }

        public TechnicalProjection(ImplementationType implementationType, String system, Map<String, String> details) {
            this(implementationType, null, system, details);
        }
    }

    public enum ImplementationType {
        API,
        DATABASE,
        MESSAGE,
        OTHER
    }

    public record TestImplementationProjection(
            String id,
            NodeType type,
            String name,
            String description,
            List<SourceReferenceProjection> sourceReferences,
            TestProjection testImplementation
    ) {
        public TestImplementationProjection {
            requireType(type, NodeType.TEST_IMPLEMENTATION);
            requireNodeFields(id, name, description, sourceReferences);
            sourceReferences = List.copyOf(sourceReferences);
            Objects.requireNonNull(testImplementation, "testImplementation");
        }
    }

    public record TestProjection(
            String code,
            ExecutionType executionType,
            List<String> preconditions,
            List<TestStepProjection> steps
    ) {
        public TestProjection {
            requireNonBlank(code, "code");
            Objects.requireNonNull(executionType, "executionType");
            preconditions = List.copyOf(Objects.requireNonNull(preconditions, "preconditions"));
            steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        }
    }

    public record TestStepProjection(int order, String action, String expectedResult) {
        public TestStepProjection {
            if (order < 1) throw new IllegalArgumentException("order must be positive");
            requireNonBlank(action, "action");
        }
    }

    public enum ExecutionType {
        AUTOMATED
    }

    public record CheckProjection(
            String id,
            NodeType type,
            String name,
            String description,
            List<SourceReferenceProjection> sourceReferences,
            CheckContentProjection check
    ) {
        public CheckProjection {
            requireType(type, NodeType.CHECK);
            requireNodeFields(id, name, description, sourceReferences);
            sourceReferences = List.copyOf(sourceReferences);
            Objects.requireNonNull(check, "check");
        }
    }

    public record CheckContentProjection(
            CheckType checkType,
            String assertion,
            Map<String, String> details
    ) {
        public CheckContentProjection {
            Objects.requireNonNull(checkType, "checkType");
            requireNonBlank(assertion, "assertion");
            details = immutableSortedMap(details);
        }
    }

    public enum CheckType {
        API,
        SQL
    }

    public record SourceReferenceProjection(
            String sourceId,
            SourceLocationProjection location,
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

    public record SourceLocationProjection(LocationType type, String value) {
        public SourceLocationProjection {
            Objects.requireNonNull(type, "type");
            requireNonBlank(value, "value");
        }
    }

    public enum LocationType {
        OTHER,
        TEST_CASE
    }

    public enum EvidenceType {
        OBSERVED
    }

    public record RelationshipProjection(
            String id,
            String from,
            RelationshipType type,
            String to
    ) {
        public RelationshipProjection {
            requireNonBlank(id, "id");
            requireNonBlank(from, "from");
            Objects.requireNonNull(type, "type");
            requireNonBlank(to, "to");
            if (from.equals(to)) throw new IllegalArgumentException("self-reference is not supported");
        }
    }

    private static void requireNodeFields(
            String id, String name, String description, List<SourceReferenceProjection> sourceReferences) {
        requireNonBlank(id, "id");
        requireNonBlank(name, "name");
        requireNonBlank(description, "description");
        Objects.requireNonNull(sourceReferences, "sourceReferences");
    }

    private static void requireType(NodeType actual, NodeType expected) {
        if (actual != expected) throw new IllegalArgumentException("type must be " + expected);
    }

    private static Map<String, String> immutableSortedMap(Map<String, String> input) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(
                Objects.requireNonNull(input, "input"))));
    }

    private static void requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
