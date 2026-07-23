# Impact Evidence Decision Analysis 02: Qualified Engineering Data and Provenance Contract

**Status:** Proposed decision analysis; not yet an ADR  
**Authority:** [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md) governs conclusion semantics  
**Target intent:** [`README.ru.codex.md`](../../README.ru.codex.md)  
**Current implementation baseline:** [Implementation Architecture Recovery Review](implementation-architecture-review.md)

## 1. Executive Summary

Impact Evidence should consume **normalized immutable evidence snapshots**, not live external systems and not unqualified Canonical QA Model JSON. Raw vendor data may enter through source adapters, but the deterministic core sees only a versioned, integrity-bound contract whose source, snapshot, datum identities, normalization lineage, and provenance are explicit.

The minimum contract has six layers:

```text
raw source payload
  -> normalized immutable snapshot
  -> structurally valid normalized datum
  -> context-qualified or rejected evidence
  -> derived proof candidate
  -> final proof governed by ADR-005
```

Normalization belongs before semantic classification and removes vendor-specific representation. Qualification belongs inside the future Impact Evidence capability because applicability, snapshot compatibility, rule support, trust, identity state, and later freshness/conflict decisions are analysis-context dependent. Capture and raw-payload custody remain outside the classifier. Derivation of paths/proof candidates belongs inside it and must retain parent evidence.

The recommended provenance design is a **hybrid**: every normalized datum has one immutable `ProvenanceRecord`; direct records point to a source locator/integrity reference, while derived records additionally contain a deterministically ordered set of parent datum references and a derivation rule/version. The connected records form an immutable provenance DAG without requiring a graph database or embedding complete ancestry into every datum.

Snapshot identity is not a timestamp. A reproducible snapshot is bound by source ID, stable snapshot ID, snapshot content fingerprint under a named canonicalization algorithm, schema and normalization versions, capture/effective-time facts, and optional lineage/revision references. A datum is bound by source ID + snapshot ID + source-local datum ID + content fingerprint. These combinations detect ID reuse, cross-snapshot reuse, duplication, and content substitution.

Qualification requires a result algebra rather than one enum. Successful evidence must carry the qualified datum and decisions supporting it; rejected/unresolved/conflicting/stale/incompatible/unsupported/untrusted outcomes require different evidence. Structurally malformed top-level contracts are failures. Well-formed but unusable data is retained as an evidence limitation and can lead to `UNKNOWN` under ADR-005.

This decision does not define completeness, freshness thresholds, conflict resolution, influence semantics, storage, transport, or final DTOs. It establishes the minimum immutable substrate those decisions need.

## 2. Definitions

| Term | Definition | Boundary |
|---|---|---|
| Raw engineering data | Source-native mutable or immutable payload, response, export, file, event, or record | Outside deterministic core; adapter input |
| Normalized engineering datum | Vendor-neutral immutable assertion with stable identity, content fingerprint, source snapshot binding, and provenance | Input contract accepted by the core after structural validation |
| Qualified engineering evidence | A normalized datum that satisfies all policies relevant to a particular analysis context and intended evidential use | Produced inside Impact Evidence qualification |
| Rejected evidence | A well-identified candidate that cannot be used, with retained categorical reason and diagnostics | Retained limitation; not silently dropped |
| Derived evidence | Deterministic output calculated from one or more evidence parents under a named versioned rule and context | Produced inside classifier; provenance-bound |
| Proof candidate | Direct or derived evidence assembled to attempt an ADR-005 proof obligation | Internal classifier state; may be accepted or rejected |
| Final proof | A proof candidate that satisfies all ADR-005 obligations for `AFFECTED` or bounded exclusion | Authoritative basis attached to a conclusion |

“Evidence” is contextual. Normalization does not make a datum qualified, and qualification for identity resolution does not automatically qualify it for relationship influence or exclusion.

## 3. Evidence Lifecycle

### 3.1 Stages and ownership

| Stage | Responsibility | Inside future classifier? |
|---|---|---|
| Capture | Obtain/freeze source-native content and source snapshot identity | No; integration/capture boundary |
| Normalize | Convert source-native fields to vendor-neutral assertions under a versioned rule; preserve original locator/hash | No semantic classification; adapter/normalization boundary |
| Contract validation | Verify required fields, references, fingerprint shape, canonical ordering rules, and local invariants | At classifier ingress or shared contract library |
| Bind context | Select verified change, subject, snapshots, policies, scope, rule versions, and reference time | Yes |
| Qualify | Assess applicability, identity state, snapshot compatibility, trust, relationship support, and later freshness/conflict policy | Yes |
| Derive | Resolve identities, traverse qualified relations, assemble paths and exclusion evaluations | Yes |
| Select proof | Apply ADR-005 proof obligations and precedence | Yes |
| Report | Retain classification, proofs/rejections, limitations, context and provenance references | Yes |

