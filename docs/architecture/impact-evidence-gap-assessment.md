# Impact Evidence Architecture Gap Assessment

**Comparison baseline:** current repository implementation and the [Implementation Architecture Recovery Review](implementation-architecture-review.md)  
**Target baseline:** [`README.ru.codex.md`](../../README.ru.codex.md), corroborated by the longer [`README.ru.md`](../../README.ru.md)  
**Method:** code is authoritative for current behavior; the product specification is authoritative for target product behavior. Existing names are not treated as proof of capability.

## Executive Summary

### Overall implementation completeness

The repository contains strong **prerequisites** for Impact Evidence, but it does not yet implement the target product's defining use case.

Implemented foundations include a verified-change result (`VerifiedChangeSet`), canonical QA graph validation, graph traversal, deterministic ordering, immutable evidence chains, exact in-process Base evidence binding, fingerprints in simulation, and clear separation of findings/planning/simulation modules. These components establish trustworthy inputs and reusable graph mechanics.

The target product capability—accepting a confirmed change plus qualified engineering data and producing per-artifact `AFFECTED`, `NOT_AFFECTED`, or `UNKNOWN` conclusions with provenance, scope, freshness, completeness, conflicts, and explanations—is **missing**. No production enum, result type, analyzer, rule set, port, service, controller, or test contains the three target classifications. Repository-wide source search finds `UNKNOWN` only in unrelated error names and finds neither `AFFECTED` nor `NOT_AFFECTED` in production Java.

The existing `qa-impact-analysis` module is not an early implementation of that target algorithm. `ImpactAnalyzer.analyze(RoadmapReport, ExecutionPlan)` maps remediation tasks to expected structural changes using `RemediationImpactCatalog`. Its output `ImpactReport` contains task impacts, not qualified conclusions about artifacts. This is a **CONFLICT only if it is treated as the product's Impact Evidence implementation**. As a separate downstream planning/simulation input it is consistent and should not be replaced.

On a weighted capability basis, estimated target completeness is **approximately 25–35%**, almost entirely foundation and boundary work. The defining classification/evidence/report/API slice is effectively 0% implemented. This percentage is an assessment aid, not a code metric.

### Estimated product readiness

**Readiness: architecture-enabling prototype, not an Impact Evidence MVP.** The target public contract and domain model are explicitly described as work in progress in `README.ru.codex.md`, and the implementation has no product entry point. Before feature implementation can safely continue, the evidence input contract and classification semantics must be decided.

### Major architectural risks

1. **Semantic name collision:** `qa-impact-analysis` can be mistaken for Impact Evidence although it answers a different question and begins downstream of findings/roadmap/planning.
2. **No serializable provenance contract:** Canonical Change deliberately binds successful evidence by Java object identity. [`ADR-001-exact-instance-evidence-binding.md`](../adr/ADR-001-exact-instance-evidence-binding.md) states that evidence cannot currently be serialized/resumed safely.
3. **No qualified engineering-data model:** source snapshot, applicability scope, freshness, completeness assertions, relation semantics, identity resolution, and source conflict are absent as first-class inputs.
4. **False-negative risk if graph traversal is reused directly:** `TraceEngine` reports found/not-found paths, but the specification forbids interpreting “not found” as `NOT_AFFECTED` without bounded, complete evidence.
5. **Unstable target contract:** Domain Model and Public Contract are still work in progress according to the target document; premature API/type choices could encode incorrect semantics.

### Major architectural strengths

1. **Verified input boundary already exists:** `FinalChangeSetVerifier` produces immutable `VerifiedChangeSet` only after retained evidence consistency checks.
2. **Validation authority is centralized:** `QaModelValidationEngine` runs schema validation before semantic validation and returns deterministic issue ordering.
3. **Deterministic functional cores are the norm:** change, validation, coverage, planning, impact, and simulation are framework-independent libraries with extensive tests.
4. **Current downstream boundaries match the target exclusion boundary:** findings, roadmap, execution planning, and simulation are separate modules and need not be pulled into Impact Evidence.
5. **Evidence preservation is already an architectural concern:** sealed success/failure results retain prior-stage evidence throughout `qa-model-change`.

## Capability Matrix

Effort is relative engineering effort after the required ADRs are accepted: **S** (days), **M** (roughly 1–3 weeks), **L** (multiple coordinated increments), **XL** (cross-cutting product contract plus implementation). It is not a delivery estimate.

