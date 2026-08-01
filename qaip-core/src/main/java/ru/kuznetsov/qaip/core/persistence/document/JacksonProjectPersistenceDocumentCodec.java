package ru.kuznetsov.qaip.core.persistence.document;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ru.kuznetsov.qaip.core.domain.DeclaredChange;
import ru.kuznetsov.qaip.core.domain.EvidenceManifest;
import ru.kuznetsov.qaip.core.domain.Metadata;
import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.domain.Subject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class JacksonProjectPersistenceDocumentCodec implements ProjectPersistenceDocumentCodec {
    static final String VERSION = "qaip-project-persistence-v1";
    private static final Set<String> VALUE_TYPES = Set.of(
            "NULL", "STRING", "BOOLEAN", "INTEGER", "LONG", "BIG_INTEGER", "BIG_DECIMAL", "LIST", "OBJECT");

    private final ObjectMapper mapper;

    JacksonProjectPersistenceDocumentCodec() {
        JsonFactory factory = JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        mapper = JsonMapper.builder(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
    }

    @Override
    public String encode(Project project) {
        Objects.requireNonNull(project, "project");
        try {
            return mapper.writeValueAsString(new ProjectPersistenceDocument(VERSION, encodeProject(project)));
        } catch (RuntimeException | IOException exception) {
            throw new ProjectPersistenceDocumentException("Cannot encode Project persistence document", exception);
        }
    }

    @Override
    public Project decode(String payload) {
        Objects.requireNonNull(payload, "payload");
        try {
            JsonNode root = mapper.readTree(payload);
            requireObject(root, "$", "persistenceDocumentVersion", "project");
            String version = text(root, "persistenceDocumentVersion", "$", false);
            if (!VERSION.equals(version)) {
                throw failure("Unsupported persistence document version: " + version);
            }
            return decodeProject(root.get("project"), "$.project");
        } catch (ProjectPersistenceDocumentException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            throw new ProjectPersistenceDocumentException("Cannot decode Project persistence document", exception);
        }
    }

    private static ProjectDocument encodeProject(Project project) {
        Metadata metadata = require(project.metadata(), "project.metadata");
        return new ProjectDocument(project.projectContractVersion(), project.schemaVersion(),
                new MetadataDocument(metadata.id(), metadata.name(), metadata.description(), metadata.version(),
                        encodeValue(metadata.attributes())),
                encodeMaps(project.sources()), new SubjectDocument(require(project.subject(), "project.subject").localArtifactId()),
                project.nodes().stream().map(JacksonProjectPersistenceDocumentCodec::encodeNode).toList(),
                project.relationships().stream().map(JacksonProjectPersistenceDocumentCodec::encodeRelationship).toList(),
                encodeEvidence(require(project.evidenceManifest(), "project.evidenceManifest")),
                project.declaredChanges().stream().map(JacksonProjectPersistenceDocumentCodec::encodeChange).toList(),
                project.analysisContext());
    }

    private static NodeDocument encodeNode(Node node) {
        require(node, "project.nodes item");
        return new NodeDocument(node.id(), node.type(), node.name(), node.description(), node.status(), node.tags(),
                encodeMaps(node.sourceReferences()), encodeValue(node.metadata()), encodeValue(node.attributes()));
    }

    private static RelationshipDocument encodeRelationship(Relationship relationship) {
        require(relationship, "project.relationships item");
        return new RelationshipDocument(relationship.id(), relationship.from(), relationship.type(), relationship.to(),
                encodeValue(relationship.properties()), encodeMaps(relationship.sourceReferences()));
    }

    private static EvidenceManifestDocument encodeEvidence(EvidenceManifest evidence) {
        return new EvidenceManifestDocument(evidence.contractVersion(), evidence.sourceId(), encodeValue(evidence.snapshot()),
                evidence.normalizationVersion(), evidence.canonicalizationVersion(), evidence.manifestFingerprint(),
                encodeMaps(evidence.identityAssertions()), encodeMaps(evidence.relationships()), encodeMaps(evidence.provenance()));
    }

    private static DeclaredChangeDocument encodeChange(DeclaredChange change) {
        require(change, "project.declaredChanges item");
        return new DeclaredChangeDocument(change.artifactCategory(), change.canonicalIdentity(), change.changeKind(),
                change.schemaVersion(), change.beforeState() == null ? null : encodeValue(change.beforeState()),
                change.afterState() == null ? null : encodeValue(change.afterState()));
    }

    private static List<PersistenceValueDocument> encodeMaps(List<? extends Map<String, Object>> maps) {
        return maps.stream().map(JacksonProjectPersistenceDocumentCodec::encodeValue).toList();
    }

    private static PersistenceValueDocument encodeValue(Object value) {
        if (value == null) return PersistenceValueDocument.ofType("NULL");
        if (value instanceof String string) return PersistenceValueDocument.scalar("STRING", string);
        if (value instanceof Boolean bool) return PersistenceValueDocument.bool(bool);
        if (value instanceof Integer integer) return PersistenceValueDocument.scalar("INTEGER", integer.toString());
        if (value instanceof Long longValue) return PersistenceValueDocument.scalar("LONG", longValue.toString());
        if (value instanceof BigInteger integer) return PersistenceValueDocument.scalar("BIG_INTEGER", integer.toString());
        if (value instanceof BigDecimal decimal) {
            return PersistenceValueDocument.decimal(decimal.unscaledValue().toString(), decimal.scale());
        }
        if (value instanceof List<?> list) {
            return PersistenceValueDocument.list(list.stream()
                    .map(JacksonProjectPersistenceDocumentCodec::encodeValue).toList());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, PersistenceValueDocument> entries = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (!(key instanceof String stringKey)) throw failure("Dynamic object key is not a String");
                entries.put(stringKey, encodeValue(item));
            });
            return PersistenceValueDocument.object(entries);
        }
        throw failure("Unsupported dynamic value type: " + value.getClass().getName());
    }

    private static Project decodeProject(JsonNode value, String path) {
        requireObject(value, path, "projectContractVersion", "schemaVersion", "metadata", "sources", "subject",
                "nodes", "relationships", "evidenceManifest", "declaredChanges", "analysisContext");
        Metadata metadata = decodeMetadata(value.get("metadata"), path + ".metadata");
        return new Project(text(value, "projectContractVersion", path, true), text(value, "schemaVersion", path, true),
                metadata, decodeObjectList(value.get("sources"), path + ".sources"),
                decodeSubject(value.get("subject"), path + ".subject"),
                decodeNodes(value.get("nodes"), path + ".nodes"),
                decodeRelationships(value.get("relationships"), path + ".relationships"),
                decodeEvidence(value.get("evidenceManifest"), path + ".evidenceManifest"),
                decodeChanges(value.get("declaredChanges"), path + ".declaredChanges"),
                decodeStringMap(value.get("analysisContext"), path + ".analysisContext"));
    }

    private static Metadata decodeMetadata(JsonNode value, String path) {
        requireObject(value, path, "id", "name", "description", "version", "attributes");
        return new Metadata(text(value, "id", path, true), text(value, "name", path, true),
                nullableText(value, "description", path), nullableText(value, "version", path),
                decodeObject(value.get("attributes"), path + ".attributes"));
    }

    private static Subject decodeSubject(JsonNode value, String path) {
        requireObject(value, path, "localArtifactId");
        return new Subject(text(value, "localArtifactId", path, true));
    }

    private static List<Node> decodeNodes(JsonNode value, String path) {
        requireArray(value, path);
        List<Node> nodes = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            JsonNode item = value.get(index);
            String itemPath = path + '[' + index + ']';
            requireObject(item, itemPath, "id", "type", "name", "description", "status", "tags",
                    "sourceReferences", "metadata", "attributes");
            nodes.add(new Node(text(item, "id", itemPath, true), text(item, "type", itemPath, true),
                    text(item, "name", itemPath, true), nullableText(item, "description", itemPath),
                    nullableText(item, "status", itemPath), decodeStrings(item.get("tags"), itemPath + ".tags"),
                    decodeObjectList(item.get("sourceReferences"), itemPath + ".sourceReferences"),
                    decodeObject(item.get("metadata"), itemPath + ".metadata"),
                    decodeObject(item.get("attributes"), itemPath + ".attributes")));
        }
        return nodes;
    }

    private static List<Relationship> decodeRelationships(JsonNode value, String path) {
        requireArray(value, path);
        List<Relationship> relationships = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            JsonNode item = value.get(index);
            String itemPath = path + '[' + index + ']';
            requireObject(item, itemPath, "id", "from", "type", "to", "properties", "sourceReferences");
            relationships.add(new Relationship(text(item, "id", itemPath, true), text(item, "from", itemPath, true),
                    text(item, "type", itemPath, true), text(item, "to", itemPath, true),
                    decodeObject(item.get("properties"), itemPath + ".properties"),
                    decodeObjectList(item.get("sourceReferences"), itemPath + ".sourceReferences")));
        }
        return relationships;
    }

    private static EvidenceManifest decodeEvidence(JsonNode value, String path) {
        requireObject(value, path, "contractVersion", "sourceId", "snapshot", "normalizationVersion",
                "canonicalizationVersion", "manifestFingerprint", "identityAssertions", "relationships", "provenance");
        return new EvidenceManifest(text(value, "contractVersion", path, true), text(value, "sourceId", path, true),
                decodeObject(value.get("snapshot"), path + ".snapshot"),
                text(value, "normalizationVersion", path, true), text(value, "canonicalizationVersion", path, true),
                text(value, "manifestFingerprint", path, true),
                decodeObjectList(value.get("identityAssertions"), path + ".identityAssertions"),
                decodeObjectList(value.get("relationships"), path + ".relationships"),
                decodeObjectList(value.get("provenance"), path + ".provenance"));
    }

    private static List<DeclaredChange> decodeChanges(JsonNode value, String path) {
        requireArray(value, path);
        List<DeclaredChange> changes = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            JsonNode item = value.get(index);
            String itemPath = path + '[' + index + ']';
            requireObject(item, itemPath, "artifactCategory", "canonicalIdentity", "changeKind", "schemaVersion",
                    "beforeState", "afterState");
            changes.add(new DeclaredChange(text(item, "artifactCategory", itemPath, true),
                    text(item, "canonicalIdentity", itemPath, true), text(item, "changeKind", itemPath, true),
                    text(item, "schemaVersion", itemPath, true),
                    nullableObject(item.get("beforeState"), itemPath + ".beforeState"),
                    nullableObject(item.get("afterState"), itemPath + ".afterState")));
        }
        return changes;
    }

    private static Map<String, Object> nullableObject(JsonNode value, String path) {
        return value.isNull() ? null : decodeObject(value, path);
    }

    private static List<Map<String, Object>> decodeObjectList(JsonNode value, String path) {
        requireArray(value, path);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            result.add(decodeObject(value.get(index), path + '[' + index + ']'));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodeObject(JsonNode value, String path) {
        Object decoded = decodeValue(value, path);
        if (!(decoded instanceof Map<?, ?> map)) throw failure(path + " must be a typed OBJECT");
        return (Map<String, Object>) map;
    }

    private static Object decodeValue(JsonNode node, String path) {
        if (node == null || !node.isObject()) throw failure(path + " must be a typed value object");
        JsonNode typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual() || typeNode.textValue().isBlank()) {
            throw failure(path + ".type must be a non-blank string");
        }
        String type = typeNode.textValue();
        if (!VALUE_TYPES.contains(type)) throw failure(path + " has unknown dynamic type: " + type);
        return switch (type) {
            case "NULL" -> { requireObject(node, path, "type"); yield null; }
            case "STRING" -> { requireObject(node, path, "type", "value"); yield text(node, "value", path, true); }
            case "BOOLEAN" -> decodeBoolean(node, path);
            case "INTEGER" -> parseInteger(node, path);
            case "LONG" -> parseLong(node, path);
            case "BIG_INTEGER" -> parseBigInteger(node, path, "value");
            case "BIG_DECIMAL" -> parseBigDecimal(node, path);
            case "LIST" -> decodeList(node, path);
            case "OBJECT" -> decodeObjectEntries(node, path);
            default -> throw failure(path + " has unknown dynamic type: " + type);
        };
    }

    private static Boolean decodeBoolean(JsonNode node, String path) {
        requireObject(node, path, "type", "value");
        JsonNode value = node.get("value");
        if (!value.isBoolean()) throw failure(path + ".value must be a boolean");
        return value.booleanValue();
    }

    private static Integer parseInteger(JsonNode node, String path) {
        String value = numericText(node, path, "INTEGER");
        try { return Integer.valueOf(value); }
        catch (NumberFormatException exception) { throw new ProjectPersistenceDocumentException(path + " invalid INTEGER", exception); }
    }

    private static Long parseLong(JsonNode node, String path) {
        String value = numericText(node, path, "LONG");
        try { return Long.valueOf(value); }
        catch (NumberFormatException exception) { throw new ProjectPersistenceDocumentException(path + " invalid LONG", exception); }
    }

    private static BigInteger parseBigInteger(JsonNode node, String path, String field) {
        String value = text(node, field, path, true);
        requireCanonicalInteger(value, path + '.' + field);
        try { return new BigInteger(value); }
        catch (NumberFormatException exception) { throw new ProjectPersistenceDocumentException(path + " invalid BIG_INTEGER", exception); }
    }

    private static BigDecimal parseBigDecimal(JsonNode node, String path) {
        requireObject(node, path, "type", "unscaledValue", "scale");
        BigInteger unscaled = parseBigInteger(node, path, "unscaledValue");
        JsonNode scale = node.get("scale");
        if (!scale.isIntegralNumber() || !scale.canConvertToInt()) throw failure(path + ".scale must be an integer");
        return new BigDecimal(unscaled, scale.intValue());
    }

    private static String numericText(JsonNode node, String path, String type) {
        requireObject(node, path, "type", "value");
        String value = text(node, "value", path, true);
        requireCanonicalInteger(value, path + ".value");
        return value;
    }

    private static void requireCanonicalInteger(String value, String path) {
        if (!value.matches("0|-?[1-9][0-9]*")) throw failure(path + " must be a canonical base-10 integer string");
    }

    private static List<Object> decodeList(JsonNode node, String path) {
        requireObject(node, path, "type", "items");
        JsonNode items = node.get("items");
        requireArray(items, path + ".items");
        List<Object> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) result.add(decodeValue(items.get(index), path + ".items[" + index + ']'));
        return result;
    }

    private static Map<String, Object> decodeObjectEntries(JsonNode node, String path) {
        requireObject(node, path, "type", "entries");
        JsonNode entries = node.get("entries");
        if (!entries.isObject()) throw failure(path + ".entries must be an object");
        Map<String, Object> result = new LinkedHashMap<>();
        entries.properties().forEach(entry -> result.put(entry.getKey(), decodeValue(entry.getValue(), path + ".entries." + entry.getKey())));
        return result;
    }

    private static Map<String, String> decodeStringMap(JsonNode value, String path) {
        if (value == null || !value.isObject()) throw failure(path + " must be an object");
        Map<String, String> result = new LinkedHashMap<>();
        value.properties().forEach(entry -> {
            if (!entry.getValue().isTextual()) throw failure(path + '.' + entry.getKey() + " must be a string");
            result.put(entry.getKey(), entry.getValue().textValue());
        });
        return result;
    }

    private static List<String> decodeStrings(JsonNode value, String path) {
        requireArray(value, path);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < value.size(); index++) {
            if (!value.get(index).isTextual()) throw failure(path + '[' + index + "] must be a string");
            result.add(value.get(index).textValue());
        }
        return result;
    }

    private static String nullableText(JsonNode object, String field, String path) {
        JsonNode value = object.get(field);
        if (value.isNull()) return null;
        if (!value.isTextual()) throw failure(path + '.' + field + " must be a string or null");
        return value.textValue();
    }

    private static String text(JsonNode object, String field, String path, boolean allowBlank) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual()) throw failure(path + '.' + field + " must be a string");
        if (!allowBlank && value.textValue().isBlank()) throw failure(path + '.' + field + " must not be blank");
        return value.textValue();
    }

    private static void requireArray(JsonNode value, String path) {
        if (value == null || !value.isArray()) throw failure(path + " must be an array");
    }

    private static void requireObject(JsonNode value, String path, String... requiredFields) {
        if (value == null || !value.isObject()) throw failure(path + " must be an object");
        Set<String> expected = new LinkedHashSet<>(List.of(requiredFields));
        Set<String> actual = new LinkedHashSet<>();
        value.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new LinkedHashSet<>(actual);
            unknown.removeAll(expected);
            throw failure(path + " fields mismatch; missing=" + missing + ", unknown=" + unknown);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw failure(name + " must not be null");
        return value;
    }

    private static ProjectPersistenceDocumentException failure(String message) {
        return new ProjectPersistenceDocumentException(message);
    }
}
