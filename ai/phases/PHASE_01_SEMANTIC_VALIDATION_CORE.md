# PHASE 01 — SEMANTIC VALIDATION CORE

## Engineering Task Specification

Read and follow:

* `ai/MASTER_PROMPT.md`
* `ai/README.md`

This Engineering Task Specification supplements `ai/MASTER_PROMPT.md` and does not replace it.

---

## 1. Task

Refactor the existing semantic validation implementation into a modular Semantic Validation Core.

The refactoring must preserve the current external behavior and must not rewrite already working functionality.

The resulting architecture must make each semantic validation rule independently identifiable, executable, testable, and extensible.

---

## 2. Context

The QA Knowledge Graph project currently provides the validation endpoint:

```text
POST /api/v1/qa-model/validate
```

The validation process already includes:

1. JSON Schema validation.
2. Semantic validation.

The currently known semantic validation findings include:

* `UNKNOWN_TO_NODE`
* `RELATIONSHIP_NOT_ALLOWED`
* `SCENARIO_WITHOUT_TEST`

These validations already work and must remain operational.

The current implementation may not match the terminology, classes, packages, or structure proposed in this document.

The repository is the source of truth.

Do not assume that any class, interface, package, or abstraction exists until it has been verified in the repository.

---

## 3. Objective

Create a Semantic Validation Core that separates semantic rules from validation orchestration.

The intended responsibility model is:

```text
Validation request
        ↓
Existing request parsing and JSON Schema validation
        ↓
Semantic validation engine
        ↓
Independent semantic rules
        ↓
Aggregated semantic findings
        ↓
Existing validation response
```

The implementation must provide:

* an explicit contract for semantic validation rules;
* an orchestration component that executes registered rules;
* a common internal representation of semantic findings;
* independent implementations of the existing semantic validations;
* deterministic aggregation of findings;
* preservation of the current REST and JSON contracts.

This phase is an architectural refactoring of existing semantic validation behavior.

It is not a phase for adding new business validations.

---

## 4. Mandatory Repository Analysis

Before modifying code, inspect the relevant repository structure.

At minimum, identify:

* Gradle modules;
* the module containing the canonical model;
* the module containing the validator application;
* the REST controller for `/api/v1/qa-model/validate`;
* request and response DTOs;
* JSON Schema validation components;
* semantic validation components;
* existing finding, warning, and error representations;
* existing tests for validation behavior;
* Spring component registration and dependency injection;
* documentation describing the validator.

Determine and report:

1. How the validation request currently flows through the system.
2. Where JSON Schema validation is performed.
3. Where semantic validation is performed.
4. How findings are currently created.
5. How findings are transformed into the public response.
6. Whether rule execution order is currently observable.
7. Which existing tests protect the current behavior.
8. Which responsibilities are currently combined and should be separated.

Do not begin implementation until this analysis is complete.

---

## 5. Scope

### 5.1 In Scope

This phase includes:

* extracting existing semantic checks into independent rule components;
* introducing a common semantic rule contract;
* introducing a semantic validation orchestration component;
* introducing or adapting an internal semantic finding representation;
* preserving existing finding codes and messages;
* preserving the existing validation endpoint;
* preserving existing JSON request and response formats;
* preserving existing JSON Schema validation behavior;
* preserving existing semantic validation behavior;
* adding focused unit tests for individual rules;
* adding tests for rule orchestration;
* updating relevant documentation.

### 5.2 Out of Scope

Do not implement:

* completeness analysis;
* coverage analysis;
* knowledge metrics;
* knowledge queries;
* dependency analysis;
* graph traversal infrastructure;
* simulation;
* decision support;
* rule configuration through REST;
* dynamic rule loading;
* plugin systems;
* scripting support;
* persistence for findings;
* parallel rule execution;
* rule priorities unless required to preserve existing behavior;
* suppression of findings;
* severity redesign;
* public API redesign;
* additional semantic rules unrelated to the existing behavior.

Do not create placeholders for these capabilities.

---

## 6. Target Design

The exact class and package names may be adjusted to fit the existing project conventions.

Do not force this design mechanically when the repository already contains equivalent abstractions.

The intended conceptual components are described below.

---

### 6.1 Semantic Rule Contract

Introduce an explicit contract representing one semantic validation rule.

Preferred conceptual form:

```java
public interface KnowledgeRule {

    String code();

    List<Finding> evaluate(QaModel model);
}
```

This signature is illustrative rather than mandatory.

Use the actual canonical root model type instead of `QaModel`.

Use the existing finding type if it already represents the required information cleanly.

A rule contract must satisfy the following requirements:

* represent one identifiable validation rule;
* expose a stable rule or finding code;
* accept the canonical model or an appropriate validation context;
* return zero or more findings;
* avoid modifying the validated model;
* avoid depending on REST DTOs;
* avoid depending on controllers;
* avoid throwing exceptions for ordinary validation failures;
* remain independently unit-testable.

Do not add methods without a demonstrated current need.

---

### 6.2 Finding Representation

The Semantic Validation Core must use a common internal representation for semantic findings.

Preferred conceptual form:

```java
public record Finding(
        String code,
        String message,
        String path
) {
}
```

This form is illustrative.

Before introducing a new type, inspect the existing representations.

Reuse or adapt an existing type when it already supports:

* a stable finding code;
* a human-readable message;
* optional location or context;
* deterministic equality for testing;
* conversion into the existing public response.

Do not introduce a second parallel finding model unless there is a clear boundary requiring it.

Do not alter the public response format.

Do not add fields to the REST response merely because the internal representation supports them.

---

### 6.3 Semantic Validation Engine

Introduce an orchestration component responsible for executing semantic rules.

Preferred conceptual form:

```java
public final class SemanticValidationEngine {

    private final List<KnowledgeRule> rules;

    public List<Finding> validate(QaModel model) {
        // Execute rules and aggregate findings.
    }
}
```

The engine must:

* receive an explicit collection of rules;
* execute all registered rules;
* aggregate their findings;
* preserve deterministic execution and finding order;
* avoid containing rule-specific business logic;
* avoid knowing about REST controllers;
* avoid performing JSON Schema validation;
* avoid modifying the canonical model;
* avoid catching and hiding unexpected failures.

The engine must not use:

* reflection-based rule discovery;
* global mutable registries;
* classpath scanning implemented manually;
* parallel streams;
* nondeterministic collections.

Spring dependency injection may be used at the application boundary when it already fits the project architecture.

The semantic core itself should remain understandable and testable without requiring a Spring application context.

---

### 6.4 Existing Semantic Rules

Extract the currently implemented semantic validations into independent rule components.

At minimum, investigate and preserve the behavior associated with:

#### `UNKNOWN_TO_NODE`

Validate relationships whose target node cannot be resolved in the canonical model.

Preserve:

* the existing code;
* the existing triggering conditions;
* the existing message;
* the existing number of findings;
* the existing response mapping.

Do not broaden identity resolution semantics during this phase.

---

#### `RELATIONSHIP_NOT_ALLOWED`

Validate relationship types that are not permitted for the relevant source and target node types.

Preserve:

* the existing compatibility rules;
* the existing code;
* the existing triggering conditions;
* the existing message;
* the existing number of findings;
* the existing response mapping.

Do not redesign the relationship compatibility model during this phase.

---

#### `SCENARIO_WITHOUT_TEST`

Validate scenarios that do not have the required test association.

Preserve:

* the existing interpretation of a scenario;
* the existing interpretation of test association;
* the existing code;
* the existing triggering conditions;
* the existing message;
* the existing number of findings;
* the existing response mapping.

Do not introduce general completeness analysis during this phase.

This rule represents only the already implemented behavior.

---

## 7. Rule Granularity

Each rule should represent one coherent semantic invariant.

Do not create:

* one large rule containing all semantic validations;
* one rule per individual model element;
* generic rule hierarchies without a current use case;
* abstract base classes solely to avoid a few duplicated lines;
* annotation-driven rule metadata;
* configuration files for static rule definitions.

Prefer a small number of explicit classes with clear names and responsibilities.

---

## 8. Validation Pipeline

Preserve the current validation pipeline unless the repository demonstrates a concrete reason for a small adjustment.

The intended pipeline is:

```text
1. Receive validation request.
2. Parse or deserialize the model.
3. Perform JSON Schema validation.
4. If current behavior allows semantic validation, execute the Semantic Validation Core.
5. Map all findings into the existing response format.
6. Return the existing HTTP status and JSON structure.
```

Before changing control flow, determine the current behavior when JSON Schema errors exist.

Preserve whether semantic validation:

* is skipped after schema failure;
* still executes after schema failure;
* executes only for a successfully constructed canonical model.

Do not choose a new policy during this phase.

Characterize and preserve the existing policy with tests.

---

## 9. Deterministic Behavior

Semantic validation output must be deterministic.

For the same model and rule set:

* rules must execute in a stable order;
* findings must appear in a stable order;
* repeated validation must produce equivalent results.

Use an explicit rule order.

Acceptable approaches include:

* constructor list order;
* existing Spring order when explicitly controlled;
* an immutable ordered rule collection.