Adapters must not pre-classify impact. The classifier must not call a live adapter during evaluation. An adapter may normalize and publish a snapshot, but it cannot decide that its own data is complete, fresh, conflict-free, or influential except by supplying explicit assertions later evaluated under product policy.

### 3.2 Monotonic evidence strength

Transformation cannot silently strengthen evidence. A derived datum may be fit for a different purpose, but its trust, snapshot compatibility, identity certainty, and semantic support cannot exceed the weakest required parent unless the derivation rule explicitly produces and justifies a new assertion (for example, deterministic resolution from an authoritative mapping). Parent rejection/conflict remains visible. No derivation may turn “unresolved” into “resolved” without retained mapping evidence.

## 4. Source Contract

### 4.1 Conceptual model: `EvidenceSource`

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | `sourceId`; `sourceContractVersion`; vendor-neutral `sourceType`; `namespace`; non-empty ordered `capabilities`; `authorityClass` or authority-policy reference; `trustPolicyRef` |
| Optional semantic fields | declared source revision/version when it changes interpretation; parent/source lineage reference; validity interval of the source declaration |
| Optional descriptive metadata | display name; source-system label; owner/contact; human description; URI; operational tags |

These are conceptual names, not final DTO fields.

**Semantic distinctions:**

- `sourceId` is stable across snapshots and never reused for another logical source.
- `sourceType` is a vendor-neutral category or versioned vocabulary value, not a product/vendor enum. Unknown types may be represented but cannot imply capabilities.
- `namespace` says how source-local artifact/datum identifiers are interpreted. It is semantic, not display text.
- `capabilities` declare what kinds of facts the source can provide (for example artifact identity assertions or relationships). A capability is not a completeness claim.
- `authorityClass`/policy reference describes how assertions may be considered. It does not make them automatically trusted.
- `trustPolicyRef` is an explicit semantic input evaluated during qualification. Owner and display name are descriptive and cannot change classification.

**Invariants:** mandatory strings are non-blank; versions/policy references use supported identifier syntax; capability values are unique; source ID + contract version identifies one immutable source declaration; metadata cannot override semantic fields.

**Identity rule:** `sourceId` identifies the logical source; `(sourceId, sourceContractVersion, sourceDeclarationFingerprint)` identifies the exact declaration used. Reuse of the same identity with different semantic content is substitution and fails validation.

**Ordering:** sources sort lexically by normalized `sourceId`, then source contract version, then declaration fingerprint. Capability declarations sort by their canonical identifiers.

## 5. Snapshot Contract

### 5.1 Conceptual model: `EvidenceSnapshot`

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | `sourceRef`; `snapshotId`; `snapshotSchemaVersion`; `normalizationVersion`; `snapshotFingerprint` including algorithm/canonicalization version; `capturedAt`; snapshot payload/datum references |
| Conditionally mandatory semantic fields | `effectiveAt` or effective interval when source facts have business validity; source model/revision reference when compatibility depends on it |
| Optional semantic fields | parent snapshot reference(s); raw capture integrity reference; snapshot lineage rule; completeness-declaration references (opaque until follow-up ADR) |
| Optional metadata | label; capture operator; description; external locator; operational notes |

**Why timestamps are insufficient:** two snapshots can be captured at the same instant, a source can change without its timestamp changing, clocks can be imprecise, and a timestamp does not bind content. `capturedAt` records capture provenance; `effectiveAt` describes applicability time; neither proves identity or integrity.

**Immutability:** after publication, the exact semantic snapshot content, datum membership, ordering semantics, source declaration reference, and fingerprints cannot change. Corrections produce a new snapshot ID/fingerprint and optional parent/supersedes lineage.

**Invariants:** snapshot source matches every contained direct datum; datum membership is duplicate-free by full datum reference; fingerprint validates against canonical semantic content; schema/normalization versions are supported at processing time; `capturedAt` includes timezone/offset; an effective interval is well ordered; parent cannot equal self.

**Identity rule:** `(sourceId, snapshotId, snapshotFingerprint)` identifies the exact snapshot. `snapshotId` alone locates; the fingerprint detects content substitution. Reusing `(sourceId, snapshotId)` with a different fingerprint is a hard anti-substitution failure, not another version of the same snapshot.

