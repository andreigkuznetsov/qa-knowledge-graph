# Impact Evidence Minimal Vertical Slice Design

**Status:** Implementation design proposal  
**Governing decisions:** [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md), [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)  
**Scope control:** reduced architecture from [Architecture Challenge v1.0](impact-evidence-architecture-challenge-v1.0.md)

## 1. Executive Summary

The vertical slice is one framework-independent Java library that accepts one existing in-process `VerifiedChangeSet`, one compact frozen normalized evidence manifest, one subject artifact, and explicit rule/algorithm versions. It validates the inputs, resolves the subject, checks for an exact direct change, qualifies a small set of `DEPENDS_ON` relationship evidence, performs deterministic breadth-first traversal over qualified evidence, and returns either:

- `AFFECTED` with an exact direct-change or ordered relationship-path proof; or
- `UNKNOWN` with stable reasons and rejected/insufficient evidence references.

Invalid requests, unsupported versions, and integrity mismatches return a separate failure result. The slice contains no code path or domain value for `NOT_AFFECTED`.

The primary hypothesis is that a contextual conclusion with qualified proof/reasons prevents false certainty and is more usable than raw “path/no path plus warnings.” The deliberately narrow implementation tests that hypothesis without building a general evidence platform. It supports one source, one snapshot, one influence relation, categorical resolved/unresolved identities, minimal lineage references, and in-process replay.

Success would prove that the repository can compose verified change, frozen evidence, qualification, deterministic traversal, and ADR-005 projection into an auditable result while reusing existing architectural patterns. It would not prove multi-source reasoning, source completeness, non-impact, freshness, external replay, public API viability, cross-domain applicability, operational scale, or production readiness.

## 2. Problem Statement

The slice addresses one engineering problem:

> For a selected canonical artifact, can the system establish impact from an accepted change using only qualified frozen evidence, and otherwise return an explicit valid uncertainty without claiming non-impact?

This differs from existing capabilities:

- **Ordinary path finding:** `TraceEngine` answers whether a directed path exists in supplied Canonical QA JSON. It does not establish source/snapshot integrity, identity resolution, normalization support, or evidence qualification.
- **Remediation impact:** `ImpactAnalyzer` consumes `RoadmapReport` and `ExecutionPlan` and describes expected structural remediation. It neither consumes `VerifiedChangeSet` nor classifies an artifact.
- **Coverage:** `CoverageService` measures rule/scenario/check connectivity. Its percentages are not evidence completeness or impact proof.
- **Validation:** `QaModelValidationEngine` establishes model conformance and semantics. A valid model may still contain incomplete impact knowledge.
- **Simulation:** `ModelSimulationEngine` materializes and validates an explicitly supplied future model; it does not assess current evidence of impact.
- **Paths plus warnings:** a path finder can return a path and separately report data issues, but it does not define whether those issues invalidate the path, whether missing paths are valid uncertainty, or which proof has deterministic precedence.

Finding a structural path means that a sequence of relationships exists in input data. Establishing a qualified impact conclusion additionally means that changed and subject identities resolve exactly; every used relation belongs to the selected frozen snapshot; integrity and provenance references are consistent; the relation's normalized semantics/direction are supported; and the chosen proof is retained under deterministic precedence.

## 3. Product Hypothesis

### Primary hypothesis

**H1:** Given the same accepted `VerifiedChangeSet`, frozen evidence manifest, subject, and rule/algorithm versions, the slice produces a deterministic auditable `AFFECTED` or `UNKNOWN` conclusion that prevents unqualified or absent relationships from being interpreted as non-impact.

**Falsified if:** any invalid/unqualified path produces `AFFECTED`; no-path behavior implies non-impact; repeated equivalent inputs change classification/proof/reason order; or consumers cannot identify why the result was produced.

### Secondary hypotheses

| Hypothesis | Falsifying observation |
|---|---|
| H2: `UNKNOWN` communicates useful knowledge limits rather than hiding failure | Users cannot distinguish it from an error, its reasons are not actionable/understandable, or most valid inputs yield only generic `INSUFFICIENT_EVIDENCE` |
| H3: qualification prevents structural paths from becoming invalid proofs | A relationship with unresolved endpoints, wrong snapshot, bad fingerprint, unsupported type/direction, or missing provenance is used in a proof |
| H4: frozen-input replay is meaningful | Identical semantic inputs/rules yield different ordered results, or replay requires querying the source again |
| H5: current repository mechanics/patterns materially reduce implementation | the slice must duplicate Canonical Change verification, general validation, remediation analysis, or substantial graph infrastructure |
| H6: contextual conclusions add value over paths plus warnings | baseline users reach the same safe decisions with less effort and no greater false certainty, while classifier complexity/debugging is materially worse |

## 4. Scope

### In Scope

- exactly one accepted in-process `VerifiedChangeSet`;
- exactly one evidence source and one frozen snapshot manifest;
- one requested subject identity;
- resolved or explicitly unresolved source-local artifact identities;
- positive `DEPENDS_ON` relationship evidence between `BUSINESS_RULE` artifacts;
- versioned contract, normalization, qualification, influence, fingerprint, and algorithm identifiers;
- structural/reference/fingerprint validation;
- minimal qualification with evidence-bearing accepted/rejected outcomes;
- exact direct-change detection;
- deterministic qualified path discovery;
- `AFFECTED` and `UNKNOWN` only;
- direct/path proofs, unknown reasons, rejected evidence references;
- deterministic in-process replay and library tests.

