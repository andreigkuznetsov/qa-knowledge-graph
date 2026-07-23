# ADR-005: Impact Conclusion Semantics and Proof Obligations

- Status: Accepted
- Date: 2026-07-23
- Decision owners: Impact Evidence architecture

## Context

Impact Evidence must classify what can be concluded about the impact of an
accepted change without turning absent or incomplete engineering knowledge into
false certainty. The product specification defines three conclusions:
`AFFECTED`, `NOT_AFFECTED`, and `UNKNOWN`.

The repository already supplies relevant foundations. Canonical Change produces
an immutable `VerifiedChangeSet`; `FinalChangeSetVerifier` checks its retained
declaration, Base, materialization, reconstruction, schema, and semantic
evidence. `TraceEngine` discovers paths in canonical relationships, and
`QaModelValidationEngine` establishes schema and semantic validity. These facts
do not by themselves define impact-conclusion semantics. In particular, a valid
model or a failed path search does not establish that the available engineering
data is complete enough to exclude impact.

The existing `qa-impact-analysis` capability answers a different question.
`ImpactAnalyzer` consumes `RoadmapReport` and `ExecutionPlan` and uses
`RemediationImpactCatalog` to produce remediation-oriented `TaskImpact` values
in `ImpactReport`. It is not the Impact Evidence classifier.

## Decision Drivers

- Make false `NOT_AFFECTED` conclusions structurally difficult.
- Preserve explicit uncertainty instead of hiding incomplete knowledge.
- Retain independently auditable proof and provenance.
- Produce deterministic conclusions from explicit, reproducible inputs.
- Reuse Canonical Change and graph traversal without changing their business
  meaning.
- Keep evidence qualification separate from remediation, planning, coverage,
  and simulation semantics.

## Decision

The classified subject is an **engineering artifact within an immutable
analysis context**. A dedicated immutable conclusion represents the result.
Classification is not mutable state on the artifact because the same artifact
may receive different correct conclusions for different changes, snapshots,
scopes, qualification policies, rule versions, or reference times.

The public result exposes exactly three mutually exclusive classifications:

- `AFFECTED`
- `NOT_AFFECTED`
- `UNKNOWN`

The internal model retains richer evidence state, including:

- direct-change proofs;
- qualified influence proofs;
- exclusion proofs;
- rejected proof candidates and their reasons;
- conflicts;
- blocking limitations;
- material non-blocking limitations;
- provenance;
- rule-set, scope, and snapshot identities.

The public classification is a deterministic projection of that evidence
state. It is not the entire evidence payload.

`AFFECTED` is existential. One exact direct-change proof from an accepted
`VerifiedChangeSet`, or one fully qualified influence path, is sufficient.
Missing or incomplete exclusion evidence does not downgrade an otherwise valid
positive proof. A stale, conflicting, untrusted, semantically unsupported, or
identity-ambiguous candidate is not a qualified positive proof.

`NOT_AFFECTED` is a positive, bounded exclusion conclusion. It may be returned
only when every mandatory exclusion obligation is satisfied. It is never
inferred from open-world absence.

`UNKNOWN` is a valid successful conclusion representing limited engineering
knowledge. It is not an exception, failure, confidence score, or probability.
One conclusion may retain multiple deterministic reasons when several
limitations apply.

## Proof Obligations

### `AFFECTED`

At least one of these proofs is mandatory:

1. **Direct-change proof:** the subject identity is exactly bound to an artifact
   changed by the accepted `VerifiedChangeSet`.
2. **Qualified influence proof:** a continuous path begins at an exactly
   identified directly changed artifact and ends at the exactly identified
   subject, and every path element:
   - belongs to an identified, applicable, compatible source snapshot;
   - is fresh under the explicit policy and reference time;
   - has resolved artifact identities;
   - uses relationship semantics and direction supported by the selected rule
     version;
   - has sufficient trust and recoverable provenance;
   - is not undermined by an unresolved relevant conflict.

Several valid paths may be retained, but one is sufficient. Rejected additional
paths do not invalidate an independent qualified path unless their evidence
creates a relevant conflict that undermines it.

### `NOT_AFFECTED`

All of these obligations are mandatory:

- subject and changed-artifact identities are resolved;
- analysis scope is explicit and bounded;
- all required sources are applicable to that scope;
- source snapshots are compatible with the verified-change/model context;
- relevant evidence is fresh under an explicit deterministic policy;
- qualified completeness assertions cover the exact required artifact
  categories, relationship categories, directions, scope, snapshots, and time;
- every relationship category and direction required by the influence rule set
  has been evaluated;
- no valid direct-change or qualified influence proof reaches the subject;
- no unresolved relevant source conflict remains;
- no unresolved relevant artifact identity remains;
- the exclusion assertions and completed evaluation are retained as evidence.

None of the following independently proves `NOT_AFFECTED`:

- no path was found;
- the artifact is absent from the graph;
- a relationship is absent;
- QA coverage is high;
- model validation succeeded;
- one source reports no dependency.

If any exclusion obligation is unproven, `NOT_AFFECTED` is prohibited.

### `UNKNOWN`

`UNKNOWN` applies after successful processing when neither an affected proof nor
a complete bounded exclusion proof survives qualification. Reasons include, at
minimum, insufficient evidence, incomplete scope, missing completeness
assertion, stale evidence, conflicting sources, unresolved artifact identity,
unsupported relationship semantics, out-of-scope artifact, incompatible source
snapshot, and untrusted or unqualified evidence.

Specific applicable reasons are retained, de-duplicated, and deterministically
ordered. A generic insufficient-evidence reason does not replace known specific
limitations.

## Classification Precedence

```text
valid direct-change proof
    -> AFFECTED

else valid qualified influence proof
    -> AFFECTED

else valid complete bounded exclusion proof
    -> NOT_AFFECTED

else
    -> UNKNOWN
```

This is precedence among **valid proofs**, not among unqualified claims. A
relevant conflict that defeats the only positive proof leads to `UNKNOWN`. A
qualified positive proof plus incomplete exclusion scope remains `AFFECTED` and
retains the scope limitation. If a purported exclusion proof coexists with a
valid positive proof in the same context, the result is `AFFECTED` and the
inconsistent exclusion evidence remains visible.

## Failure versus Valid Uncertainty

The following remain outside the classification algebra and produce failure
outcomes with diagnostics:

- invalid or malformed requests/evidence contracts;
- an unverifiable change or a rejected Canonical Change result;
- an unsupported influence or qualification rule version;
- internal processing failures or invariant violations.

Normal limits of well-formed engineering knowledge produce successful
`UNKNOWN` conclusions. These include stale evidence, unavailable or
incompatible snapshots, missing completeness assertions, unresolved identities,
conflicting sources, unsupported relationship semantics, and out-of-scope
subjects.

Failures must never be converted into `UNKNOWN`, and `UNKNOWN` must never be
used as an exception wrapper.

## Determinism Requirements

Semantic classification is fully determined by explicit inputs:

- stable verified-change reference and changed-artifact identities;
- engineering-data source and snapshot identities and integrity references;
- exact subject identity;
- normalized analysis scope;
- normalized completeness assertions;
- qualification policy version and parameters;
- influence rule-set version;
- deterministic reference time and freshness parameters;
- normalization, proof selection, and output-ordering rules.

Classification must not depend on an implicit wall clock, random identifiers,
mutable live source state, nondeterministic collection iteration, or silently
substituted rule versions. Unsupported versions fail explicitly. Paths,
reasons, sources, limitations, and conclusions have stable ordering. Generated
report/request identifiers, if later introduced, are metadata and do not affect
semantic equality or classification.

## Compatibility with Existing Architecture

The following may be reused without changing their business meaning:

- `VerifiedChangeSet` and Canonical Change evidence as the confirmed-change and
  direct-proof foundation;
- `TraceEngine` and `TracePath` as deterministic path-discovery mechanics, but
  never as evidence qualification;
- canonical relationships and `RelationshipType` as discovered facts and
  structural vocabulary, subject to a separate influence-semantics catalog;
- `QaModelValidationEngine` and validation diagnostics for model validity and
  diagnostic conventions, not source completeness;
- immutable records, defensive copies, and sealed outcomes as implementation
  patterns for evidence-preserving results.

The process-local identity binding accepted by
`ADR-001-exact-instance-evidence-binding` remains valid inside Canonical Change.
A separate decision is required before `VerifiedChangeSet` evidence crosses a
serialization or process boundary.

The following remain separate and must not be treated as the classifier:

- `ImpactAnalyzer`;
- remediation `ImpactReport` and `TaskImpact`;
- `RemediationImpactCatalog`;
- coverage percentages;
- absence of a `TracePath`;
- simulation results.

This ADR does not rename or modify those components.

## Consequences

### Positive

- False `NOT_AFFECTED` conclusions become structurally difficult to construct.
- Uncertainty remains explicit and actionable as an evidence limitation.
- Proofs, rejected candidates, conflicts, and provenance remain auditable.
- Classification is deterministic and reproducible from explicit inputs.
- Existing Canonical Change validation/evidence and traversal mechanics can be
  reused without redefining them.
- Downstream consumers receive one authoritative categorical meaning instead of
  independently interpreting graph-search absence.

### Negative

- Exclusion evidence is more expensive to produce than positive path evidence.
- Evidence payloads and internal state are larger than a bare enum result.
- Additional contracts are required for scope, completeness, provenance,
  identity, conflict, freshness, and influence semantics.
- Many open-world analyses will correctly return `UNKNOWN`.
- The public API cannot be finalized until follow-up decisions stabilize its
  required inputs and evidence references.
- Deterministic retention and ordering add implementation and conformance-test
  obligations.

## Alternatives Considered

- **Bare three-value result without retained proof state:** rejected because it
  cannot be independently audited and hides why a value was justified.
- **Classification plus confidence score:** rejected because no probability or
  calibration semantics exist, and the product treats uncertainty as explicit
  evidence limitations rather than likelihood.
- **Expose only raw internal evidence state publicly:** rejected because every
  consumer could derive incompatible classifications. Rich state remains
  internal/supporting evidence for one authoritative public value.
- **Treat a missing graph path as `NOT_AFFECTED`:** rejected because path absence
  in open-world data cannot prove relevant completeness.
- **Repurpose remediation `ImpactAnalyzer`:** rejected because it consumes
  roadmap/execution inputs and owns expected remediation structure, not impact
  qualification from a verified change.

## Conformance Requirements

An implementation conforms only if all of these invariants hold:

- no constructor or factory can create successful `NOT_AFFECTED` evidence
  without every mandatory exclusion input;
- absence of a path never maps directly to `NOT_AFFECTED`;
- failures never become `UNKNOWN` conclusions;
- qualified positive evidence is never downgraded because exclusion
  completeness is missing;
- relevant conflicts and rejected proof reasons remain visible;
- output ordering is deterministic regardless of input map/set iteration;
- rule, policy, scope, and snapshot identities are retained;
- no implicit clock or live mutable source affects classification;
- unsupported rule versions are rejected rather than substituted;
- adversarial contract tests cover all scenarios in the decision analysis,
  especially incomplete, stale, conflicting, ambiguous, unsupported, and
  out-of-scope cases that could otherwise yield false `NOT_AFFECTED`.

## Follow-up Decisions

Separate ADRs are required for:

1. qualified engineering data and provenance;
2. analysis scope and completeness assertions;
3. stable external binding of `VerifiedChangeSet`;
4. identity ambiguity and source conflicts;
5. influence-rule catalog and versioning;
6. deterministic freshness policy;
7. public report and API contract;
8. module naming and boundary relative to remediation impact analysis.

No storage technology or graph-database assumption is selected by this ADR.

## References

- [Impact Evidence conclusion-semantics decision analysis](../impact-evidence-conclusion-semantics-decision-analysis.md)
- [Impact Evidence gap assessment](../impact-evidence-gap-assessment.md)
- [Implementation Architecture Recovery Review](../implementation-architecture-review.md)
- [Impact Evidence product specification](../../../README.ru.codex.md)
- [`VerifiedChangeSet.java`](../../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java)
- [`FinalChangeSetVerifier.java`](../../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/FinalChangeSetVerifier.java)
- [Canonical Change ADR-001: Exact-instance evidence binding](../../adr/ADR-001-exact-instance-evidence-binding.md)
- [`TraceEngine.java`](../../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)
- [`QaModelValidationEngine.java`](../../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/QaModelValidationEngine.java)
- [`ImpactAnalyzer.java`](../../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java)
- [`ImpactReport.java`](../../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/model/ImpactReport.java)
- [`RemediationImpactCatalog.java`](../../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/mapping/RemediationImpactCatalog.java)

