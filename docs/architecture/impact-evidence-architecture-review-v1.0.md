# Impact Evidence Architecture Review v1.0

**Status:** Current architecture review  
**Audience:** senior engineers, architects, technical leadership, and future contributors  
**Scope:** implemented QA platform plus the decided architecture for the emerging Impact Evidence capability  
**Decision authority:** [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md) and [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)

This document is a consolidated architectural view. It does not replace the product vision, detailed decision analyses, accepted ADRs, or source code. Source remains authoritative for implemented behavior; accepted ADRs govern future Impact Evidence implementation.

## 1. Executive Summary

The repository already implements a modular Java 21 platform for modeling and analyzing QA knowledge. Its implemented flow begins with the Canonical QA Model, validates model structure and semantics, verifies declared model changes, measures QA coverage and traceability, derives findings, produces remediation roadmaps and execution waves, describes expected remediation impact, and simulates explicitly materialized future models. Most decision logic is implemented in framework-independent library modules; Spring Boot applications provide selected HTTP boundaries. The complete recovered view is documented in the [Implementation Architecture Recovery Review](implementation-architecture-review.md).

Impact Evidence adds a new responsibility between verified change/data sources and downstream engineering decisions: it determines which impact conclusions are justified by available evidence. Its public result is not a dependency list, remediation plan, risk score, or simulation. It is a context-bound conclusion for an engineering artifact: `AFFECTED`, `NOT_AFFECTED`, or `UNKNOWN`, accompanied by auditable proof or limitations.

The new capability does not yet exist in production code. What exists is a strong foundation and two accepted governing decisions:

- [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md) fixes classification meaning, proof obligations, precedence, determinism, and the boundary between failure and valid uncertainty.
- [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md) fixes the normalized immutable evidence-snapshot boundary, integrity identities, provenance, qualification outcomes, adapters, derivation, and replay requirements.

The architecture evolves incrementally because the current modules solve coherent, tested problems and already supply important prerequisites. `VerifiedChangeSet` is the confirmed-change basis; validation and traversal can be reused within their existing meanings; immutable records and sealed outcomes provide proven implementation patterns. Replacing these components would add risk without addressing the actual gap. The missing work is a separate evidence qualification and proof capability, not a replacement platform.

## 2. Current Platform Architecture

The current platform is a modular monorepo with an acyclic Gradle dependency graph. Libraries own deterministic domain behavior; deployable Spring applications adapt selected behavior to HTTP. The platform's implemented capabilities are:

| Capability | Implemented responsibility | Principal evidence |
|---|---|---|
| Canonical QA Model | Defines the normalized JSON contract for projects, sources, typed nodes, and typed relationships | `qa-model` schema and shared model types |
| Validation | Establishes whether a complete Canonical QA document is structurally and semantically valid; schema validation precedes semantic rules | `QaModelValidationEngine`, `SemanticValidationRules` in `qa-model-validation-core` |
| Canonical Change | Verifies declared changes against exact Base evidence, atomically materializes the proposed model, validates the complete root, and preserves stage evidence | `VerifiedChangeSet`, `FinalChangeSetVerifier`, staged results in `qa-model-change` |
| Coverage | Measures rule, scenario, and check coverage and analyzes traceability chains/breaks over a validated model | `CoverageService`, coverage analyzers, traceability services in `qa-coverage-engine` |
| Findings | Converts validated coverage problems into deterministic actionable findings | `FindingsService` in `qa-findings-engine` |
| Roadmap | Converts supported findings into explicit remediation tasks and dependencies | `RoadmapService` in `qa-roadmap-engine` |
| Execution Planning | Validates task dependencies and groups tasks into deterministic execution waves | `ExecutionPlanner` in `qa-execution-planner` |
| Remediation Impact | Maps roadmap tasks and execution waves to expected structural remediation changes | `ImpactAnalyzer`, `ImpactReport`, `RemediationImpactCatalog` in `qa-impact-analysis` |
| Simulation | Applies explicit task materializations to a current model, fingerprints Base/future models, and validates the candidate | `ModelSimulationEngine`, `SimulationResult` in `qa-model-simulation` |

