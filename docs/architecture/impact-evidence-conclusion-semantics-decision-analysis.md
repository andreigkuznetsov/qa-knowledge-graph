# Impact Evidence Decision Analysis 01: Conclusion Semantics and Proof Obligations

**Status:** Proposed decision analysis; not yet an ADR  
**Scope:** semantics of `AFFECTED`, `NOT_AFFECTED`, and `UNKNOWN`  
**Authority for current behavior:** repository source code  
**Authority for target behavior:** [`README.ru.codex.md`](../../README.ru.codex.md), supported by [`README.ru.md`](../../README.ru.md)  
**Related assessments:** [Implementation Architecture Recovery Review](implementation-architecture-review.md), [Impact Evidence Gap Assessment](impact-evidence-gap-assessment.md)

## 1. Executive Summary

The classified subject should be **an engineering artifact within an immutable analysis context**, represented publicly by a dedicated conclusion object. An artifact alone cannot carry a stable classification: the same artifact may correctly be `AFFECTED`, `NOT_AFFECTED`, or `UNKNOWN` for different verified changes, source snapshots, scopes, rule versions, qualification policies, or reference times. Relationships are evidence used to reach conclusions, not the primary public subject of classification.

The recommended public model remains the product specification's three mutually exclusive values:

- `AFFECTED`: a verified direct change or at least one fully qualified influence proof establishes impact on the subject.
- `NOT_AFFECTED`: a positive, bounded exclusion proof establishes non-impact for this subject in this exact analysis context.
- `UNKNOWN`: neither proof obligation is satisfied, or a relevant limitation prevents the available evidence from supporting either stronger conclusion.

Internally, the engine should preserve a richer evidence state: candidate positive proofs, candidate exclusion proofs, qualification results, conflicts, limitations, and rejected proof reasons. The public classification is a deterministic projection of that state; evidence is not discarded merely because the public value is singular.

The governing rule is:

```text
if a valid direct-change proof exists:
    AFFECTED
else if a fully qualified influence proof exists and no limitation defeats that proof:
    AFFECTED
else if a complete positive exclusion proof exists and no relevant limitation defeats it:
    NOT_AFFECTED
else:
    UNKNOWN
```

Positive influence is existential: one sound proof can establish `AFFECTED`; incomplete evidence elsewhere does not erase that proof. Exclusion is universal within a declared boundary: all evidence categories required by the scope and completeness claim must have been evaluated, and no relevant unresolved source, identity, freshness, snapshot, conflict, trust, or relationship-semantics limitation may remain. This asymmetry makes false `NOT_AFFECTED` results structurally difficult.

Malformed requests, unverified change inputs, unsupported rule versions, and internal failures are not `UNKNOWN`. `UNKNOWN` is a successful analysis result that accurately reports limited engineering knowledge.

## 2. Definitions

### 2.1 Analysis context

An **analysis context** is the complete immutable input identity needed to reproduce one classification run:

- verified change reference and its verified artifact identities;
- engineering-data source and snapshot identities;
- subject-set definition;
- bounded analysis scope;
- completeness assertions;
- evidence-qualification policy version;
- influence rule-set version;
- deterministic reference time;
- canonical ordering/version rules.

The existing generic `AnalysisContext` in `qaip-core` contains `analysisId`, `requestedAt`, and arbitrary attributes. It is not equivalent to this target concept because it does not structurally bind the inputs above. The current process-local provenance limitation is documented by [`ADR-001-exact-instance-evidence-binding.md`](../adr/ADR-001-exact-instance-evidence-binding.md).

### 2.2 Subject of classification

Four candidates were evaluated:

| Candidate | Assessment |
|---|---|
| Engineering artifact | Necessary identity, but insufficient by itself because classification varies by context |
| Relationship | Evidence may itself be qualified/rejected, but a relationship is not the product's engineering conclusion subject |
| Artifact within analysis context | Correct semantic subject: `(artifact identity, analysis context identity)` |
| Dedicated conclusion object | Correct public representation: binds subject, context, classification, proof basis and limitations |

**Decision:** classify an artifact within a context and expose the result as a dedicated immutable conclusion. The artifact remains independently identifiable; the classification never mutates or becomes an attribute of the artifact itself.

### 2.3 Evidence datum

An **evidence datum** is a source-attributed fact—such as an artifact identity assertion or relationship—bound to a named source snapshot. A canonical relationship from the QA model is currently only a discovered graph fact. `TraceEngine` can traverse such facts, while `RelationshipRules` validates structural legality. Neither component establishes target evidential qualification.

### 2.4 Qualified evidence

Evidence is **qualified** only when all conditions required for its intended use are true:

- source identity and provenance are available;
- source is applicable to the subject and analysis scope;
- source snapshot is compatible with the verified-change/model context;
- evidence is fresh under the explicit policy and reference time;
- artifact identities and relationship endpoints are resolved;
- relationship semantics and direction are supported by the rule version;
- no unresolved relevant contradiction invalidates the datum or path;
- trust/qualification requirements for that source and datum are satisfied.

Qualification is purpose-sensitive. Evidence sufficient for a positive path does not automatically prove completeness for exclusion.

### 2.5 Influence proof

An **influence proof** is either:

1. a direct binding between the subject and an artifact changed by the `VerifiedChangeSet`; or
2. a continuous, directionally valid path from a directly changed artifact to the subject in which every artifact identity and every relationship is qualified under the selected influence rules.

Several paths produce several independent proof candidates. Independence strengthens auditability and resilience but does not change the categorical result once one proof is sufficient.

### 2.6 Exclusion proof

An **exclusion proof** is a positive assertion that the subject is outside the influence closure **within a precisely bounded, applicable, compatible, fresh, and complete evidence domain**. It is not merely failure to discover a path.

The proof must identify:

- subject identity and changed-artifact identity;
- the exact bounded scope;
- applicable sources and compatible snapshots;
- evaluated artifact categories;
- evaluated relationship categories and directions;
- the completeness assertions authorizing closed-world reasoning for those categories/snapshots;
- the deterministic traversal/qualification rule version;
- absence of unresolved relevant conflicts and identities;
- the completed evaluation showing no valid influence route within that bounded complete domain.

### 2.7 Limitation

A **limitation** is a condition that constrains what can be concluded. A limitation is *blocking* when it undermines every candidate proof for a stronger classification. A non-blocking limitation is retained for audit but does not erase an independently sufficient proof. Relevance is determined only by explicit scope/rules, never by implementation convenience.

### 2.8 Classification

The three classifications are categorical conclusions, not probabilities:

- `AFFECTED`: impact is proven in this context.
- `NOT_AFFECTED`: non-impact is proven in the bounded context.
- `UNKNOWN`: impact and non-impact are both unproven in this context, or relevant limitations defeat the available proof candidates.

`UNKNOWN` does not mean “probably affected,” “low confidence,” invalid input, or engine failure.

## 3. Proof Obligations

### 3.1 Common obligations

Before any classification is returned, the analysis request itself must be processable:

1. the verified-change reference is authentic/verifiable under the chosen boundary;
2. the subject identity is syntactically valid;
3. source snapshots, scope, policies and rule versions are well formed and supported;
4. all collections and evaluation steps use canonical deterministic ordering;
5. evidence qualification produces explicit results rather than exceptions for normal knowledge limitations.

Request failures are handled as specified in section 5, not converted to conclusions.

### 3.2 `AFFECTED`: directly changed artifact

Minimum sufficient proof:

- the subject identity is exactly bound to an artifact in the accepted `VerifiedChangeSet`; and
- the verified change records an accepted change for that artifact.

The current `VerifiedChangeSet` retains the declared, intrinsic, Base and complete-validation evidence checked by `FinalChangeSetVerifier`. This can support the direct proof without re-running Canonical Change.

A missing engineering-data snapshot does **not** defeat direct impact on the changed artifact itself. The verified change is the positive proof. It does limit propagation to other artifacts and must be retained as a context limitation. If the requested subject is an external ambiguous alias rather than the exact canonical changed identity, the direct binding is not established and the result is `UNKNOWN` with `UNRESOLVED_ARTIFACT_IDENTITY`.

### 3.3 `AFFECTED`: qualified influence path

Minimum sufficient proof:

- at least one path begins at an exactly identified directly changed artifact and ends at the exactly identified subject;
- every path element comes from an identified compatible snapshot;
- every edge is applicable, fresh, trusted and supported in that direction by the selected influence rules;
- every endpoint identity is resolved;
- no unresolved conflict undermines any fact required by that path;
- the complete path and its provenance are retained.

`TraceEngine.trace` may supply path-discovery mechanics, but its `TracePath.found()` result alone is not a proof because it consumes raw canonical relationships without the target qualification context.

### 3.4 Several independent positive paths

One fully qualified path is sufficient. All qualified paths should be retained in deterministic order when report-size policy allows; otherwise the report must retain a deterministic complete proof index/count plus the selected canonical proof. A rejected or incomplete second path does not downgrade a valid first path unless its evidence creates a relevant contradiction that undermines the first path.

“Independent” must not be inferred merely from different edge sequences; independence requires separately identifiable source/evidence lineage if it is claimed publicly.

### 3.5 Stale, conflicting, or ambiguous positive paths