### Out of Scope

- `NOT_AFFECTED` and completeness proof;
- global/general scope model;
- multiple sources, source arbitration, or conflict resolution;
- confidence/probability;
- freshness-sensitive conclusions;
- Neo4j, RDF persistence, Evidence Graph component, provenance store;
- Drools, OPA, DMN, generic rule/proof engines;
- proprietary provenance framework;
- REST, Spring Boot, persistence, brokers, distributed processing;
- source SDK/integration framework or live capture;
- raw payload custody;
- universal ontology/cross-domain generalization;
- operational security or deployment design.

### Deferred after the funding gate

Scope/completeness and `NOT_AFFECTED`, multi-source conflict, stable external `VerifiedChangeSet` identity, richer influence/freshness policies, public report/API, additional source adapters, raw custody, operations, and other domains. These do not impose extension-point requirements on this slice.

## 5. Supported Scenarios

### Scenario 1 — Directly changed artifact

The resolved subject canonical identity exactly matches a declaration in the accepted `VerifiedChangeSet`.

- Result: `AFFECTED`.
- Selected proof: direct-change proof containing the exact `VerifiedChangeSet` instance, declaration index, canonical identity, category, and change kind.
- Manifest paths are irrelevant to precedence, though additional valid evidence may be summarized deterministically.

### Scenario 2 — Transitively affected artifact

The changed artifact is `BR-BASE`; the subject is `BR-DEPENDENT`; the manifest contains a qualified source relation `BR-DEPENDENT DEPENDS_ON BR-BASE`. Impact propagates from the dependency target to the dependent source.

- Result: `AFFECTED`.
- Selected proof: ordered influence path `BR-BASE -> BR-DEPENDENT`, retaining the original normalized relationship evidence reference and its reversed propagation interpretation.

### Scenario 3 — No path in limited evidence

Changed and subject identities resolve, but qualified traversal reaches no subject.

- Result: `UNKNOWN`.
- Reason: `NO_QUALIFIED_IMPACT_PROOF` (the slice-specific precise form of insufficient evidence).
- Never `NOT_AFFECTED`.

### Scenario 4 — Unresolved subject identity

The source-local subject assertion has no single canonical identity.

- Result: `UNKNOWN`.
- Reason: `UNRESOLVED_SUBJECT_IDENTITY`.
- Candidate/local identity and assertion reference remain in output.

### Scenario 5 — Unqualified relationship

A structural path exists, but an edge has a mismatched snapshot, unsupported type/direction, unresolved endpoint, bad datum reference, or missing provenance.

- Result: `UNKNOWN` if no independent qualified proof exists.
- Reason: `NO_QUALIFIED_IMPACT_PROOF`, plus stable rejected relationship references and qualification reasons.

### Scenario 6 — Invalid evidence input

The manifest contract is malformed, uses unsupported contract/fingerprint versions, has duplicate identity reuse with different content, or its manifest fingerprint does not match semantic content.

- Result: analysis failure (`INVALID_MANIFEST`, `UNSUPPORTED_VERSION`, or `INTEGRITY_MISMATCH`).
- No conclusion object and no `UNKNOWN`.

### Scenario 7 — Deterministic replay

Repeated evaluation uses the same exact accepted `VerifiedChangeSet`, semantically identical frozen manifest, subject/context, and rule/algorithm versions. Input list orders are shuffled before manifest canonicalization.

- Result: semantically equal output with identical proof/reason/rejection ordering and fingerprint/reference values.

### Scenario 8 — Direct proof precedence

Subject is directly changed and also reachable through a qualified relationship path.

- Result: `AFFECTED`.
- Selected proof: direct-change proof.
- Additional qualified path count/references may be retained in canonical order; they never change the selected proof.

## 6. Minimal Domain Model

These are proposed Java-domain shapes, not final code. The slice uses records, small enums, and sealed outcomes. No builders, generic metadata bags, or speculative interfaces are needed.

