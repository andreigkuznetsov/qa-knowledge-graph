# Scenario Authority Manifest Semantic Outcome V1 Contract

Contract identifier: `scenario-authority-manifest-semantic-outcome-v1`

Status: normative subordinate contract of ADR-014 and ADR-015. This document is
not an ADR and does not broaden either ADR.

Date: 2026-08-20

Owner: Evidence Governance

Normative basis:

- [ADR-014: Logical Source Authority Snapshot and Qualification Boundary](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015: Logical Source Semantic Canonicalization and Fingerprint Contract](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)
- [Scenario Authority Scenario Identity Group V1](scenario-authority-scenario-identity-group-v1.md)
- [Scenario Authority Semantic Provenance V1](scenario-authority-semantic-provenance-v1.md)

## Purpose and boundary

`ManifestSemanticCompositionOutcomeV1` represents the semantic availability of
one structurally admitted Scenario Authority Manifest occurrence. Structural
admission is a prerequisite established by the approved normalization and
admission boundary; it is not an outcome decided here.

This contract does not redefine structural admission, authority attribution,
Scenario duplicate classification, authority qualification, or Operation and
Business Rule resolution. It introduces no fingerprint domain.

## Contract identifiers

The exact V1 identifiers are:

| Contract role | Identifier |
| --- | --- |
| Manifest semantic-outcome contract/version | `scenario-authority-manifest-semantic-outcome-v1` |
| Manifest semantic composition-attempt version | `scenario-authority-manifest-semantic-composition-attempt-v1` |
| Normalized Manifest semantic input version | `scenario-authority-normalized-manifest-semantic-composition-input-v1` |
| Outcome vocabulary version | `scenario-authority-manifest-semantic-outcome-vocabulary-v1` |
| Unavailable-reason vocabulary version | `scenario-authority-manifest-semantic-unavailable-reason-v1` |
| Verified admitted Manifest proof version | `scenario-authority-verified-admitted-manifest-v1` |

An unsupported or substituted identifier is a processing/compatibility failure.
It is not a Manifest semantic-unavailable reason. This contract does not change
the existing `ManifestSemanticFingerprint` version, domain, encoding, digest,
canonical sequence, authored ordering, or golden bytes.

## Closed outcome vocabulary

Evidence Governance derives exactly one of two authoritative states:

- `COMPOSED`
- `UNAVAILABLE`

The unavailable-reason vocabulary contains exactly:

- `SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`

There is no `OTHER`, `UNKNOWN`, generic, exception-derived, or caller-defined
state or reason. Exact child `ScenarioOccurrenceCompositionOutcomeV1` values
retain whether child unavailability is
`UNSUPPORTED_SEMANTIC_CONTRACT`,
`SCENARIO_COMPOSITION_INTEGRITY_FAILURE`, or a mixture. The Manifest outcome
does not duplicate or summarize that child taxonomy.

## Verified admitted Manifest

`VerifiedAdmittedManifestV1` is the authoritative proof-bearing source for one
structurally admitted Manifest before semantic composition. It binds exactly:

```text
VerifiedAdmittedManifestV1 {
  verifiedAdmittedManifestVersion
  RepositoryCaptureAttestation
  ParentCapturedMemberRef
  ManifestOccurrenceIdentity
  exact claimed authority
  exact structural-admission result and proof
  exact normalized Manifest datum
  derived authored Scenario declarations[] {
    exact normalized Scenario declaration occurrence
    exact RFC 6901 structural location
    zero-based authored Scenario index
  }
  all required source-processing, admission, normalization,
    identity, and Manifest semantic V1 identifiers
}
```

Its controlled construction proves positively that the parent member belongs to
the exact attested Repository Capture; the occurrence identity agrees with the
parent source, snapshot, capture fingerprint, and member path; claimed authority
agrees with the attributed member; structural admission is authoritative; and
the normalized Manifest belongs to that exact admitted member. Its authored
Scenario declarations, structural locations, and indexes are derived from that
exact normalized Manifest and are never accepted as an independent caller list.

`VerifiedAdmittedManifestV1` may be created only from the already-approved
complete Scenario source processing and admission boundary containing the
verified Repository Capture, exact attributed member, structural-admission
result, and exact normalized admitted Manifest. Callers cannot independently
combine a capture attestation, parent member, occurrence identity, normalized
Manifest, authority, or authored Scenario list. Construction performs no
filesystem rediscovery.

The derived declaration sequence inside `VerifiedAdmittedManifestV1` is the
sole authoritative enumeration truth for Manifest semantic composition.

## Authoritative normalized input