- A path containing stale evidence is not qualified. If no other qualified proof exists, classification is `UNKNOWN` with `STALE_EVIDENCE`.
- A conflict relevant to an edge, endpoint, source applicability, or identity in the only positive path defeats that candidate. With no unaffected qualified path, result is `UNKNOWN` with `CONFLICTING_SOURCES` and any additional reason.
- Ambiguous identity at any required endpoint defeats that candidate and yields `UNKNOWN` unless another proof uses resolved identities.
- A conflict elsewhere in the source data that is outside the explicit proof and cannot affect its applicability is retained as a non-blocking limitation only if the rule set can deterministically establish irrelevance.

### 3.6 Does positive evidence take precedence over incomplete exclusion evidence?

**Yes, when the positive proof itself is fully qualified.** `AFFECTED` is existential: a proven route establishes impact even when the overall scope is incomplete. Missing completeness cannot refute a fact already proven. This precedence does not allow stale, conflicting, untrusted, semantically unsupported, or identity-ambiguous data to become a positive proof.

### 3.7 `NOT_AFFECTED`: minimum sufficient proof

All of the following are mandatory:

1. **Exact subject and change identity:** subject and directly changed artifacts are resolved.
2. **Bounded scope:** artifact categories, relationship categories/directions, sources, snapshots and temporal boundary are explicit.
3. **Source applicability:** every source required by that scope is applicable to the artifacts and relationship semantics being evaluated.
4. **Snapshot compatibility:** source snapshots correspond to the verified-change/Base or other explicitly defined compatible analysis state.
5. **Freshness:** relevant evidence is fresh at the supplied deterministic reference time under the supplied policy.
6. **Qualified completeness assertion:** the applicable source(s) positively claim completeness for the exact artifact categories, relationships, directions, scope, snapshot and time required by the evaluation.
7. **Complete evaluation:** all relation categories/directions required by the influence rule set were evaluated.
8. **No qualified positive proof:** no valid direct-change or influence proof reaches the subject.
9. **No unresolved relevant conflict:** no source contradiction could change the exclusion result.
10. **No unresolved relevant identity:** no ambiguous/missing mapping could conceal a path to the subject.
11. **Retained exclusion evidence:** the report records the assertions and evaluation basis, not only a boolean “no path.”

If any obligation is unproven, `NOT_AFFECTED` is prohibited.

### 3.8 Why common signals are insufficient for `NOT_AFFECTED`

| Signal | Why insufficient | Existing implementation evidence |
|---|---|---|
| No path was found | Search is open-world unless relevant data completeness is positively established | `TraceEngine` returns a `TracePath` but has no source completeness/snapshot/scope input |
| Artifact absent from graph | Absence may mean missing ingestion, unresolved identity, incompatible snapshot or out-of-scope source | canonical validation checks the supplied document, not the universe of engineering artifacts |
| Relationship missing | Missing fact can be incomplete knowledge rather than a negative fact | semantic rules report missing links as validation findings; they do not prove non-impact |
| Graph coverage is high | Rule/scenario/check coverage is a ratio over modeled QA links, not a completeness assertion about all relevant influence data | `CoverageService` and `CoverageMetric` compute domain coverage metrics |
| Validation succeeded | Validity proves the document obeys schema/semantic rules, not that it is complete for exclusion | `QaModelValidationEngine.valid()` means zero validation errors |
| One source reports no dependency | Other applicable sources may disagree or the source may be incomplete, stale, or out of scope | no current multi-source completeness/conflict policy exists |

### 3.9 `UNKNOWN`: valid conclusion and reason taxonomy

`UNKNOWN` is returned when processing succeeded but neither a sufficient affected proof nor a sufficient exclusion proof survives qualification. Multiple reasons **must** be retained because several independent limitations can be simultaneously true and each may guide later evidence improvement. Reasons are de-duplicated and sorted by a stable rule-defined order, then stable subject/source identifiers.

Proposed minimum taxonomy:

| Reason | Meaning |
|---|---|
| `INSUFFICIENT_EVIDENCE` | Available qualified data supports neither proof; use only when no more specific reason fully explains the gap |
| `INCOMPLETE_SCOPE` | Supplied/evaluable scope does not cover categories or directions required for the question |
| `MISSING_COMPLETENESS_ASSERTION` | No applicable positive completeness claim permits exclusion reasoning |
| `STALE_EVIDENCE` | Required evidence fails explicit freshness policy |
| `CONFLICTING_SOURCES` | Relevant qualified sources assert incompatible facts and policy does not resolve them |
| `UNRESOLVED_ARTIFACT_IDENTITY` | Subject or required endpoint cannot be mapped unambiguously |
| `UNSUPPORTED_RELATIONSHIP_SEMANTICS` | A relevant relationship has no influence meaning in the selected rule set/direction |
| `OUT_OF_SCOPE_ARTIFACT` | The requested subject is outside the declared analysis boundary; this is not non-impact |
| `INCOMPATIBLE_SOURCE_SNAPSHOT` | Source snapshot cannot be bound to the verified change/model context |
| `UNTRUSTED_OR_UNQUALIFIED_EVIDENCE` | Candidate data fails source trust, provenance or qualification requirements |

