# ADR-006: Qualified Engineering Data and Provenance Contract

- Status: Accepted
- Date: 2026-07-23
- Decision owners: Impact Evidence architecture

## Context

[ADR-005](ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
requires auditable proofs and deterministic classification from explicit inputs.
Canonical QA Model sources, `sourceReferences`, canonical relationships, and
`TraceEngine` provide useful facts and traversal mechanics, but do not bind each
datum to an immutable source snapshot, normalization rule, fingerprint, identity
resolution, or complete provenance.

The evidence boundary must support heterogeneous source namespaces and later
scope, completeness, freshness, conflict, and influence decisions without
depending on a vendor payload, live external system, storage engine, transport,
or graph database.

## Decision Drivers

- Deterministic and replayable classification inputs.
- Source, snapshot, datum, and derivation provenance.
- Detection of content substitution and identity reuse.
- Vendor-independent normalized semantics.
- Explicit unresolved, conflicting, stale, incompatible, unsupported, and
  untrusted evidence.
- Compatibility with ADR-005 and current immutable/sealed-result patterns.
- No premature dependency on storage or transport infrastructure.

## Decision

The deterministic Impact Evidence core consumes only **versioned normalized
immutable evidence snapshots**. Raw source/vendor payloads are captured and
normalized through adapter boundaries before classification. Semantic
evaluation never queries a live mutable external system.

A normalized snapshot is not qualified evidence by itself. Qualification is
contextual and produces evidence-bearing outcomes. Evidence identity is bound
to source, snapshot, datum identity, semantic content fingerprints, and
versioned canonicalization/normalization rules.

Canonical QA Model data may be adapted into this contract but is insufficient
as the complete evidence contract. This ADR defines conceptual obligations, not
final Java types or transport DTOs.

## Evidence Lifecycle

```text
raw source payload
  -> normalized immutable snapshot
  -> structurally valid normalized datum
  -> qualified or rejected evidence
  -> derived proof candidate
  -> final proof under ADR-005
```

Capture and source-specific normalization precede the deterministic core.
Contract validation occurs at ingress. Context binding, qualification,
derivation, and proof selection belong to Impact Evidence. Normalizers do not
classify impact, establish influence, or infer completeness.

## Source Contract

An evidence source declaration includes, at minimum:

- stable source identity;
- source-contract version;
- vendor-neutral source category;
- source identity namespace;
- declared capabilities;
- authority/trust-policy reference;
- integrity-bound source declaration reference.

Source-system labels, display names, owners, descriptions, and operational URIs
are descriptive metadata unless a later policy explicitly makes them semantic.
A capability declaration states what facts a source can provide; it is not a
completeness assertion.

The source ID is never reused for another logical source. The exact source
declaration is identified by source ID, contract version, and declaration
fingerprint.

## Snapshot Contract

Snapshot identity requires:

- stable source identity and source declaration reference;
- stable snapshot ID;
- snapshot schema version;
- normalization version;
- content fingerprint with canonicalization/digest version;
- capture time;
- effective time, interval, model revision, or equivalent reference when
  semantically required;
- deterministic datum membership.

A timestamp alone is never snapshot identity. Capture time records when content
was frozen; effective time/revision records when it applies; the fingerprint
binds actual semantic content.

Published semantic content and membership are immutable. Corrections create a
new snapshot ID/fingerprint and may retain parent/supersession lineage. Reusing
the same source/snapshot ID with a different fingerprint is a hard substitution
failure.

Completeness declarations may be retained by reference, but this ADR does not
define their semantics or authorize exclusion conclusions.

## Datum Identity and Anti-substitution

Every normalized datum has an integrity-bound identity containing:

- source ID;
- snapshot ID;
- source-local datum ID or deterministic derived datum ID;
- datum kind;
- semantic content fingerprint and algorithm/canonicalization version.

The same source/snapshot/local datum tuple may occur only once. Reuse with
different semantic content is a hard substitution failure. The same local ID in
another snapshot identifies a distinct observation. Content-equivalent data in
different snapshots remain provenance-distinct.

Derived datum identity is deterministic from analysis context, derivation
rule/version, output kind and content, and ordered unique parent references.
Random UUIDs may be metadata but cannot provide semantic integrity.

Fingerprints cover canonical semantic serialization. Unsupported
canonicalization or normalization versions are rejected and never silently
replaced. Duplicate evidence is de-duplicated/diagnosed and cannot increase
evidential strength.

## Artifact Identity Assertions

A source-local object is related to an engineering artifact through an explicit
identity assertion with one categorical state:

- `RESOLVED`
- `UNRESOLVED`
- `AMBIGUOUS`
- `REJECTED`

A resolved assertion names exactly one canonical target and retains supporting
mapping evidence. An ambiguous assertion retains at least two candidates without
choosing a hidden winner. An unresolved assertion has no canonical target.
Aliases and source-native artifact types may be retained, but do not establish
identity alone.

No probabilistic confidence score determines identity resolution. The numeric
`confidence` currently allowed in Canonical QA `sourceReferences` may remain
source-native information; it has no classification meaning without a separate
accepted policy.

## Relationship Evidence

Relationship evidence retains:

- source and snapshot binding;
- its own integrity-bound datum identity;
- source and target endpoint identities or identity-assertion references;
- normalized relationship type;
- source-native relationship type;
- explicit direction;
- normalization rule/version;
- provenance and content fingerprint;
- effective time/interval when required;
- optional explicit polarity for a positive or explicit negative assertion.

Endpoint order remains semantic. Missing positive relationship data is not an
explicit negative assertion. An explicit negative assertion is still not an
exclusion proof without the later applicability, scope, completeness,
freshness, conflict, and influence decisions.

Relationship normalization records what source fact means in the normalized
vocabulary. It does not determine whether or how that relation propagates
impact. Influence semantics are governed separately.

## Provenance Model

Provenance uses immutable hybrid records whose references form a directed
acyclic provenance structure:

- captured records identify source origin, snapshot, source locator/content
  reference, and integrity;
- normalized records identify raw origin and normalization rule/version;
- derived records identify analysis context, ordered parent evidence, and
  derivation rule/version.

Each record binds its output datum and semantic content fingerprint. Parent
references resolve consistently and cycles are invalid. Parent order is
canonical unless the derivation explicitly declares sequence as semantic.

This provenance DAG is a contract invariant, not a graph-database requirement.
Shared references avoid embedding and duplicating every ancestry chain while
preserving audit navigation.

## Qualification Outcomes

Qualification uses an evidence-bearing result algebra, not a single enum. It
distinguishes at least:

- qualified evidence;
- rejected evidence;
- unresolved identity;
- conflicting evidence;
- stale evidence;
- incompatible snapshot;
- unsupported semantics;
- untrusted provenance;
- malformed datum.

Outcomes retain the datum/reference, analysis context, policy/rule versions,
stable reasons/diagnostics, and outcome-specific evidence. Well-formed but
unusable evidence remains visible as a limitation and may contribute to
`UNKNOWN` under ADR-005.

Structurally invalid top-level contracts, unsupported contract or
canonicalization versions, missing snapshot identity, fingerprint mismatch, and
internal invariant failures are processing failures. They are not qualification
limitations and must not become `UNKNOWN`.

## Adapter Boundary

```text
source-native payload
  -> capture adapter
  -> versioned normalizer
  -> immutable normalized snapshot bundle
  -> deterministic classifier
```

Adapters own source authentication, transport, pagination, vendor schemas,
capture, and source-native mapping. The normalized bundle retains native types,
origin locators/hashes, and normalization provenance. The classifier accepts the
same contract regardless of how it was delivered.

No adapter call occurs during semantic evaluation. A new live response is a new
capture and snapshot, even when the same query is repeated.

## Derived Evidence

Derived evidence retains:

- every required parent evidence reference;
- relevant parent qualification outcomes;
- derivation rule/version and semantic parameters;
- analysis context identity;
- deterministic output identity/content fingerprint;
- rejected parents and relevant conflicts considered by derivation.

Derived evidence cannot silently become stronger than its weakest required
parent. A stale required edge makes that path stale; an unresolved required
endpoint makes it unresolved; a relevant parent conflict remains visible. A new
stronger identity assertion is permitted only when an explicit versioned rule
and authoritative parent evidence justify it; it is a new derived assertion,
not mutation of its parents.

## Serialization and Replay

Deterministic replay uses frozen snapshots or integrity-bound resolvable
manifests containing source declarations, snapshot/datum/provenance references
and fingerprints, schema/normalization/canonicalization versions, semantic
times/revisions, rule/policy versions, analysis context, and derived evidence
lineage. Re-querying a live source is a new capture, not replay.

Full raw payload retention is not universally mandatory. The contract supports:

1. embedded integrity-bound raw payload;
2. immutable external custody with stable locator and content hash;
3. retained normalized semantic snapshot plus original locator/hash and an
   explicit source-level audit limitation.

A live URI without a captured integrity reference is not reproducible. Raw
payload absence and resulting audit limitations are explicit. This ADR selects
no custody/storage technology.

## Compatibility with Existing Architecture

- Canonical QA `sources` provide useful IDs/types/versions/locators/checksums,
  but lack immutable snapshot and datum/provenance identities, capabilities,
  namespaces, and qualification state.
- `sourceReferences` provide locations and source-native evidence metadata, but
  lack snapshot, fingerprint, normalization, and derivation binding.
- canonical node IDs and `CanonicalIdentity` are valid resolved identities in
  their Canonical QA namespace, not universal source identities.
- `VerifiedChangeSet` remains the accepted change/direct-proof foundation under
  ADR-005; stable external binding requires a follow-up ADR.
- `TraceRelationship`/`TraceEngine` may supply traversal values/mechanics, but
  do not qualify evidence.
- validation diagnostics, immutable records, defensive copies, and sealed
  outcomes are reusable implementation patterns.
- simulation's canonical fingerprinting is a precedent, not the final evidence
  canonicalization contract.

Remediation `ImpactAnalyzer`/`ImpactReport`, coverage percentages, missing trace
paths, and simulation results remain outside this capability. QA coverage is not
evidence completeness.

## Consequences

### Positive

- Classification inputs become deterministic and replayable.
- Source and derivation provenance remains auditable.
- The core remains vendor and infrastructure independent.
- Snapshot/datum substitution and accidental ID reuse are detectable.
- Unusable evidence remains explicit instead of being silently discarded.
- Heterogeneous source identity namespaces and ambiguity are representable.
- Existing immutable and sealed-result implementation patterns can be reused.

### Negative

- Evidence contracts and retained state are larger.
- Canonical serialization, fingerprints, and reference validation add
  complexity.
- Source adapters and versioned normalizers assume additional responsibilities.
- Rejected evidence, conflicts, and limitations require storage/transport space
  wherever replay bundles are kept.
- Raw-payload custody, confidentiality, and locator durability remain separate
  governance concerns.
- Public classifier implementation still depends on accepted scope,
  completeness, freshness, identity/conflict, influence, and API decisions.

## Alternatives Considered

- **Canonical QA Model JSON as the full contract:** rejected; adaptable but
  lacks the required snapshot, datum, provenance, identity-state, and integrity
  semantics.
- **Only source IDs on artifacts/relations:** rejected; cannot bind snapshots,
  detect reuse/substitution, or replay normalization/derivation.
- **Live external queries during classification:** rejected; mutable state and
  availability would become hidden semantic inputs.
- **Timestamps as snapshot identity:** rejected; time does not bind content.
- **Random UUIDs without semantic fingerprints:** rejected; they locate but do
  not detect content substitution.
- **Full raw payload in every report:** rejected as a universal rule; custody is
  separable when normalized evidence and integrity-bound locators/hashes are
  retained.
- **One qualification enum:** rejected; outcome-specific evidence and failure
  distinctions would be lost.

Normalized immutable evidence snapshots are accepted as the deterministic core
boundary.

## Conformance Requirements

An implementation conforms only if:

- live source state cannot influence semantic classification;
- a source/snapshot ID cannot be reused with different fingerprinted content;
- a datum ID cannot be reused within one snapshot with different content;
- unsupported contract/canonicalization/normalization versions are never
  silently substituted;
- source, snapshot, datum, and provenance references resolve consistently;
- duplicate evidence cannot increase evidential strength;
- unresolved and conflicting evidence is never silently discarded;
- derived evidence retains required parent lineage and qualification outcomes;
- semantic qualification never reads an implicit wall clock;
- deterministic ordering is defined for sources, snapshots, data, parents,
  outcomes, reasons, and diagnostics;
- raw-payload absence and audit limitations are explicit;
- structurally invalid contracts and integrity failures do not become
  qualification limitations or `UNKNOWN`;
- no `NOT_AFFECTED` implementation is permitted before acceptance of the scope
  and completeness ADR;
- the first slice is limited to direct/propagated `AFFECTED` and `UNKNOWN` from
  unresolved identity or unqualified evidence.

## Follow-up Decisions

Separate ADRs are required for:

1. source capability and trust vocabulary;
2. stable external `VerifiedChangeSet` binding;
3. analysis scope and completeness assertions;
4. deterministic freshness policy;
5. identity ambiguity and source-conflict handling;
6. influence-semantics catalog and versioning;
7. canonical evidence serialization;
8. raw-payload custody;
9. public report and API contract;
10. classifier module naming and boundary relative to remediation impact.

No persistence technology, graph database, message broker, or transport is
selected by this ADR.

## References

- [Qualified Engineering Data and Provenance decision analysis](../impact-evidence-qualified-data-provenance-decision-analysis.md)
- [ADR-005: Impact Conclusion Semantics and Proof Obligations](ADR-005-impact-conclusion-semantics-and-proof-obligations.md)
- [Impact Evidence product specification](../../../README.ru.codex.md)
- [Canonical QA Model schema](../../../qa-model/src/main/resources/schema/qa-model-v0.1.schema.json)
- [`CanonicalIdentity.java`](../../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/model/CanonicalIdentity.java)
- [`VerifiedChangeSet.java`](../../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/VerifiedChangeSet.java)
- [Canonical Change ADR-001: Exact-instance evidence binding](../../adr/ADR-001-exact-instance-evidence-binding.md)
- [`TraceRelationship.java`](../../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/trace/TraceRelationship.java)
- [`QaModelFingerprintCalculator.java`](../../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/QaModelFingerprintCalculator.java)
- [`ImpactAnalyzer.java`](../../../qa-impact-analysis/src/main/java/ru/kuznetsov/qaip/impact/service/ImpactAnalyzer.java)