| Type | Purpose and essential fields | Invariants | Reuse decision / necessity |
|---|---|---|---|
| `ImpactEvidenceRequest` record | `VerifiedChangeSet`, `FrozenEvidenceManifest`, `SubjectArtifactRef`, `SliceAnalysisContext` | all non-null; context versions match manifest-supported versions | Introduce; one explicit invocation boundary |
| `SubjectArtifactRef` record | source-local subject ID | non-blank; exactly one source manifest supplies its assertion | Introduce; subject cannot be global mutable state |
| `SliceAnalysisContext` record | qualification rule version, influence rule version, algorithm version | exact supported constants; no time or attribute map | Introduce; makes semantics contextual/replayable |
| `FrozenEvidenceManifest` record | contract version, source ID, snapshot ref, normalization version, fingerprint version/value, identity assertions, relationships, provenance references | immutable sorted copies; one source/snapshot; unique IDs; fingerprint covers canonical semantic fields | Introduce compact subset of ADR-006 |
| `EvidenceSnapshotRef` record | source ID, snapshot ID, semantic fingerprint | non-blank; source matches manifest; fingerprint format/version valid | Introduce; timestamp intentionally absent because freshness is out of scope |
| `ArtifactIdentityAssertion` record | assertion ID, snapshot ref, local ID, `NodeType`, `IdentityResolution`, content fingerprint, provenance ref | unique local/assertion IDs; resolution state consistent; fingerprint valid | Introduce; source IDs may differ from canonical IDs |
| `IdentityResolution` sealed interface | `Resolved(CanonicalIdentity)` or `Unresolved(reasonCode)` | resolved exactly one canonical identity; unresolved has stable non-blank reason | Introduce only two slice states; ambiguity is represented as unresolved reason, not a third implementation |
| `RelationshipEvidence` record | datum ID, snapshot ref, local source/target IDs, `RelationshipType`, direction/native type, normalization version, content fingerprint, provenance ref | positive only; unique ID; exact endpoint assertions exist; semantic fields fingerprinted | Introduce; `TraceRelationship` lacks evidence fields |
| `ProvenanceRef` record | provenance ID, origin locator/hash, normalization activity/version | non-blank; fingerprint/hash syntax; referenced by existing datum | Introduce minimal conceptual PROV reference; not a subsystem |
| `RelationshipQualification` sealed interface | `QualifiedRelationship(evidence, resolvedPropagationFrom, resolvedPropagationTo)` or `RejectedRelationship(evidenceRef, reasons)` | qualified endpoints resolved and supported; rejected has sorted non-empty reasons | Introduce; outcome-specific evidence is required by ADR-006 |
| `ImpactEvidenceResult` sealed interface | `Completed(ImpactConclusion)` or `Failed(ImpactAnalysisFailure)` | exactly one outcome | Introduce; failures stay outside conclusion algebra |
| `ImpactConclusion` record | subject, `ImpactClassification`, `ImpactProof`, unknown reasons, exact change evidence reference, snapshot ref, context, rejected evidence refs, additional-proof summary | `AFFECTED` has proof/no unknown reasons; `UNKNOWN` has no proof/non-empty reasons; no other classification | Introduce authoritative contextual output |
| `ImpactClassification` enum | `AFFECTED`, `UNKNOWN` | contains no `NOT_AFFECTED` constant | Introduce slice-local public vocabulary; compile-time prevention |
| `ImpactProof` sealed interface | `DirectChangeProof` or `RelationshipPathProof` | only analyzer-owned factories/constructors produce valid proofs | Introduce; proof variants require different fields |
| `DirectChangeProof` record | exact `VerifiedChangeSet` reference, declaration index, identity, category, change kind | declaration at index is same accepted declaration and identity | Introduce; uses existing evidence without reverification |
| `RelationshipPathProof` record | changed identity, subject identity, ordered non-empty `QualifiedPathStep` list | contiguous, simple path; all steps qualified; endpoints match | Introduce; auditable propagated proof |
| `QualifiedPathStep` record | propagation from/to canonical IDs and original relationship evidence reference | direction follows influence rule; reference resolves | Introduce; preserves normalized edge lineage |
| `UnknownReason` enum | `UNRESOLVED_SUBJECT_IDENTITY`, `NO_QUALIFIED_IMPACT_PROOF` | stable order encoded explicitly | Introduce; minimal usable reasons only |
| `ImpactAnalysisFailure` record | failure code, stable diagnostics/paths | non-empty diagnostics; no conclusion | Introduce for invalid request/manifest/integrity/version/incompatible change |

`ArtifactIdentity` need not be another type: resolved canonical identity reuses `CanonicalIdentity`; source-local identity is a string constrained within the assertion. A generic `AnalysisContext` is not reused because `qaip-core.AnalysisContext` contains timestamps and a generic attributes map, both contrary to this slice's explicit semantic input requirement.

## 7. Input Contracts

### Verified change input

The request directly retains the existing package-constructed `VerifiedChangeSet`. The slice does not call `FinalChangeSetVerifier` again and cannot accept `RejectedChangeSet` because the Java type excludes it. Direct change candidates are read from the accepted change's ordered `declaredChangeSet().changes()`/retained verified evidence. A direct proof retains the same `VerifiedChangeSet` object and declaration index.

Temporary limitation: the slice is in-process only. Replay requires the same accepted evidence instance, consistent with Canonical Change's exact-instance binding and its accepted ADR. No ID, serialization, hash, or cross-process substitution mechanism is invented.

### Frozen evidence input

The compact `FrozenEvidenceManifest` contains only:

- one source ID;
- one `EvidenceSnapshotRef` with snapshot ID and semantic manifest fingerprint;
- manifest contract, fingerprint/canonicalization, and normalization versions;
- immutable artifact identity assertions;
- immutable positive `RelationshipEvidence` values;
- minimal `ProvenanceRef` values used by those assertions/relationships.