Implementations may add more precise reasons only through a versioned contract. They must not use `UNKNOWN` to catch unexpected exceptions.

## 4. Classification Precedence Matrix

The matrix applies after request-level validation succeeds. “Relevant conflict/limitation” means it can undermine the candidate proof under explicit scope and rules.

| Situation | Classification | Required retained basis/reason |
|---|---|---|
| Exact subject is directly changed by verified change; no engineering snapshot | `AFFECTED` | direct-change proof; snapshot limitation retained for propagation only |
| Qualified positive path plus incomplete overall scope | `AFFECTED` | valid path; `INCOMPLETE_SCOPE` retained as non-blocking limitation |
| Qualified positive path plus missing completeness assertion | `AFFECTED` | valid path; completeness limitation is relevant only to exclusion |
| Candidate positive path contains stale evidence; no other proof | `UNKNOWN` | `STALE_EVIDENCE` |
| Fresh qualified positive path and separate stale path | `AFFECTED` | fresh proof; stale rejected-path reason retained |
| Candidate positive path is contradicted on a required edge/identity | `UNKNOWN` | `CONFLICTING_SOURCES` and possibly identity reason |
| Qualified positive path plus unrelated conflict proven outside proof/scope | `AFFECTED` | path proof; conflict retained as non-blocking if relevance is deterministically excluded |
| Two paths: one qualified, one unsupported | `AFFECTED` | qualified proof; `UNSUPPORTED_RELATIONSHIP_SEMANTICS` on rejected path |
| Several paths, all stale/unqualified | `UNKNOWN` | all applicable blocking reasons |
| Complete positive exclusion proof, no limitations | `NOT_AFFECTED` | scope, completeness assertions, evaluated categories/directions and negative closure result |
| Exclusion candidate plus one unresolved applicable source | `UNKNOWN` | `CONFLICTING_SOURCES`, `UNRESOLVED_ARTIFACT_IDENTITY`, or `UNTRUSTED_OR_UNQUALIFIED_EVIDENCE` as applicable |
| Exclusion candidate plus incompatible relevant snapshot | `UNKNOWN` | `INCOMPATIBLE_SOURCE_SNAPSHOT` |
| High graph coverage and no path | `UNKNOWN` | `MISSING_COMPLETENESS_ASSERTION` (plus specific scope reasons) |
| No positive path and artifact outside declared scope | `UNKNOWN` | `OUT_OF_SCOPE_ARTIFACT` |
| Direct changed artifact plus a source claiming non-dependency | `AFFECTED` | verified direct-change proof; contradictory source is retained but cannot negate the change fact unless identity binding itself is disputed |
| Both fully valid positive proof and purported exclusion proof | `AFFECTED`; flag inconsistency | positive proof demonstrates exclusion premises/result are inconsistent; retain conflict diagnostic; never return `NOT_AFFECTED` |
| Neither proof candidate exists and no specific limitation is known | `UNKNOWN` | `INSUFFICIENT_EVIDENCE` |

Precedence is therefore not simply `AFFECTED > NOT_AFFECTED > UNKNOWN`. It is **valid direct proof → valid qualified positive proof → valid complete exclusion proof → unknown**. Invalid candidate evidence never receives precedence merely because it points toward a stronger value.

## 5. Failure vs Valid Uncertainty Matrix

| Condition | Processing outcome | Why |
|---|---|---|
| Malformed request/JSON or missing mandatory request field | Failure: invalid request | The engine cannot identify a deterministic analysis invocation |
| Object presented as `VerifiedChangeSet` cannot be verified/bound under the accepted contract | Failure: unverifiable change | Impact Evidence requires confirmed change; it must not downgrade an untrusted input to knowledge uncertainty |
| Rejected Canonical Change result supplied | Failure: change not verified | `RejectedChangeSet` is not a confirmed change |
| Unsupported influence/qualification rule version | Failure: unsupported version | Semantics are undefined; choosing another version would be nondeterministic |
| Malformed completeness assertion or contradictory fields within one assertion | Failure: invalid evidence contract | The datum itself cannot be interpreted |
| Well-formed source has no completeness assertion | Successful `UNKNOWN` | Engineering knowledge is limited, but request is valid |
| Compatible source snapshot is unavailable | Successful `UNKNOWN` with `INCOMPATIBLE_SOURCE_SNAPSHOT` or `INSUFFICIENT_EVIDENCE` | Normal limitation of available knowledge |
| Evidence is stale under explicit policy | Successful `UNKNOWN` unless another valid proof succeeds | Qualification result, not processing failure |
| Sources conflict on relevant facts | Successful `UNKNOWN` unless an independent proof remains valid and conflict is irrelevant to it | Conflict is an engineering-data limitation |
| Subject identity is syntactically valid but ambiguous across sources | Successful `UNKNOWN` | Normal identity-resolution limitation |
| Subject is outside declared scope | Successful `UNKNOWN` | Out of scope is not invalid and not non-impact |
| Unsupported relationship appears in an otherwise well-formed snapshot | Successful `UNKNOWN` for conclusions dependent on it | Rule coverage limitation; request version is still supported |
| Unexpected exception, invariant violation, resource exhaustion | Failure: internal processing failure | Must be observable/retryable and never masquerade as a conclusion |

