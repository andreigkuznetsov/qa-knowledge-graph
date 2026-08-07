# ADR-009: Scenario evidence formation

- Status: Accepted
- Date: 2026-08-08
- Decision owners: QAIP architecture

## Context

ADR-008 defines an existential rule-verification proof whose authoritative
scenario-mediated witness contains:

```text
BUSINESS_OPERATION --GOVERNED_BY--> BUSINESS_RULE
BUSINESS_OPERATION --SPECIFIED_BY--> SCENARIO
SCENARIO --COVERS--> BUSINESS_RULE
TEST_IMPLEMENTATION --VALIDATES--> SCENARIO
```

That proof contract consumes qualified canonical facts but does not decide
which repository constructs may author a `SCENARIO` or the three
scenario-related relationships. Without a source-authority boundary, an
extractor could manufacture apparently complete witnesses from test names,
assertions, documentation prose, shared implementations, or adjacent graph
nodes. Such witnesses would be deterministic in shape but not authoritative in
meaning.

Repository analysis already observes HTTP operations, Bean Validation
constraints, tests, HTTP interactions, and checks. Those facts are useful, but
none implicitly declares an independently identified scenario. This decision
defines the evidence-formation boundary that must be satisfied before ADR-008
may consume `SPECIFIED_BY`, `COVERS`, or `VALIDATES`.

## Decision

Only explicit, structured, qualified source declarations or a separately
approved deterministic derivation profile may create an authoritative canonical
`SCENARIO`. Scenario specification evidence and test verification evidence are
independent assertions, even when they are stored in the same file or source
repository.

No evidence means no engineering claim. Unsupported, absent, or ambiguous
source evidence must not be repaired with inference.

## Scenario authority classes

### Explicitly authored scenario

An explicitly authored scenario is the baseline authoritative class. It is a
structured source construct whose declared semantics include:

- a stable scenario key;
- scenario content or an exact reference to that content;
- an exact reference to the business operation it specifies; and
- exact references for every business rule it claims to cover.

Acceptable forms include a versioned machine-readable scenario manifest or a
structured executable-specification construct whose format defines exact
operation and rule reference fields. Human prose is not an authored scenario
for this contract merely because it contains headings, steps, endpoint text,
or expected results.

One qualified authored declaration may create the canonical `SCENARIO` and its
`SPECIFIED_BY` relationship. It creates `COVERS` only for explicitly referenced
rules. A scenario with no rule references remains authoritative scenario
knowledge but supplies no rule-coverage claim. Each emitted fact retains its
own source evidence and resolution outcome.

### Derived scenario

A derived scenario is permitted only under a separately accepted, versioned
derivation profile. The profile must define:

- the exact supported source construct and its normative semantics;
- the deterministic mapping to scenario identity and content;
- the exact operation and rule reference rules;
- ambiguity and unsupported-case behavior;
- provenance and fingerprint inputs; and
- compatibility with this ADR and ADR-008.

General source-code structure is not such a profile. No current Bean Validation
annotation, HTTP test method, assertion, README section, or operation
implementation is approved by this ADR to create a derived scenario.

A derived scenario remains distinguishable from an authored scenario in
retained provenance. Derivation may normalize explicit semantics; it may not
invent missing specification intent.

### Test-declared scenario reference

A `TEST_IMPLEMENTATION` may explicitly declare that it validates an existing
scenario by carrying an exact stable scenario reference in a supported,
structured test metadata construct. A framework-provided binding to an
independently authored executable scenario may provide the same evidence when
that binding has exact, deterministic identity semantics.

The declaration may establish `VALIDATES` after both identities resolve and
the evidence qualifies. It does not create the scenario, establish
`SPECIFIED_BY`, or establish `COVERS`.

A test method never implicitly becomes a `SCENARIO`. Inputs, actions,
assertions, display names, method names, parameter sets, and HTTP interactions
do not independently author scenario specification.

## Bean Validation authority

A supported Bean Validation constraint may author a `BUSINESS_RULE` observation
and, through an exact validated request-binding construct, contribute to the
existing determination that a business operation is `GOVERNED_BY` that rule.

A Bean Validation constraint must not:

- create a `SCENARIO`;
- establish `SPECIFIED_BY`;
- establish `COVERS`; or
- imply that any test validates the rule.

A constraint states a rule on valid input. It does not independently specify a
Given/When/Then case, expected operation outcome, or verification binding.
Generating a scenario per constraint would conflate rule extraction with
scenario authorship and is rejected by this contract.

## Relationship formation rules

### SPECIFIED_BY

`BUSINESS_OPERATION --SPECIFIED_BY--> SCENARIO` may be emitted only when one
qualified scenario declaration contains an explicit operation reference that
resolves to exactly one canonical `BUSINESS_OPERATION` in the accepted evidence
snapshot.

The minimum reference identifies the operation by an authoritative stable key
or by a versioned exact operation identity tuple defined by the source format.
For an HTTP operation, a profile may define an exact normalized method and path
tuple. Text mentioning an endpoint is not an operation reference.

### COVERS