Do not rely on undocumented dependency injection ordering.

Do not introduce rule priority metadata unless it is necessary to make existing behavior explicit.

If rule priority is introduced, keep it minimal and document why it is required.

---

## 10. Spring Integration

Inspect the current dependency injection style before choosing the integration strategy.

Preferred behavior:

* concrete rules may be registered as Spring beans at the application boundary;
* the engine may receive a list of rule beans through constructor injection;
* rule and engine logic must remain testable without starting Spring;
* no field injection;
* no manual access to the Spring application context;
* no static service locator;
* no custom framework for rule registration.

If the current module is intentionally framework-independent, preserve that boundary and configure the rules in the Spring application module.

Do not add Spring annotations to canonical domain model classes.

---

## 11. Package and Module Boundaries

Determine the correct module for the Semantic Validation Core based on the existing repository.

The core should be placed where:

* it can depend on the canonical model;
* the REST layer can depend on it;
* the canonical model does not depend on the validator application;
* framework-independent logic does not acquire unnecessary Spring dependencies.

Potential conceptual package structure:

```text
...validation.semantic
    KnowledgeRule
    SemanticValidationEngine
    Finding

...validation.semantic.rule
    UnknownToNodeRule
    RelationshipNotAllowedRule
    ScenarioWithoutTestRule
```

This is not a mandatory package layout.

Follow existing naming and module conventions where they are coherent.

Do not move unrelated classes.

Do not create a new Gradle module unless the current module boundaries make that clearly necessary.

A new module requires explicit justification.

---

## 12. Backward Compatibility Requirements

The following must remain unchanged unless the existing tests prove that no public contract exists:

* endpoint path;
* HTTP method;
* request JSON;
* response JSON;
* HTTP status behavior;
* JSON Schema validation behavior;
* semantic finding codes;
* semantic finding messages;
* classification of errors and warnings;
* number of findings for existing test fixtures;
* behavior for valid models;
* behavior for malformed JSON;
* behavior for schema-invalid models.

If the current public output depends on finding order, preserve that order.

If an exact message must change because the current message is generated inconsistently, do not change it in this phase. Report it as technical debt.

---

## 13. Testing Requirements

Add or adapt tests at the lowest appropriate level.

### 13.1 Characterization Tests

Before changing behavior, ensure tests characterize the existing results for:

* a valid model;
* an unknown relationship target;
* a disallowed relationship;
* a scenario without a test;
* a model producing multiple semantic findings;
* a schema-invalid model;
* malformed JSON, when covered by the current endpoint.

Characterization tests must verify externally observable behavior.

Where current tests already provide this protection, preserve and reuse them.

---

### 13.2 Rule Unit Tests

Each extracted rule must have focused unit tests.

For every rule, test at minimum:

* no finding for valid input;
* one finding for a single violation;
* multiple findings when multiple independent violations exist, if supported by current behavior;
* stable finding code;
* stable message;
* stable finding order;
* no mutation of the input model where practical to verify.

Do not start the Spring context for pure rule unit tests.

---

### 13.3 Engine Unit Tests

Test the Semantic Validation Engine independently.

Verify:

* all rules are executed;
* findings are aggregated;
* rule order is preserved;
* finding order is deterministic;
* empty rule results are handled;
* an empty rule collection produces no findings;
* unexpected rule exceptions are not silently converted into validation findings.

Use test rules or stubs when appropriate.

Do not use mocking when simple deterministic test implementations are clearer.

---

### 13.4 Integration Tests

Preserve or add integration tests proving that:

* the existing endpoint uses the new engine;
* existing JSON request and response contracts are unchanged;
* existing semantic finding codes remain unchanged;
* valid requests still succeed;
* invalid requests still return the expected validation result;
* JSON Schema and semantic validation remain correctly integrated.

Use the current project testing approach.

Do not introduce a new testing framework.

---

## 14. Implementation Constraints

Do not:

* rewrite the validator application;
* redesign the canonical model;
* modify the JSON Schema unless required to fix a regression introduced by this work;
* alter REST DTOs without explicit justification;
* rename public finding codes;
* convert ordinary findings into exceptions;
* introduce a generic rules framework;
* introduce Drools or another rule engine;
* introduce event-driven validation;
* introduce asynchronous or parallel validation;
* introduce caching;
* introduce persistence;
* introduce runtime rule configuration;
* expose rules through new endpoints;
* implement future QBOK capabilities;
* replace clear Java code with reflection or annotation magic.

Prefer the smallest architecture that cleanly separates orchestration from rules.

---

## 15. Execution Plan