Failures may return diagnostics, but they must not produce a conclusion object that downstream consumers can mistake for a completed assessment.

## 6. Adversarial Scenario Table

These scenarios are minimum contract tests. `BR-1` is a business rule directly changed by a valid `VerifiedChangeSet`; other artifacts are subjects. “Complete” always means an applicable qualified assertion for the exact listed categories/directions/snapshot, not a generic source claim.

| # | Verified change | Subject | Available evidence | Scope and completeness | Expected | Proof basis / reason |
|---:|---|---|---|---|---|---|
| 1 | `BR-1` changed | `BR-1` | verified change only | no engineering snapshot | `AFFECTED` | exact direct-change proof |
| 2 | `BR-1` changed | `SC-1` | fresh qualified `BR-1 -> SC-1` influence edge | scope includes BR/scenario and edge direction; completeness unknown | `AFFECTED` | qualified positive path; exclusion completeness unnecessary |
| 3 | `BR-1` changed | `TEST-1` | two fresh independent qualified paths reach test | applicable scope; completeness unknown | `AFFECTED` | both paths retained; one would suffice |
| 4 | `BR-1` changed | `SC-1` | only path uses stale edge | scope includes edge; no fresh replacement | `UNKNOWN` | `STALE_EVIDENCE` |
| 5 | `BR-1` changed | `SC-1` | source A asserts edge; source B denies same edge; both relevant/current | same scope; no deterministic conflict resolution | `UNKNOWN` | `CONFLICTING_SOURCES` |
| 6 | `BR-1` changed | alias `Scenario Login` | path ends at two possible canonical IDs | scope includes both candidates | `UNKNOWN` | `UNRESOLVED_ARTIFACT_IDENTITY` |
| 7 | `BR-1` changed | `SC-2` | trace search finds no path | source makes no completeness claim | `UNKNOWN` | `MISSING_COMPLETENESS_ASSERTION` |
| 8 | `BR-1` changed | `SC-2` | subject absent from graph | source scope nominally includes scenarios but ingestion completeness unknown | `UNKNOWN` | `MISSING_COMPLETENESS_ASSERTION` and possibly `UNRESOLVED_ARTIFACT_IDENTITY` |
| 9 | `BR-1` changed | `SC-2` | no relationship present | high rule/scenario coverage percentage | scope not declared complete for influence relations | `UNKNOWN` | coverage is not exclusion proof; `MISSING_COMPLETENESS_ASSERTION` |
| 10 | `BR-1` changed | `SC-2` | canonical model validates; no path | valid model, no source completeness | `UNKNOWN` | validity does not establish completeness |
| 11 | `BR-1` changed | `SC-2` | source A reports no dependency | A is complete only for API dependencies, not BR→scenario relations | required relation scope incomplete | `UNKNOWN` | `INCOMPLETE_SCOPE` |
| 12 | `BR-1` changed | `SC-3` | no path after full evaluation | fresh compatible source positively complete for BR/scenario artifacts and all applicable relation directions | `NOT_AFFECTED` | bounded positive exclusion proof |
| 13 | `BR-1` changed | `SC-3` | same as #12 plus relevant source B with unresolved identity mapping | B is declared applicable; completeness across applicable sources not resolved | `UNKNOWN` | `UNRESOLVED_ARTIFACT_IDENTITY`; exclusion defeated |
| 14 | `BR-1` changed | `SC-3` | otherwise complete snapshot is for model revision before Base | scope complete but snapshot incompatible | `UNKNOWN` | `INCOMPATIBLE_SOURCE_SNAPSHOT` |
| 15 | `BR-1` changed | `SC-3` | fresh complete relations, but one required relation type has no influence semantics in selected rules | scope includes unsupported type | `UNKNOWN` | `UNSUPPORTED_RELATIONSHIP_SEMANTICS` |
| 16 | `BR-1` changed | `SC-3` | no path in complete source, but source provenance signature/qualification is absent | exact scope declared complete | `UNKNOWN` | `UNTRUSTED_OR_UNQUALIFIED_EVIDENCE` |
| 17 | `BR-1` changed | `TEST-2` | fresh qualified positive path; other source is incomplete and has no path | subject in scope | `AFFECTED` | positive path proves impact; incomplete source cannot negate it |
| 18 | `BR-1` changed | `TEST-2` | one stale positive path and one current source with no path but no completeness | scope otherwise valid | `UNKNOWN` | `STALE_EVIDENCE`, `MISSING_COMPLETENESS_ASSERTION` |
| 19 | `BR-1` changed | `API-9` | no evidence because API artifacts excluded by requested scope | scope only BR/scenario/test | `UNKNOWN` | `OUT_OF_SCOPE_ARTIFACT` |
| 20 | `BR-1` changed | `SC-4` | complete fresh source evaluated incoming edges only; rule allows outgoing propagation from BR | direction required by rule was not evaluated | `UNKNOWN` | `INCOMPLETE_SCOPE` |
| 21 | `BR-1` changed | `SC-4` | positive qualified path and a generated exclusion result from same evidence set | exact common context | `AFFECTED` | positive proof exposes inconsistent exclusion; conflict retained |
| 22 | `BR-1` changed | `SC-5` | one qualified path; another path has ambiguous intermediate identity | both paths in scope | `AFFECTED` | sound path suffices; ambiguity retained on rejected path |
| 23 | `BR-1` changed | `SC-6` | no path; sources complete for nodes but not relationships | node inventory complete only | `UNKNOWN` | `MISSING_COMPLETENESS_ASSERTION` for relations |
| 24 | `BR-1` changed | `SC-7` | no path; completeness assertion has no effective time and freshness cannot be evaluated | scope otherwise bounded | `UNKNOWN` | `STALE_EVIDENCE` or `UNTRUSTED_OR_UNQUALIFIED_EVIDENCE` per policy; never `NOT_AFFECTED` |

