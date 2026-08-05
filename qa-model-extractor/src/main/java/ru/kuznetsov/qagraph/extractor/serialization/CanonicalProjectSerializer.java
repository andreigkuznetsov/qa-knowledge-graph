package ru.kuznetsov.qagraph.extractor.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qagraph.extractor.assembly.EvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.assembly.ProjectEvidenceGraphProjection;
import ru.kuznetsov.qagraph.extractor.rest.mapping.BusinessOperationProjection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CanonicalProjectSerializer {
    public static final String PROJECT_CONTRACT_VERSION = "qaip-project-v1";
    public static final String BASE_MODEL_SCHEMA_VERSION = "0.1";

    private static final String NORMALIZATION_VERSION = "impact-evidence-normalization-v1";
    private static final ObjectMapper JSON = new ObjectMapper();

    public byte[] serialize(EvidenceGraphProjection graph, ProjectSerializationMetadata metadata) {
        Objects.requireNonNull(graph, "graph");
        return serializeProject(new ProjectEvidenceGraphProjection(
                List.of(graph.businessOperation()), graph.businessRules(), graph.technicalImplementations(),
                graph.testImplementations(), graph.checks(), graph.relationships()), metadata);
    }

    public byte[] serializeProject(ProjectEvidenceGraphProjection graph, ProjectSerializationMetadata metadata) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(metadata, "metadata");
        verifySubject(graph, metadata.subjectLocalArtifactId());
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(document(graph, metadata));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize canonical QAIP project", exception);
        }
    }

    private static ObjectNode document(ProjectEvidenceGraphProjection graph, ProjectSerializationMetadata metadata) {
        ObjectNode root = JSON.createObjectNode();
        root.put("projectContractVersion", PROJECT_CONTRACT_VERSION);
        root.set("baseModel", baseModel(graph, metadata));
        root.set("declaredChanges", declaredChanges(graph, metadata.subjectLocalArtifactId(),
                metadata.repositorySource().id()));
        root.set("evidenceManifest", evidenceManifest(metadata));
        root.set("subject", object("localArtifactId", metadata.subjectLocalArtifactId()));
        root.set("analysisContext", analysisContext());
        return root;
    }

    private static ObjectNode baseModel(ProjectEvidenceGraphProjection graph, ProjectSerializationMetadata metadata) {
        ObjectNode base = JSON.createObjectNode();
        base.put("schemaVersion", BASE_MODEL_SCHEMA_VERSION);
        ObjectNode project = base.putObject("project");
        project.put("id", metadata.projectId());
        project.put("name", metadata.projectName());
        putNullable(project, "description", metadata.projectDescription());
        putNullable(project, "version", metadata.projectVersion());
        project.set("metadata", stringMap(metadata.projectMetadata()));
        base.set("sources", sources(metadata.repositorySource()));
        base.set("nodes", nodes(graph, metadata.repositorySource().id()));
        base.set("relationships", relationships(graph));
        return base;
    }

    private static ArrayNode sources(ProjectSerializationMetadata.RepositorySource source) {
        ArrayNode sources = JSON.createArrayNode();
        ObjectNode value = sources.addObject();
        value.put("id", source.id());
        value.put("type", "OTHER");
        value.put("name", source.name());
        value.put("version", source.revision());
        value.put("externalRef", source.externalReference());
        value.put("uri", source.uri());
        value.put("checksum", source.checksum());
        value.set("metadata", stringMap(source.metadata()));
        return sources;
    }

    private static ArrayNode nodes(ProjectEvidenceGraphProjection graph, String sourceId) {
        List<ObjectNode> nodes = new ArrayList<>();
        graph.businessOperations().forEach(value -> nodes.add(operationNode(value, sourceId)));
        graph.businessRules().forEach(value -> nodes.add(ruleNode(value, sourceId)));
        graph.technicalImplementations().forEach(value -> nodes.add(technicalNode(value, sourceId)));
        graph.testImplementations().forEach(value -> nodes.add(testNode(value, sourceId)));
        graph.checks().forEach(value -> nodes.add(checkNode(value, sourceId)));
        nodes.sort(Comparator.comparing(value -> value.get("id").textValue()));
        ArrayNode result = JSON.createArrayNode();
        nodes.forEach(result::add);
        return result;
    }

    private static ObjectNode operationNode(BusinessOperationProjection value, String sourceId) {
        ObjectNode node = commonNode(value.id(), value.type().name(), value.name(), value.description());
        node.set("sourceReferences", operationReferences(value.sourceReferences(), sourceId));
        ObjectNode content = node.putObject("operation");
        content.put("code", value.operation().code());
        content.put("domain", value.operation().domain());
        putNullable(content, "businessOutcome", value.operation().businessOutcome());
        return node;
    }

    private static ObjectNode ruleNode(EvidenceGraphProjection.BusinessRuleProjection value, String sourceId) {
        ObjectNode node = commonNode(value.id(), value.type().name(), value.name(), value.description());
        node.set("sourceReferences", references(value.sourceReferences(), sourceId));
        ObjectNode content = node.putObject("rule");
        content.put("code", value.rule().code());
        content.put("ruleType", value.rule().ruleType().name());
        content.put("text", value.rule().text());
        putNullable(content, "expression", value.rule().expression());
        return node;
    }

    private static ObjectNode technicalNode(
            EvidenceGraphProjection.TechnicalImplementationProjection value, String sourceId) {
        ObjectNode node = commonNode(value.id(), value.type().name(), value.name(), value.description());
        node.set("sourceReferences", references(value.sourceReferences(), sourceId));
        ObjectNode content = node.putObject("technicalImplementation");
        content.put("implementationType", value.technicalImplementation().implementationType().name());
        if (value.technicalImplementation().implementationRole() != null) {
            content.put("implementationRole", value.technicalImplementation().implementationRole().name());
        }
        content.put("system", value.technicalImplementation().system());
        content.set("details", stringMap(value.technicalImplementation().details()));
        return node;
    }

    private static ObjectNode testNode(
            EvidenceGraphProjection.TestImplementationProjection value, String sourceId) {
        ObjectNode node = commonNode(value.id(), value.type().name(), value.name(), value.description());
        node.set("sourceReferences", references(value.sourceReferences(), sourceId));
        ObjectNode content = node.putObject("testImplementation");
        content.put("code", value.testImplementation().code());
        content.put("executionType", value.testImplementation().executionType().name());
        content.set("preconditions", JSON.valueToTree(value.testImplementation().preconditions()));
        ArrayNode steps = content.putArray("steps");
        value.testImplementation().steps().stream()
                .sorted(Comparator.comparingInt(EvidenceGraphProjection.TestStepProjection::order))
                .forEach(step -> {
                    ObjectNode item = steps.addObject();
                    item.put("order", step.order());
                    item.put("action", step.action());
                    putNullable(item, "expectedResult", step.expectedResult());
                });
        return node;
    }

    private static ObjectNode checkNode(EvidenceGraphProjection.CheckProjection value, String sourceId) {
        ObjectNode node = commonNode(value.id(), value.type().name(), value.name(), value.description());
        node.set("sourceReferences", references(value.sourceReferences(), sourceId));
        ObjectNode content = node.putObject("check");
        content.put("checkType", value.check().checkType().name());
        content.put("assertion", value.check().assertion());
        content.set("details", stringMap(value.check().details()));
        return node;
    }

    private static ObjectNode commonNode(String id, String type, String name, String description) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", id);
        node.put("type", type);
        node.put("name", name);
        node.put("description", description);
        node.set("sourceReferences", JSON.createArrayNode());
        node.set("metadata", JSON.createObjectNode());
        return node;
    }

    private static ArrayNode operationReferences(
            List<BusinessOperationProjection.SourceReferenceProjection> values, String sourceId) {
        ArrayNode result = JSON.createArrayNode();
        values.stream().sorted(Comparator.comparing(value -> value.location().value())).forEach(value -> {
            ObjectNode reference = result.addObject();
            reference.put("sourceId", sourceId);
            reference.set("location", location(value.location().type().name(), value.location().value()));
            reference.put("text", value.text());
            reference.put("confidence", value.confidence());
            reference.put("evidenceType", value.evidenceType().name());
        });
        return result;
    }

    private static ArrayNode references(
            List<EvidenceGraphProjection.SourceReferenceProjection> values, String sourceId) {
        ArrayNode result = JSON.createArrayNode();
        values.stream().sorted(Comparator.comparing(value -> value.location().value())).forEach(value -> {
            ObjectNode reference = result.addObject();
            reference.put("sourceId", sourceId);
            reference.set("location", location(value.location().type().name(), value.location().value()));
            reference.put("text", value.text());
            reference.put("confidence", value.confidence());
            reference.put("evidenceType", value.evidenceType().name());
        });
        return result;
    }

    private static ObjectNode location(String type, String value) {
        ObjectNode location = JSON.createObjectNode();
        location.put("type", type);
        location.put("value", value);
        return location;
    }

    private static ArrayNode relationships(ProjectEvidenceGraphProjection graph) {
        ArrayNode result = JSON.createArrayNode();
        graph.relationships().stream()
                .sorted(Comparator.comparing(EvidenceGraphProjection.RelationshipProjection::id))
                .forEach(value -> {
                    ObjectNode relationship = result.addObject();
                    relationship.put("id", value.id());
                    relationship.put("from", value.from());
                    relationship.put("type", value.type().name());
                    relationship.put("to", value.to());
                    relationship.set("properties", JSON.createObjectNode());
                    relationship.set("sourceReferences", JSON.createArrayNode());
                });
        return result;
    }

    private static ArrayNode declaredChanges(
            ProjectEvidenceGraphProjection graph, String subjectId, String sourceId) {
        ObjectNode subject = findNode(nodes(graph, sourceId), subjectId);
        if (subject == null) throw new IllegalArgumentException("subjectLocalArtifactId is not a graph node");
        ArrayNode changes = JSON.createArrayNode();
        ObjectNode change = changes.addObject();
        change.put("artifactCategory", "NODE");
        change.put("canonicalIdentity", subjectId);
        change.put("changeKind", "ADDED");
        change.put("schemaVersion", BASE_MODEL_SCHEMA_VERSION);
        change.set("afterState", subject.deepCopy());
        return changes;
    }

    private static ObjectNode findNode(ArrayNode nodes, String id) {
        for (JsonNode node : nodes) if (id.equals(node.get("id").textValue())) return (ObjectNode) node;
        return null;
    }

    private static ObjectNode evidenceManifest(ProjectSerializationMetadata metadata) {
        var source = metadata.repositorySource();
        var evidence = metadata.evidence();
        ObjectNode manifest = JSON.createObjectNode();
        manifest.put("contractVersion", "impact-evidence-manifest-v1");
        manifest.put("sourceId", source.id());
        ObjectNode snapshot = manifest.putObject("snapshot");
        snapshot.put("sourceId", source.id());
        snapshot.put("snapshotId", evidence.snapshotId());
        snapshot.put("contentFingerprint", evidence.contentFingerprint());
        manifest.put("normalizationVersion", NORMALIZATION_VERSION);
        manifest.put("canonicalizationVersion", "impact-evidence-canonical-v1");
        manifest.put("manifestFingerprint", evidence.manifestFingerprint());
        manifest.set("identityAssertions", JSON.createArrayNode());
        manifest.set("relationships", JSON.createArrayNode());
        ArrayNode provenance = manifest.putArray("provenance");
        ObjectNode item = provenance.addObject();
        item.put("provenanceId", evidence.provenanceId());
        item.put("originLocator", evidence.originLocator());
        item.put("originHash", evidence.originHash());
        item.put("normalizationActivity", evidence.normalizationActivity());
        item.put("normalizationVersion", NORMALIZATION_VERSION);
        return manifest;
    }

    private static ObjectNode analysisContext() {
        ObjectNode context = JSON.createObjectNode();
        context.put("qualificationVersion", "impact-evidence-qualification-v1");
        context.put("influenceVersion", "impact-evidence-business-rule-depends-on-v1");
        context.put("algorithmVersion", "impact-evidence-analysis-v1");
        return context;
    }

    private static ObjectNode stringMap(Map<String, String> values) {
        ObjectNode result = JSON.createObjectNode();
        values.forEach(result::put);
        return result;
    }

    private static ObjectNode object(String field, String value) {
        ObjectNode result = JSON.createObjectNode();
        result.put(field, value);
        return result;
    }

    private static void putNullable(ObjectNode target, String field, String value) {
        if (value == null) target.putNull(field); else target.put(field, value);
    }

    private static void verifySubject(ProjectEvidenceGraphProjection graph, String subjectId) {
        boolean present = graph.businessOperations().stream().anyMatch(node -> node.id().equals(subjectId))
                || graph.businessRules().stream().anyMatch(node -> node.id().equals(subjectId))
                || graph.technicalImplementations().stream().anyMatch(node -> node.id().equals(subjectId))
                || graph.testImplementations().stream().anyMatch(node -> node.id().equals(subjectId))
                || graph.checks().stream().anyMatch(node -> node.id().equals(subjectId));
        if (!present) throw new IllegalArgumentException("subjectLocalArtifactId is not a graph node");
    }
}