| Capability | Current implementation | Target design | Status | Required work | Risk | Effort |
|---|---|---|---|---|---|---|
| Confirmed change as analysis input | `qa-model-change` produces `VerifiedChangeSet` from staged verification | Impact Evidence starts with a Canonical Change confirmed change | IMPLEMENTED | Define a stable consumption boundary without weakening verification | Medium: current binding is process-local | M |
| Exact Base binding | `CanonicalBaseModelEvidence`, `BaseChangeVerifier`, and later stages require the exact evidence instance | Conclusions must be tied to the applicable model/snapshot | PARTIALLY IMPLEMENTED | Add stable cross-process evidence identity/fingerprint contract before external consumption | High | L |
| Canonical graph validation | `QaModelValidationEngine` validates JSON Schema then semantics | Engineering data must be checked before it supports conclusions | IMPLEMENTED | Reuse as model-validity prerequisite; do not equate validity with evidential sufficiency | Low | S |
| Relationship discovery | `TraceEngine`, traceability builders/analyzers, canonical relationships | Consume relationships from trace/dependency/knowledge sources | PARTIALLY IMPLEMENTED | Adapt discovered relations into qualified evidence; retain source and relation semantics | High | L |
| Qualified engineering-data input | Canonical nodes have `sourceReferences`; no source snapshot/scope/freshness/completeness contract is consumed by an analyzer | Every usable datum has source, snapshot, provenance, applicability, freshness and declared scope | MISSING | Stabilize domain contract and validators for evidence sources/snapshots/assertions | Critical | XL |
| Artifact identity resolution | Canonical IDs and `CanonicalIdentity`; change verification matches exact artifacts | Unresolved identity must yield a visible limitation/`UNKNOWN` | PARTIALLY IMPLEMENTED | Define cross-source identity evidence and unresolved/ambiguous outcomes | High | L |
| Relation influence semantics | Canonical `RelationshipType` and allowed-pair validation exist | Only predefined, applicable relation meanings may propagate impact | PARTIALLY IMPLEMENTED | Specify an influence semantics catalog separate from structural legality | High | L |
| Evidence applicability/scope | No analysis-scope object or applicability rule exists | Every conclusion is bounded to declared artifact/relation/time scope | MISSING | Define scope model, containment/matching rules, and validation | Critical | L |
| Freshness assessment | No source timestamp/freshness policy in current impact path | Stale source must constrain the conclusion and can cause `UNKNOWN` | MISSING | Define snapshot time and deterministic freshness policy inputs; never use wall clock implicitly | High | M |
| Completeness assertions | Coverage percentages exist, but they are measured graph coverage, not source completeness claims | `NOT_AFFECTED` requires explicit relevant completeness within bounded scope | MISSING | Model qualified completeness claims and conservative evaluation rules | Critical | L |
| Cross-source conflict detection | Duplicate/invalid graph facts are validated; no multi-source contradiction model exists | Source conflicts remain visible and may produce `UNKNOWN` | MISSING | Define conflict identity, precedence/non-precedence policy, diagnostics and tests | High | L |
| `AFFECTED` classification | No production type or rule emits it | Direct verified change or applicable qualified path proves influence | MISSING | Define result algebra and positive propagation rules over qualified evidence | Critical | L |
| `NOT_AFFECTED` classification | No production type or rule emits it | Positive exclusion only inside explicit, current and complete scope | MISSING | Define strict exclusion proof rules and adversarial false-negative tests | Critical | L |
| `UNKNOWN` as valid result | Existing “unknown” names are errors (unknown node/task), not impact conclusions | Insufficient, stale, conflicting, unresolved or out-of-scope data yields a normal conclusion | MISSING | Encode reason taxonomy and non-error outcome behavior | Critical | M |
| Per-conclusion explanation | Validation/change diagnostics and trace paths exist; `TaskImpact` explains expected remediation structure | Each classification retains its basis or exact limitation | PARTIALLY IMPLEMENTED | Reuse diagnostic/result conventions; add qualified evidence references and reason types | High | L |
| Per-conclusion provenance | Change stages retain in-process evidence; `TaskImpact` retains finding/task lineage only | Every conclusion can be traced back through the engineering data used | PARTIALLY IMPLEMENTED | Create immutable serializable evidence references and lineage graph/list | Critical | XL |
| Reproducible analysis context | Deterministic algorithms/fingerprints exist; reports can contain clocks/UUIDs and change evidence is process-local | Same change, data snapshots and rules reproduce conclusions; old report remains independently auditable | PARTIALLY IMPLEMENTED | Version rule set; persist input identities/hashes/scope and deterministic policy parameters | High | L |
| Impact Evidence report | `ImpactReport` reports remediation task impacts and structural expectations | Report contains change, context, per-artifact classifications, bases/limitations | CONFLICT | Do not repurpose current DTO; define a separately named target report and clarify module naming | Critical if consumers conflate contracts | M |
| Impact qualification engine | `ImpactAnalyzer` maps roadmap tasks via a static catalog | Qualifies what conclusions are supportable from confirmed change + engineering evidence | CONFLICT | Keep current analyzer for remediation; implement a separate classification engine after contracts stabilize | Critical | XL |
| Public product API | No controller consumes `VerifiedChangeSet` or emits classifications; Public Contract marked WIP | Stable interface for target analysis/report, transport-neutral first | MISSING | Write public-contract ADR/spec, then add library API and optional REST adapter | High | L |
| Storage independence | Most cores are pure libraries; model registration is in-memory and concrete | Product must not become a graph store and must accept existing source data | PARTIALLY IMPLEMENTED | Define source/evidence ports without embedding storage technology; do not reuse model repository as product store | Medium | M |
| No recommendations/decisions | Current `qa-impact-analysis` produces expectations used by simulation; roadmap creates remediation tasks | Impact Evidence stops at evidence conclusions; decisions/planning remain downstream | PARTIALLY IMPLEMENTED | Preserve module separation and explicitly prevent dependencies from classifier to findings/roadmap/execution/simulation | High | S plus architecture tests |
| Deterministic classification | Sorting, immutable results, catalogs and fingerprints demonstrate deterministic practice | Identical change/data/rules produce identical conclusions | PARTIALLY IMPLEMENTED | Define deterministic rule ordering, stable report ordering and versioned rule identity | Medium | M |
| Independent auditability | Change evidence is rich but process-local; no target report persists all context | A report can be independently checked later | PARTIALLY IMPLEMENTED | Stable evidence IDs/hashes and self-describing rule/scope/snapshot metadata | Critical | XL |