## 7. Alternatives Considered

### Alternative 1: three mutually exclusive public values

`AFFECTED | NOT_AFFECTED | UNKNOWN`

**Advantages:** matches product specification; simple for downstream consumers; prevents consumers inventing thresholds; makes uncertainty explicit; supports exhaustive handling with an enum or sealed outcomes.

**Disadvantages:** a single value alone cannot expose multiple proofs, rejected paths, or simultaneous limitations.

**Assessment:** use as the public classification, but never as the entire conclusion payload.

### Alternative 2: classification plus confidence score

**Advantages:** superficially expresses degrees of evidence and can rank results.

**Disadvantages:** the product specification explicitly says an uncertainty reason is not an impact probability and does not rank unknown objects. No statistical model, calibration set, probability semantics, or threshold contract exists in code or specification. A score would encourage threshold-based false certainty and make reproducibility dependent on unsupported calibration choices.

**Assessment:** reject. Confidence is neither required nor evidenced by the current architecture.

### Alternative 3: evidence-state model with independent dimensions

Example internal dimensions:

- direct-change proof: absent/valid/rejected;
- positive influence proofs: qualified/rejected with reasons;
- exclusion proof: absent/valid/rejected;
- limitations: zero or more blocking/non-blocking values;
- conflicts and identity resolutions;
- qualification decisions and provenance.

**Advantages:** preserves audit evidence; expresses several paths/reasons; makes the projection rules testable; avoids forcing evidence ingestion directly into a conclusion.

**Disadvantages:** too complex as the only public contract; consumers could reinterpret raw states inconsistently and recreate the exact ambiguity the product is intended to remove.

**Assessment:** adopt internally and expose relevant evidence in the conclusion, while keeping the authoritative public classification three-valued.

### Recommended combination

Use **Alternative 3 internally** and derive **Alternative 1 publicly**. A public conclusion should contain the categorical value plus proof basis, all blocking reasons, material non-blocking limitations, context/rule/snapshot references, and deterministic ordering. Do not expose a confidence score.

## 8. Compatibility with Existing Architecture

### 8.1 Reusable without changing business meaning

| Existing component | Reuse boundary |
|---|---|
| `VerifiedChangeSet` | authoritative in-process confirmed-change input and direct-change evidence; do not reconstruct or revalidate it in Impact Evidence |
| Canonical Change evidence objects | retain Base/proposed identities, declarations and verification lineage; add a stable external reference only through a separate decision |
| `TraceEngine` and `TracePath` | neutral deterministic path-discovery mechanics over a supplied model; qualification must wrap/precede traversal and interpret paths separately |
| Canonical relationships and `RelationshipType` | discovered graph facts and structural vocabulary; influence semantics require a separately versioned catalog |
| `QaModelValidationEngine` and validation diagnostics | reject invalid canonical model inputs and reuse immutable diagnostic conventions; validity is not evidential completeness |
| Immutable records, defensive copies and sealed outcomes | model conclusions, proof candidates and failures with the same evidence-preserving discipline |
| `QaModelFingerprintCalculator` pattern | evidence for deterministic canonical hashing; its exact package-private simulation implementation is not automatically a public provenance contract |