**Ordering:** snapshots sort by source ID, snapshot ID, then fingerprint. Datum membership uses full `EvidenceDatumReference` ordering, never ingestion order unless source order itself is explicitly semantic and normalized.

Completeness declarations may be referenced now so replay does not lose them, but this analysis does not define their truth conditions or authorize `NOT_AFFECTED`.

## 6. Datum Identity Contract

### 6.1 Conceptual model: `EvidenceDatumReference`

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | `sourceId`; `snapshotId`; `sourceLocalDatumId`; `datumKind`; `contentFingerprint` with canonicalization/algorithm version |
| Conditionally mandatory | `derivationId` for derived data; parent/context references through provenance |
| Optional metadata | external display locator or label (not identity) |

**Canonical evidence ID:** the system may expose a deterministic ID derived from the mandatory tuple, but the tuple remains auditable. A random UUID is not sufficient. The ID must change when content or snapshot binding changes.

**Identity and reuse rules:**

- `(sourceId, snapshotId, sourceLocalDatumId)` may occur only once per snapshot.
- the same tuple with different content fingerprints is a substitution/invariant failure;
- the same source-local ID in different snapshots is allowed, but the full references are distinct;
- byte/content-identical data in different snapshots remain distinct observations; an optional common content fingerprint permits equivalence checks without collapsing provenance;
- duplicate full references in a snapshot are rejected or deterministically de-duplicated with an explicit duplicate diagnostic, never counted as independent evidence.

**Derived identity:** derived data use a deterministic identity calculated from context identity, derivation rule/version, derived datum kind, canonical output content, and the ordered unique parent references. Changing any parent, rule, context, or output changes identity.

**Ordering:** compare source ID, snapshot ID, datum kind, source-local/derivation ID, then content fingerprint using normalized Unicode/identifier rules fixed by contract version.

## 7. Artifact Identity Assertions

### 7.1 Conceptual model: `ArtifactIdentityAssertion`

An identity assertion is evidence that a source-local object corresponds—or may correspond—to an engineering artifact. It is not an unconditional mapping.

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | own `EvidenceDatumReference`; source-local artifact identity; source namespace; artifact type as source-normalized vocabulary; `resolutionState`; provenance reference |
| Conditionally mandatory | exactly one canonical identity for `RESOLVED`; ordered non-empty candidate identities for `AMBIGUOUS`; reason code for `UNRESOLVED`/`REJECTED`; mapping-evidence references for resolved or ambiguous mappings |
| Optional semantic fields | source-native artifact type; normalized aliases; identity-resolution rule/version; effective time |
| Optional metadata | display name; descriptive aliases; source location text |

Proposed categorical `resolutionState` values are `RESOLVED`, `UNRESOLVED`, `AMBIGUOUS`, and `REJECTED`. This is not probabilistic confidence. The Canonical QA schema's numeric `sourceReference.confidence` may be retained as source-native metadata/evidence but cannot determine resolution or Impact Evidence classification without a later explicit policy.

**Invariants:** resolved has one canonical identity and supporting mapping evidence; ambiguous has at least two unique candidates and no chosen winner; unresolved has no canonical identity; source-local identity matches the assertion's source namespace; aliases cannot create identity by themselves; artifact type normalization version is retained.

**Identity rule:** assertion identity follows the datum identity tuple. Two assertions about the same local object are separate evidence when sourced from different snapshots. Contradictory resolutions are not overwritten; they are inputs to later conflict handling.

**Ordering:** aliases and candidate identities are unique and lexically sorted by namespace then value. Assertions sort by datum reference. A canonical identity such as current `CanonicalIdentity` is a valid resolved target only within its supported Canonical QA namespace/version.

## 8. Relationship Evidence

### 8.1 Conceptual model: `RelationshipEvidence`

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | own `EvidenceDatumReference`; source/snapshot binding through that reference; source endpoint local identity/assertion reference; target endpoint local identity/assertion reference; normalized relationship type; direction; source-native relationship type; normalization rule/version; provenance reference; integrity/content fingerprint |
| Conditionally mandatory | effective time/interval where relationship validity is temporal; explicit assertion polarity (`POSITIVE` or explicit `NEGATIVE`) |
| Optional semantic fields | resolved canonical endpoint references; source-native relationship ID; relationship attributes allowed by the contract |
| Optional metadata | source location; label; explanatory text |