It contains no raw payload, timestamps, completeness, trust hierarchy, arbitrary metadata, negative assertion, multiple sources, or storage locator policy. Manifest fingerprint covers canonical ordered semantic fields including every assertion/relationship/provenance reference and content fingerprint. Per-datum content fingerprints are supplied and validated for format/reference consistency; the manifest validator recomputes the whole manifest fingerprint. The design does not require raw-content re-hashing because raw custody is excluded.

### Subject and context

The subject is the source-local ID named by `SubjectArtifactRef`, resolved through exactly one manifest assertion. Context contains only exact qualification, influence, and algorithm version constants. The combination `(exact VerifiedChangeSet instance, manifest fingerprint, source-local subject, context versions)` defines the slice analysis context. Classification is never stored on the canonical artifact.

## 8. Minimal Qualification Rules

A relationship qualifies only when all rules pass:

1. relationship source/snapshot equals the manifest source/snapshot;
2. relationship datum ID is unique and its content fingerprint/reference is valid;
3. both local endpoint IDs resolve to exactly one manifest assertion;
4. both assertions are `Resolved`, have matching source/snapshot, valid fingerprints, and referenced provenance;
5. relationship has a present, resolvable provenance reference;
6. relationship type is exactly `DEPENDS_ON`;
7. both resolved endpoint types are `BUSINESS_RULE`;
8. normalized storage direction is source-dependent → target-dependency;
9. normalization version and qualification/influence versions exactly equal supported slice versions;
10. the evidence is not structurally rejected by manifest validation.

Each failed well-formed relationship produces one `RejectedRelationship` with all applicable qualification reasons in explicit enum order. It remains auditable and cannot enter traversal. Top-level structural/reference/fingerprint failures stop the analysis instead.

Freshness is deferred. The slice makes no freshness-sensitive claim, contains no clock/reference time, and must be documented as accepting the frozen snapshot exactly as supplied. This is safe for demonstrating positive-path mechanics but insufficient for production propagation or any exclusion conclusion.

## 9. Influence Semantics

The slice supports one rule:

| Normalized relationship | Stored source type | Stored target type | Impact propagation | Justification |
|---|---|---|---|---|
| `DEPENDS_ON` | `BUSINESS_RULE` (dependent) | `BUSINESS_RULE` (dependency) | target → source | if rule A explicitly depends on rule B, an accepted change to B has a direct dependency route to A; this is the narrow semantics encoded by the relation name and allowed type pair |

The initial rule ID/version is a fixed semantic input such as `ie-influence-business-rule-depends-on-v1`; the exact constant is finalized in implementation tests, not dynamically configured.

`RelationshipRules` already permits `BUSINESS_RULE DEPENDS_ON BUSINESS_RULE`, establishing structural legality only. The slice adds the explicit reverse propagation interpretation. All other `RelationshipType` values are rejected as `UNSUPPORTED_RELATIONSHIP_TYPE` and retained. They never silently propagate. No general influence ontology or registry is created.

## 10. Processing Algorithm

1. Require non-null request parts. Return `INVALID_REQUEST` failure for absent/blank user inputs; constructor invariant violations remain programming errors.
2. Require the concrete accepted `VerifiedChangeSet` and supported schema version. Return `INCOMPATIBLE_VERIFIED_CHANGE` for a well-formed but unsupported change/model version.
3. Validate manifest contract/normalization/fingerprint versions, one-source/one-snapshot invariants, unique identities, reference resolution, per-datum fingerprint formats, and recomputed manifest fingerprint. Return failure for any structural or integrity error.
4. Resolve the source-local subject assertion. If structurally absent/duplicate, manifest/request failure as appropriate; if explicitly unresolved, return completed `UNKNOWN` with `UNRESOLVED_SUBJECT_IDENTITY`.
5. Enumerate ordered accepted change declarations. If the resolved subject canonical identity exactly matches a declaration, construct the canonical direct proof and remember any optional additional path summary.
6. Qualify every relationship independently in canonical datum-reference order. Produce qualified or rejected outcomes.
7. Project each qualified stored `dependent DEPENDS_ON dependency` relation into one propagation step `dependency -> dependent`. Sort adjacency by propagation target canonical ID, then original evidence reference.
8. Run breadth-first search from all changed `BUSINESS_RULE` canonical identities in deterministic order. Use a visited identity set to prevent cycles. Maintain predecessor step/source root.
9. When subject is reached, reconstruct the first canonical minimum-hop path. Source roots are ordered by declaration index then canonical identity; adjacency ordering breaks equal-hop ties. No “shortest path sophistication” beyond deterministic BFS is required.
10. Apply precedence: if direct proof exists, select it; else select found qualified path proof; else return `UNKNOWN` with `NO_QUALIFIED_IMPACT_PROOF` and sorted rejected evidence references.
11. Return `Completed`. Failures from steps 1–3 never create conclusions.

### Ordering and duplicates

- changes: declaration index, then canonical identity;
- assertions: local ID, assertion ID, content fingerprint;
- relationships: datum ID, content fingerprint;
- qualification reasons: explicit enum order;
- adjacency: propagation target, evidence datum ID/fingerprint;
- rejected references: same relationship order;
- proofs: direct always selected; otherwise minimum hops and canonical BFS tie-breakers.