These capabilities answer different questions. Validation answers whether a model is valid. Trace and coverage answer what is connected or missing in the modeled QA structure. Findings and roadmap answer what remediation work follows. Execution planning answers in what dependency-safe order work can run. Remediation impact describes expected structural changes from that work. Simulation answers whether a supplied future materialization produces a valid candidate.

None answers the Impact Evidence question: what impact conclusion is justified for an artifact given a verified change and qualified, bounded engineering evidence. The [gap assessment](impact-evidence-gap-assessment.md) establishes this distinction from code.

## 3. Position of Impact Evidence

Impact Evidence sits after confirmed change and immutable engineering-data capture, and before planning, risk, release, audit, or other decision consumers.

```text
Canonical Change                         Engineering source systems
      │                                           │
      │ accepted VerifiedChangeSet                │ raw source payloads
      │                                           ▼
      │                                  Capture + normalization adapters
      │                                           │
      │                                           ▼
      │                              Immutable normalized evidence snapshots
      │                                           │
      └───────────────────────────┬───────────────┘
                                  ▼
                         Evidence contract validation
                                  │
                                  ▼
                         Contextual qualification
                                  │
                                  ▼
                    Qualified/rejected evidence structure
                    (relationships + provenance lineage)
                                  │
                                  ▼
                      Derived proof candidates / paths
                                  │
                                  ▼
                    ADR-005 proof engine and precedence
                                  │
                                  ▼
                 AFFECTED | NOT_AFFECTED | UNKNOWN
                    with proofs, reasons, and context
                                  │
                                  ▼
                    External downstream consumers
```

The stages have deliberately narrow meanings:

- **Verified change:** Canonical Change establishes which artifact changes are accepted and retains their Base/proposed evidence. Impact Evidence does not repeat that verification.
- **Capture and normalization:** source-specific adapters freeze external data and transform it into the vendor-neutral contract. They do not classify impact.
- **Immutable snapshots:** each snapshot and datum is bound to source, versioned normalization/canonicalization rules, semantic content fingerprints, identity assertions, relationships, and provenance.
- **Contract validation:** rejects structurally invalid bundles, unsupported contract versions, missing identities, or integrity mismatches before semantic reasoning.
- **Qualification:** evaluates whether well-formed evidence is applicable and usable for this exact analysis context. Unresolved, conflicting, stale, incompatible, unsupported, or untrusted evidence remains visible.
- **Evidence structure:** qualified and rejected facts plus immutable provenance references form an auditable structure. This is a conceptual evidence/provenance graph; no graph database is implied.
- **Derived proof candidates:** traversal and identity resolution create context-bound paths or later exclusion evaluations while retaining every required parent and rejected limitation.
- **Proof engine:** applies ADR-005. A valid direct change or qualified path proves `AFFECTED`; only complete bounded positive exclusion evidence may prove `NOT_AFFECTED`; otherwise the valid result is `UNKNOWN`.
- **Conclusions:** immutable artifact-within-context results preserve the classification, proof basis or reasons, snapshots, scope/rules, and provenance needed by external consumers.

Impact Evidence stops at the conclusion. It does not select tests, create remediation tasks, calculate risk, decide release readiness, or mutate/simulate a future model.

## 4. Architectural Principles

### Deterministic core

Classification must be a function of explicit frozen inputs: verified-change reference, source/snapshot identities, subject, scope, completeness assertions, policies, rule versions, and deterministic reference time. Implicit clocks, live reads, random IDs, and collection iteration order cannot affect semantics. This protects replay and makes proof behavior testable.

### Immutable evidence

Snapshots, normalized data, provenance records, qualification outcomes, and derived evidence are immutable. Corrections create new identities/fingerprints rather than rewriting earlier evidence. This prevents a later state from silently changing an earlier conclusion.

### Explicit provenance

Every usable or rejected datum retains its source/snapshot origin, normalization or derivation rule, integrity reference, and required parents. A conclusion can therefore be audited back to the facts and transformations that produced it.

### Replayability