Direction is explicit even when a normalized relation name appears directional. Endpoint order is semantic and cannot be sorted/reversed. Source-native type is retained so normalization is auditable. The normalized type says what fact was normalized, not whether it propagates impact; the influence catalog is a later decision.

An explicit negative assertion means the source positively asserts absence/non-existence of a named relationship under its own semantics. It is distinct from a missing positive record. It is not sufficient for `NOT_AFFECTED` until source applicability, completeness, conflict, scope, freshness, and influence semantics satisfy ADR-005 and follow-up decisions.

**Invariants:** endpoints are non-blank and belong to the declared source namespace or explicit assertion references; source and snapshot match the datum reference; normalized/source-native types are non-blank; normalization rule version is supported for qualification; self-relations are retained/rejected according to normalized contract rules rather than silently removed; polarity is explicit if negative assertions are supported.

**Identity rule:** prefer a stable source-native relation ID as `sourceLocalDatumId`; otherwise derive a deterministic local ID from source-local endpoint identities, native type, direction/polarity, and stable source locator. Content fingerprint still binds all semantic content and detects reuse.

**Ordering:** relationship evidence sorts by datum reference. Within derived indexes, order by normalized source endpoint, type, direction/polarity, target endpoint, then full datum reference. Parent/source evidence order never changes edge direction.

## 9. Provenance Model

### 9.1 Alternatives

| Model | Strength | Limitation |
|---|---|---|
| Simple source reference | small and easy | cannot explain normalization, derivation, parents, or substitutions |
| Embedded parent chain | self-contained | duplicates ancestry, grows rapidly, awkward for shared parents |
| Immutable provenance graph | complete and reusable | can become infrastructure-heavy if required as a separate graph system |
| Hybrid records + parent references | direct facts stay small; derived ancestry remains navigable | replay bundle must include/resolve referenced records |

### 9.2 Recommendation: hybrid immutable provenance DAG

Use immutable `ProvenanceRecord` values referenced by datum. Direct records point to capture origin and integrity evidence. Normalized records identify normalization rule/version and the raw capture reference. Derived records contain ordered parent datum/provenance references and derivation context. Together they form a directed acyclic provenance structure; “DAG” is a contract invariant, not a database choice.

### 9.3 Conceptual model: `ProvenanceRecord`

| Category | Proposed fields |
|---|---|
| Mandatory semantic fields | `provenanceId`; provenance contract version; provenance kind (`CAPTURED`, `NORMALIZED`, `DERIVED`); source/snapshot reference; output datum reference; original locator or verifiable raw-content reference for captured/normalized data; rule ID/version for normalized/derived data; ordered parent datum/provenance references; provenance fingerprint |
| Conditionally mandatory | analysis-context identity for context-derived evidence; capture integrity/hash and canonicalization description when raw content is not retained |
| Optional semantic fields | tool identity/version; parent snapshot lineage; transformation parameters if semantic |
| Optional metadata | operator, human-readable explanation, display URI, processing host |

**Invariants:** captured provenance has no derived parents; normalized provenance links to captured content/reference and a normalization rule; derived provenance has at least one parent and a derivation rule/version; graph is acyclic; every parent resolves in the replay bundle or through a stable integrity-bound reference; output/source/snapshot bindings agree; fingerprint covers all semantic fields.

**Identity rule:** `provenanceId` is deterministic from provenance kind, output datum, rule/version, context where applicable, and ordered parents. Reusing it with different content is failure.

**Ordering:** parents are unique and sorted by full datum reference unless rule semantics explicitly require an ordered sequence; in that case positions are semantic and included in the fingerprint. Provenance records topologically sort parents before children, with provenance ID as tie-breaker.

The contract needs locatability or verifiability, not mandatory embedding of original payload bytes. A report can retain a stable locator plus hash when custody rules ensure later resolution; absence of raw bytes must be explicit.

## 10. Integrity and Anti-substitution

Integrity uses stable references plus content fingerprints over canonical semantic serialization. Signing infrastructure is not selected by this decision.

Minimum mechanisms:

1. **Versioned canonicalization:** every fingerprint identifies its canonicalization and digest algorithm version. Object/map keys and unordered sets use deterministic ordering; arrays retain order only when semantic.
2. **Snapshot binding:** snapshot fingerprint covers source declaration reference, schema/normalization versions, semantic times/revisions, completeness references, and sorted datum references/content fingerprints.
3. **Datum binding:** datum fingerprint covers all semantic fields, including endpoints, types, polarity, effective time, and provenance reference.
4. **Provenance binding:** provenance fingerprint covers source/snapshot/output, origin reference, rule/version, semantic parameters, context, and ordered parents.
5. **Reference consistency:** referenced source/snapshot/datum/provenance identities must resolve to matching fingerprints. Cross-source provenance attachment is invalid unless an explicit derived record lists the other-source parent.
6. **Version rejection:** unsupported normalization/canonicalization versions are never silently substituted.