Evidence Governance owns the finite typed
`NormalizedManifestSemanticCompositionInputV1` boundary. Conceptually it binds:

```text
NormalizedManifestSemanticCompositionInputV1 {
  normalizedManifestSemanticInputVersion
  manifestSemanticOutcomeContractVersion
  manifestSemanticCompositionAttemptVersion
  outcomeVocabularyVersion
  unavailableReasonVocabularyVersion

  VerifiedAdmittedManifestV1
  candidate authoritative ScenarioOccurrenceCompositionOutcomeV1[]
}
```

The verified Manifest contains the exact identity and data needed to construct
the existing `ManifestSemanticFingerprintInput`: source normalization version,
Manifest semantic contract version, claimed authority, format, schema version,
Scenario identity scheme, and complete authored Scenario sequence. Candidate
child outcomes prove the semantic result of each expected child; they define
neither child count, child identity, nor authored order. The Scenario
fingerprint sequence is derived only from revalidated child outcomes and is
never accepted as an independent caller claim.

The verified Manifest and candidate child outcomes must bind to the same
capture, member, authority, and admitted Manifest occurrence. The input excludes
parsed `JsonNode`, raw JSON, human diagnostics, qualification, resolved targets,
timestamps, Git or host data, and other operational metadata.

## Exact child completeness and order

The authoritative attempt derives expected membership and order only from
`VerifiedAdmittedManifestV1`, then verifies total authored-Scenario accounting:

- every authored Scenario declaration appears exactly once;
- no authored declaration is omitted;
- no extra child outcome appears;
- no child belongs to another Manifest, member, capture, or authority;
- every child's normalized occurrence and structural location match the exact
  authored declaration;
- child order equals exact authored Manifest array order; and
- duplicate Scenario declarations remain distinct occurrence evidence.

For every expected declaration, correspondence is field-by-field across the
Manifest occurrence identity, `ParentCapturedMemberRef`, Repository Capture
identity and fingerprint, claimed authority, Scenario occurrence identity,
structural path/location, zero-based authored index, claimed Scenario identity,
and exact normalized Scenario occurrence input represented by the child proof.
The complete child outcome sequence is verified against the sole enumeration
truth. A caller-selected enumeration, subset, permutation, duplicate, or
independently supplied fingerprint list is never authoritative.

### Child proof revalidation

Typed public shape alone does not make a child outcome authoritative. Before
Manifest availability is decided, Evidence Governance revalidates every
candidate through the approved Scenario occurrence proof boundary.

For a `COMPOSED` child, revalidation proves the exact normalized occurrence,
accepted Scenario composition request, complete leaf attestations, and exact
`ScenarioSemanticFingerprint` produced by the approved Scenario composer. For
an `UNAVAILABLE` child, it authoritatively repeats the occurrence attempt and
proves the exact finite unavailable reason. A fabricated or substituted child
state, reason, request, attestation, occurrence, or fingerprint is a
processing/integrity failure. Revalidation never manufactures a replacement
child outcome.

## Manifest fingerprint composition authority

Evidence Governance is the sole authoritative owner of Manifest semantic
fingerprint composition. Its V1 primitive is conceptually:

```text
composeManifestSemanticFingerprintV1(
  VerifiedAdmittedManifestV1 manifest,
  ordered authoritative COMPOSED child outcomes
) -> ManifestSemanticFingerprint
```

It accepts only the verified admitted Manifest and the complete ordered,
revalidated `COMPOSED` child proofs. It derives the existing
`ManifestSemanticFingerprintInput` and uses exactly the approved domain,
encoding, canonical sequence, authored Scenario order, digest, and golden
bytes. It creates no second Manifest serialization or fingerprint truth and
depends only on Evidence Governance models and proofs. It must not depend on
`qa-model-extractor`, `ScenarioNormalizedSemanticFingerprinter`, or legacy
`NormalizedScenarioSemanticAttestation`.

The current Extractor-owned `ManifestSemanticFingerprintComposer` and
`NormalizedScenarioSemanticAttestation` must not remain a competing
authoritative path. Implementation must move, relocate, or re-express the
existing canonical composition authority inside Evidence Governance while
preserving exact bytes. Extractor becomes mapping and orchestration only.
Temporary legacy compatibility APIs may remain solely as non-authoritative
adapters or projections onto the Evidence Governance capability; Extractor
must never independently recompute an authoritative Manifest fingerprint after
this capability is implemented.

### Finite composer rejection model

Evidence Governance owns a finite typed Manifest composition-rejection model
corresponding exactly to the existing approved
`ManifestSemanticCompositionException.Code` values that remain normatively
valid after migration. Only those explicitly enumerated codes may be recognized
as finite Manifest composition integrity failures. A finite rejection produces
no Manifest outcome and no Manifest fingerprint; it never becomes
`UNAVAILABLE(SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE)`.