Replay means evaluating the same frozen semantic inputs, not repeating a live query. A new source response is a new snapshot. Raw bytes may be embedded or held externally, but normalized evidence and integrity-bound locators/hashes must make the audit limitation explicit.

### Conservative reasoning

Positive influence is existential; one sound proof is sufficient. Non-impact is a stronger closed-boundary assertion and requires complete positive exclusion evidence. No path, missing artifact, high coverage, valid model, or one negative source report can independently prove `NOT_AFFECTED`.

### Explicit uncertainty

`UNKNOWN` is a successful conclusion, not an error or confidence score. Missing completeness, stale evidence, conflicts, unresolved identity, unsupported semantics, incompatible snapshots, and other limitations remain visible and deterministically ordered.

### Separation of capture, normalization, and reasoning

Adapters own vendor transport and raw mapping. Normalization produces a vendor-neutral immutable contract. Qualification and proof remain in the classifier. This prevents source-specific behavior or mutable external state from entering semantic rules.

### Framework-independent domain logic

The repository's strongest implemented cores are plain Java libraries. The classifier should follow this existing boundary: domain evidence/proof semantics remain independent of Spring, HTTP, persistence, brokers, or source SDKs. Transport and operations can be added around a stable core.

### Product contracts before implementation

False `NOT_AFFECTED` results are materially harmful and difficult to correct after clients depend on them. The project therefore resolves proof, evidence, scope, completeness, identity/conflict, influence, and public-contract semantics before exposing implementation. The accepted ADR sequence is a risk-control mechanism, not documentation ceremony.

## 5. What Has Already Been Decided

### Closed by ADR-005

- The classified subject is an engineering artifact within an immutable analysis context, represented by a dedicated conclusion.
- The public classification has exactly three mutually exclusive values.
- Rich internal proof/rejection/limitation state remains retained.
- `AFFECTED` is established by exact direct change or at least one fully qualified influence path.
- `NOT_AFFECTED` requires all mandatory bounded exclusion obligations.
- `UNKNOWN` represents successful but limited knowledge and may retain multiple reasons.
- Request/change/version/internal failures remain outside the conclusion algebra.
- Proof precedence and deterministic semantic inputs are fixed.
- Remediation `ImpactAnalyzer`, coverage, missing paths, and simulation are not classifiers.

Implementation is therefore constrained against confidence scores, mutable artifact classifications, path-absence shortcuts, swallowed conflicts, or failure-to-`UNKNOWN` conversion.

### Closed by ADR-006

- The core consumes normalized immutable evidence snapshots only.
- Raw capture and normalization occur through adapters before classification; live external queries are prohibited during evaluation.
- Snapshot and datum identities include semantic fingerprints and versioned canonicalization/normalization information; timestamps/UUIDs alone are insufficient.
- Artifact identity resolution is categorical, including unresolved and ambiguous states, without probabilistic confidence.
- Relationship evidence retains native and normalized semantics, direction, endpoints, versions, provenance, integrity, and optional polarity; normalization does not establish influence.
- Immutable hybrid provenance records form an auditable DAG contract without choosing graph infrastructure.
- Qualification uses evidence-bearing outcomes and separates processing failures from well-formed limitations.
- Derived evidence retains parent lineage and cannot silently exceed required parent strength.
- Replay uses frozen bundles/manifests, not live re-query.
- The first vertical slice cannot emit `NOT_AFFECTED`.

Implementation is therefore constrained against accepting raw vendor DTOs in the core, using mutable live data, collapsing provenance to source IDs, discarding rejected evidence, or choosing infrastructure as part of the domain contract.

## 6. What Has NOT Been Decided Yet