## Domain Comparison

| Target concept | Existing concept(s) | Relationship | Assessment |
|---|---|---|---|
| Confirmed Change | `VerifiedChangeSet` | Equivalent within the in-process Canonical Change boundary | Semantically aligned; transport/persistence identity is missing |
| Changed Artifact | `DeclaredChange`, `CanonicalIdentity`, `ArtifactState` | Extended prerequisite | Existing types carry precise Base/proposed state and change kind, more detail than the product summary requires |
| Engineering Artifact | canonical QA node represented by `JsonNode` and `NodeType` | Partially equivalent | Artifact IDs/types exist, but evidence-source applicability and cross-source identity do not |
| Discovered Relationship | canonical relationship JSON, `RelationshipType`, `TraceRelationship` | Equivalent as a discovered fact | It is not yet qualified as influence evidence |
| Qualified Relationship/Evidence Datum | none; `sourceReferences` is only a field validated for existence in some cases | Missing | No source snapshot, freshness, scope, provenance chain, or qualification state |
| Evidence Source | source JSON objects and source references | Extended but insufficient | Canonical source data exists; no typed source capability/completeness/freshness assertions exist |
| Source Snapshot | simulation model fingerprint; repository creation timestamp | Missing for target semantics | Neither identifies the engineering-data snapshot used per conclusion |
| Analysis Scope | none | Missing | No bounded artifact/relation/time applicability object |
| Completeness Claim | coverage metrics | Conflicting if treated as equivalent | Coverage percentages measure connected QA artifacts; they are not proof that a source is complete for an exclusion claim |
| Influence Semantics Rule | allowed relationship matrix and remediation catalog | Extended but not equivalent | Structural legality and remediation mapping do not define evidential propagation semantics |
| Impact Conclusion | `TaskImpact` | Conflicting | `TaskImpact` describes expected remediation structure, not impact classification of an artifact |
| Classification | none | Missing | No target three-state algebra exists |
| Positive Evidence Chain | `TracePath`, change evidence chain | Partially equivalent | Paths and retained verification evidence exist, but trace edges lack target qualification/context |
| Exclusion Evidence | none | Missing | No positive proof of non-impact or bounded complete-world evaluation exists |
| Unknown Reason/Constraint | validation/change diagnostic codes | Pattern equivalent; domain missing | Diagnostic modeling can be reused, but target reasons are absent |
| Analysis Context | `AnalysisContext` in `qaip-core` (`analysisId`, `requestedAt`, `attributes`) | Extended but insufficient | Generic metadata does not bind verified change, source snapshots, scope, rules or completeness claims |
| Impact Evidence Report | `ImpactReport` | Conflicting | Same broad name, different input, semantics and output |
| Downstream Decision | roadmap/execution/simulation outputs | Correctly separate | These must remain consumers or adjacent capabilities, not enter classification semantics |

