package ru.kuznetsov.qaip.core.importing.binding;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;
import ru.kuznetsov.qaip.core.importing.parsing.SchemaValidProjectDocument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultProjectBinder implements ProjectBinder {
    private final DocumentTreeReader treeReader;

    public DefaultProjectBinder() {
        this(DefaultProjectBinder::readInternalTree);
    }

    DefaultProjectBinder(DocumentTreeReader treeReader) {
        this.treeReader = Objects.requireNonNull(treeReader, "treeReader");
    }

    @Override
    public BindingResult bind(SchemaValidProjectDocument document) {
        Objects.requireNonNull(document, "document");
        try {
            JsonNode root = treeReader.read(document);
            JsonNode baseModel = root.get("baseModel");
            JsonNode projectNode = baseModel.get("project");

            Metadata metadata = new Metadata(text(projectNode, "id"), text(projectNode, "name"),
                    nullableText(projectNode, "description"), nullableText(projectNode, "version"),
                    object(projectNode.get("metadata")));
            Subject subject = new Subject(text(root.get("subject"), "localArtifactId"));
            List<Node> nodes = bindNodes(baseModel.get("nodes"));
            List<Relationship> relationships = bindRelationships(baseModel.get("relationships"));
            EvidenceManifest evidenceManifest = bindEvidence(root.get("evidenceManifest"));
            List<DeclaredChange> declaredChanges = bindChanges(root.get("declaredChanges"));

            Project project = new Project(text(root, "projectContractVersion"),
                    text(baseModel, "schemaVersion"), metadata, maps(baseModel.get("sources")), subject,
                    nodes, relationships, evidenceManifest, declaredChanges, stringMap(root.get("analysisContext")));
            return new BindingSuccess(new BoundProjectDocument(project));
        } catch (BindingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BindingException("Unexpected failure while binding schema-valid project", exception);
        }
    }

    private static List<Node> bindNodes(JsonNode array) {
        List<Node> result = new ArrayList<>();
        for (JsonNode value : array) {
            Map<String, Object> attributes = object(value);
            for (String common : List.of("id", "type", "name", "description", "status", "tags",
                    "sourceReferences", "metadata")) attributes.remove(common);
            result.add(new Node(text(value, "id"), text(value, "type"), text(value, "name"),
                    nullableText(value, "description"), nullableText(value, "status"),
                    strings(value.get("tags")), maps(value.get("sourceReferences")),
                    object(value.get("metadata")), attributes));
        }
        return List.copyOf(result);
    }

    private static List<Relationship> bindRelationships(JsonNode array) {
        List<Relationship> result = new ArrayList<>();
        for (JsonNode value : array) {
            result.add(new Relationship(text(value, "id"), text(value, "from"), text(value, "type"),
                    text(value, "to"), object(value.get("properties")), maps(value.get("sourceReferences"))));
        }
        return List.copyOf(result);
    }

    private static EvidenceManifest bindEvidence(JsonNode value) {
        return new EvidenceManifest(text(value, "contractVersion"), text(value, "sourceId"),
                object(value.get("snapshot")), text(value, "normalizationVersion"),
                text(value, "canonicalizationVersion"), text(value, "manifestFingerprint"),
                maps(value.get("identityAssertions")), maps(value.get("relationships")),
                maps(value.get("provenance")));
    }

    private static List<DeclaredChange> bindChanges(JsonNode array) {
        List<DeclaredChange> result = new ArrayList<>();
        for (JsonNode value : array) {
            result.add(new DeclaredChange(text(value, "artifactCategory"), text(value, "canonicalIdentity"),
                    text(value, "changeKind"), text(value, "schemaVersion"),
                    nullableObject(value.get("beforeState")), nullableObject(value.get("afterState"))));
        }
        return List.copyOf(result);
    }

    private static JsonNode readInternalTree(SchemaValidProjectDocument document) {
        try {
            Method method = SchemaValidProjectDocument.class.getDeclaredMethod("internalJsonTreeCopy");
            method.setAccessible(true);
            return (JsonNode) method.invoke(document);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new BindingException("Cannot access schema-valid document content", exception);
        }
    }

    private static String text(JsonNode parent, String field) {
        return parent.get(field).textValue();
    }

    private static String nullableText(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        return value == null || value.isNull() ? null : value.textValue();
    }

    private static List<String> strings(JsonNode array) {
        if (array == null) return List.of();
        List<String> result = new ArrayList<>();
        array.forEach(value -> result.add(value.textValue()));
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> maps(JsonNode array) {
        if (array == null) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        array.forEach(value -> result.add(object(value)));
        return List.copyOf(result);
    }

    private static Map<String, String> stringMap(JsonNode value) {
        Map<String, String> result = new LinkedHashMap<>();
        value.properties().forEach(entry -> result.put(entry.getKey(), entry.getValue().textValue()));
        return Map.copyOf(result);
    }

    private static Map<String, Object> nullableObject(JsonNode value) {
        return value == null || value.isNull() ? null : object(value);
    }

    private static Map<String, Object> object(JsonNode value) {
        if (value == null || value.isNull()) return new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        value.properties().forEach(entry -> result.put(entry.getKey(), javaValue(entry.getValue())));
        return result;
    }

    private static Object javaValue(JsonNode value) {
        if (value.isObject()) return object(value);
        if (value.isArray()) {
            List<Object> result = new ArrayList<>();
            value.forEach(item -> result.add(javaValue(item)));
            return result;
        }
        if (value.isTextual()) return value.textValue();
        if (value.isIntegralNumber()) return value.isInt() ? value.intValue() : value.longValue();
        if (value.isFloatingPointNumber()) return value.decimalValue();
        if (value.isBoolean()) return value.booleanValue();
        return null;
    }

    @FunctionalInterface
    interface DocumentTreeReader {
        JsonNode read(SchemaValidProjectDocument document);
    }
}