Current-behavior evidence: `FinalChangeSetVerifier` checks exact retained evidence and does not re-run validation; `TraceEngine` constructs paths from canonical nodes/relationships; `QaModelValidationEngine` owns schema-first validation; change result interfaces are sealed and successful evidence construction is restricted.

### 8.2 Must not be treated as the classifier

| Existing component | Why not |
|---|---|
| remediation `ImpactAnalyzer` | consumes `RoadmapReport` and `ExecutionPlan`, then maps tasks through `RemediationImpactCatalog`; it does not consume a verified change or qualified source evidence |
| remediation `ImpactReport` / `TaskImpact` | describe expected structural remediation changes, waves and dependencies; contain no target classifications |
| coverage percentages | measure modeled rule/scenario/check connections; do not assert source completeness within an exclusion scope |
| absence of `TracePath` | only states traversal found no route in supplied data; open-world missing knowledge remains possible |
| `SimulationResult` | proves an explicitly materialized candidate validates and carries fingerprints; it does not classify current impact |
| findings and roadmap | recommend/plan remediation downstream of structural gaps; target Impact Evidence explicitly stops before decisions/recommendations |

No production component should be renamed, modified, or replaced by this decision. The future classifier must remain semantically separate from the existing remediation impact pipeline.

## 9. Recommended Decision

Adopt the following decision for the future ADR:

1. **Classify `(engineering artifact, immutable analysis context)` and represent it with a dedicated immutable conclusion object.** Never store classification as mutable artifact state.
2. **Expose exactly three public values:** `AFFECTED`, `NOT_AFFECTED`, and `UNKNOWN`. Do not add confidence scores.
3. **Maintain richer internal evidence state.** Preserve all proof candidates, qualification decisions, blocking reasons, material non-blocking limitations, conflicts, provenance and rule/snapshot identities.
4. **Prove `AFFECTED` existentially.** An exact direct-change proof or one fully qualified influence path is sufficient. Incomplete exclusion coverage does not defeat a valid positive proof.
5. **Prove `NOT_AFFECTED` through positive bounded exclusion.** Require explicit applicable compatible fresh completeness for all relevant artifact/relationship categories and directions, a complete evaluation, resolved identities, and no relevant conflict. Absence signals are never enough.
6. **Use `UNKNOWN` for valid knowledge limitations.** Retain all applicable deterministic reasons; do not collapse them to a generic message when specific causes are known.
7. **Keep failures outside the classification algebra.** Invalid requests, unverifiable changes, unsupported rule versions and internal failures produce failure outcomes with diagnostics, never `UNKNOWN` conclusions.
8. **Make result determination a pure function of explicit inputs.** No implicit clock, random identifier, unordered map/set iteration, ambient source state, or unversioned rule may affect classification.
9. **Make contradictory proofs safe.** A valid positive proof prevents `NOT_AFFECTED`; relevant contradiction that undermines the only positive proof produces `UNKNOWN`. An exclusion result coexisting with a valid positive proof is internally inconsistent and must be surfaced, not hidden.
10. **Enforce the proof obligations with adversarial contract tests.** At minimum, implement the scenarios in section 6 before exposing a public classifier.

This decision makes `NOT_AFFECTED` structurally harder to construct than `AFFECTED`: the former requires a complete evidence bundle and closed bounded evaluation, while the latter requires one valid proof. Constructors/factories for successful exclusion evidence should be restricted in the same style as successful provenance-bearing outcomes in `qa-model-change`.

### 9.1 Determinism requirements

The classification and serialized semantic content must be fully determined by:

- stable verified-change reference and canonical changed-artifact identities;
- complete ordered list of engineering-data source/snapshot identities and integrity references;
- exact subject identity;
- normalized analysis scope;
- normalized completeness assertions;
- evidence trust/qualification policy version and parameters;
- influence rule-set version;
- explicit deterministic reference time and freshness parameters;
- normalization, path-selection and output-ordering rules.

Requirements:

- the wall clock may not be read during semantic classification;
- iteration over maps/sets must not determine proof selection or output order;
- paths, reasons, sources and conclusions require stable comparators;
- generated request/report IDs, if later added, are metadata and must not influence semantic equality or classification;
- the rule version must be rejected if unsupported rather than silently substituted;
- source data must be snapshot-bound; live mutable source reads during evaluation would violate reproducibility;
- if several equally sufficient proofs exist, a canonical deterministic selection rule must choose the primary proof while retaining the others as contract permits.

## 10. Open Questions