Two full relationship references that are identical are duplicate input and fail manifest validation; same logical endpoints/type with distinct datum IDs are allowed but only the first canonical edge participates in predecessor selection. Others are retained as additional qualified evidence count/references, not independent strength claims.

### Traversal bound

No arbitrary hop limit is needed for the slice. Finite manifest membership plus visited canonical identities bounds BFS and handles cycles. A manifest-size guard is operational/API work and out of scope.

## 11. Reuse of Existing Components

| Candidate | Decision | Reason |
|---|---|---|
| `VerifiedChangeSet` | REUSE DIRECTLY | accepted in-process change and exact retained evidence; type cannot represent rejected outcome |
| `FinalChangeSetVerifier` | DO NOT REUSE in analyzer | verification already occurred; rerunning violates downstream ownership/no-recalculation and accepts the wrong input stage |
| `QaModelValidationEngine` | DO NOT REUSE for manifest | validates Canonical QA JSON, not the compact evidence manifest; reuse would conflate model validity/evidence qualification |
| `TraceEngine` | REUSE PATTERN ONLY | BFS is suitable, but class is a Spring component in `qa-model-validator`, consumes raw `JsonNode`, throws service exceptions, follows stored direction, depends on insertion order for tie-breaking, and loses evidence IDs. Direct dependency violates library boundary. Implement one small evidence-specific BFS, not a generic traversal framework |
| canonical `NodeType` / `RelationshipType` | REUSE DIRECTLY | slice supports existing `BUSINESS_RULE`/`DEPENDS_ON` vocabulary without duplicating enums |
| `CanonicalIdentity` | REUSE DIRECTLY | exact Canonical QA v0.1 resolved identity with constructor invariants |
| canonical JSON node/relationship objects | DO NOT REUSE as domain types | nodes/relationships are `JsonNode`, and lack source/snapshot/datum/provenance identity; manifest uses narrow typed records |
| `QaModelFingerprintCalculator` | REUSE PATTERN ONLY | versioned canonical SHA-256 precedent is correct, but implementation is package-private and defines JSON-model number/array semantics rather than manifest record/set semantics. Define one slice canonical manifest serialization and use standard digest utilities |
| `ValidationIssue` / change diagnostics | REUSE PATTERN ONLY | stable code/message/object/path and immutable lists are suitable; layers/severity are model-validation-specific |
| `List.copyOf`, records, sealed outcomes | REUSE DIRECTLY as JDK patterns | existing repository convention and immediate immutability value |
| architecture tests | ADAPT | add dependency rules for the new module; current repository has no identified general architecture-test suite enforcing this exact boundary |

The two non-reuse decisions involving traversal/hash have concrete contract and dependency incompatibilities. Their replacements are slice-specific functions, not duplicate subsystems.

## 12. Proposed Module Boundary

Create one new framework-independent Gradle `java-library` module:

- proposed module: `qa-impact-evidence-core`;
- proposed root package: `ru.kuznetsov.qaip.evidence`;
- public package: `ru.kuznetsov.qaip.evidence.analysis` and necessary immutable result/model packages only;
- package-private validation, qualification, fingerprint, and path-finding helpers.

This name avoids the existing remediation-oriented `qa-impact-analysis` and states the new capability explicitly. A package inside `qa-model-change` would incorrectly make change verification own downstream conclusion semantics. A package inside `qaip-analysis` would bind the slice to a generic orchestration SPI that the in-process evidence contract does not need.

Proposed dependencies:

```text
qa-impact-evidence-core
  -> qa-model-change      (`VerifiedChangeSet`, `CanonicalIdentity`)
  -> qa-model             (`NodeType`, `RelationshipType`)

No reverse dependency.
```

If `qa-model-change` does not expose `qa-model` transitively—as its build uses `implementation`—the explicit `qa-model` dependency is required. The module must not depend on Spring, `qa-model-validator`, coverage, findings, roadmap, execution planner, remediation impact, simulation, `qaip-analysis`, HTTP, or persistence. Jackson is unnecessary if the compact manifest fingerprint uses a deliberately specified field serialization; if used solely for canonical serialization, it must be an implementation dependency and not leak into the public contract.

## 13. Public Java API

One concrete stateless class is sufficient; no interface has two implementations.

```java
public final class ImpactEvidenceAnalyzer {
    public ImpactEvidenceResult analyze(ImpactEvidenceRequest request);
}
```

### Responsibility

The analyzer validates the invocation/manifest, qualifies evidence, detects direct change, finds a qualified path, applies precedence, and constructs an immutable result. Helpers are package-private and owned by this use case.

### Result shape

`ImpactEvidenceResult` is sealed:

- `Completed(ImpactConclusion conclusion)`;
- `Failed(ImpactAnalysisFailure failure)`.

Expected evidence limitations are `Completed(UNKNOWN)`. Invalid request/contract/integrity/version inputs are `Failed`. Nulls are rejected at domain constructors where they are programming misuse; `analyze(null)` may throw `NullPointerException` consistently with repository domain services, while missing values representable in a parsed request yield `Failed(INVALID_REQUEST)`. Impossible post-construction invariant violations may throw `IllegalStateException`; they must not be caught as `UNKNOWN`.

