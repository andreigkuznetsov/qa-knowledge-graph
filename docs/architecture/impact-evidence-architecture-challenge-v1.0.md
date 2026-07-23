# Architecture Challenge v1.0: Attempt to Disprove Impact Evidence

**Review posture:** independent pre-funding red team  
**Question:** Does Impact Evidence deserve to exist as an independent architectural capability?  
**Material reviewed:** current source, architecture recovery/gap/review documents, decision analyses, ADR-005, and ADR-006

## 1. Executive Verdict

The strongest argument against building Impact Evidence is simple: the proposal risks turning a small conservative rule—“do not infer non-impact from missing data”—into a new evidence platform.

Almost every proposed mechanism already exists elsewhere. Graph stores and traversal frameworks find paths. RDF/OWL reasoners derive facts from formal vocabularies. Rule and policy engines evaluate explicit inputs deterministically. W3C PROV models entities, activities, agents, derivations, and provenance interchange. OpenLineage and OpenMetadata capture lineage. Backstage models named entities and relations. ArchUnit and SonarQube evaluate structural/code rules. The repository itself already has verified changes, model validation, traversal, diagnostics, immutable evidence chains, fingerprints, and deterministic analysis pipelines.

The architecture is therefore not justified merely because it contains snapshots, provenance, qualification, a graph, and rules. Those are commodity concepts. ADR-006 is especially vulnerable: it specifies an evidence source model, snapshot identity, datum identity, identity assertions, relationships, provenance DAG, qualification algebra, anti-substitution, adapters, derivation, and replay before a single Impact Evidence conclusion has been implemented or validated with a user. That is a large speculative surface.

The architecture deserves funding only if it is reduced to one independently valuable semantic boundary:

> Given a verified change and frozen evidence, issue a deterministic, auditable conclusion without allowing missing knowledge to become `NOT_AFFECTED`.

Everything else should be borrowed, adapted, merged, or deferred. The project should not build a graph database, general provenance system, metadata catalog, generic rule engine, source-integration platform, or universal engineering ontology. If it attempts those, reject it.

## 2. Is the Problem Real?

### 2.1 The repository does lack the defining result

Source search finds no production `AFFECTED`, `NOT_AFFECTED`, or conclusion-valued `UNKNOWN` types. `ImpactAnalyzer.analyze(RoadmapReport, ExecutionPlan)` produces expected remediation structure, not a conclusion about what a verified change affects. `TraceEngine` returns a path or no path. `CoverageService` reports QA link coverage. None qualifies source freshness, completeness, conflicts, identity ambiguity, or exclusion proof.

The gap is real at the semantic output boundary. The current platform cannot distinguish:

- “no path in incomplete evidence” from “proven excluded in a complete bounded scope”;
- a fresh qualified path from a stale or conflicting path;
- an unknown external identity from a canonical resolved artifact;
- valid uncertainty from a failed analysis.

### 2.2 But most enabling mechanics already exist

The repository already supplies:

- confirmed change and exact in-process evidence binding through `VerifiedChangeSet` and `FinalChangeSetVerifier`;
- schema-first and semantic validation through `QaModelValidationEngine`;
- graph path mechanics through `TraceEngine`;
- source/source-reference fields in the Canonical QA schema;
- immutable records, sealed outcomes, diagnostics, deterministic sorting, and defensive copying;
- versioned canonical hashing precedent in `QaModelFingerprintCalculator`;
- modular, framework-independent Java libraries.

A reasonable estimate is that **35–45% of enabling infrastructure** exists if measured broadly: verified inputs, normalized QA graph, traversal, validation, immutable outcomes, and deterministic practices. Less than **10% of classifier-specific behavior** exists: there is no qualified-evidence contract in code, qualification engine, influence rule set, contextual conclusion, exclusion evaluator, public report, or classifier API. These estimates are deliberately separate; claiming “40% complete” without that distinction would be misleading.

The problem is therefore real, but the architecture can still be disproportionate to it.

## 3. Existing Platform Comparison