The migrated boundary preserves the old finite meanings without preserving the
old ownership or exception API:

| Existing code | V1 boundary treatment |
| --- | --- |
| `STRUCTURALLY_UNADMITTED_MANIFEST` | Stage 1 processing/integrity failure |
| `UNSUPPORTED_MANIFEST_CONTRACT` | Stage 2 processing/compatibility failure |
| `SCENARIO_SEMANTIC_UNAVAILABLE` | superseded by authoritative Stage 4 child-outcome inspection; never a composer rejection |
| `SCENARIO_ATTESTATION_MISMATCH` | Stage 3 processing/integrity failure |
| `SCENARIO_MANIFEST_OCCURRENCE_MISMATCH` | Stage 3 processing/integrity failure |
| `SCENARIO_AUTHORITY_MISMATCH` | Stage 3 processing/integrity failure |
| `SCENARIO_COUNT_MISMATCH` | Stage 3 processing/integrity failure |
| `SCENARIO_POSITION_MISMATCH` | Stage 3 processing/integrity failure |
| `SCENARIO_DECLARATION_SUBSTITUTION` | Stage 3 processing/integrity failure |

If the Evidence Governance composition primitive independently detects one of
the applicable enumerated correspondence or integrity conditions after Stage 3,
it reports the same finite typed meaning as a Stage 5 processing/integrity
failure. No generic fallback admits a future code.

Future, unexpected, or unclassified failures propagate. No broad catch of
`IllegalArgumentException` or `RuntimeException`, and no exception message or
class fallback, may classify a failure as authoritative evidence.

## Authoritative composition attempt

Evidence Governance owns one deterministic operation:

```text
attemptManifestSemanticCompositionV1(
  NormalizedManifestSemanticCompositionInputV1 input
) -> ManifestSemanticCompositionOutcomeV1
```

The operation accepts no caller-supplied state, unavailable reason, Manifest
fingerprint, partial result, or public outcome constructor. Outcome values are
immutable and factory-controlled by this attempt.

### Validation precedence

The attempt executes these stages in exact order:

1. **Admission and occurrence binding.** Verify
   `VerifiedAdmittedManifestV1`, including structural admission, Manifest
   occurrence identity, approved Repository Capture attestation, parent-member
   binding, and same-capture ownership. Failure is a processing/integrity
   failure and produces no Manifest semantic outcome.
2. **Manifest contract support.** Verify every fixed Manifest semantic contract
   and version identifier. An unsupported value is a processing/compatibility
   failure and is never converted to child or Manifest unavailability.
3. **Child closure.** Derive expected membership and order from the verified
   Manifest, verify complete authored Scenario membership, exact field-by-field
   correspondence, authority, parent, capture, and revalidate every child
   outcome through the approved Scenario proof boundary. Failure is a
   processing/integrity failure and produces no Manifest semantic outcome.
4. **Availability decision.** If at least one authoritative child outcome is
   `UNAVAILABLE`, return authoritative Manifest `UNAVAILABLE` with the sole V1
   reason. The complete child sequence remains bound in the proof.
5. **Manifest fingerprint composition.** Only when every child outcome is
   `COMPOSED`, derive the exact authored-order Scenario fingerprint sequence and
   invoke the Evidence Governance authoritative Manifest fingerprint composer.
   Any finite Manifest-composition identity, correspondence, completeness,
   anti-substitution, or composer rejection is a processing/integrity failure.
   V1 defines no Manifest-level integrity-unavailable reason, so no outcome or
   Manifest fingerprint is emitted.
6. **Success.** Package the exact verified input, complete ordered child proofs,
   derived ordered fingerprint sequence, and composer-returned fingerprint into
   authoritative `COMPOSED`.

Unexpected failures at every stage propagate as processing failures. Validation
order cannot be used to convert an earlier processing, compatibility, or
integrity failure into authoritative evidence.

The authoritative paths are acyclic:

```text
VerifiedAdmittedManifestV1
  + complete ordered candidate child outcomes
    -> child closure and proof revalidation
      -> all children COMPOSED
        -> Evidence Governance Manifest fingerprint composer
          -> ManifestSemanticFingerprint
            -> ManifestSemanticCompositionOutcomeV1.COMPOSED

VerifiedAdmittedManifestV1
  + complete ordered candidate child outcomes
    -> child closure and proof revalidation
      -> at least one authoritative child UNAVAILABLE
        -> ManifestSemanticCompositionOutcomeV1.UNAVAILABLE(
             SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE)
          -> no Manifest fingerprint composer invocation
          -> no ManifestSemanticFingerprint
```