| Open architectural decision | Why it remains open |
|---|---|
| Source capability and trust vocabulary | ADR-006 requires declarations/policy references but intentionally does not define which capabilities or authority classes exist |
| Stable external `VerifiedChangeSet` identity | current Canonical Change uses exact in-process evidence identity; cross-process anti-substitution needs a separate stable contract |
| Analysis scope | the artifact categories, relationship categories/directions, source set, snapshots, and temporal boundary need formal representation |
| Completeness assertions | `NOT_AFFECTED` depends on qualified bounded completeness, including possible multi-source composition; QA coverage is explicitly not equivalent |
| Freshness | captured/effective time fields are available conceptually, but thresholds, reference-time supply, and policy vocabulary are undecided |
| Influence semantics | canonical/normalized relationship type and structural legality do not establish impact propagation direction or meaning |
| Identity ambiguity and source conflicts | representation is decided; relevance, authority, precedence/non-precedence, and conservative resolution rules are not |
| Canonical evidence serialization | fingerprints require exact Unicode, time, scalar, default-field, collection-order, and algorithm-version rules |
| Raw-payload custody | the contract permits embedded or external custody; retention, confidentiality, redaction, and locator durability are governance decisions |
| Public report and API | conclusion semantics are stable, but packaging, versioning, errors, transport mapping, evidence size, and compatibility are not |
| Classifier module name/boundary | it must remain separate from remediation impact, but its concrete Gradle/API boundary has not been selected |

These are intentional decision gaps. Choosing classes, endpoints, storage, or transports before resolving them would hard-code semantics the accepted ADRs explicitly defer.

## 7. Stable Architecture

### Stable

- Canonical QA Model v0.1 as the implemented normalized QA graph contract.
- Schema-first and semantic validation authority in `qa-model-validation-core`.
- Canonical Change's staged verification and `VerifiedChangeSet` as confirmed-change foundation.
- Existing coverage, findings, roadmap, execution-planning, remediation-impact, and simulation responsibilities.
- One-way modular dependencies and framework-independent deterministic libraries.
- Impact Evidence's three-valued public semantics and proof precedence from ADR-005.
- Normalized immutable snapshot boundary, integrity-bound identities, provenance, qualification-result algebra, and replay model from ADR-006.
- Separation between impact conclusions and downstream decisions/remediation.

These foundations should change only through explicit replacement ADRs with compatibility analysis.

### Expected Evolution

- Evidence source/capability and trust vocabularies.
- Stable cross-process change/snapshot/provenance references.
- Scope, completeness, freshness, identity-conflict, and influence rule catalogs.
- Concrete conceptual-domain-to-Java mapping and validation rules.
- Evidence-aware path/proof engine built around, rather than confused with, existing traversal.
- Report packaging, evidence referencing/truncation, API mapping, and operational adapters.
- Additional source normalizers and artifact/relationship vocabularies.
- Later support for bounded `NOT_AFFECTED` once exclusion prerequisites exist.

These areas are expected to evolve through versioned contracts. Their extension points are deliberate, but compatibility rules are still to be decided.

## 8. Architectural Risks

| Rank | Risk | Probability | Impact | Mitigation |
|---:|---|---|---|---|
| 1 | False `NOT_AFFECTED` caused by incomplete/open-world data | Medium without enforcement | Critical | Enforce ADR-005 construction invariants; prohibit exclusion until scope/completeness ADR and adversarial tests are accepted |
| 2 | Provenance or snapshot substitution across process boundaries | High when external integration begins | Critical | Decide stable `VerifiedChangeSet` binding and canonical evidence serialization; validate every source/snapshot/datum/provenance fingerprint/reference |
| 3 | Semantic conflation with remediation `ImpactAnalyzer` | Medium | High | Maintain separate module/API vocabulary and dependency boundary; architecture tests forbid classifier dependency on roadmap/execution/simulation |
| 4 | Scope/completeness model too weak for defensible exclusion | Medium | High | Complete dedicated decision analysis/ADR before `NOT_AFFECTED`; test multi-source gaps, categories, directions, snapshots, and time boundaries |
| 5 | Identity ambiguity or conflicts hidden by normalization | Medium | High | Preserve categorical unresolved/ambiguous/conflicting outcomes and original native identities; decide relevance/precedence explicitly |
| 6 | Nondeterminism from time, ordering, or mutable source reads | Medium | High | Explicit reference time, frozen snapshots, canonical serialization, stable comparators, repeatability tests |
| 7 | Influence catalog encodes unsupported causal assumptions | Medium | High | Separate normalization from influence; version rules and require evidence-backed direction semantics with conservative unsupported outcomes |
| 8 | Evidence/provenance payload growth harms usability | High as sources expand | Medium | Decide reference packaging and deterministic truncation/indexing without discarding audit lineage; keep raw custody separate |
| 9 | Premature public API freezes unstable internal concepts | Medium | High | Complete semantic ADRs and vertical-slice domain validation before external API commitment |
| 10 | Operational controls lag behind a sound domain core | Medium | Medium/High | Add auth, limits, audit, observability, custody and deployment decisions only after stable public contract, without leaking them into core semantics |

