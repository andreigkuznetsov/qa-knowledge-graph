# Semantic Validation Core

`qa-model-validation-core` validates a normalized QA model in two stages.
`QaModelValidationEngine` first executes JSON Schema validation. Semantic
validation is executed only when the schema produces no issues, preserving the
existing validation policy and public response contract.

## Semantic components

- `KnowledgeRule` is the contract for one identifiable semantic rule. A rule
  receives the model as a Jackson `JsonNode` and returns existing
  `ValidationIssue` values. Ordinary violations are findings, not exceptions.
- `SemanticValidationEngine` executes an immutable ordered list of rules and
  concatenates their findings in rule and finding order. It contains no
  rule-specific decisions and requires no Spring context.
- `SemanticValidationRules.defaults()` is the explicit registration point for
  the built-in rules. Its list order is stable.
- `SemanticQaModelValidator` remains the compatibility entry point used by
  `QaModelValidationEngine` and delegates to the semantic engine.

The built-in rules preserve the existing checks for node identity, source
references, relationship integrity and compatibility, test-step order, and the
existing operation, scenario, business-rule, test, and check association
warnings. In particular, `UNKNOWN_TO_NODE`, `RELATIONSHIP_NOT_ALLOWED`, and
`SCENARIO_WITHOUT_TEST` retain their codes, messages, severity, and context.

The public validation engine applies its existing final ordering by severity,
layer, object ID, and finding code. Consequently, REST output remains stable
even though semantic execution order is now explicit independently.

## Adding a semantic rule

1. Implement `KnowledgeRule` in the semantic validation package.
2. Return immutable or freshly-created `ValidationIssue` values without
   modifying the input model.
3. Add the rule at the intended position in
   `SemanticValidationRules.defaults()`.
4. Add focused unit tests for valid input, violations, stable messages and
   ordering, and verify the validation endpoint when public behavior changes.

Rules are registered explicitly. The core does not use reflection, classpath
scanning, parallel execution, runtime configuration, or Spring annotations.