Follow this order.

### Step 1 — Inspect

Inspect the project structure and current validation flow.

Produce a concise analysis before modifying code.

---

### Step 2 — Characterize

Identify existing behavior and existing test coverage.

Add characterization tests first where behavior is not sufficiently protected.

Run the relevant tests before refactoring.

Record the baseline result.

---

### Step 3 — Design

Propose the smallest target design that fits the existing repository.

Identify:

* rule contract;
* finding representation;
* engine;
* concrete rule classes;
* integration point;
* package and module placement.

Explain any deviation from the conceptual design in this specification.

---

### Step 4 — Implement Core

Introduce the semantic rule contract, finding representation if needed, and validation engine.

Keep the core free of rule-specific logic.

---

### Step 5 — Extract Existing Rules

Move existing semantic validations into independent rule classes without changing their behavior.

Perform the extraction incrementally.

Run tests after each meaningful extraction.

---

### Step 6 — Integrate

Connect the existing validation pipeline to the new engine.

Preserve JSON Schema validation and REST mapping.

Remove obsolete semantic orchestration code only after equivalent behavior is verified.

---

### Step 7 — Test

Add focused rule tests, engine tests, and required integration tests.

Run all relevant module tests.

Then run the complete project test suite if the repository supports it in the current environment.

---

### Step 8 — Review

Review the final diff for:

* accidental public contract changes;
* message changes;
* finding order changes;
* unrelated modifications;
* duplicated validation logic;
* unused abstractions;
* unnecessary Spring coupling;
* speculative future functionality.

---

### Step 9 — Document

Update documentation describing:

* the Semantic Validation Core;
* how rules are registered;
* how a new semantic rule can be added;
* which existing rules are implemented.

Document only implemented behavior.

---

## 16. Acceptance Criteria

This phase is complete only when all applicable criteria are satisfied.

### Architecture

* There is an explicit semantic rule contract.
* There is a semantic validation orchestration component.
* Existing semantic checks are implemented as independent rules.
* The engine contains no rule-specific business logic.
* Rules can be tested without starting Spring.
* The canonical model does not depend on the REST or validator application layers.
* No unnecessary framework or generic rule infrastructure is introduced.

### Behavior

* `UNKNOWN_TO_NODE` behavior is preserved.
* `RELATIONSHIP_NOT_ALLOWED` behavior is preserved.
* `SCENARIO_WITHOUT_TEST` behavior is preserved.
* Valid model behavior is preserved.
* JSON Schema validation behavior is preserved.
* Public REST and JSON contracts are preserved.
* Finding order is deterministic.
* Repeated validation of the same model produces equivalent results.

### Tests

* Existing tests continue to pass.
* Each semantic rule has focused unit tests.
* The semantic engine has focused unit tests.
* Endpoint integration remains covered.
* Multiple-rule aggregation is covered.
* No test is deleted or weakened solely to make the build pass.

### Documentation

* Relevant architectural documentation is updated.
* The rule extension process is documented.
* Documentation does not describe unimplemented future capabilities.

### Scope

* No future phase is implemented.
* No unrelated files are modified.
* No speculative abstraction is introduced.

---

## 17. Deliverables

At completion, provide the report structure required by `ai/MASTER_PROMPT.md`.

Include the following details.

### Summary

Explain:

* the previous semantic validation structure;
* the new structure;
* why the change was necessary;
* how backward compatibility was preserved.

### Files Changed

For each file, state:

* whether it was added, modified, moved, or deleted;
* its responsibility;
* why the change was required.

### Architecture Decisions

Document:

* the selected rule interface;
* the selected finding representation;
* rule registration strategy;
* deterministic ordering strategy;
* package and module placement;
* relevant alternatives that were rejected.

### Tests Executed

List exact commands and observed results.

Include:

* baseline tests executed before refactoring;
* rule unit tests;
* engine unit tests;
* integration tests;
* complete test suite, if executed.

Do not claim unexecuted tests passed.

### Backward Compatibility

Explicitly state whether any of the following changed:

* endpoint;
* request format;
* response format;
* status codes;
* finding codes;
* finding messages;
* finding ordering;
* JSON Schema behavior.

### Remaining Technical Debt

Report only concrete technical debt discovered during this phase.

Do not fix unrelated technical debt.

### Recommended Follow-up

Recommend only the next justified engineering action.

Do not implement Phase 02.

---

## 18. Completion Condition

Stop after the Semantic Validation Core has been implemented, integrated, tested, and documented.

Do not begin:

```text
Phase 02 — Completeness Analysis
```

Wait for an explicit instruction before implementing any subsequent phase.