## 9. Implementation Roadmap

No dates are implied. The order is dependency-driven.

1. **Complete remaining ADRs.** Resolve stable change binding, source capabilities/trust, scope/completeness, freshness, identity/conflicts, influence semantics, canonical serialization, raw custody, public contract, and module boundary. These choices define what valid implementation can mean.
2. **Define the domain model.** Translate accepted conceptual contracts into immutable domain types and validators. Keep final transport DTOs separate. Make illegal successful evidence—especially exclusion evidence—difficult to construct.
3. **Build the minimal vertical slice.** Support one verified changed artifact, one source, one immutable snapshot, one resolved relationship, direct/propagated `AFFECTED`, and `UNKNOWN` for unresolved/unqualified evidence. Do not implement `NOT_AFFECTED`.
4. **Implement the deterministic classifier core.** Add context qualification, evidence-bearing outcomes, derived path proofs, ADR-005 precedence, stable ordering, replay fixtures, and failure separation in a framework-independent module.
5. **Expand evidence support.** Add multiple relationships, paths, source adapters/normalizers, provenance lineage, ambiguous identities, rejected parents, and conflict inputs according to accepted policies.
6. **Add `NOT_AFFECTED` support.** Only after scope/completeness/freshness/conflict/influence decisions and canonical construction invariants exist. Use adversarial tests that attempt every known false-exclusion shortcut.
7. **Stabilize the public report and API.** Map the proven library contract to a versioned external report/error boundary. Preserve evidence references, reasons, rule/snapshot/scope identities, and deterministic ordering.
8. **Add operational concerns.** Introduce authentication/authorization, request limits, audit, observability, raw evidence custody, lifecycle, health, and deployment configuration around the established semantics.

This sequence minimizes risk by testing the hardest semantic boundaries with the smallest evidence set before multi-source scale, exclusion, public compatibility, or operations multiply change cost.

## 10. Architecture Maturity

| Area | Maturity | Justification |
|---|---|---|
| Existing deterministic platform core | Architecture Stable | Implemented modular libraries, acyclic dependencies, immutable result contracts, extensive tests, centralized validation, and released Canonical Change behavior |
| Existing runtime/platform operations | Prototype | in-memory model registration, separate Boot applications, inconsistent errors, and no production security/persistence/operability contract |
| Impact Evidence product semantics | Architecture Stable for ADR-005/006 scope | classification and evidence/provenance foundations are accepted and implementation-governing |
| Remaining Impact Evidence semantics | Experimental / Decision In Progress | scope, completeness, freshness, conflicts, influence, stable external identity, serialization, and public API remain intentionally open |
| Impact Evidence implementation | Experimental | no production classifier types, engine, service, controller, or classifications exist |
| Minimal vertical-slice readiness | Not yet Implementation Ready | ADR-005/006 are sufficient to shape it, but stable external binding, initial influence/qualification rules, serialization, and module boundary still need decisions |
| Production readiness | Not Production Ready | defining classifier and public/operational contracts are absent |

The maturity assessment is intentionally mixed. A mature existing QA analysis platform can coexist with an architecturally defined but unimplemented new product capability.

## 11. Long-Term Vision

The current decisions are expressed in terms of engineering artifacts, source namespaces, normalized relationships, immutable snapshots, qualification, provenance, and proof. They are not intrinsically limited to QA artifacts, although the implemented platform and initial change foundation are QA-focused.

If future evidence and influence vocabularies are defined carefully, the same architecture could potentially support:

