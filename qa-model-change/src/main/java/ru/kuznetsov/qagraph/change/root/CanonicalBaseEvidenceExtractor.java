package ru.kuznetsov.qagraph.change.root;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.kuznetsov.qagraph.change.base.BaseArtifactIndex;
import ru.kuznetsov.qagraph.change.model.ArtifactState;
import ru.kuznetsov.qagraph.change.model.CanonicalQaModelVersion;
import ru.kuznetsov.qagraph.change.model.NodeArtifactState;
import ru.kuznetsov.qagraph.change.model.RelationshipArtifactState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.BASE_ROOT_NOT_AVAILABLE;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_COLLECTION_INVALID;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_COLLECTION_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_ARTIFACT_EXTRACTION_FAILED;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_INPUT_INVALID;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_SCHEMA_VERSION_INVALID;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_SCHEMA_VERSION_MISSING;
import static ru.kuznetsov.qagraph.change.root.BaseEvidenceExtractionDiagnosticCode.ROOT_VERSION_UNSUPPORTED;

/**
 * Extracts complete immutable Base evidence from an untrusted root JSON tree.
 */
public final class CanonicalBaseEvidenceExtractor {

    private static final Set<String> RECONSTRUCTED_FIELDS = Set.of(
            "schemaVersion",
            "nodes",
            "relationships"
    );

    public CanonicalBaseEvidenceExtractionResult extract(JsonNode root) {
        if (root == null) {
            return failure(
                    BASE_ROOT_NOT_AVAILABLE,
                    "$",
                    "Base root is not available"
            );
        }
        if (!root.isObject()) {
            return failure(
                    ROOT_INPUT_INVALID,
                    "$",
                    "Base root must be a JSON object"
            );
        }

        JsonNode versionNode = root.get("schemaVersion");
        if (versionNode == null) {
            return failure(
                    ROOT_SCHEMA_VERSION_MISSING,
                    "schemaVersion",
                    "Root schemaVersion is missing"
            );
        }
        if (!versionNode.isTextual() || versionNode.textValue().isBlank()) {
            return failure(
                    ROOT_SCHEMA_VERSION_INVALID,
                    "schemaVersion",
                    "Root schemaVersion must be non-blank text"
            );
        }
        CanonicalQaModelVersion version = new CanonicalQaModelVersion(
                versionNode.textValue()
        );
        if (!version.isSupported()) {
            return failure(
                    ROOT_VERSION_UNSUPPORTED,
                    "schemaVersion",
                    "Root schemaVersion is unsupported"
            );
        }

        for (String required : List.of("project", "sources")) {
            if (!root.has(required)) {
                return failure(
                        ROOT_REQUIRED_CONTEXT_PROPERTY_MISSING,
                        required,
                        "Required retained root property is missing"
                );
            }
        }

        JsonNode nodes = root.get("nodes");
        if (nodes == null) {
            return failure(
                    ROOT_ARTIFACT_COLLECTION_MISSING,
                    "nodes",
                    "Root nodes collection is missing"
            );
        }
        if (!nodes.isArray()) {
            return failure(
                    ROOT_ARTIFACT_COLLECTION_INVALID,
                    "nodes",
                    "Root nodes property must be an array"
            );
        }
        JsonNode relationships = root.get("relationships");
        if (relationships == null) {
            return failure(
                    ROOT_ARTIFACT_COLLECTION_MISSING,
                    "relationships",
                    "Root relationships collection is missing"
            );
        }
        if (!relationships.isArray()) {
            return failure(
                    ROOT_ARTIFACT_COLLECTION_INVALID,
                    "relationships",
                    "Root relationships property must be an array"
            );
        }

        List<ArtifactState> artifacts = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            try {
                artifacts.add(new NodeArtifactState(
                        version,
                        nodes.get(index)
                ));
            } catch (RuntimeException exception) {
                return failure(
                        ROOT_ARTIFACT_EXTRACTION_FAILED,
                        "nodes[" + index + "]",
                        "Node artifact could not be recognized"
                );
            }
        }
        for (int index = 0; index < relationships.size(); index++) {
            try {
                artifacts.add(new RelationshipArtifactState(
                        version,
                        relationships.get(index)
                ));
            } catch (RuntimeException exception) {
                return failure(
                        ROOT_ARTIFACT_EXTRACTION_FAILED,
                        "relationships[" + index + "]",
                        "Relationship artifact could not be recognized"
                );
            }
        }

        ObjectNode retained = JsonNodeFactory.instance.objectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!RECONSTRUCTED_FIELDS.contains(field.getKey())) {
                retained.set(field.getKey(), field.getValue().deepCopy());
            }
        }
        CanonicalRootContext context = new CanonicalRootContext(
                version,
                retained
        );
        BaseArtifactIndex index = new BaseArtifactIndex(version, artifacts);
        return new CanonicalBaseEvidenceExtracted(
                new CanonicalBaseModelEvidence(context, index)
        );
    }

    private CanonicalBaseEvidenceExtractionFailure failure(
            BaseEvidenceExtractionDiagnosticCode code,
            String path,
            String message
    ) {
        return new CanonicalBaseEvidenceExtractionFailure(List.of(
                new BaseEvidenceExtractionDiagnostic(code, path, message)
        ));
    }
}