No target domain event is required by the product specification, and none exists in code. Event introduction is therefore not identified as a gap.

## API Comparison

### Existing relevant APIs

- `FinalChangeSetVerifier.verify(CompleteProposedRootValidationResult)` returns `VerifiedChangeSet` or `RejectedChangeSet`; it is a library API, not REST.
- `ImpactAnalyzer.analyze(RoadmapReport, ExecutionPlan)` returns remediation-oriented `ImpactReport`; it has no controller.
- `POST /api/v1/coverage` and registered-model endpoints expose validation, tracing, coverage, findings, assessment, roadmap and execution planning.
- `POST /api/v1/simulation` consumes current model, remediation `ImpactReport`, and explicit materializations; it returns a validated candidate model.

### Required product API

The target documents require a product result but explicitly state that **Public Contract is still in progress**. Therefore exact HTTP paths, request DTO names, status codes, sync/async behavior and persistence semantics cannot be compared without speculation.

The minimum semantic contract implied by the specification is nevertheless clear:

```text
Input:
  confirmed change
  qualified engineering-data snapshot(s)
  explicit analysis scope
  versioned influence/qualification rules

Output:
  analysis context
  per-artifact AFFECTED | NOT_AFFECTED | UNKNOWN
  evidence basis or limitation reason
  provenance/snapshot/scope/rule references
```

No current authored REST or library method satisfies this semantic signature. The correct status for the product API is **MISSING**, not conflict, because no stable required transport shape has yet been specified. `ImpactAnalyzer` and `POST /api/v1/simulation` must not be renamed or relabeled as that API: their inputs begin at remediation planning and their outputs describe candidate construction.

## Validation Comparison

### Existing validation semantics

`QaModelValidationEngine` establishes that a canonical model is structurally and semantically coherent. It short-circuits semantic validation when schema validation fails. `SemanticValidationRules.defaults()` then checks duplicate identities, missing references, illegal/self/duplicate relationships, step order and missing traceability links. Canonical Change adds intrinsic declaration, Base truth, materialization, aggregate, complete-model and final evidence-consistency checks.

These are strong prerequisites, but they validate **model correctness and transition correctness**.

### Target validation semantics

Impact Evidence additionally requires validation of **evidential fitness for a specific conclusion**:

- Is the source known and the datum's provenance recoverable?
- Does the source snapshot apply to the changed/model snapshot?
- Is the datum fresh under an explicit deterministic policy?
- Is the relationship type permitted to propagate influence in this direction?
- Does the declared scope include the subject artifact and relevant relation types?
- Is a completeness claim valid for this category, relation set and time?
- Do sources conflict or artifact identities remain unresolved?
- Is positive evidence sufficient for `AFFECTED`?
- Is positive exclusion evidence sufficient for `NOT_AFFECTED`?
- Otherwise, which `UNKNOWN` reason applies?

None of those target predicates is implemented. Existing missing-link semantic rules often emit warnings/errors such as scenario-without-test; they do not and must not silently serve as impact classifications. Likewise, `TracePath.found() == false` proves only that the current graph search found no path.

Alignment status is therefore **PARTIALLY IMPLEMENTED** for the overall validation foundation and **MISSING** for evidence qualification/classification validation.

## Evidence Model Comparison

### What the repository already models well