- **software architecture:** conclusions about services, interfaces, components, and dependency changes;
- **requirements traceability:** evidence-qualified impact between requirements, decisions, scenarios, and verification artifacts;
- **engineering compliance:** auditable conclusions about which controls or obligations a verified change can be shown to affect;
- **broader change impact analysis:** heterogeneous requirements, API, code, data, deployment, and test evidence without reducing absence to non-impact;
- **digital engineering knowledge:** reproducible conclusions over frozen, source-attributed engineering snapshots.

This is architectural potential, not an implemented scope commitment. Each domain would need its own artifact identity, source capabilities, normalization, influence semantics, completeness claims, and validation. The current design enables such extension because the core contracts avoid vendor and storage assumptions; it does not prove that all domains share the same rules.

## 12. Architecture Timeline

```text
Initial repository
      ↓
Recovered implementation architecture
      ↓
Impact Evidence gap assessment
      ↓
ADR-005: conclusion semantics and proof obligations
      ↓
ADR-006: qualified data and provenance contract
      ↓
Remaining semantic and contract ADRs
      ↓
First deterministic classifier vertical slice
      ↓
Expanded evidence and bounded exclusion
      ↓
Versioned public product and operational hardening
```

- **Initial repository:** established the Canonical QA Model and deterministic validation, analysis, remediation, change, and simulation modules.
- **Recovered implementation architecture:** documented what code actually implements, its module/layer boundaries, APIs, domain model, validation pipeline, patterns, debt, and maturity.
- **Gap assessment:** demonstrated that remediation impact is not Impact Evidence and identified the missing classification/evidence capabilities without proposing replacement of working modules.
- **ADR-005:** closed the meaning of the three conclusions, their asymmetric proof obligations, precedence, failure boundary, and determinism requirements.
- **ADR-006:** closed the normalized immutable evidence boundary, identity/integrity/provenance contract, qualification outcomes, adapter separation, derivation, and replay model.
- **Remaining work:** must define the semantic policies that ADR-005/006 reference but deliberately leave open.
- **First classifier:** will validate the architecture through a narrow direct/propagated `AFFECTED` and evidence-limited `UNKNOWN` slice.
- **Future product:** may add multi-source evidence, defensible `NOT_AFFECTED`, stable public contracts, operational integration, and carefully governed domain extensions.

## 13. Conclusions

The architecture is internally coherent. Existing modules retain their implemented responsibilities, while Impact Evidence occupies a distinct boundary between verified change/qualified data and downstream decisions. ADR-005 and ADR-006 align on the same core properties: contextual conclusions, immutable evidence, conservative proof, explicit limitations, integrity binding, deterministic replay, and no infrastructure assumptions.

Incremental evolution remains the correct strategy. Canonical Change, validation, traversal, coverage, remediation, planning, impact, and simulation are reusable or adjacent capabilities with tested semantics. The missing classifier should be added as a separate framework-independent core; no existing subsystem warrants replacement.

The next architectural milestone is acceptance of the decisions required for a safe minimal vertical slice—most immediately stable `VerifiedChangeSet` binding, initial source/trust and influence semantics, canonical evidence serialization, identity/conflict handling, and the classifier module boundary. Scope/completeness and freshness must follow before any `NOT_AFFECTED` implementation. Once those decisions are closed, the project can move from architecture-defined capability to implementation-ready classifier work.

## References

- [Implementation Architecture Recovery Review](implementation-architecture-review.md)
- [Impact Evidence Gap Assessment](impact-evidence-gap-assessment.md)
- [Conclusion Semantics Decision Analysis](impact-evidence-conclusion-semantics-decision-analysis.md)
- [Qualified Data and Provenance Decision Analysis](impact-evidence-qualified-data-provenance-decision-analysis.md)
- [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
- [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [Impact Evidence product vision](../../README.ru.codex.md)
- [`QaModelValidationEngine.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/QaModelValidationEngine.java)
- [`VerifiedChangeSet.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java)
- [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)
- [`ImpactAnalyzer.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java)
- [`ModelSimulationEngine.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/ModelSimulationEngine.java)