Threat handling:

| Threat | Detection/response |
|---|---|
| Replace snapshot with another | snapshot ID + fingerprint mismatch; hard failure/unverifiable bundle |
| Change content without changing datum identity | datum fingerprint mismatch; malformed/untrusted outcome or bundle failure according to boundary |
| Reuse local datum ID with different content | uniqueness check on source + snapshot + local ID detects collision |
| Attach provenance from another source | source/snapshot/output consistency check rejects attachment |
| Normalize with unsupported version | retained `UNSUPPORTED_SEMANTICS`/qualification outcome when well formed; unsupported top-level contract version is failure |
| Duplicate same datum as independent corroboration | full reference/content de-duplication; duplicates cannot increase evidential strength |

Fingerprints provide integrity and anti-substitution within the supplied trust boundary; they do not prove who authored data. Source authentication/signatures may be added only through a later trust decision if required.

## 11. Qualification Outcomes

### 11.1 Conceptual model: `EvidenceQualificationResult`

One enum is insufficient because outcomes require different mandatory evidence. Use a conceptual sealed result algebra with a common datum/context reference and outcome-specific fields.

| Outcome | Mandatory content | Processing meaning |
|---|---|---|
| `Qualified` | datum; context; qualification policy/version; satisfied checks; qualified uses; provenance | usable only for stated purpose(s) |
| `Rejected` | datum reference where identifiable; stable reason/diagnostics; policy/version | well-formed candidate fails qualification |
| `Unresolved` | datum; unresolved identity/field; candidates/evidence where present | retained limitation |
| `Conflicting` | datum(s); conflict key; contradictory evidence refs; no hidden winner | retained limitation pending conflict policy |
| `Stale` | datum; effective/capture facts; explicit reference time; freshness policy/version | retained limitation, not exception |
| `IncompatibleSnapshot` | datum/snapshot; verified-change/model reference; compatibility reason | retained limitation |
| `UnsupportedSemantics` | datum; unsupported normalized/native type or normalization/influence version | retained limitation if contract itself is supported |
| `UntrustedProvenance` | datum/provenance; failed trust requirement | retained limitation |
| `MalformedDatum` | locatable candidate; structural diagnostic/path | rejected ingestion item when snapshot policy permits partial retention |

**Mandatory common semantic fields:** outcome kind; evidence datum or locatable reference; analysis context identity; policy/rule versions; deterministic diagnostics/reasons. Optional metadata is human explanation only.

**Invariants:** `Qualified` cannot contain a blocking failure; conflicting includes at least two distinct evidence references or one assertion plus a contradictory qualified declaration; stale includes explicit reference time; malformed cannot masquerade as a normalized usable datum; every outcome retains source/snapshot identity when recoverable.

**Identity/order:** result identity is deterministic from context, datum reference, qualification purpose, policy version, and outcome semantic content. Sort by datum reference, qualification purpose, outcome severity/order fixed by contract, then reason code.

### 11.2 Failures versus limitations

- Unsupported top-level evidence contract/canonicalization version, missing mandatory snapshot identity, fingerprint mismatch for the bundle, or malformed request is a processing failure.
- A structurally locatable but malformed individual datum may be retained as `MalformedDatum` only if snapshot contract explicitly supports partial ingestion and its exclusion from the semantic snapshot is fingerprinted. Otherwise the snapshot fails validation.
- Unresolved identity, relevant conflict, staleness, snapshot incompatibility, unsupported relationship semantics, and untrusted provenance are evidence limitations and can contribute to `UNKNOWN`.
- Internal exceptions/invariant violations remain failures, never qualification outcomes or `UNKNOWN`.

## 12. Adapter Boundary

Impact Evidence supports raw and normalized data through **separate boundaries**:

```text
vendor/source payload
    -> source-specific capture adapter
    -> versioned normalizer
    -> immutable normalized EvidenceSnapshot bundle
    -> contract validation
    -> deterministic Impact Evidence classifier
```

The core accepts only the normalized bundle. Source adapters own authentication, pagination, vendor schemas, rate limits, transport retries, and capture. Normalizers own source-native-to-contract mapping and retain native types/locators plus normalization provenance. Neither owns classification, influence semantics, freshness decisions, conflict resolution, or `NOT_AFFECTED`.

