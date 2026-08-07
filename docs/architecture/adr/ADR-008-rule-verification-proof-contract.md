# ADR-008: Rule verification proof contract

- Status: Accepted
- Date: 2026-08-07
- Decision owners: QAIP architecture

## Context

The next engineering question after Operation Overview is:

> Which business rules govern this operation, and what evidence verifies each
> rule?

The Canonical Model can represent operations, business rules, scenarios, test
implementations, and checks. It also defines the relationships required for a
scenario-mediated proof chain:

```text
BUSINESS_OPERATION --GOVERNED_BY--> BUSINESS_RULE
BUSINESS_OPERATION --SPECIFIED_BY--> SCENARIO
SCENARIO --COVERS--> BUSINESS_RULE
TEST_IMPLEMENTATION --VALIDATES--> SCENARIO
```

The model separately defines `TEST_IMPLEMENTATION --HAS_CHECK--> CHECK` as an
ownership relationship for supporting check evidence. It is not part of the
baseline semantic proof chain.

These relationships currently express individual facts. They do not, by
themselves, define when Runtime may derive the engineering interpretation that
a rule is verified. In particular, graph proximity, shared implementation
nodes, similar names, and operation-level verification cannot safely establish
which rule a test verifies.

Some extractors do not currently produce `SPECIFIED_BY`, `COVERS`, or
`VALIDATES` evidence. For example, repository analysis may extract governing
rules, operation-qualified tests, and checks without producing an authoritative
link between those tests and individual rules. The absence of that link must
not be converted into a negative claim.

## Decision

Runtime owns a versioned, deterministic rule-verification proof contract.
Extractor-owned observations provide its facts; Explorer presents its result.

### Proof subject and snapshot

A proof result concerns one known `BUSINESS_RULE` governing one selected
`BUSINESS_OPERATION`. Runtime evaluates it from one immutable, validated
project/evidence snapshot. Every node and relationship in a proof witness must
resolve in that snapshot and satisfy the Canonical Model's type and direction
constraints.

The governing relationship is part of the proof subject, not inferred from the
scenario chain. Runtime must establish:

```text
BUSINESS_OPERATION --GOVERNED_BY--> BUSINESS_RULE
```

before classifying verification for that operation-rule pair.

### VERIFIED

`VERIFIED` is an existential evidence claim. A governed rule is `VERIFIED`
when Runtime can retain at least one complete authoritative witness containing:

```text
BUSINESS_OPERATION --GOVERNED_BY--> BUSINESS_RULE
BUSINESS_OPERATION --SPECIFIED_BY--> SCENARIO
SCENARIO --COVERS--> BUSINESS_RULE
TEST_IMPLEMENTATION --VALIDATES--> SCENARIO
```

Every required relationship must be qualified, provenance-retained,
deterministic, and resolved from the same accepted evidence snapshot. The test
must satisfy the authoritative Runtime qualification semantics applicable to
verification evidence.

Scenario-mediated evidence is therefore sufficient for rule-level
`VERIFIED`. It preserves the existing meanings of `SPECIFIED_BY`, `COVERS`,
and `VALIDATES`; Runtime derives the rule-level interpretation only from their
explicit composition.

`VERIFIED` means only:

> At least one authoritative test-level verification witness exists for this
> governed rule.

It does not mean that the rule is fully tested, that behavioral coverage is
complete, that every boundary is tested, that the test currently passes, or
that no tests are missing. It also does not establish production correctness or
absence of defects.

Multiple complete, compatible witnesses for the same operation-rule pair add
evidence. Their multiplicity is not ambiguity.

### UNKNOWN

`UNKNOWN` means that Runtime has insufficient authoritative evidence to build
a complete witness for a known governed rule. Causes include unsupported
extraction, a missing scenario or required relationship, or evidence that
cannot be authoritatively resolved. Missing CHECK evidence is not, by itself, a
reason for `UNKNOWN`.

`UNKNOWN` does not mean `UNVERIFIED`, failed, false, uncovered, or untested. It
is not a negative inference and carries no completeness claim. Runtime retains
deterministic reason codes identifying which proof obligation could not be
established.

