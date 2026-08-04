package ru.kuznetsov.qaip.core.validation;

import ru.kuznetsov.qaip.core.domain.Node;
import ru.kuznetsov.qaip.core.domain.Project;
import ru.kuznetsov.qaip.core.domain.Relationship;
import ru.kuznetsov.qaip.core.importing.binding.BoundProjectDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DefaultProjectApplicationValidator implements ProjectApplicationValidator {
    private static final String DUPLICATE_NODE_ID = "DUPLICATE_NODE_ID";
    private static final String UNKNOWN_FROM_NODE = "UNKNOWN_FROM_NODE";
    private static final String UNKNOWN_TO_NODE = "UNKNOWN_TO_NODE";
    private static final String RELATIONSHIP_NOT_ALLOWED = "RELATIONSHIP_NOT_ALLOWED";
    private static final String SCENARIO_WITHOUT_TEST = "SCENARIO_WITHOUT_TEST";

    private static final Set<String> ALLOWED_RELATIONSHIPS = Set.of(
            triple("USER_STORY", "DESCRIBES", "BUSINESS_OPERATION"),
            triple("BUSINESS_OPERATION", "GOVERNED_BY", "BUSINESS_RULE"),
            triple("BUSINESS_OPERATION", "SPECIFIED_BY", "SCENARIO"),
            triple("BUSINESS_OPERATION", "IMPLEMENTED_BY", "TECHNICAL_IMPLEMENTATION"),
            triple("TEST_IMPLEMENTATION", "VALIDATES", "SCENARIO"),
            triple("TEST_IMPLEMENTATION", "USES", "TECHNICAL_IMPLEMENTATION"),
            triple("TECHNICAL_IMPLEMENTATION", "USES", "TECHNICAL_IMPLEMENTATION"),
            triple("TEST_IMPLEMENTATION", "HAS_CHECK", "CHECK"),
            triple("SCENARIO", "COVERS", "BUSINESS_RULE"),
            triple("SCENARIO", "REFINES", "SCENARIO"),
            triple("BUSINESS_RULE", "DEPENDS_ON", "BUSINESS_RULE"),
            triple("BUSINESS_RULE", "SUPERSEDES", "BUSINESS_RULE"),
            triple("USER_STORY", "RELATED_TO", "USER_STORY"),
            triple("BUSINESS_OPERATION", "RELATED_TO", "BUSINESS_OPERATION"));

    @Override
    public ApplicationValidationResult validate(BoundProjectDocument document) {
        Objects.requireNonNull(document, "document");
        try {
            Project project = document.project();
            Map<String, List<Node>> nodesById = indexNodes(project.nodes());
            List<ApplicationValidationFinding> findings = new ArrayList<>();
            findDuplicateNodeIds(project.nodes(), findings);
            findUnknownSources(project.relationships(), nodesById, findings);
            findUnknownTargets(project.relationships(), nodesById, findings);
            findDisallowedRelationships(project.relationships(), nodesById, findings);
            findUncoveredScenarios(project.nodes(), project.relationships(), findings);
            List<ApplicationValidationFinding> immutableFindings = List.copyOf(findings);
            if (immutableFindings.stream().anyMatch(DefaultProjectApplicationValidator::isError)) {
                return new ApplicationValidationFailure(immutableFindings);
            }
            return new ApplicationValidationSuccess(new ApplicationValidProjectDocument(project), immutableFindings);
        } catch (ApplicationValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApplicationValidationException("Application validation failed internally", exception);
        }
    }

    private static Map<String, List<Node>> indexNodes(List<Node> nodes) {
        Map<String, List<Node>> result = new LinkedHashMap<>();
        for (Node node : nodes) result.computeIfAbsent(node.id(), ignored -> new ArrayList<>()).add(node);
        result.replaceAll((ignored, occurrences) -> List.copyOf(occurrences));
        return result;
    }

    private static void findDuplicateNodeIds(List<Node> nodes, List<ApplicationValidationFinding> findings) {
        Set<String> seen = new LinkedHashSet<>();
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (!seen.add(node.id())) {
                findings.add(error(DUPLICATE_NODE_ID,
                        "Node id '" + node.id() + "' is declared more than once.",
                        "project.nodes[" + index + "].id"));
            }
        }
    }

    private static void findUnknownSources(List<Relationship> relationships, Map<String, List<Node>> nodesById,
                                           List<ApplicationValidationFinding> findings) {
        for (int index = 0; index < relationships.size(); index++) {
            Relationship relationship = relationships.get(index);
            if (!nodesById.containsKey(relationship.from())) {
                findings.add(error(UNKNOWN_FROM_NODE,
                        "Relationship '" + relationship.id() + "' references unknown source node '"
                                + relationship.from() + "'.",
                        "project.relationships[" + index + "].from"));
            }
        }
    }

    private static void findUnknownTargets(List<Relationship> relationships, Map<String, List<Node>> nodesById,
                                           List<ApplicationValidationFinding> findings) {
        for (int index = 0; index < relationships.size(); index++) {
            Relationship relationship = relationships.get(index);
            if (!nodesById.containsKey(relationship.to())) {
                findings.add(error(UNKNOWN_TO_NODE,
                        "Relationship '" + relationship.id() + "' references unknown target node '"
                                + relationship.to() + "'.",
                        "project.relationships[" + index + "].to"));
            }
        }
    }

    private static void findDisallowedRelationships(List<Relationship> relationships,
                                                     Map<String, List<Node>> nodesById,
                                                     List<ApplicationValidationFinding> findings) {
        for (int index = 0; index < relationships.size(); index++) {
            Relationship relationship = relationships.get(index);
            List<Node> from = nodesById.get(relationship.from());
            List<Node> to = nodesById.get(relationship.to());
            if (from == null || to == null || from.size() != 1 || to.size() != 1) continue;
            String actual = triple(from.getFirst().type(), relationship.type(), to.getFirst().type());
            if (!ALLOWED_RELATIONSHIPS.contains(actual)) {
                findings.add(error(RELATIONSHIP_NOT_ALLOWED,
                        "Relationship type '" + relationship.type() + "' is not allowed from '"
                                + from.getFirst().type() + "' to '" + to.getFirst().type() + "'.",
                        "project.relationships[" + index + "].type"));
            }
        }
    }

    private static void findUncoveredScenarios(List<Node> nodes, List<Relationship> relationships,
                                                List<ApplicationValidationFinding> findings) {
        Set<String> validatedScenarios = new LinkedHashSet<>();
        for (Relationship relationship : relationships) {
            if ("VALIDATES".equals(relationship.type())) validatedScenarios.add(relationship.to());
        }
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if ("SCENARIO".equals(node.type()) && !validatedScenarios.contains(node.id())) {
                findings.add(warning(SCENARIO_WITHOUT_TEST,
                        "Scenario node '" + node.id() + "' has no associated test.",
                        "project.nodes[" + index + "]"));
            }
        }
    }

    private static String triple(String from, String relationship, String to) {
        return from + '\u0000' + relationship + '\u0000' + to;
    }

    private static boolean isError(ApplicationValidationFinding finding) {
        return finding.severity() == ApplicationValidationSeverity.ERROR;
    }

    private static ApplicationValidationFinding error(String code, String message, String location) {
        return new ApplicationValidationFinding(code, ApplicationValidationSeverity.ERROR, message, location);
    }

    private static ApplicationValidationFinding warning(String code, String message, String location) {
        return new ApplicationValidationFinding(code, ApplicationValidationSeverity.WARNING, message, location);
    }
}