A caller that already has normalized data may bypass raw adapters and submit the same immutable contract. The classifier never queries live mutable source state during analysis. Any new live response is first frozen as a new snapshot.

No repository interface, persistence engine, message bus, or network protocol is selected here. A “port” is a semantic boundary, not an infrastructure requirement.

## 13. Derived Evidence

Traversal paths, identity-resolution outputs, normalized composite relationships, and later exclusion evaluations are derived evidence.

Every derived datum must retain:

- full ordered/unique parent `EvidenceDatumReference` values;
- parent qualification outcomes relevant to derivation;
- derivation rule ID/version and semantic parameters;
- analysis context identity;
- deterministic output content/fingerprint and derived identity;
- rejected parents or conflicts considered by the derivation, either directly or through an associated derivation diagnostic set;
- deterministic parent and output ordering.

A qualified path derived from relationship evidence contains references to every edge and endpoint identity assertion. `TracePath` currently contains only node and relationship values; an evidence-aware derived path would adapt its traversal result and add parent/provenance bindings rather than change `TraceEngine` business meaning.

Derived evidence cannot silently become stronger than a required parent. Rules include:

- one stale required edge makes that path stale;
- one unresolved required endpoint makes that path unresolved;
- a relevant conflict makes the proof candidate conflicting unless another independent path avoids it;
- unsupported normalization/influence semantics prevents qualification;
- rejected parents remain visible even if a different parent set yields a valid proof;
- de-duplication prevents identical parent evidence from masquerading as independent corroboration.

The qualifier may produce a stronger *resolution assertion* only when a versioned derivation rule and authoritative parent evidence explicitly justify that new assertion. This is new evidence with parents, not mutation of the original.

## 14. Serialization and Replay

Reproducibility means replaying the same frozen semantic inputs, not re-querying a source later.

Minimum replay bundle or resolvable manifest:

- source declarations and fingerprints;
- snapshot identities, fingerprints, schema/normalization versions, semantic times/revisions, and parent references;
- every normalized datum used or rejected materially, with full datum references and fingerprints;
- artifact identity assertions and resolution evidence;
- relationship evidence including native/normalized types, direction/polarity, endpoints and effective time;
- provenance records and resolvable/hash-verifiable original locators;
- verified-change stable reference (subject to follow-up ADR) and changed-artifact identities;
- analysis context, subject, scope reference, completeness references, qualification/influence/freshness policy versions and explicit reference time;
- derived evidence/proof references and deterministic rule parameters;
- canonical ordering/canonicalization versions.

Full external payload retention is not universally mandatory. One of these must be explicit:

1. raw payload embedded and integrity-bound;
2. immutable externally retained payload with stable locator plus content hash;
3. raw payload unavailable, but normalized semantic snapshot fully retained and original hash/locator recorded, limiting source-level re-audit.

A live URI without a captured hash is not reproducible. A timestamp without content identity is not reproducible. Re-running a query against today's source is a new capture and new snapshot, even if query parameters are identical.

Serialized map/set order cannot affect fingerprints or results. Canonicalization must define Unicode handling, scalar representation, field presence/defaults, time normalization, ordered versus unordered collections, and algorithm version.

## 15. Compatibility with Existing Architecture

| Existing type/component | Reusable meaning | Insufficiency / required adaptation |
|---|---|---|
| Canonical QA `sources` | source ID, type, name, version, external reference, URI, checksum provide useful capture descriptors | no immutable snapshot identity, capabilities, namespace, trust policy, effective time, normalization version, or source-declaration fingerprint |
| `sourceReferences` | locate source evidence with source ID, location, text and `evidenceType` | no snapshot/datum/provenance identity; optional numeric `confidence` is not categorical identity resolution and must not become Impact Evidence confidence |
| Canonical node IDs | valid resolved identities within one Canonical QA model | external sources may use other namespaces; IDs alone do not prove cross-source correspondence |
| `CanonicalIdentity` | validated exact Canonical QA v0.1 identity syntax and value | no namespace/source/snapshot or unresolved/ambiguous mapping state |
| `VerifiedChangeSet` | authoritative accepted in-process change evidence and direct-change basis under ADR-005 | exact-instance provenance is process-local; stable external binding remains a follow-up decision |
| `TraceRelationship` | minimal type/from/to traversal value | no datum/source/snapshot/native type/normalization/provenance/integrity/effective time/qualification |
| Validation diagnostics | immutable severity/layer/code/message/object/path pattern | validate model correctness, not evidence provenance or qualification; pattern can be reused |
| `QaModelFingerprintCalculator` | demonstrates versioned canonical JSON + SHA-256 fingerprinting and deterministic object-key/number normalization | package-private and specific to simulation/model semantics; evidence canonicalization requires its own reviewed contract and collection/time rules |
| immutable records/sealed outcomes | suitable evidence/result discipline | conceptual types must be decided before Java API creation |