### AMBIGUOUS

`AMBIGUOUS` means relevant authoritative evidence exists, but Runtime cannot
deterministically bind it into a unique, internally consistent semantic
witness. Examples include unresolved competing identities, a relationship
whose target has multiple incompatible interpretations, or conflicting source
facts about which scenario or rule an evidence item addresses.

Runtime retains the conflicting candidates and deterministic reason codes.
Missing evidence alone is `UNKNOWN`, not `AMBIGUOUS`; multiple compatible
witnesses for the same rule are `VERIFIED`, not `AMBIGUOUS`.

### Partial knowledge

Rule verification state is independent per known governed rule. A `VERIFIED`
rule remains verified when another rule is `UNKNOWN` or `AMBIGUOUS`. An
unresolved rule does not invalidate the operation, its known governing rules,
or other proof witnesses.

Known governed rules remain visible even when their verification state is
`UNKNOWN`. Conversely, the absence of extracted governing rules must not be
reported as "this operation has no rules." It is unavailable rule knowledge.
Results and retained evidence collections are immutable and deterministically
ordered.

### Check-level attribution

CHECK is supporting retained evidence only. CHECK is not mandatory for the
baseline rule-level `VERIFIED` state. The qualified, provenance-retained
test-to-scenario `VALIDATES` and scenario-to-rule `COVERS` relationships supply
the semantic verification evidence within the complete operation-rule witness.

`HAS_CHECK` establishes ownership only; it does not identify which assertion
proves which rule or scenario outcome. CHECK presence, absence, text, category,
or graph proximity must not affect rule attribution or the baseline
`VERIFIED` classification.

The result must not claim that a particular CHECK verifies a rule or scenario
outcome unless a future accepted Assertion Semantics contract provides explicit
attribution. Assertion Semantics begins when the system interprets a CHECK as a
proposition about a scenario outcome or business-rule clause. Such attribution
may strengthen future explanations, but must not be inferred or implemented by
overloading `HAS_CHECK`, `VALIDATES`, or `COVERS`.

### Retained proof evidence

Every `VERIFIED` result retains enough evidence to explain and replay the
derivation:

- stable operation, rule, scenario, and test identities and types;
- the exact typed, directed canonical relationships forming each witness;
- the immutable project/evidence snapshot identity;
- available source references and provenance for the facts used;
- the qualification outcome for the test;
- the deterministically ordered compatible witnesses;
- the proof-contract or derivation-rule version; and
- explicit limitations, including the absence of completeness and check-level
  attribution claims.

When CHECKs owned by a validating test are available, they may be retained as
deterministically ordered supporting evidence with their identities, types,
source references, provenance, and qualification outcomes. Their retention
does not make them part of the semantic proof and does not attribute them to a
rule or scenario outcome.

Canonical source references support repository-static explanation. Claims
requiring audit-grade or cross-source provenance must additionally satisfy the
qualified immutable evidence and provenance requirements of ADR-006.

### Acceptable proof sources

Authoritative proof facts may come from:

- explicit, validated canonical relationships supplied by an authoritative
  source;
- deterministic extractor observations that emit the exact canonical facts
  with retained source references; or
- immutable qualified evidence satisfying ADR-006.

Runtime evaluates only retained snapshot evidence. Live mutable sources are
not consulted while deriving the state.

Unsupported extraction produces `UNKNOWN` with an explicit reason. Extractors
must not manufacture scenarios or relationships to fill a proof gap.

The following are never proof:

- graph proximity or reachability through relationships not in this contract;
- text, identifier, or name similarity;
- common source location or shared implementation dependencies;
- inheritance from operation-level verification status; or
- absence of evidence.

No result may claim that all rules, scenarios, tests, checks, or behaviors are
known or complete unless a separate future contract explicitly defines and
proves that closed-world claim.

### Ownership