Canonical Change retains a precise evidence chain. Successful objects carry prior results; `FinalChangeSetVerifier` checks declaration binding, exact Base evidence identity, reconstructed root equality, schema version agreement and complete validation evidence. This prevents stale/substituted in-process evidence from advancing. Simulation adds deterministic SHA-style model fingerprinting and retains base/future fingerprints, applied materializations and validation evidence in `SimulationResult`.

### What the target evidence model requires

The product specification uses “evidence” at a broader boundary: engineering facts from one or more external trace/dependency/knowledge sources must be qualified, scoped and retained for each conclusion. The report must explain both what supported a conclusion and what prevented a stronger one.

| Evidence property | Current state | Gap |
|---|---|---|
| Verified change lineage | rich, exact in-process identity | needs stable external reference for product consumption |
| Data source identity | canonical source IDs/references | lacks source authority/type/capability contract |
| Snapshot identity | model fingerprints only in simulation | no engineering-source snapshot binding per conclusion |
| Provenance | retained within change stages; task/finding lineage downstream | no datum-to-conclusion provenance |
| Scope | implicit whole JSON model | no explicit bounded claim scope |
| Freshness | absent | no timestamps/policy/reference time |
| Completeness | absent; coverage is a different metric | no qualified closed-world assertion |
| Consistency/conflict | model duplicates/illegal edges rejected | no multi-source contradiction evidence |
| Positive influence basis | paths can be returned | paths are not qualified for influence |
| Positive exclusion basis | absent | required for `NOT_AFFECTED` |
| Limitation reason | rich diagnostics in other domains | no target `UNKNOWN` reason taxonomy |
| Rule version | schema version exists | no influence/qualification rule-set version |
| Independent replay/audit | tests and deterministic cores; evidence process-local | report cannot reconstruct complete target analysis context |

The existing evidence approach should be **extended**, not discarded. Its sealed outcomes, immutable retained inputs, defensive copying, diagnostic codes and consistency gates are directly aligned patterns. Exact Java-instance identity, however, cannot cross a REST/persistence boundary and must not be presented as sufficient target provenance.

## Missing Architectural Decisions

### Priority 0 — blocking semantic correctness

1. **ADR: Impact conclusion semantics and proof obligations.** Define exactly what proves `AFFECTED`, what positive evidence permits `NOT_AFFECTED`, and when `UNKNOWN` is mandatory. Without this, false negative behavior cannot be tested.
2. **ADR: Qualified engineering-data and provenance contract.** Define source, datum, snapshot, lineage, integrity reference and applicability without choosing a storage engine.
3. **ADR: Scope and completeness assertion semantics.** Define the artifact categories, relation types, direction, time and snapshot to which a completeness claim applies.
4. **ADR: Stable binding from `VerifiedChangeSet` to external analysis.** Decide fingerprint/evidence ID/signed token semantics to replace process identity at the boundary while preserving anti-substitution guarantees.
5. **ADR: Identity ambiguity and source-conflict handling.** Define conservative outcomes and whether any precedence is allowed; default behavior must not hide conflict.

### Priority 1 — blocking public implementation

6. **ADR: Influence-semantics rule catalog and versioning.** Separate graph structural legality from evidential propagation meaning and define directionality.
7. **ADR: Deterministic freshness policy.** Define explicit reference time and policy inputs so results do not depend on an implicit wall clock.
8. **ADR: Impact Evidence report/public contract.** Define immutable result algebra, evidence references, limitations, ordering and compatibility/version guarantees before REST mapping.
9. **ADR: Module boundary and naming relative to `qa-impact-analysis`.** Preserve remediation impact behavior while preventing semantic conflation with the new classifier.

### Priority 2 — operational continuation

10. **ADR: Replay, persistence and audit boundary.** Decide what inputs/results must be serialized for independent reproduction; do not assume a graph database.
11. **ADR: Integration ports for trace/dependency/knowledge sources.** Define pull/push and snapshot ingestion only after the qualified-data contract exists.
12. **ADR: Failure versus valid uncertainty.** Separate malformed/untrusted input failures from valid `UNKNOWN` conclusions in library and transport contracts.

## Refactoring Opportunities

Only alignment-driven changes are justified now:

1. **Clarify remediation impact naming and documentation.** Keep `ImpactAnalyzer`, `ImpactReport`, and simulation behavior intact, but document them as expected remediation impact. If public compatibility permits later, use a more specific package/report name. Justification: their actual inputs are `RoadmapReport` and `ExecutionPlan`, not a confirmed change.
2. **Introduce a stable verified-change reference adjacent to Canonical Change.** Extend the existing evidence boundary with a canonical fingerprint/ID after ADR approval; do not weaken exact-instance checks inside the current process. Justification: the accepted ADR explicitly identifies serialization as unsupported.
3. **Extract graph traversal as a neutral reusable library only when the classifier needs it.** Reuse `TraceEngine` mechanics while keeping evidence qualification outside traversal. Justification: traversal is already tested, but found/not-found semantics are insufficient.
4. **Add architecture tests for prohibited dependencies.** The future classifier must not depend on findings, roadmap, execution planner or simulation. Justification: the specification stops before decisions, and current module separation already supports that boundary.
5. **Reuse immutable result/diagnostic conventions.** Follow sealed outcomes and immutable records from `qa-model-change`; do not duplicate its verification rules. Justification: this increases evidence integrity without replacing working code.
6. **Separate source completeness from QA coverage terminology.** Do not reuse `CoverageMetric` for completeness assertions. Justification: `CoverageService` measures rule/scenario/check connection ratios, which cannot prove source completeness.
7. **Version rule inputs explicitly.** Extend the existing schema/version discipline to influence and qualification rule sets. Justification: reproducibility requires more than current `schemaVersion`.

The following are **not justified** by the target specification: replacing JSON Schema validation, replacing Canonical Change, introducing event sourcing, adopting a graph database, merging Boot applications, or moving findings/planning into the classifier.

## Final Recommendation

**Evolve incrementally. Do not replace any working subsystem. Add the Impact Evidence classification capability as a new, narrowly bounded deterministic core after the Priority 0 ADRs are accepted.**

Implementation evidence supports this conclusion:

- `qa-model-change` already provides the exact confirmed-change prerequisite and strong evidence integrity; replacement would discard verified behavior and release-gate tests.
- `QaModelValidationEngine` and trace components already provide model validity and traversal mechanics; they require qualification adapters/rules, not replacement.
- `qa-impact-analysis` is internally coherent for remediation: `RemediationImpactCatalog` maps three `RemediationTaskType` values to expected structural changes, and `ModelSimulationEngine` consumes that report with explicit materializations. Replacing it would damage a separate working pipeline.
- No existing class implements the target three-state classification, so attempting to “refactor” an existing analyzer into it would change the analyzer's business responsibility and break current consumers. A separate core avoids that conflict.
- Existing one-way module dependencies and framework-independent libraries make incremental addition natural: the new core can depend on stable Canonical Change and neutral evidence/traversal contracts while remaining independent of decision/planning modules.

The only subsystem-level correction required before implementation is **semantic separation**, not replacement: current remediation-oriented impact analysis must not be advertised or exposed as Impact Evidence. Once the target domain/public contracts are stable, build a thin vertical slice that accepts one `VerifiedChangeSet`, one qualified snapshot and explicit scope, and proves all three classifications through adversarial tests—especially that absent paths never become `NOT_AFFECTED` without positive bounded completeness evidence.

## Implementation Evidence Index

- target principles and boundaries: [`README.ru.codex.md`](../../README.ru.codex.md), [`README.ru.md`](../../README.ru.md)
- recovered implementation baseline: [Implementation Architecture Recovery Review](implementation-architecture-review.md)
- confirmed-change output: [`VerifiedChangeSet.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java), [`FinalChangeSetVerifier.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/FinalChangeSetVerifier.java)
- current provenance limitation: [`ADR-001-exact-instance-evidence-binding.md`](../adr/ADR-001-exact-instance-evidence-binding.md)
- validation semantics: [`QaModelValidationEngine.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/QaModelValidationEngine.java), [`SemanticValidationRules.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/validation/semantic/SemanticValidationRules.java)
- current remediation impact: [`ImpactAnalyzer.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java), [`ImpactReport.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/model/ImpactReport.java), [`TaskImpact.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/model/TaskImpact.java), [`RemediationImpactCatalog.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/mapping/RemediationImpactCatalog.java)
- downstream simulation: [`SimulationRequest.java`](../../qa-model-simulation-api/src/main/java/ru/kuznetsov/qaip/simulation/api/SimulationRequest.java), [`ModelSimulationEngine.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/ModelSimulationEngine.java)
- graph traversal: [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)