No asynchronous, generic engine SPI, repository, builder, plugin, or REST contract is designed.

## 14. Result Contract

### `AFFECTED`

Contains:

- source-local subject and resolved `CanonicalIdentity`/`NodeType`;
- `ImpactClassification.AFFECTED`;
- selected `DirectChangeProof` or `RelationshipPathProof`;
- exact in-process `VerifiedChangeSet` reference through proof/context plus declaration/root evidence reference;
- `EvidenceSnapshotRef` and manifest fingerprint;
- qualification/influence/algorithm versions;
- optional deterministic additional-qualified-path/reference count, not full alternative proof enumeration in v1;
- rejected evidence references retained when present but non-blocking.

### `UNKNOWN`

Contains:

- source-local subject and resolved identity when available;
- `ImpactClassification.UNKNOWN`;
- one or more stable ordered reasons;
- sorted rejected relationship/assertion evidence references relevant to the attempt;
- exact in-process verified-change evidence reference;
- snapshot/manifest reference and rule/algorithm versions;
- no `ImpactProof`.

### Failure

`ImpactAnalysisFailure` uses codes:

- `INVALID_REQUEST`;
- `INVALID_MANIFEST`;
- `INTEGRITY_MISMATCH`;
- `INCOMPATIBLE_VERIFIED_CHANGE`;
- `UNSUPPORTED_CONTRACT_VERSION` (including normalization/fingerprint/algorithm variants through precise diagnostic codes);
- `INTERNAL_INVARIANT_VIOLATION` only if the API deliberately converts a detected impossible state rather than throwing.

Failures contain deterministic diagnostics and no classification. Ordinary unresolved/unqualified evidence is never a failure unless it makes the manifest structurally inconsistent rather than explicitly unresolved/rejected.

## 15. Provenance Strategy

There is no provenance subsystem. `ProvenanceRef` is a small immutable value inside the manifest, and selected proof steps refer to the original relationship datum and its provenance reference.

Minimum audit answers:

- where relation came from: manifest source/snapshot plus origin locator/hash in `ProvenanceRef`;
- how normalized: normalization activity/rule version;
- which snapshot contains it: `EvidenceSnapshotRef` carried by datum;
- which evidence forms proof: ordered `QualifiedPathStep` references.

Conceptual W3C PROV mapping avoids incompatible terminology:

| Slice concept | PROV concept |
|---|---|
| frozen snapshot, assertion, relationship datum | `Entity` |
| normalization operation | `Activity` |
| source/tool identifier | `Agent` where responsibility matters |
| normalized relationship from source capture | `wasDerivedFrom` |
| datum produced by normalization | `wasGeneratedBy` |

The slice does not require RDF, PROV-O serialization, a provenance graph, store, query engine, or complete PROV implementation.

## 16. Replay Contract

The slice claims only deterministic **in-process semantic replay**. Replay inputs are:

- the same exact accepted `VerifiedChangeSet` instance;
- a semantically identical frozen manifest with the same canonical fingerprint;
- the same subject local ID;
- the same qualification, influence, canonicalization/fingerprint, and algorithm versions.

Semantic equality means identical:

- result variant and classification/failure code;
- subject/resolved identity;
- selected proof kind and ordered proof content/evidence references;
- unknown reasons and rejected references in order;
- snapshot/manifest fingerprints and rule versions.

Object instance equality, diagnostic stack traces, execution duration, collection construction order, and generated test names are not semantic. The analyzer reads no clock, random source, environment variable, network, filesystem, or mutable global data. No replay service or live source fetch exists.

## 17. Test Strategy

### Unit tests

- `FrozenEvidenceManifestTest`: uniqueness, immutable copies, supported versions, reference invariants.
- `ManifestFingerprintTest`: stable canonical fingerprint; shuffled input order; semantic mutation mismatch.
- `IdentityResolutionTest`: resolved/unresolved invariants.
- `RelationshipQualificationTest`: every rule in section 8 independently accepted/rejected; stable reason order.
- `DirectChangeDetectorTest`: added/modified/removed exact identities and non-match.
- `QualifiedPathFinderTest`: reverse `DEPENDS_ON` propagation, minimum hops, cycles, duplicate logical edges, deterministic tie-break.
- `ConclusionProjectorTest`: direct > path > unknown; no possible `NOT_AFFECTED`.
- `FailureBoundaryTest`: invalid manifest/version/integrity never creates `UNKNOWN`.

### Contract and integration tests

- `ImpactEvidenceDirectAffectedTest` — Scenario 1.
- `ImpactEvidencePropagatedAffectedTest` — Scenario 2.
- `ImpactEvidenceNoPathUnknownTest` — Scenario 3.
- `ImpactEvidenceUnresolvedSubjectUnknownTest` — Scenario 4.
- `ImpactEvidenceUnqualifiedEdgeUnknownTest` — Scenario 5.
- `ImpactEvidenceInvalidManifestFailureTest` — Scenario 6.
- `ImpactEvidenceReplayDeterminismTest` — Scenario 7.
- `ImpactEvidenceDirectProofPrecedenceTest` — Scenario 8.
- `ImpactEvidenceAdversarialAbsenceTest` — missing subject relation, valid model/high coverage fixtures cannot enter slice classification as non-impact.