- **Extractors own observations.** They identify supported source constructs
  and emit explicit canonical nodes, relationships, source references, and
  evidence. They do not classify a rule as `VERIFIED`, `UNKNOWN`, or
  `AMBIGUOUS`.
- **Runtime owns engineering interpretation.** It qualifies evidence, composes
  the proof witness, resolves ambiguity, assigns per-rule state, preserves
  partial knowledge, orders results, and retains the derivation from one
  snapshot.
- **Explorer owns presentation.** It projects Runtime states, reasons, and
  evidence without graph traversal, correlation, qualification, similarity
  matching, status derivation, or completeness inference.

## Rationale

The scenario is the semantic point at which an operation specification, a
business rule, and a validating test can be joined without changing the
meaning of existing canonical relationships. Requiring an explicit complete
witness of qualified, provenance-retained relationships from one accepted
snapshot prevents accidental proof by proximity while permitting independent
extractors and evidence sources to contribute facts.

The three states distinguish positive evidence, insufficient evidence, and
conflicting evidence. That distinction preserves open-world semantics:
missing knowledge remains unknown instead of becoming a false negative.

Test-level scenario attribution is sufficient for the bounded existential
claim. Requiring CHECK presence or check-level attribution would reject
otherwise authoritative scenario-mediated evidence, while attributing an owned
check to the rule would assert more than `HAS_CHECK` means.

## Compatibility with existing decisions

- **ADR-004:** the proof state has one owning capability in Runtime. Downstream
  consumers reuse it and must not recalculate it.
- **ADR-006:** proof uses immutable qualified evidence and retained provenance;
  missing positive evidence never becomes a negative assertion. Audit-grade
  claims require ADR-006's stronger provenance boundary.
- **ADR-007:** rule verification is cross-capability engineering
  interpretation owned by Runtime and presented by Explorer. It uses one
  project snapshot and preserves independent partial knowledge.

## Consequences

- Rule verification is explainable as retained witnesses rather than an opaque
  Boolean or inherited operation status.
- Existing canonical relationship meanings remain unchanged.
- Current repositories may legitimately yield many `UNKNOWN` results until
  authoritative scenario and validation relationships are extracted.
- Runtime contracts must preserve reasons, candidates, witnesses, provenance,
  qualification, and derivation version as well as the state.
- Evidence and results may be larger because replayable witnesses are retained.
- A future change to proof obligations or acceptable sources requires a new
  proof-contract version and compatibility decision.
- The contract answers whether evidence exists for each known rule; it does not
  answer whether the rule set or test suite is complete.

## Rejected alternatives

- **Graph-proximity inference.** Shared implementations, nearby tests, or
  reachability do not establish semantic attribution to a rule.
- **Text or name matching.** Similar wording is heuristic evidence and cannot
  create an authoritative proof relationship.
- **Inheriting operation verification.** A verified operation may have tests
  without evidence binding any test to an individual rule.
- **Treating missing evidence as unverified.** This violates open-world
  semantics and turns extractor limitations into false negative claims.
- **Requiring direct test/check-to-rule edges.** A mandatory direct edge would
  duplicate the scenario's existing semantic role. Explicit check-level
  attribution remains optional strengthening evidence.
- **Attributing every owned check to the rule.** `HAS_CHECK` expresses
  containment, not the behavior established by each check.
- **Explorer-owned proof composition.** This would duplicate engineering
  reasoning, introduce a second source of truth, and violate ADR-004 and
  ADR-007.
- **A completeness classification.** The available model and evidence do not
  establish a closed world of all rules or all necessary tests.

## Unresolved questions

- Which source constructs will be accepted for deterministic scenario,
  coverage, and validation extraction?
- Should future explicit check-level attribution use a new canonical
  relationship or a qualified evidence record?
- What trust, freshness, and conflict policies apply when witnesses combine
  multiple authoritative sources?
- Which claims require ADR-006 audit-grade provenance rather than
  repository-static source references?
- How should observed test-execution outcomes be represented separately from
  static verification evidence?
- What stable public representation will carry proof-contract versions,
  reason codes, candidates, and retained witnesses?