`SCENARIO --COVERS--> BUSINESS_RULE` may be emitted only when the qualified
scenario declaration contains an explicit rule reference that resolves to
exactly one canonical `BUSINESS_RULE` in the accepted evidence snapshot.

The minimum reference is an authoritative stable rule key. A field name,
constraint message, scenario sentence, assertion operand, or similar text is
not a rule reference. Each rule relationship is formed independently; a
scenario that references one rule must not be assumed to cover sibling rules.

### VALIDATES

`TEST_IMPLEMENTATION --VALIDATES--> SCENARIO` may be emitted only when a
qualified test declaration or framework binding contains an explicit stable
scenario reference that resolves to exactly one existing canonical `SCENARIO`
in the accepted evidence snapshot.

The test identity must independently resolve to exactly one canonical
`TEST_IMPLEMENTATION`. Exercising the scenario's operation, owning checks, or
asserting a compatible outcome does not substitute for the explicit scenario
reference.

## Minimum stable scenario identity

Every authoritative scenario declaration contains a non-blank stable scenario
key within a declared source authority namespace. The canonical scenario
identity is deterministically formed from at least:

- source authority identity;
- stable scenario key; and
- a versioned identity-normalization scheme.

The source contract or format version is retained to interpret and qualify the
declaration, but ordinary format evolution must not silently change scenario
identity when the authority namespace and stable key are unchanged.

The stable key must survive ordinary file movement and content reformatting.
A display name, test method name, repository-relative path, line number, or
content text alone is not a stable scenario identity. File location and content
fingerprints remain provenance, not identity substitutes.

Duplicate declarations of the same stable key are allowed only when the source
contract explicitly defines deterministic composition and the declarations are
semantically compatible. Otherwise the identity is ambiguous.

## Independent specification and verification evidence

Specification evidence and verification evidence must be independently
asserted:

- the scenario declaration authors the scenario, its operation reference, and
  its rule references;
- the test declaration or framework binding separately references the existing
  scenario.

Independence is semantic, not necessarily physical. The declarations may be in
the same repository or file, but no single untyped observation may satisfy both
roles merely by being treated differently downstream.

An executable-specification framework may colocate scenario text and executable
binding. It qualifies only when its source contract exposes separate stable
scenario identity and test-execution binding facts. The extractor must retain
both facts and their respective provenance.

## Circular-evidence prevention

The following cycles are prohibited:

- creating a scenario from a test and using the same test to establish
  `VALIDATES` for that scenario;
- creating a scenario from assertions and retaining those assertions as the
  evidence that the scenario is validated;
- creating `COVERS` from a test's field or assertion references and then using
  that relationship to claim the test validates the rule;
- creating a rule-to-scenario mapping from operation qualification and then
  using shared operation qualification to establish `VALIDATES`; and
- using a Runtime rule-verification result as source evidence for any
  relationship from which that same result is derived.

Every ADR-008 witness must expose independent source evidence for scenario
specification and test validation. Provenance must make the two formation paths
auditable.

## Qualification and provenance

Before a scenario or relationship becomes authoritative, its source evidence
must satisfy ADR-006 and the applicable source profile:

- the source authority and capability are declared and accepted;
- the source snapshot is immutable and identified;
- source contract, parser, normalization, identity, and derivation versions are
  retained;
- the exact source construct has a stable locator and content fingerprint;
- every explicit reference and its resolution outcome are retained;
- endpoint node types and relationship direction satisfy the Canonical Model;
- identity resolution is unique and deterministic;
- conflicting or rejected candidates and reason codes are retained; and
- collection and candidate ordering is deterministic.

Repository paths and line/column spans may be retained as locators, but are not
sufficient provenance without snapshot and content binding. Live mutable source
state is not consulted during proof derivation.

## Same-snapshot rule

The `SCENARIO`, `SPECIFIED_BY`, `COVERS`, and `VALIDATES` facts consumed by one
ADR-008 witness must resolve in the same accepted evidence snapshot as the
operation, rule, and test identities.

Facts may originate from different immutable source snapshots only when an
accepted composite evidence snapshot explicitly binds those inputs, records
their compatibility and provenance, and supplies the single snapshot identity
consumed by Runtime. Runtime must not join unrelated live or independently
changing source states.

## Ambiguous and unsupported evidence

When a declaration or reference has multiple incompatible resolutions, the
extractor must not select a preferred candidate. It retains the candidates,
provenance, and deterministic ambiguity reason. No authoritative relationship
is emitted from that unresolved reference. Runtime may consequently classify a
governed rule as `AMBIGUOUS` under ADR-008 when relevant retained evidence
cannot be bound consistently.

When a source construct, framework, reference form, or derivation profile is
unsupported, the extractor records the limitation when its contract supports
diagnostics and emits no fabricated scenario or relationship. Missing or
unsupported evidence leads to unavailable knowledge and may produce ADR-008
`UNKNOWN`; it never means that a scenario, relationship, rule, or test does not
exist.

Multiple compatible declarations or witnesses for the same stable identities
are additive evidence, not ambiguity.

## Prohibited inference

None of the following may create a scenario or establish `SPECIFIED_BY`,
`COVERS`, or `VALIDATES`:

- test class, method, or display names;
- README or other unstructured prose;
- text or identifier similarity;
- field-name coincidence;
- assertion text, category, operand, or expected value;
- graph proximity or arbitrary reachability;
- shared operation, controller, service, repository, or implementation usage;
- common file, package, module, or source location;
- the presence or absence of CHECKs; or
- an existing operation-level or rule-level verification status.

These observations may be retained for other capabilities, but they do not
satisfy this authority contract.

## Ownership

- **Source contracts define author intent.** An accepted structured format or
  derivation profile defines what its declarations and references mean.
- **Extractors own observation and formation.** They parse supported source
  constructs, retain provenance, resolve explicit identities, emit qualified
  canonical nodes and relationships, and preserve ambiguity or unsupported
  diagnostics. They do not derive rule-verification state.
- **The Canonical Model owns vocabulary and structural validity.** It defines
  node and relationship types, legal endpoint types and directions, and
  immutable content shape. It does not infer author intent or evidence
  authority from a structurally valid graph.
- **Runtime owns engineering interpretation.** It consumes qualified facts from
  one accepted snapshot and applies ADR-008 to derive per-rule `VERIFIED`,
  `UNKNOWN`, or `AMBIGUOUS`. It does not manufacture missing scenario evidence.
- **Explorer owns presentation.** It presents Runtime results and retained
  evidence without parsing source constructs, resolving references, traversing
  the graph, or deriving scenario or verification semantics.

## Rationale

An authoritative rule-verification witness requires a specification that is
independent of the implementation used to verify it. Explicit stable references
make the evidence reproducible and prevent a test from proving a scenario that
was invented from that same test.

The Canonical Model can structurally represent scenario relationships without
asserting that every legal edge is authoritative. Separating structural
validity, extractor qualification, and Runtime interpretation preserves the
ownership boundaries of ADR-006 and ADR-008.

Permitting versioned derived scenarios leaves room for future deterministic
source formats while requiring their semantic authority to be decided before
use. It does not authorize heuristic derivation from current Java repositories.

## Compatibility with existing decisions

- **ADR-004:** evidence formation has one owner in the extractor/source-profile
  boundary; Runtime and Explorer do not recreate it.
- **ADR-006:** every authoritative fact is snapshot-bound, qualified,
  provenance-retained, deterministic, and conservative under missing or
  conflicting evidence.
- **ADR-007:** Runtime owns cross-capability engineering interpretation and
  Explorer owns presentation only.
- **ADR-008:** this ADR supplies the qualified scenario facts consumed by its
  proof contract without changing existential `VERIFIED`, open-world
  `UNKNOWN`, explicit `AMBIGUOUS`, CHECK support, or partial-knowledge semantics.

## Consequences

- Existing repositories with rules, tests, and checks but no explicit scenario
  declarations or references cannot qualify an ADR-008 `VERIFIED` witness.
- Bean Validation extraction continues to provide rules and governing evidence,
  not generated scenarios.
- Tests may validate scenarios only through exact structured references or
  accepted framework bindings.
- Scenario source formats require stable keys and explicit operation/rule
  references; prose-only scenarios remain unsupported.
- Extractors must retain formation provenance and unresolved candidates rather
  than emit best-effort relationships.
- Specification and verification declarations may be colocated, but remain
  separately identifiable evidence facts.
- Supporting CHECK evidence remains independent of scenario formation and rule
  attribution.
- Conservative `UNKNOWN` results will be common until repositories adopt an
  accepted scenario authority format.

## Rejected alternatives

- **Treat each HTTP test as a scenario.** Rejected because it conflates
  specification with verification and creates circular evidence.
- **Generate a scenario per Bean Validation constraint.** Rejected because a
  constraint authors a rule, not an operation outcome or scenario.
- **Infer COVERS from fields, messages, or assertions.** Rejected because
  similarity and co-occurrence cannot establish rule identity.
- **Infer VALIDATES from operation exercise.** Rejected because many tests of
  one operation may address different scenarios or concerns.
- **Parse README prose as authoritative scenarios.** Rejected because
  unrestricted natural language lacks stable identity and bounded reference
  semantics.
- **Allow a test to create and validate the same scenario.** Rejected because
  the witness would not contain independent specification evidence.
- **Resolve ambiguous references by ranking candidates.** Rejected because a
  hidden winner converts uncertainty into fabricated authority.
- **Let Runtime or Explorer repair missing relationships.** Rejected because it
  duplicates extractor responsibility and creates a second source of truth.

## Unresolved questions

- Which versioned scenario source format will be accepted first?
- Which exact operation and rule identity schemes will that format reference?
- Which executable-specification frameworks expose sufficiently stable,
  independent scenario and test-binding identities?
- Will any derived-scenario profile be justified, or will authored scenarios
  remain the only accepted source class?
- How will compatible source snapshots be composed when specification and test
  evidence originate from different repositories?
- What source capability and trust policy qualifies authored declarations?
- Which diagnostics contract will retain unsupported constructs, unresolved
  references, and ambiguity candidates?