## `COMPOSED` contract

`COMPOSED` is valid if and only if:

- the Manifest is structurally admitted;
- every authored child outcome is authoritative `COMPOSED`;
- every child binds its authoritative `ScenarioSemanticFingerprint`;
- fingerprints occur in exact authored Scenario order; and
- the Evidence Governance authoritative Manifest composer successfully returns the
  Manifest fingerprint for that exact input.

The immutable proof binds the exact `VerifiedAdmittedManifestV1`, exact ordered
revalidated child `COMPOSED` outcomes, derived ordered Scenario fingerprint
sequence, and authoritative `ManifestSemanticFingerprint` returned by the
composer. A naked or caller-supplied Manifest fingerprint is insufficient and
cannot substitute for this proof.

## `UNAVAILABLE` contract

`UNAVAILABLE` is valid if and only if:

- the Manifest is structurally admitted;
- at least one authored child outcome is authoritative `UNAVAILABLE`;
- all child outcomes are otherwise authoritative, complete, and correctly
  bound;
- no unexpected processing failure occurred; and
- no `ManifestSemanticFingerprint` is emitted.

The immutable proof binds the exact `VerifiedAdmittedManifestV1`, exact complete
authored revalidated child outcome sequence, and reason exactly
`SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`. The caller cannot select the reason.

All child outcomes `COMPOSED` deterministically produces `COMPOSED`. One or more
child outcomes `UNAVAILABLE` deterministically produces `UNAVAILABLE`,
irrespective of unavailable-child count, position, or mixture of finite child
reasons. Exact child reasons remain in child evidence.

## Unexpected-failure boundary

Unexpected programming, infrastructure, resource, linkage, runtime, or other
unclassified failures produce neither state and no Manifest fingerprint. They
propagate as processing failures. No broad conversion of
`IllegalArgumentException`, `RuntimeException`, exception message, or exception
class into `UNAVAILABLE` is permitted. Only a verified authoritative finite
child `UNAVAILABLE` outcome can cause Manifest `UNAVAILABLE`.

## Manifest fingerprint compatibility

`ManifestSemanticFingerprint` exists only after every authored child outcome is
`COMPOSED`. Its canonical input remains exactly the existing complete authored-
order Scenario fingerprint sequence plus the already-approved Manifest semantic
fields. No partial fingerprints, unavailable markers, child unavailable
reasons, outcome state, or proof fields enter its canonical bytes.

## Attributed Member Outcome binding

The full Manifest outcome is proof-bearing validation input, not a new
`AttributedMemberOutcomeFingerprint` canonical field. Evidence Governance must
verify it before constructing the existing attributed-outcome canonical input:

| Structural state / Manifest outcome | Manifest occurrence identity | Canonical optional Manifest fingerprint |
| --- | --- | --- |
| `STRUCTURALLY_ADMITTED` / `COMPOSED` | present | present |
| `STRUCTURALLY_ADMITTED` / `UNAVAILABLE` | present | absent |
| `STRUCTURALLY_REJECTED` / no outcome | absent | absent |

A structurally rejected member has no Manifest occurrence or Manifest outcome.
The existing attributed-outcome canonical field order and optional encoding do
not change.

## Logical V2 binding

The complete proof-bearing outcome is verified during construction but is not
serialized verbatim into Logical V2. Each structurally admitted Manifest entry
canonically contains exactly:

1. `ManifestOccurrenceIdentity`;
2. availability state;
3. explicit optional `ManifestSemanticFingerprint`; and
4. explicit optional unavailable reason.

For `COMPOSED`, state is `COMPOSED`, the fingerprint is present, and reason is
absent. For `UNAVAILABLE`, state is `UNAVAILABLE`, the fingerprint is absent,
and reason is exactly `SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`. Every other
combination is an integrity failure.

A normalized `MANIFEST` datum is included only for `COMPOSED`. The occurrence
and outcome remain direct evidence for `UNAVAILABLE`. Scenario Identity Group
evidence is independently retained for both states.

## Semantic provenance compatibility

For `COMPOSED`, the completed `ManifestSemanticFingerprint` may later have
`COMPOSE_MANIFEST_SEMANTIC_CONTENT` provenance under Semantic Provenance V1.
For `UNAVAILABLE`, there is no Manifest fingerprint, no Manifest semantic
provenance fingerprint, and no new provenance activity. This contract does not
activate Manifest-unavailable provenance.

## Anti-substitution requirements

The authoritative boundary rejects:

- a foreign Manifest occurrence or `ParentCapturedMemberRef`;
- cross-capture, cross-member, or cross-authority Manifest substitution;
- coordinated relabeling of the Manifest occurrence, parent member, Scenario
  occurrences, and child outcomes to appear mutually consistent under another
  capture, authority, or Manifest;
- a child outcome from another Manifest, member, capture, declaration, or
  authored position;
- an omitted, extra, duplicated, reordered, or incomplete child proof;
- a structural-location mismatch;
- a substituted or independently supplied Scenario fingerprint;
- a naked or caller-supplied Manifest fingerprint;
- a fabricated `COMPOSED` or `UNAVAILABLE` state; and
- a caller-selected unavailable reason.

State and Manifest fingerprint are derived, never accepted from callers. The
authoritative composer result is bound to the exact normalized input and exact
ordered child proofs before `COMPOSED` is created.

Internal mutual consistency is insufficient. Positive verification against the
`RepositoryCaptureAttestation` and the admitted-member/normalized-Manifest
source inside `VerifiedAdmittedManifestV1` must defeat coordinated relabeling.

## Ownership and immutability

Evidence Governance owns `VerifiedAdmittedManifestV1`, the normalized Manifest
composition input, child proof revalidation, the finite Manifest composition
rejection taxonomy, Manifest fingerprint composition authority, outcome and
reason vocabularies, authoritative attempt decision, proof verification, and
factory control for immutable outcome values. The existing
`ManifestSemanticFingerprint` domain and encoder remain the sole Manifest
fingerprint truth.

Extractor may later map and orchestrate approved source-processing evidence and
authoritative child Scenario outcomes into this boundary. It does not construct
independent verified Manifest proof from caller-selected parts, derive the
Manifest state, select a reason or fingerprint, validate proof authoritatively,
reproduce canonical serialization, or retain independent Manifest fingerprint
authority.

## Normative golden and rejection requirements

The production capability gate must include independent normative vectors for:

### `COMPOSED`

- one Scenario;
- multiple Scenarios;
- exact authored child ordering; and
- duplicate-equivalent and duplicate-conflicting Scenario groups where every
  child fingerprint exists.

### `UNAVAILABLE`

- one child `UNSUPPORTED_SEMANTIC_CONTRACT`;
- one child `SCENARIO_COMPOSITION_INTEGRITY_FAILURE`;
- mixed child unavailable reasons;
- multiple unavailable children; and
- an unavailable child at first, middle, and last position.

### Boundary rejection

- omitted, extra, duplicated, or reordered child;
- foreign Manifest child, capture, member, or authority;
- coordinated capture, member, Manifest, Scenario, and child-outcome relabeling;
- Capture A admitted Manifest paired with Capture B attestation;
- a normalized Manifest paired with an independently supplied different child
  enumeration;
- structural-location mismatch;
- fabricated child `COMPOSED` proof;
- fabricated child `UNAVAILABLE` proof;
- substituted child `COMPOSED` Scenario fingerprint;
- substituted child `UNAVAILABLE` reason;
- caller-supplied Manifest fingerprint;
- caller-selected unavailable reason or state;
- incomplete child proof;
- finite typed Manifest-composer rejection propagation with no outcome;
- unexpected or non-enumerated Manifest-composer failure propagation;
- unexpected runtime failure;
- proof that the legacy Extractor composer cannot act as a second authoritative
  truth; and
- unsupported Manifest semantic contract or version.

### Compatibility and mappings

- existing Manifest fingerprint golden bytes remain unchanged;
- existing successful Manifest composition remains byte-identical;
- the migrated Evidence Governance composer reproduces the exact existing
  Manifest fingerprint golden bytes;
- `UNAVAILABLE` emits no Manifest fingerprint;
- admitted/`COMPOSED` and admitted/`UNAVAILABLE` attributed-outcome proof
  mappings; and
- exact Logical V2 `COMPOSED` and `UNAVAILABLE` canonical entries.

## Conformance requirements

An implementation conforms only if it implements the closed state and reason
vocabularies, exact validation precedence, complete child closure, authoritative
composer binding, failure propagation, anti-substitution rules, ownership
boundary, compatibility guarantees, and every required normative vector above.
It must not introduce a new fingerprint domain, partial Manifest fingerprint,
unavailable provenance, qualification, resolution, duplicate classification,
or caller-derived authoritative evidence.

## Scope exclusions

This contract does not implement production code, change ADR-015, correct the
current Attributed Member Outcome or Logical V2 implementation contracts,
activate provenance, or modify Runtime, Coverage, Explorer, or canonical graph
behavior.