These questions must be answered by subsequent decisions or the Public Contract; this analysis does not invent answers unsupported by the specification:

1. What stable identifier or fingerprint makes `VerifiedChangeSet` consumable across serialization/process boundaries while preserving anti-substitution guarantees?
2. What exact artifact identity namespace and cross-source identity evidence are supported in the first product version?
3. Which canonical `RelationshipType` values propagate influence, in which direction, and under what artifact-type constraints?
4. Can completeness be composed across several sources, and if so, what proves that their scopes jointly cover the required domain without gaps?
5. Are explicit negative dependency assertions allowed as exclusion evidence, and what source qualifications make them sufficient?
6. How is conflict relevance determined when a conflict is outside the selected positive proof but inside the broader scope?
7. Must every qualified positive path be serialized, or may the public report retain one canonical proof plus hashes/indexes for the rest?
8. What is the initial freshness policy vocabulary, and who supplies deterministic reference time?
9. Is `OUT_OF_SCOPE_ARTIFACT` always a returned `UNKNOWN`, or may a public API reject subjects not admitted by the request's subject-set contract? This analysis recommends `UNKNOWN` when the subject request is valid but outside evidential scope.
10. What report/version compatibility guarantees are required before a REST adapter is authored?
11. Which limitations are exposed as non-blocking alongside `AFFECTED`, and how should downstream consumers display them without treating them as confidence?
12. What is the maximum evidence/report size and deterministic truncation/reference strategy?

## 11. Proposed ADR Outline

**Title:** ADR-IE-001 — Impact Conclusion Semantics and Proof Obligations

1. **Status** — Proposed / Accepted date and owners.
2. **Context** — Product principle; distinction between discovered paths and justified conclusions; existing Canonical Change and traversal capabilities.
3. **Decision drivers** — prevent false `NOT_AFFECTED`, preserve uncertainty/provenance, deterministic reproducibility, compatibility with current modules.
4. **Definitions** — analysis context, subject, qualified datum, influence proof, exclusion proof, limitation, failure.
5. **Decision** — contextual artifact conclusion; three public values; richer internal evidence state.
6. **`AFFECTED` proof obligation** — direct and propagated paths; qualification; conflicts; positive-proof precedence.
7. **`NOT_AFFECTED` proof obligation** — bounded scope, applicability, snapshots, freshness, completeness, directions, resolved conflicts/identities.
8. **`UNKNOWN` semantics and reason taxonomy** — multiple deterministic reasons retained.
9. **Precedence algorithm** — valid direct proof → qualified influence proof → complete exclusion proof → unknown.
10. **Failure boundary** — invalid/unverifiable/unsupported/internal failures outside classification.
11. **Determinism contract** — complete explicit inputs, reference time, rule versions and ordering.
12. **Compatibility** — reuse `VerifiedChangeSet`, Canonical Change evidence, traversal mechanics and diagnostics; exclude remediation impact/coverage/simulation from classifier semantics.
13. **Consequences** — conservative exclusion, larger evidence payloads, need for qualification/scope/completeness contracts.
14. **Alternatives rejected** — bare three-value result, confidence score, raw evidence state as sole public contract.
15. **Conformance tests** — adversarial scenarios in section 6 and invariant tests prohibiting unproven `NOT_AFFECTED`.
16. **Follow-up ADRs** — stable verified-change reference; qualified data/provenance; scope/completeness; identity/conflict; influence rule catalog; public report contract.

## Implementation Evidence Index

- verified outcome and evidence consistency: [`VerifiedChangeSet.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java), [`FinalChangeSetVerifier.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/FinalChangeSetVerifier.java)
- process-local evidence decision: [`ADR-001-exact-instance-evidence-binding.md`](../adr/ADR-001-exact-instance-evidence-binding.md)
- canonical traversal: [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java), [`TracePath.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TracePath.java)
- model validation: [`QaModelValidationEngine.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/QaModelValidationEngine.java), [`RelationshipRules.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/validation/RelationshipRules.java), [`SemanticValidationRules.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/validation/semantic/SemanticValidationRules.java)
- remediation impact that remains separate: [`ImpactAnalyzer.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java), [`ImpactReport.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/model/ImpactReport.java), [`RemediationImpactCatalog.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/mapping/RemediationImpactCatalog.java)
- coverage metrics that are not completeness claims: [`CoverageService.java`](../../qa-coverage-engine/src/main/java/ru/kuznetsov/qaip/coverage/service/CoverageService.java), [`CoverageMetric.java`](../../qa-coverage-engine/src/main/java/ru/kuznetsov/qaip/coverage/model/CoverageMetric.java)
- simulation result that is not a conclusion: [`SimulationResult.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/model/SimulationResult.java), [`ModelSimulationEngine.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/ModelSimulationEngine.java)