### Determinism tests

Generate equivalent manifests with shuffled assertions, relations, provenance records, and changed declaration source fixtures where possible. Run repeatedly and compare canonical semantic result serialization/records. Diamond paths and cycles prove canonical path selection independent of hash/set iteration.

### Mutation/adversarial tests

- reflection/public-API compilation test establishes no `NOT_AFFECTED` enum/value/factory;
- mutate one endpoint fingerprint/snapshot/provenance/type and assert it cannot yield path proof;
- replace malformed manifest with integrity mismatch and assert `Failed`, never `UNKNOWN`;
- make subject unresolved and assert no proof constructor can accept it;
- mismatch relation snapshot and assert rejected audit reference remains;
- shuffle equal-length paths and assert the same path proof;
- duplicate the same evidence under identical reference and assert validation failure/de-duplication cannot increase evidence count.

### Architecture tests

Add a test that imports the new module's classes and prohibits dependencies on Spring and packages/modules for validator API, coverage, findings, roadmap, execution, remediation impact, simulation, HTTP, and persistence. If ArchUnit is not already a dependency, a Gradle dependency inspection/convention test is preferable to adding it solely for one assertion.

## 18. Evaluation Against Simpler Baseline

Use the same eight fixtures with two implementations:

### Baseline: paths plus warnings

- run structural traversal over normalized relationship records without qualification;
- return path/no path;
- attach validation/warning list separately;
- consumer must decide whether warning invalidates path and what no path means.

### Proposed slice

- validate/qualify evidence before traversal;
- return contextual `AFFECTED` or `UNKNOWN`;
- select direct/path proof or explicit reasons;
- retain rejected evidence;
- separate invalid input from valid uncertainty.

### Evaluation criteria

| Criterion | Measurement/question |
|---|---|
| False-certainty prevention | Can either output be misread as non-impact for no path? Do unqualified edges appear as impact? |
| Explainability | Can an engineer identify the exact accepted/rejected evidence and rule in one inspection? |
| Reproducibility | Does shuffled/repeated frozen input yield identical ordered semantic output? |
| Complexity | domain type count, production LOC, rule count, test-to-code ratio, concepts a consumer must learn |
| Consumer usability | time/errors when five engineers independently interpret each scenario without oral guidance |
| Debugging effort | time to locate why an apparent structural path was rejected |
| Rejected evidence | is rejected edge/identity information preserved and linked, or lost in warnings? |

The classifier fails the value test if the baseline produces equally safe, consistently interpreted, reproducible decisions with materially less code/conceptual burden; if users ignore classification/proof and prefer raw paths; or if `UNKNOWN` reasons do not improve diagnosis compared with warnings. That result stops expansion and triggers simplification or termination.

## 19. Acceptance Criteria

Implementation is accepted only when:

- all eight mandatory scenarios pass through the public library API;
- the public classification vocabulary and every factory contain no `NOT_AFFECTED` path;
- exact direct proof deterministically wins over a path proof;
- unresolved or unqualified paths cannot produce `AFFECTED`;
- invalid request/manifest/integrity/version input cannot become `UNKNOWN`;
- semantically identical frozen inputs produce identical ordered semantic outputs;
- `VerifiedChangeSet`, canonical enums/identity, and immutable JDK patterns are reused directly;
- traversal/fingerprint non-reuse is documented by the concrete incompatibilities in section 11 and replacements remain slice-specific;
- core has no Spring, HTTP, persistence, roadmap, execution, remediation-impact, simulation, or live-source dependency;
- no Evidence Graph component, generic proof/rule engine, or provenance framework exists;
- ADR-005 constraints and adversarial cases are executable tests;
- the complete slice runs through standard Gradle library tests;
- baseline comparison results are recorded rather than assumed.

## 20. Exit Criteria and Funding Gate

Expansion is allowed only if the slice demonstrates:

- fewer unsafe/misinterpreted results than paths plus warnings;
- `UNKNOWN` reasons understandable and useful to target consumers;
- manageable domain/code/test complexity;
- deterministic replay without external state;
- real reuse of `VerifiedChangeSet`, canonical vocabulary, and repository patterns;
- audit proof concise enough to inspect and sufficient to trace evidence;
- no need for speculative graph/provenance/rule/integration infrastructure.

Stop or redesign if:

- consumers prefer and safely interpret raw paths;
- qualification rejects little or provides no practical protection;
- manifest/fingerprint/provenance complexity exceeds conclusion value;
- evidence-specific traversal cannot remain small and causes major duplication;
- result/proof distinction is difficult to explain;
- `UNKNOWN` dominates with generic/non-actionable reasons;
- proof output is too verbose to use or too weak to audit;
- the implementation begins creating generic extension frameworks;
- the slice cannot demonstrate deterministic behavior without infrastructure.

Any move to `NOT_AFFECTED`, multiple sources, public API, persistence/operations, or another domain requires a separate post-evaluation decision. Passing code tests alone is not the funding gate; semantic advantage over the baseline is mandatory.

## 21. Implementation Plan

