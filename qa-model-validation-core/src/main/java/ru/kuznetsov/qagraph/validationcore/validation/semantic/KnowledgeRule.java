package ru.kuznetsov.qagraph.validationcore.validation.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import ru.kuznetsov.qagraph.validationcore.model.ValidationIssue;

import java.util.List;

public interface KnowledgeRule {

    String code();

    List<ValidationIssue> evaluate(JsonNode model);
}