| Existing capability | What it already solves | Can it be extended instead? | Red-team decision |
|---|---|---|---|
| `VerifiedChangeSet` | Trusted in-process confirmed change with retained Base/proposed/validation evidence | Yes, as input and direct-change proof; stable external binding needs a narrow extension | Reuse; no new change-verification layer |
| `QaModelValidationEngine` | Structural and semantic validity of Canonical QA JSON | Extend only with evidence-contract validation in a separate concern; do not mix completeness into model validity | Reuse patterns, not semantics |
| `TraceEngine` | Deterministic path discovery over canonical nodes/relationships | Adapt with qualified edge/identity inputs or filter; do not create a second generic traversal engine | Extend/reuse traversal mechanics |
| Coverage | Measures business-rule/scenario/check links and traceability gaps | Cannot prove source completeness or exclusion; extending it would corrupt its metric meaning | Keep separate |
| Findings | Converts coverage problems into remediation findings | Not a classifier; it assumes current structural analysis | Keep downstream/adjacent |
| `ImpactAnalyzer` | Expected structural effect of remediation tasks and execution waves | Renaming/repurposing would break its input/output meaning and simulation consumers | Do not extend into classifier |
| Simulation | Materializes explicit future nodes, fingerprints and validates candidate model | Can validate consequences after proposed remediation; cannot classify current impact evidence | Keep separate |

A new module reduces complexity only if it prevents existing components from acquiring incompatible semantics. It increases complexity if it duplicates traversal, validation, hashing, rule execution, or provenance standards. The justified boundary is a small classifier library that composes reusable mechanics and owns only qualification/proof semantics.

## 4. Industry Comparison

No single listed product solves the complete target problem, but the proposed architecture overlaps heavily with existing categories.

| Technology/category | What it already supplies | What remains different | Verdict |
|---|---|---|---|
| Neo4j / Cypher | Property graph storage, pattern matching, reachability, filtered and shortest paths | Does not decide whether absence is evidentially complete, qualify provenance/freshness, or define ADR-005 proof obligations | Partially solved; storage/traversal replaceable, conclusion semantics not |
| GraphRAG | Extracted graph/community structure plus retrieval and synthesis over text | Optimized for retrieval/summarization and LLM answers, not deterministic exclusion proofs or frozen qualified evidence | Adjacent, not replacement |
| RDF / OWL reasoners | Formal vocabularies, graph interchange, entailment and consistency under declared semantics | Open-world reasoning generally reinforces that absence is not negation, but source snapshots, trust, freshness, change binding, and product classifications still need application rules | Strong reusable foundation; not complete product |
| OpenLineage | Standard run/job/dataset lineage model, consistent names, extensible facets | Data-pipeline scope; no artifact impact conclusion or bounded non-impact proof | Reuse/adapt where data lineage is source evidence |
| OpenMetadata | Entity metadata, lineage/dependencies, data origins and governance context | Catalog/lineage platform, not conservative conclusion proof | Potential source, not classifier |
| Backstage | Software catalog entities, namespaces, relations, processors, source ingestion | Human-maintained/high-level catalog; its own docs caution against treating it as exhaustive truth or fine-grained real-time model | Potential source and identity namespace, not proof engine |
| Structurizr | Versionable architecture model with elements/relationships and views | Architecture description, not evidence qualification or impact proof | Potential source only |
| ArchUnit | Executable rules over Java bytecode dependencies, layers, cycles | Narrow Java structural facts; can prove particular architecture violations but not heterogeneous source completeness | Excellent source/rule adapter, not general classifier |
| SonarQube | Static-analysis rules and findings over source | Code-quality/security issue semantics; not cross-source change impact or exclusion proof | Potential evidence source |
| Policy engines (OPA) | Deterministic decisions from explicit input/data, versionable policy bundles and decision logs | Inputs, provenance, completeness, identity resolution, graph derivation, and proof payload must be supplied by application | Could replace rule-evaluation implementation, not evidence contract |
| Rule engines / Drools | Forward/rule evaluation, fact matching, explanation support depending on design | Same missing application semantics; stateful agendas can complicate deterministic proof ordering | Possible implementation engine; not architecture replacement |
| DMN | Standardized decision tables/requirements and explainable business decisions | Poor fit for graph path derivation and evidence lineage without preprocessing | Useful for small qualification/precedence tables |
| Provenance systems / W3C PROV | Standard concepts for Entity, Activity, Agent, generation, usage, derivation, responsibility and interchange | Does not define Impact Evidence trust, qualification, completeness, snapshot compatibility, or conclusions | ADR-006 should map to it rather than invent a competing ontology |
| Knowledge graph systems | Entity/relation representation, querying, reasoning, sometimes provenance | Stored fact is not qualified evidence; open-world absence cannot become non-impact automatically | Necessary mechanics, insufficient semantics |
| Graph traversal frameworks | Efficient reachability/path algorithms and filters | No source authority, snapshot, completeness, or exclusion proof | Commodity implementation detail |