Canonical Change's identity-sensitive evidence chain and `FinalChangeSetVerifier` anti-substitution checks are architectural precedents. They should not be weakened or treated as a serialized provenance solution. Remediation `ImpactAnalyzer`, `ImpactReport`, coverage metrics, and simulation remain outside evidence qualification.

## 16. Alternatives Considered

### 1. Reuse Canonical QA Model JSON directly

**Reject as the complete evidence contract.** It is a valuable normalized engineering model and can be adapted into one `EvidenceSnapshot`, but its source/source-reference shape lacks snapshot identity, datum fingerprints, namespaces, mapping states, derivation lineage, qualification policy, and anti-substitution coverage required here.

### 2. Store only source IDs on canonical nodes and relationships

**Reject.** A source ID cannot identify which snapshot supplied a fact, distinguish reused datum IDs, locate normalization rules, reconstruct parents, assess integrity, or replay analysis.

### 3. Introduce normalized immutable evidence snapshot contract

**Accept.** This is the deterministic core boundary. It is storage- and transport-neutral, supports heterogeneous source namespaces, and enables later scope/completeness/freshness/conflict decisions.

### 4. Let classifier query live external systems

**Reject.** Mutable responses, availability, pagination order, and source changes would affect classification outside explicit inputs and violate ADR-005 determinism. Capture adapters must freeze snapshots first.

### 5. Persist full raw payloads inside every report

**Reject as a universal requirement.** It duplicates large/sensitive data and conflates evidence custody with conclusions. Reports must retain sufficient normalized evidence and integrity-bound provenance; raw bytes may be embedded or externally retained by explicit custody policy.

### Recommended staged combination

Adopt alternative 3. Permit Canonical QA Model and vendor payloads only as adapter inputs. Retain full raw content where governance requires it; otherwise retain an immutable locator plus content hash and the complete normalized semantic evidence required for replay. Never replace frozen input with a live query.

## 17. Minimal Vertical Slice

The smallest deterministic classifier slice requires:

1. one accepted in-process `VerifiedChangeSet` changing one exactly identified canonical artifact;
2. one `EvidenceSource` declaration with stable ID, namespace, relationship capability, and explicit trust-policy reference;
3. one immutable `EvidenceSnapshot` with ID, content fingerprint, schema/normalization versions, captured/effective/model reference, and deterministic datum membership;
4. identity assertions for changed and target source-local objects, each resolved to one canonical identity or explicitly unresolved;
5. one positive `RelationshipEvidence` with source-local endpoints, one normalized type/direction, source-native type, normalization rule/version, content fingerprint, and provenance;
6. qualification policy/rule inputs sufficient to accept or reject source, snapshot, identity and relationship for the slice;
7. a derived path proof retaining identity/relationship parent references and derivation version;
8. sealed qualification outcomes and deterministic reason ordering.

Supported conclusions:

- **Direct `AFFECTED`:** exact subject matches the changed artifact in `VerifiedChangeSet`; engineering snapshot is not required for the direct proof.
- **Propagated `AFFECTED`:** resolved changed/target identities and the one relationship are fully qualified; derived path retains every parent.
- **`UNKNOWN` — unresolved identity:** target assertion is `UNRESOLVED` or `AMBIGUOUS`.
- **`UNKNOWN` — unqualified evidence:** relationship/source/provenance/snapshot fails qualification while remaining a well-formed limitation.

The slice must not emit `NOT_AFFECTED`. A missing relationship/path yields `UNKNOWN`, because scope and completeness semantics are intentionally deferred. It also excludes multi-source conflict, freshness thresholds, negative assertions as exclusion proof, and persistence/API choices.

## 18. Recommended Decision

Record in the subsequent ADR that:

1. the deterministic Impact Evidence core consumes only versioned normalized immutable evidence snapshots;
2. source-specific raw capture and normalization occur through adapters before classification, with raw origin and normalization provenance retained;
3. evidence identity is composite and integrity-bound: source + snapshot + local/derived datum identity + content fingerprint;
4. snapshot identity requires stable ID and canonical content fingerprint; timestamps alone are never identity;
5. artifact identity assertions support resolved, unresolved, ambiguous, and rejected categorical states without probabilistic confidence;
6. relationship evidence retains source/native and normalized semantics, direction/polarity, endpoints, normalization version, effective time, integrity and provenance, without deciding influence meaning;
7. provenance uses immutable hybrid records whose ordered parent references form an auditable DAG;
8. qualification uses an evidence-bearing sealed result model, not a single enum and not exceptions for normal limitations;
9. derived evidence retains every required parent, context and rule version and cannot silently exceed the strength of required parents;
10. fingerprints use versioned canonical semantic serialization and reference-consistency checks; signing/storage infrastructure is deferred;
11. deterministic replay uses frozen snapshots/bundles or integrity-bound resolvable manifests, never live source re-query;
12. Canonical QA Model source data may be adapted into the contract but is not sufficient as-is;
13. the first slice supports direct/propagated `AFFECTED` and evidence-limited `UNKNOWN`, but not `NOT_AFFECTED`.

## 19. Open Questions

The following require later ADRs or contract work:

1. Exact vocabulary and governance for vendor-neutral source types, capabilities, authority classes and trust policies.
2. Stable external `VerifiedChangeSet` reference and compatibility between change/Base/proposed state and evidence snapshots.
3. Formal analysis-scope and completeness-assertion contracts, including composition across sources.
4. Freshness policy vocabulary and the use of captured time versus effective time.
5. Cross-source identity resolution, authoritative mappings, ambiguity and conflict relevance/precedence.
6. Influence relationship catalog, directionality and versioning.
7. Whether explicit negative relationship assertions enter the initial normalized contract or a later version.
8. Canonical serialization specification for evidence types, including Unicode, times, absent/default fields and ordered collections.
9. Raw-payload custody requirements, locator durability, confidentiality/redaction and audit access.
10. Whether a snapshot may retain malformed individual candidates while excluding them from semantic membership, or must fail atomically.
11. Report packaging/truncation for complete provenance and rejected proof candidates.
12. Public library/API DTOs, error contract and compatibility/versioning.
13. Module name and dependency boundary relative to existing remediation `qa-impact-analysis`.

## 20. Proposed ADR Outline

**Title:** ADR-006 — Qualified Engineering Data and Provenance Contract

1. Status, date and owners.
2. Context: ADR-005 proof obligations require qualified, reproducible data.
3. Decision drivers: provenance, integrity, heterogeneous identities, deterministic replay, infrastructure neutrality.
4. Decision: normalized immutable snapshot as core boundary.
5. Evidence lifecycle and adapter ownership.
6. Source and snapshot identity contracts.
7. Datum identity, fingerprint and anti-substitution rules.
8. Artifact identity assertions and categorical resolution states.
9. Relationship evidence without influence semantics.
10. Hybrid provenance-record DAG.
11. Evidence-bearing qualification outcomes and failure boundary.
12. Derived-evidence strength/parent retention rules.
13. Serialization/replay minimum and raw-payload options.
14. Compatibility with Canonical QA sources, Canonical Change, traversal and fingerprints.
15. Consequences and rejected alternatives.
16. Minimal vertical slice and conformance invariants.
17. Follow-up decisions for scope/completeness, stable change binding, identity/conflict, influence, freshness, public contract and module boundary.

## Implementation Evidence Index

- authoritative conclusion semantics: [ADR-005](adr/ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
- Canonical QA source/source-reference contract: [`qa-model-v0.1.schema.json`](../../qa-model/src/main/resources/schema/qa-model-v0.1.schema.json)
- canonical identity: [`CanonicalIdentity.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/model/CanonicalIdentity.java)
- verified change/evidence: [`VerifiedChangeSet.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java), [`CanonicalBaseModelEvidence.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/root/CanonicalBaseModelEvidence.java)
- process-local provenance limitation: [`ADR-001-exact-instance-evidence-binding.md`](../adr/ADR-001-exact-instance-evidence-binding.md)
- current path value: [`TraceRelationship.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceRelationship.java), [`TraceEngine.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceEngine.java)
- validation diagnostic pattern: [`ValidationIssue.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/model/ValidationIssue.java)
- existing canonical fingerprint precedent: [`QaModelFingerprintCalculator.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/QaModelFingerprintCalculator.java)
- remediation impact kept separate: [`ImpactAnalyzer.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java), [`ImpactReport.java`](../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/model/ImpactReport.java)