| Step | Resulting artifact | Depends on | Validation | Stop condition |
|---:|---|---|---|---|
| 1 | `qa-impact-evidence-core` Gradle module and dependency-boundary test | accepted module name/dependencies | compile empty library; prohibited dependency check | module requires Spring or downstream analysis dependency |
| 2 | minimal records/enums/sealed outcomes | module | constructor/invariant/immutability tests | illegal affected/unknown/proof combinations remain constructible |
| 3 | canonical manifest validation/fingerprint | domain model and one fixed canonicalization spec | mutation, duplicate, shuffled-order, version tests | stable fingerprint cannot be specified simply |
| 4 | subject resolution/direct change detector | `VerifiedChangeSet`, assertions | direct/unresolved/version fixtures | direct identity cannot be read without redefining change verification |
| 5 | minimal relationship qualification | manifest + fixed rule versions | rule-by-rule rejection/audit tests | rejected evidence cannot remain visible |
| 6 | evidence-specific deterministic BFS | qualified relations and fixed `DEPENDS_ON` propagation | cycles, diamond, tie, duplicate tests | traversal expands into generic framework or loses evidence refs |
| 7 | direct/path proof construction | detector + BFS | proof invariants/lineage tests | unresolved/unqualified parent can enter proof |
| 8 | ADR-005 slice projection | outcomes/proofs | precedence and failure separation tests | any absence maps to non-impact or failure maps to unknown |
| 9 | replay/determinism fixtures | complete analyzer | repeated/shuffled semantic equality tests | hidden clock/order/global state changes output |
| 10 | baseline comparator/evaluation fixtures | same scenario corpus | measured interpretation/debug exercise and report | no semantic/usability gain over baseline |
| 11 | repository validation | complete slice | module tests, full Gradle tests, `git diff --check` | existing capability regressions or architecture-boundary violation |

## 22. Open Questions

Only four choices block implementation; each can be resolved within this design and tests rather than a new ADR because it does not change ADR-005/006 or deferred product semantics.

| Question | Why it matters now | Recommended slice default | ADR? |
|---|---|---|---|
| Exact module/package name | prevents collision with remediation impact and fixes Gradle boundary | `qa-impact-evidence-core`, `ru.kuznetsov.qaip.evidence` | No; accept in implementation review |
| Exact supported version identifiers | replay and qualification require stable constants | one constant each for manifest contract, normalization, fingerprint/canonicalization, qualification, influence, and algorithm; reject all others | No; record in test fixtures/domain constants |
| Canonical manifest serialization | integrity tests need exact bytes/fields/order | UTF-8 length-prefixed or canonical JSON specification over sorted semantic values; select the smallest already supported approach during implementation, document golden vectors | No separate ADR for slice; ADR required before public interoperability |
| Direct change set includes removed artifacts | removed subject may not exist in manifest, but exact direct change still proves the changed identity | accept `ADDED`, `MODIFIED`, and `REMOVED` as direct `AFFECTED` using declaration identity; subject may be addressed by canonical ID via a resolved assertion | No; ADR-005 direct proof already governs |

Freshness, completeness, conflicts, external verified-change identity, public serialization/API, and source capture are not blockers because they are excluded, not unanswered slice requirements.

## 23. Final Recommendation

**READY WITH EXPLICIT ASSUMPTIONS**

The slice is implementable once the four defaults in section 22 are confirmed during implementation review. Its scope is sufficiently small: one library, one source/snapshot, one influence rule, two conclusions, two proof forms, two unknown reasons, and a separate failure algebra. It exercises the distinct semantic value while avoiding every platform component rejected by the architecture challenge.

The exact recommended next Codex task is:

> implement the minimal vertical slice according to this specification.

## Red-Team Guardrail Verification

| Guardrail | Verification |
|---|---|
| Evidence Graph component | absent; relations are immutable manifest values indexed inside analyzer |
| Generic proof engine | absent; two proof records plus fixed precedence |
| Generic rules platform | absent; fixed qualification predicates and one influence rule |
| Provenance subsystem | absent; one `ProvenanceRef` value and conceptual PROV mapping |
| Source integration framework | absent; manifest is supplied directly |
| Public service/API | absent; one in-process Java class |
| `NOT_AFFECTED` | absent from enum, algorithm, and factories; adversarial test enforces absence |
| Multi-source reasoning | absent; manifest invariant requires one source/snapshot |
| Cross-domain abstractions | absent; uses Canonical QA `BUSINESS_RULE`/`DEPENDS_ON` |
| Infrastructure decisions | absent; no storage, transport, broker, database, deployment, or security design |

## References

- [Implementation Architecture Recovery Review](implementation-architecture-review.md)
- [Impact Evidence Gap Assessment](impact-evidence-gap-assessment.md)
- [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
- [ADR-006](adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [Impact Evidence Architecture Review v1.0](impact-evidence-architecture-review-v1.0.md)
- [Architecture Challenge v1.0](impact-evidence-architecture-challenge-v1.0.md)
- [`VerifiedChangeSet.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java)
- [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)
- [`RelationshipRules.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/validation/RelationshipRules.java)
- [`QaModelFingerprintCalculator.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/QaModelFingerprintCalculator.java)