Official references support these boundaries: Cypher is a declarative graph query language with rich path matching ([Neo4j Cypher overview](https://neo4j.com/docs/cypher-manual/current/introduction/cypher-overview/)); PROV-O is explicitly an interoperable provenance model for heterogeneous systems ([W3C PROV-O](https://www.w3.org/TR/prov-o/)); OWL provides formally defined ontology semantics ([W3C OWL 2 overview](https://www.w3.org/TR/owl-overview/)); OPA makes policy decisions over supplied input documents ([OPA integration](https://www.openpolicyagent.org/docs/integration)); OpenLineage models run/job/dataset lineage with extensible facets ([OpenLineage specification](https://github.com/OpenLineage/OpenLineage/blob/main/spec/OpenLineage.md)); Backstage models namespaced entities and relations but describes its catalog as a caching/high-level model rather than ultimate truth ([Backstage catalog graph](https://backstage.io/docs/features/software-catalog/creating-the-catalog-graph/)); ArchUnit tests architecture rules over Java bytecode ([ArchUnit guide](https://www.archunit.org/userguide/html/000_Index.html)); Structurizr is a model-as-code C4 tool ([Structurizr docs](https://docs.structurizr.com/)).

Impact Evidence is **partially solved by a stack of existing technologies**, not already solved by one. Its distinct part is the governed composition and conclusion contract, not the underlying mechanisms.

## 5. Layer Elimination Challenge

| Proposed layer/concept | Can the system function without it? | Decision | Required reduction |
|---|---|---|---|
| Immutable Evidence Snapshot | A demo can, but replay and stable qualification cannot | KEEP | Use a small manifest/bundle; do not build a snapshot platform |
| Qualification | Raw paths could classify affected, but stale/conflicting/ambiguous data would become false proof | KEEP | Implement as pure predicates/outcomes, not a general workflow |
| Provenance | Direct-only MVP can reference inputs; multi-step audit cannot work without lineage | MERGE | Adopt/map W3C PROV concepts; fold provenance into evidence records/manifests instead of a standalone subsystem |
| Evidence Graph | Qualified evidence can remain lists/indexes and use existing traversal | REMOVE as a named layer | Treat it as a logical view, not a component or store |
| Proof Engine | Precedence could be a small function plus validators | SIMPLIFY | Do not create a generic theorem prover; implement ADR-005 projection over evidence-bearing results |
| Context | Without it, classifications become mutable/global artifact properties | KEEP | Make it a compact immutable input identity, not an attribute bag/platform session |
| `UNKNOWN` | Binary output is simpler but converts missing knowledge into false certainty or failures | KEEP | Keep categorical reasons; no confidence machinery |
| `NOT_AFFECTED` | MVP can deliver direct/propagated affected and unknown without it | REMOVE FROM MVP / DEFER | Implement only after demonstrated user demand and defensible scope/completeness contracts |
| Replay | Operational prototype can re-query, but audit and reproducibility claims collapse | SIMPLIFY | Retain frozen input manifest + hashes + rule versions; do not build a replay service initially |

After elimination, the minimum architecture is much smaller:

```text
VerifiedChangeSet + frozen evidence manifest
  -> contract validation/qualification
  -> existing traversal + small proof projection
  -> contextual AFFECTED or UNKNOWN
```

`NOT_AFFECTED`, multi-source conflict composition, full provenance packaging, and public service infrastructure should be funded only after this slice demonstrates value.

## 6. Can Rule Engines Replace It?

### Where they succeed

- OPA can evaluate explicit JSON inputs using versioned policy and can produce decision logs containing input and bundle metadata.
- Drools can match facts and apply complex rules/derivations.
- DMN can express transparent decision tables for qualification and precedence.
- All can replace hand-written conditional logic if rules become numerous and business-managed.

### Where they fail as complete replacements

They do not create trustworthy inputs. A rule engine cannot know that a graph is complete, a snapshot compatible, an identity resolved, or provenance authentic unless the application represents and supplies those facts. It can enforce ADR-005 only after the evidence contract exists. It also does not automatically retain graph-path parent lineage or prevent source/datum substitution.

The architecture should not mandate a custom proof engine where OPA/DMN/plain Java could work. The decision should specify semantics, inputs, outputs, and conformance tests. Engine selection is replaceable implementation detail. For the initial small rule set, plain deterministic Java is simpler than adding Drools or OPA runtime/deployment complexity.

## 7. Can Knowledge Graphs Replace It?

Assume all artifacts and relationships are loaded into Neo4j and all analysis uses Cypher:

```cypher
MATCH p = (changed)-[:INFLUENCES*]->(subject)
RETURN p
```

This replaces storage, indexing, relationship queries, path discovery, and perhaps some rule filters. Properties can record source, snapshot, timestamps, trust, and normalization. Cypher can require every edge in a matched path to satisfy qualification predicates.

Important losses remain:

- `MATCH` returning zero rows does not prove the database contains every relevant relationship.
- A completeness statement is a claim about the source/scope, not a graph topology property.
- Identity ambiguity cannot be resolved by selecting a convenient node without policy/evidence.
- Conflicting facts can coexist; Cypher does not decide their authority.
- “fresh” needs explicit reference time and policy.
- a graph mutation between queries destroys replay unless a snapshot is frozen/versioned;
- paths do not automatically carry normalization/derivation provenance and anti-substitution guarantees;
- `VerifiedChangeSet` binding and ADR-005 failure/conclusion distinctions remain application semantics.

Neo4j can implement the logical evidence view, but it cannot be the entire solution. Conversely, nothing in the current use case requires Neo4j: the existing `TraceEngine` and immutable in-memory snapshots may be sufficient at initial scale. Selecting a graph database now would be premature.

## 8. Can Provenance Frameworks Replace It?

W3C PROV already models the core ADR-006 lineage ideas:

- evidence data/snapshots as `prov:Entity`;
- capture, normalization, and derivation as `prov:Activity`;
- source/tool/owner responsibility as `prov:Agent`;
- generation, use, derivation, attribution, and association relationships.

ADR-006's bespoke “captured/normalized/derived provenance records forming a DAG” is substantially a specialization of PROV. Recreating a parallel provenance vocabulary would be accidental complexity. The future contract should map to PROV concepts or explain concrete incompatibility.

OpenLineage offers a narrower implementation for data workflows: runs, jobs, datasets, and facets. It could supply source evidence and transformation lineage when Impact Evidence analyzes data pipelines. OpenMetadata similarly supplies catalog/lineage data.

What these frameworks do not define:

- exact binding to `VerifiedChangeSet`;
- artifact identity resolution status;
- evidence qualification for impact;
- snapshot compatibility with the changed model;
- source completeness in bounded impact scope;
- influence semantics and proof precedence;
- `AFFECTED | NOT_AFFECTED | UNKNOWN`.

Verdict: provenance is not independent product value and should be **adopted/adapted**, not built as a proprietary subsystem. Impact qualification remains application-specific.

## 9. Is `UNKNOWN` Actually Needed?

### Binary affected/not affected

This succeeds only under a closed-world assumption: every relevant fact is present and identities/semantics are resolved. The product explicitly targets partial engineering knowledge. Binary output forces absence or error into `NOT_AFFECTED`, recreating the false-certainty defect.

### Confidence score

A score appears more nuanced but has no calibration model, event frequency, ground truth, or probabilistic semantics. “0.4 affected” does not say whether data is stale, incomplete, conflicting, or out of scope. Thresholds simply hide the binary decision in configuration. No current code or product evidence supports probabilities.

### Fail instead of unknown

Failure conflates a valid limited-knowledge result with malformed input or engine malfunction. It prevents batch analysis from reporting what is known while identifying gaps.

`UNKNOWN` survives the challenge. It is the only clean categorical value for valid open-world uncertainty. Its reason taxonomy should remain small and evidence-based; otherwise it becomes an unbounded diagnostic dump.

## 10. Is `NOT_AFFECTED` Overengineered?

Yes, operationally—and necessarily so semantically.

Attempts to weaken it fail:

| Shortcut | Why it fails |
|---|---|
| No path | Could mean missing ingestion, unsupported relation, stale snapshot, or unresolved identity |
| “Complete graph” | Completeness must specify artifact types, relations/directions, sources, snapshot, time, and boundary; “complete” globally is not credible |
| High coverage | `CoverageMetric` measures selected QA link ratios, not evidence-source completeness |
| Validated model | `QaModelValidationEngine` proves conformance/invariants, not exhaustive knowledge |
| One source denies dependency | Other applicable sources may disagree; the source may be incomplete or inapplicable |

The proof obligation cannot be weakened without changing the meaning to “no impact found.” If users actually need that weaker result, it should be named `NO_AFFECTED_PATH_FOUND`, not `NOT_AFFECTED`.

The red-team recommendation is not to weaken `NOT_AFFECTED`; it is to **defer or delete it from initial scope**. Its cost may exceed its value. Only real user cases and sources capable of defensible completeness should justify funding it.

## 11. Can Replay Be Removed?

A low-stakes interactive tool could query live systems again and label results “current.” That eliminates snapshots, manifests, hashes, and some provenance.

What breaks:

- the same request can produce a different conclusion after source mutation;
- stale-versus-current evidence cannot be demonstrated;
- an audit cannot establish what data supported an earlier decision;
- conflicts can disappear or appear between runs;
- normalization/rule changes cannot be distinguished from source changes;
- bugs cannot be reproduced against original input;
- a previous `NOT_AFFECTED` conclusion becomes indefensible.

Replay is essential only if the product claims reproducibility/auditability, which both product documents and ADRs do. It need not be a service. A content-addressed manifest containing frozen normalized input, rule versions, reference time, and hashes is enough initially. Full raw payload retention and automated replay orchestration can be omitted.

## 12. Complexity Challenge

| Rank | Complexity source | Essential or accidental? | Challenge result |
|---:|---|---|---|
| 1 | Scope/completeness proof for `NOT_AFFECTED` | Essential to that classification, optional to MVP | Defer entire feature until demanded |
| 2 | Multi-source identity resolution/conflict | Essential for heterogeneous evidence | Start single-source/resolved; add only with cases |
| 3 | Snapshot/datum fingerprints and canonicalization | Essential for anti-substitution/replay | Use one narrow canonical manifest spec; reuse standards/libraries |
| 4 | Provenance DAG | Partly essential, largely accidental if bespoke | Map to W3C PROV; retain references, not a platform |
| 5 | Evidence qualification result algebra | Essential distinctions; number of types may be accidental | Begin with qualified/unresolved/rejected and stable reasons |
| 6 | Separate “Evidence Graph” | Accidental | Remove as component; use logical indexes/view |
| 7 | Generic Proof Engine | Accidental at current rule volume | Plain deterministic projection; engine pluggability later |
| 8 | Raw adapters and normalization ecosystem | Essential at integration boundary, but not classifier MVP | One adapter/source first; avoid framework |
| 9 | Full replay/audit packaging | Partly essential | Minimal manifest/hash bundle; defer orchestration/raw custody |
| 10 | Generalization beyond QA | Accidental until demonstrated | Do not design universal artifact ontology; preserve only cheap namespace extensibility |

The architecture contains essential semantic complexity, but its current documents allow too much accidental platform complexity. Funding should be conditional on the reduced slice.

## 13. Independent Value

If the QA platform disappeared, the core problem would still exist in architecture, requirements, compliance, and data lineage: a found dependency is not automatically a qualified impact proof, and absent dependency data is not automatically non-impact. A verified change plus frozen source-attributed evidence can support conservative conclusions in other domains.

However, independent value is unproven. The current concrete assets—Canonical QA IDs, `RelationshipType`, `VerifiedChangeSet`, coverage/findings/remediation pipeline—are QA-specific. Removing them leaves only abstract contracts and a claim. Another domain would need its own verified-change source, identity resolution, normalization, influence semantics, completeness assertions, and validation.

Therefore Impact Evidence is *conceptually separable* from QA but not yet *commercially or technically validated* outside it. Do not fund cross-domain generalization in v1.

## 14. What Is Truly Novel?

Nothing at the mechanism level is novel:

- immutable snapshots and hashes are established content-addressing practice;
- provenance DAGs are standardized by W3C PROV and related systems;
- graphs and traversal are mature;
- policy/rule evaluation is mature;
- three-valued/unknown-aware logics are established;
- open-world reasoning is established in semantic-web systems;
- decision logs and replay manifests are established.

What remains distinct is the application contract:

1. bind a verified engineering change to source-qualified evidence;
2. treat positive impact and non-impact asymmetrically;
3. permit one qualified existential path to prove impact;
4. prohibit non-impact unless a bounded complete exclusion proof exists;
5. emit a stable contextual conclusion with proof/limitations rather than a path list or recommendation.

That combination is precise and useful, but it is a domain-specific composition of established ideas, not a new reasoning technology. Its value must be demonstrated through avoided false conclusions and usable downstream evidence, not architectural sophistication.

## 15. Kill Test

| Board objection | Honest answer |
|---|---|
| “There is no classifier, customer evidence, or benchmark. This is architecture astronautics.” | Correct. Fund only a narrow vertical slice and falsifiable evaluation, not the full platform vision. |
| “Neo4j plus Cypher already does impact analysis.” | It does traversal, not bounded evidence qualification or defensible absence. Use it if operationally justified; do not confuse it with conclusion semantics. |
| “W3C PROV already defines your provenance model.” | Largely correct. ADR-006 should specialize/map PROV instead of inventing a competing ontology. |
| “OPA/Drools can implement the rules.” | Correct. Rule execution is replaceable. The product value is the evidence/proof contract, not a custom engine. |
| “You invented ten concepts to express three enum values.” | Substantially correct for MVP. Reduce to frozen manifest, qualification, existing traversal, small proof projection, and conclusion. |
| “`NOT_AFFECTED` is impossible to prove in real engineering data.” | Often true. Defer it until a source can make credible bounded completeness assertions. Many valid results will remain `UNKNOWN`. |
| “Provenance payloads will become enormous.” | Likely. Start with references/hashes and one canonical proof; define deterministic packaging only after measured data. |
| “The existing remediation `ImpactAnalyzer` already owns impact.” | Naming is confusing, but semantics differ. Do not repurpose it; clarify boundaries and avoid duplicate public naming. |
| “The architecture is generic but all implementation is QA-specific.” | Correct. Cross-domain claims are potential only. Do not fund generic ontologies/adapters now. |
| “No storage/API/deployment design means this is not implementable.” | The core can be implemented as an in-process library without those decisions. A public product cannot; API/operations must follow demonstrated core value. |
| “Accepted ADRs froze speculative complexity.” | Partly correct. ADR-005 freezes necessary semantics. ADR-006 should be interpreted minimally and implemented through standards/reuse, not as a mandate for a new evidence platform. |
| “The product could just return paths plus warnings.” | That may satisfy users. The vertical slice must compare this simpler baseline against categorical conclusions before further funding. |

### Rejection reasons that survive

- No demonstrated user or economic need for defensible `NOT_AFFECTED`.
- No implemented classifier proving the contracts are usable.
- ADR-006 can generate excessive bespoke infrastructure.
- Cross-domain ambition is unsupported.
- Public naming conflicts with existing remediation impact.
- Industry standards/tool adoption strategy is not decided.

These are real. They should remain funding gates, not be reframed as minor follow-ups.

## 16. Final Verdict

**VIABLE WITH SIGNIFICANT RISK**

Impact Evidence deserves to exist as an independent **semantic capability**, but not yet as the broad architectural platform implied by the full evidence vision.

It earns independence because no current repository component or single industry product owns the exact boundary: contextual, deterministic impact conclusions from verified change and qualified frozen evidence, with asymmetric positive/exclusion proof and explicit `UNKNOWN`. Extending validation, coverage, remediation impact, or simulation would mix incompatible business meanings. Neo4j, W3C PROV, OPA/Drools/DMN, OpenLineage, Backstage, and similar products can supply mechanics or inputs but do not collectively enforce the conclusion contract without application-specific composition.

The risk is that the project builds all of that composition itself. The funded architecture should be reduced to:

1. accepted `VerifiedChangeSet` input;
2. one frozen normalized evidence manifest;
3. contract/integrity validation and minimal qualification;
4. existing traversal mechanics;
5. a small deterministic ADR-005 proof projection;
6. direct/propagated `AFFECTED` and reasoned `UNKNOWN`;
7. replay by retained manifest/hash/rule versions.

Remove “Evidence Graph” as a deployable layer. Map provenance to W3C PROV concepts. Treat rule engines and graph stores as replaceable implementation choices. Defer `NOT_AFFECTED`, multi-source conflict, general provenance infrastructure, cross-domain abstraction, public API, and operational platform work until the minimal classifier outperforms the simpler baseline of “paths plus warnings” in real use.

If the project refuses this reduction, the correct verdict becomes **REJECT**.

## References

### Repository evidence

- [Implementation Architecture Recovery Review](implementation-architecture-review.md)
- [Impact Evidence Gap Assessment](impact-evidence-gap-assessment.md)
- [Impact Evidence Architecture Review v1.0](impact-evidence-architecture-review-v1.0.md)
- [Conclusion Semantics Decision Analysis](impact-evidence-conclusion-semantics-decision-analysis.md)
- [Qualified Data and Provenance Decision Analysis](impact-evidence-qualified-data-provenance-decision-analysis.md)
- [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
- [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [`FinalChangeSetVerifier.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/FinalChangeSetVerifier.java)
- [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)
- [`ImpactAnalyzer.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java)

### Industry primary sources

- [Neo4j Cypher overview](https://neo4j.com/docs/cypher-manual/current/introduction/cypher-overview/) and [shortest paths](https://neo4j.com/docs/cypher-manual/current/patterns/shortest-paths/)
- [W3C PROV-O](https://www.w3.org/TR/prov-o/)
- [W3C OWL 2 overview](https://www.w3.org/TR/owl-overview/)
- [OpenLineage specification](https://github.com/OpenLineage/OpenLineage/blob/main/spec/OpenLineage.md)
- [OpenMetadata lineage model](https://openmetadatastandards.org/lineage/lineage/)
- [Backstage entity references](https://backstage.io/docs/features/software-catalog/references/) and [catalog graph](https://backstage.io/docs/features/software-catalog/creating-the-catalog-graph/)
- [Structurizr documentation](https://docs.structurizr.com/)
- [ArchUnit user guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Open Policy Agent integration](https://www.openpolicyagent.org/docs/integration) and [decision logs](https://www.openpolicyagent.org/docs/management-decision-logs)
- [Microsoft GraphRAG repository](https://github.com/microsoft/graphrag)

