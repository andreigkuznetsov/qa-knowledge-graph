# Semantic Validation Core

`qa-model-validation-core` validates a normalized QA model in two stages.
`QaModelValidationEngine` first executes JSON Schema validation. Semantic
validation is executed only when the schema produces no issues, preserving the
existing validation policy and public response contract.

## Semantic components

- `KnowledgeRule` is the contract for one identifiable semantic invariant. A
  rule receives the model as a Jackson `JsonNode`, and `code()` returns the one
  existing finding code emitted by that rule. Ordinary violations are returned
  as existing `ValidationIssue` values, not exceptions.
- `SemanticValidationEngine` executes an immutable ordered list of rules and
  concatenates their findings in rule and finding order. It contains no
  rule-specific decisions and requires no Spring context.
- `SemanticValidationRules.defaults()` is the explicit registration point for
  the built-in rules. Its list order is stable.
- `SemanticQaModelValidator` remains the compatibility entry point used by
  `QaModelValidationEngine` and delegates evaluation directly to the semantic
  engine.

Each built-in rule represents exactly one pre-existing finding code. Together
they preserve the checks for node identity, source references, relationship
integrity and compatibility, test-step order, and the existing operation,
scenario, business-rule, test, and check association warnings. No completeness
or coverage analysis has been added.

## Ordering boundaries

### Internal semantic ordering

Semantic rules are registered explicitly in an immutable, deterministic list.
The semantic engine executes them sequentially and aggregates findings in rule
registration order. Findings produced by an individual rule preserve that
rule's deterministic traversal of the model. The former monolithic validator's
cross-rule encounter interleaving is not a supported contract.

### Supported application ordering

`QaModelValidationEngine` owns canonical result ordering. It sorts the final
supported result by severity, layer, object ID, and finding code. REST clients
observe this canonical application ordering.

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
