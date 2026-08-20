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

  ManifestOccurrenceIdentity
  verified ParentCapturedMemberRef
  exact claimed authority
  verified structural-admission proof

  source normalization version
  Manifest semantic contract version
  exact format
  exact schemaVersion
  exact scenarioIdentityScheme
  exact normalized Manifest semantic data
  exact authored Scenario declaration occurrences[]
  exact authoritative ScenarioOccurrenceCompositionOutcomeV1[]
}
```

The normalized Manifest semantic data contains exactly the identity and data
needed to construct the existing `ManifestSemanticFingerprintInput`: source
normalization version, Manifest semantic contract version, claimed authority,
format, schema version, Scenario identity scheme, and the complete authored
Scenario sequence. The Scenario fingerprint sequence is derived from the child
outcomes; it is never accepted as an independent caller claim.

The Manifest occurrence, verified parent member, structural-admission proof,
normalized Manifest, authored declarations, and child outcomes must all bind to
the same capture, member, and admitted Manifest occurrence. The input excludes
parsed `JsonNode`, raw JSON, human diagnostics, qualification, resolved targets,
timestamps, Git or host data, and other operational metadata.

## Exact child completeness and order

The authoritative attempt verifies total authored-Scenario accounting:

- every authored Scenario declaration appears exactly once;
- no authored declaration is omitted;
- no extra child outcome appears;
- no child belongs to another Manifest, member, capture, or authority;
- every child's normalized occurrence and structural location match the exact
  authored declaration;
- child order equals exact authored Manifest array order; and
- duplicate Scenario declarations remain distinct occurrence evidence.

The complete child outcome sequence is derived and verified against the exact
normalized Manifest. A caller-selected subset, permutation, duplicate, or
independently supplied fingerprint list is never authoritative.

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

1. **Admission and occurrence binding.** Verify structural admission, Manifest
   occurrence identity, approved Repository Capture verification, parent-member
   binding, and same-capture ownership. Failure is a processing/integrity
   failure and produces no Manifest semantic outcome.
2. **Manifest contract support.** Verify every fixed Manifest semantic contract
   and version identifier. An unsupported value is a processing/compatibility
   failure and is never converted to child or Manifest unavailability.
3. **Child closure.** Verify complete authored Scenario membership, exact order,
   occurrence correspondence, authority, parent, capture, and every child
   outcome proof. Failure is a processing/integrity failure and produces no
   Manifest semantic outcome.
4. **Availability decision.** If at least one authoritative child outcome is
   `UNAVAILABLE`, return authoritative Manifest `UNAVAILABLE` with the sole V1
   reason. The complete child sequence remains bound in the proof.
5. **Manifest fingerprint composition.** Only when every child outcome is
   `COMPOSED`, derive the exact authored-order Scenario fingerprint sequence and
   invoke the existing authoritative `ManifestSemanticFingerprint` composer.
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

## `COMPOSED` contract

`COMPOSED` is valid if and only if:

- the Manifest is structurally admitted;
- every authored child outcome is authoritative `COMPOSED`;
- every child binds its authoritative `ScenarioSemanticFingerprint`;
- fingerprints occur in exact authored Scenario order; and
- the existing authoritative Manifest composer successfully returns the
  Manifest fingerprint for that exact input.

The immutable proof binds the exact normalized Manifest input, exact ordered
child `COMPOSED` outcomes, derived ordered Scenario fingerprint sequence, and
authoritative `ManifestSemanticFingerprint` returned by the composer. A naked
or caller-supplied Manifest fingerprint is insufficient and cannot substitute
for this proof.

## `UNAVAILABLE` contract

`UNAVAILABLE` is valid if and only if:

- the Manifest is structurally admitted;
- at least one authored child outcome is authoritative `UNAVAILABLE`;
- all child outcomes are otherwise authoritative, complete, and correctly
  bound;
- no unexpected processing failure occurred; and
- no `ManifestSemanticFingerprint` is emitted.

The immutable proof binds the exact normalized Manifest input, exact complete
authored child outcome sequence, and reason exactly
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

## Ownership and immutability

Evidence Governance owns the normalized Manifest composition input, outcome and
reason vocabularies, authoritative attempt decision, proof verification, and
factory control for immutable outcome values. The existing
`ManifestSemanticFingerprint` encoder and composer remain the sole Manifest
fingerprint authority.

Extractor may later map approved normalized Manifest data and authoritative
child Scenario outcomes into this boundary. It does not derive the Manifest
state, select a reason or fingerprint, validate proof authoritatively, or
reproduce canonical serialization.

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
- structural-location mismatch;
- substituted Scenario fingerprint;
- caller-supplied Manifest fingerprint;
- caller-selected unavailable reason or state;
- incomplete child proof;
- unexpected runtime failure; and
- unsupported Manifest semantic contract or version.

### Compatibility and mappings

- existing Manifest fingerprint golden bytes remain unchanged;
- existing successful Manifest composition remains byte-identical;
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
