# Scenario Authority Scenario Identity Group V1 Contract

Contract identifier: `scenario-authority-scenario-identity-group-v1`

Status: normative subordinate contract of ADR-015. This document is not an ADR
and does not broaden ADR-014 or ADR-015.

Normative basis:

- [ADR-014](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)
- [Scenario Authority Semantic Provenance V1](scenario-authority-semantic-provenance-v1.md)

## Purpose and scope

This contract freezes the authoritative construction, anti-substitution,
classification, and fingerprint rules for one complete group of structurally
admitted Scenario declaration occurrences that claim the same Scenario
identity. It persists duplicate classification without importing normalized
Scenario presentation or processing diagnostics into the group fingerprint.

This contract does not activate `CLASSIFY_SCENARIO_IDENTITY_GROUP` semantic
provenance, define Logical Source Authority Snapshot V2, qualify authority, or
resolve HTTP Operation or Business Rule references.

## Fingerprint domain

The authoritative identifiers are:

```text
domain:                    QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_IDENTITY_GROUP\u0000V1
encoding:                  scenario-authority-scenario-identity-group-c14n-v1
digest:                    sha-256-v1
value:                     scenario-authority-scenario-identity-group-v1:<64 lowercase hex>
duplicate-outcome contract: scenario-authority-duplicate-outcomes-v1
claimed identity scheme:   qaip-scenario-identity-v1
```

Each displayed `\u0000` denotes one actual U+0000 code point and therefore one
`00` byte in strict UTF-8. The six displayed escape characters are never
canonical domain bytes.

Canonical encoding uses the shared ADR-015 primitives: strict UTF-8
length-prefixed text, big-endian unsigned 64-bit integers, explicit optional
tags, and counted ordered collections. The fingerprint is SHA-256 over the
complete canonical bytes and uses lowercase hexadecimal.

Unsupported domain, encoding, digest, duplicate-outcome, identity, occurrence,
fingerprint, or canonicalization versions are processing/compatibility
failures. V1 never infers compatibility or substitutes a version.

## Claimed Scenario identity

The exact group identity is this ordered typed tuple:

1. exact claimed authority as `text`;
2. exact normalized `scenarioKey` as `text`;
3. `qaip-scenario-identity-v1` as `text`.

Occurrence, path, title, Steps, and source array index are not claimed Scenario
identity. All occurrences in one request must claim exactly this identity.

## Canonical top-level sequence

The encoder writes exactly:

1. domain;
2. encoding identifier;
3. digest identifier;
4. duplicate-outcome contract version;
5. exact claimed Scenario identity;
6. authoritatively derived group state as `text`;
7. occurrence count as `uint64`;
8. each verified occurrence in the supplied canonical order defined below.

No other top-level field is permitted. In particular, state is not supplied by
the caller and no provenance reference is part of this V1 fingerprint.

## Canonical occurrence encoding

Every occurrence is encoded in this exact sequence:

1. Scenario declaration occurrence identity:
   1. Manifest occurrence identity:
      1. parent sourceId as `text`;
      2. parent snapshotId as `text`;
      3. Repository Capture fingerprint value identifier as `text`;
      4. complete Repository Capture fingerprint value as `text`;
      5. normalized repository-relative member path as `text`;
      6. `qaip-scenario-manifest-occurrence-identity-v1` as `text`;
   2. Scenario structural path as `text`;
   3. `qaip-scenario-declaration-occurrence-identity-v1` as `text`;
2. complete verified `ParentCapturedMemberRef` in ADR-014 order:
   1. parent sourceId as `text`;
   2. parent snapshotId as `text`;
   3. Repository Capture fingerprint value identifier as `text`;
   4. complete Repository Capture fingerprint value as `text`;
   5. normalized repository-relative member path as `text`;
   6. raw byte length as `uint64`;
   7. raw-member fingerprint algorithm identifier as `text`;
   8. complete raw-member fingerprint value as `text`;
3. RFC 6901 structural location as `text`;
4. explicit optional authoritative Scenario semantic fingerprint:
   - `00` when absent; or
   - `01`, then the Scenario semantic fingerprint value identifier and complete
     value as `text` when present;
5. explicit optional stable unavailable reason:
   - `00` when absent; or
   - `01`, then the V1 reason as `text` when present.

Exactly one of the two optional values must be present. Both present or both
absent is an integrity failure. The structural location must be a canonical
RFC 6901 pointer of the form `/scenarios/<canonical zero-based array index>`
and must exactly equal the structural path in the Scenario occurrence
identity.

## Parent-capture and member binding

Each `ParentCapturedMemberRef` must be positively verified through the approved
Repository Capture attestation boundary. The group boundary does not accept a
caller-created tuple merely because its fields agree internally.

The Manifest occurrence identity and verified parent-member reference must be
exactly equal in:

- parent sourceId;
- parent snapshotId;
- parent Repository Capture fingerprint;
- normalized repository-relative member path.

The parent-member reference additionally binds raw byte length and raw-member
fingerprint. Consistently relabeled foreign members are rejected by the
Repository Capture fingerprint-input attestation, not trusted as a new parent
merely because every caller-supplied label was changed together.

## `NormalizedScenarioOccurrenceInputV1`

Evidence Governance owns this finite immutable typed input. It is the complete
source-native normalized Scenario occurrence evaluated by the authoritative
composition-attempt boundary. Its fields are validation evidence and do not
add fields to the canonical identity-group fingerprint.

The input contains exactly:

1. source-normalization version;
2. Scenario semantic canonicalization version;
3. Scenario semantic contract version;
4. exact Scenario declaration occurrence identity;
5. complete `ParentCapturedMemberRef` verified through the Repository Capture
   attestation;
6. exact RFC 6901 structural location;
7. exact claimed Scenario identity: authority, scenarioKey, and identity
   scheme;
8. exact title;
9. exact authored `GIVEN` text sequence and ordered normalized `GIVEN` Steps;
10. exact authored `WHEN` text sequence and ordered normalized `WHEN` Steps;
11. exact authored `THEN` text sequence and ordered normalized `THEN` Steps;
12. exact unresolved HTTP Operation-reference input;
13. ordered unresolved Business Rule-reference inputs.

Each normalized Step contains:

- exact claimed Scenario identity;
- phase;
- zero-based ordinal;
- `qaip-scenario-step-identity-v1` identity version;
- exact authored Step text;
- Step semantic canonicalization version.

The unresolved HTTP Operation reference contains:

- exact claimed Scenario identity;
- fixed role;
- datum identity version;
- target profile;
- exact normalized method;
- exact normalized path;
- HTTP Operation-reference semantic canonicalization version.

Each unresolved Business Rule reference contains:

- zero-based authored array position;
- exact claimed Scenario identity;
- exact referenced authority;
- exact stableRuleKey;
- Business Rule identity scheme;
- datum identity version;
- Business Rule-reference semantic canonicalization version.

The type carries the exact identifier values presented for evaluation; the
authoritative attempt boundary, not a public constructor or Java exception,
decides whether they are supported V1 values. Lists are immutable and retain
exact caller order. No field is inferred, normalized, sorted, repaired,
defaulted, or ignored.

It contains no parsed `JsonNode`, raw JSON, human diagnostic, qualification,
resolved target, or operational metadata.

## Exact occurrence-to-composition correspondence

After V1 support has passed, Evidence Governance validates this complete
correspondence between `NormalizedScenarioOccurrenceInputV1` and the accepted
`ScenarioSemanticCompositionRequest` and its typed leaf attestations:

| Source occurrence field | Required composition field/evidence |
| --- | --- |
| authority, scenarioKey, identity scheme | exact equality with the parent claimed Scenario identity |
| exact title | exact equality with `exactTitle` |
| source-normalization version | exact equality with the request normalization version |
| Scenario semantic canonicalization version | exact equality with the request canonicalization version |
| Scenario semantic contract version | exact equality with the request semantic contract version |
| each authored phase-text sequence | same count and exact text at every position as that phase's normalized Steps |
| each `GIVEN` Step | same collection position, `GIVEN` phase, zero-based ordinal, claimed identity, Step identity version, exact text, and Step semantic version in the corresponding Step attestation input |
| each `WHEN` Step | same collection position, `WHEN` phase, zero-based ordinal, claimed identity, Step identity version, exact text, and Step semantic version in the corresponding Step attestation input |
| each `THEN` Step | same collection position, `THEN` phase, zero-based ordinal, claimed identity, Step identity version, exact text, and Step semantic version in the corresponding Step attestation input |
| Operation-reference claimed identity | exact equality with the Operation-reference attestation input identity |
| Operation-reference role and datum identity version | exact equality with the attestation input role and datum version |
| Operation target profile, method, and path | exact equality with the attestation input target profile, method, and path |
| Operation semantic version | exact equality with the attestation input canonicalization version |
| Business Rule-reference count | exact equality with positioned Rule-attestation count |
| every Business Rule reference | same authored position and collection position; exact claimed identity, referenced authority, stableRuleKey, identity scheme, datum identity version, and semantic version |

For every phase, normalized Step ordinal must equal collection position and
ordinals must be the complete sequence `0..count-1`. For Business Rule
references, authored position must equal collection position and positions
must be the complete sequence `0..count-1`. Any count, phase, ordinal,
position, ownership, text, identity, profile, target, version, or order
disagreement is a finite composition integrity failure. Equality is exact;
locale, case folding, reparsing, renormalization, and semantic approximation
are forbidden.

## Authoritative Scenario composition outcomes

An occurrence attestation contains exactly one authoritative composition
outcome. The proof-bearing fields in this section are validation inputs; they
are not additional group-fingerprint fields.

### `COMPOSED`

`ScenarioOccurrenceCompositionOutcomeV1.COMPOSED` is an immutable
Evidence Governance value with no public constructor. It contains:

- the exact normalized Scenario declaration occurrence;
- the exact accepted `ScenarioSemanticCompositionRequest`;
- its typed Step, HTTP Operation-reference, and Business Rule-reference
  fingerprint attestations;
- the authoritative `ScenarioSemanticFingerprint`.

Its sole factory is the successful result of the authoritative composition
attempt. Evidence Governance verifies the request through the approved
`ScenarioSemanticFingerprintComposer` path and verifies that its claimed
Scenario identity and all source-native semantic fields correspond exactly to
the normalized occurrence. The recomputed fingerprint must equal the supplied
authoritative value.

A naked `ScenarioSemanticFingerprint`, a publicly constructible composition
result, or a supported value prefix alone is not authoritative occurrence
evidence. Substitution between different occurrences is rejected even when the
occurrences claim the same Scenario identity or happen to have equal semantic
content.

### `UNAVAILABLE`

`ScenarioOccurrenceCompositionOutcomeV1.UNAVAILABLE` is an immutable Evidence
Governance value with no public constructor. It contains:

- the exact normalized Scenario declaration occurrence; and
- one authoritative finite V1 unavailable reason produced by the approved
  typed Scenario composition-attempt boundary.

Its sole factory is the unavailable branch of the authoritative composition
attempt. The attempt boundary, not the caller, classifies the result. A naked
enum or a caller-selected reason is not authoritative evidence.

## Unavailable-reason vocabulary

Evidence Governance owns exactly these V1 reasons:

### `UNSUPPORTED_SEMANTIC_CONTRACT`

The exact normalized occurrence requires a semantic contract, version,
identity scheme, or finite semantic shape unsupported by Scenario Semantic V1.

### `SCENARIO_COMPOSITION_INTEGRITY_FAILURE`

The occurrence claims supported V1 semantics, but authoritative Scenario
composition rejects identity, position, completeness, or anti-substitution
invariants.

These reasons are semantic compatibility/integrity outcomes, not exception
buckets. Unexpected programming, infrastructure, resource, or runtime
failures produce no authoritative occurrence outcome and no group fingerprint.
Human messages, exception messages, stack traces, and exception classes cannot
select or enter either reason. Adding, removing, splitting, merging, or changing
a reason requires a future versioned contract.

## Authoritative composition-attempt API

Evidence Governance owns one deterministic operation:

```text
attemptScenarioOccurrenceCompositionV1(
  NormalizedScenarioOccurrenceInputV1 occurrence
) -> ScenarioOccurrenceCompositionOutcomeV1
```

It returns exactly one factory-created value:

- `COMPOSED`;
- `UNAVAILABLE(UNSUPPORTED_SEMANTIC_CONTRACT)`; or
- `UNAVAILABLE(SCENARIO_COMPOSITION_INTEGRITY_FAILURE)`.

There is no public result constructor and no overload accepting a naked
fingerprint, state, unavailable reason, or current Extractor
`ScenarioIdentityGroup`.

### Complete V1 support decision table

Stage 2 support evaluation compares every listed identifier independently and
collects the presence of unsupported values without short-circuiting or
depending on check or exception order. Any mismatch in this table produces
one `UNSUPPORTED_SEMANTIC_CONTRACT`, regardless of how many mismatches exist:

| Input identifier/profile | Only supported V1 value |
| --- | --- |
| claimed Scenario identity scheme | `qaip-scenario-identity-v1` |
| source-normalization version | `scenario-authority-source-normalization-v1` |
| Step identity version | `qaip-scenario-step-identity-v1` |
| Step semantic canonicalization version | `scenario-authority-step-semantic-c14n-v1` |
| Operation-reference role | `OPERATION_REF` |
| Operation-reference datum identity version | `qaip-scenario-operation-reference-datum-identity-v1` |
| Operation-reference target profile | `qaip-http-operation-reference-v1` |
| Operation-reference semantic canonicalization version | `scenario-authority-http-operation-reference-semantic-c14n-v1` |
| Business Rule-reference datum identity version | `qaip-scenario-business-rule-reference-datum-identity-v1` |
| Business Rule identity scheme | `qaip-business-rule-identity-v1` |
| Business Rule-reference semantic canonicalization version | `scenario-authority-business-rule-reference-semantic-c14n-v1` |
| Scenario semantic canonicalization version | `scenario-authority-scenario-semantic-c14n-v1` |
| Scenario semantic contract version | `scenario-authority-scenario-semantic-c14n-v1` |

The checks apply to the parent and to every Step, Operation reference, and
Business Rule reference. A future identifier, profile, scheme, version, or
finite reference shape is unsupported even if another field would also fail
integrity validation.

After every support check passes, only these finite categories produce
`SCENARIO_COMPOSITION_INTEGRITY_FAILURE`:

- parent claimed Scenario identity disagreement;
- occurrence-to-composition correspondence mismatch under the complete table
  above;
- authored phase-text versus normalized-Step disagreement;
- Step phase, ordinal, count, position, ownership, text, or attestation/input
  mismatch;
- Operation-reference ownership, role binding, datum binding, profile binding,
  method, path, or attestation/input mismatch;
- Business Rule-reference ownership, authority, key, scheme binding, datum
  binding, count, authored position, order, or attestation/input mismatch;
- missing required Step, Operation-reference, or Rule-reference composition
  input;
- leaf attestation input/fingerprint mismatch;
- Scenario composition input/fingerprint mismatch;
- any finite identity, completeness, position, or anti-substitution rejection
  explicitly enforced by the approved V1 `ScenarioSemanticFingerprintComposer`.

No arbitrary `IllegalArgumentException`, exception type, or message is a
classification rule. A new finite integrity category requires a compatible
revision of this contract before it can produce an authoritative reason.

### Validation precedence

The attempt executes these stages in exact order:

1. Validate the occurrence identity, RFC 6901 location, approved Repository
   Capture attestation, and parent-member binding needed to establish which
   exact occurrence is being evaluated. Failure is an identity-group
   processing/integrity failure and produces no authoritative composition
   outcome.
2. Evaluate all finite V1 support checks in the complete table. If one or more
   unsupported conditions exist, return exactly
   `UNAVAILABLE(UNSUPPORTED_SEMANTIC_CONTRACT)`. The result is independent of
   mismatch enumeration order.
3. Only when Stage 2 passes, deterministically construct the leaf attestation
   inputs and `ScenarioSemanticCompositionRequest` from the normalized
   occurrence, validate the complete correspondence table, verify every leaf
   attestation, and invoke `ScenarioSemanticFingerprintComposer`. A listed
   finite validation or composer rejection returns exactly
   `UNAVAILABLE(SCENARIO_COMPOSITION_INTEGRITY_FAILURE)`.
4. Otherwise package the exact accepted request and the fingerprint returned
   by that composer invocation into the factory-created `COMPOSED` result. The
   attempt API accepts no caller-supplied Scenario fingerprint to substitute.

An unexpected programming, infrastructure, resource, linkage, runtime, or
other unclassified failure at any stage propagates as a processing failure.
It never produces `UNAVAILABLE` and no group fingerprint is emitted from the
failed attempt. An occurrence containing both an unsupported condition and a
finite integrity defect is classified unsupported because Stage 2 precedes
Stage 3.

## Authoritative group construction

The current public Extractor `ScenarioIdentityGroup` is not an authoritative
fingerprint input and must not be fingerprinted directly.

The authoritative boundary conceptually accepts:

```text
ScenarioIdentityGroupCompositionRequest {
  duplicateOutcomeContract
  exact ClaimedScenarioIdentity
  ordered VerifiedScenarioOccurrence[]
}
```

Evidence Governance validates each occurrence attestation, validates canonical
order and unique ordering keys, derives the state, validates the resulting
state invariant, constructs the immutable canonical input, and owns the sole
canonical encoder and fingerprinter.

The group request accepts only factory-created
`ScenarioOccurrenceCompositionOutcomeV1` values from the authoritative attempt
API. It accepts neither naked Scenario fingerprints nor caller-created
unavailable reasons.

Extractor owns only exact mapping from approved normalized Scenario records,
the approved Repository Capture/member binding, and typed authoritative
Scenario composition attempts. It must not derive an independently trusted
state or reproduce group canonical serialization.

## Derived states and invariants

The boundary derives exactly one state:

- `UNIQUE`: exactly one occurrence, whose outcome is `COMPOSED` with a
  supported comparable Scenario fingerprint.
- `DUPLICATE_EQUIVALENT`: at least two occurrences, every outcome is
  `COMPOSED`, and every complete comparable Scenario fingerprint is equal.
- `DUPLICATE_CONFLICTING`: at least two occurrences, every outcome is
  `COMPOSED`, and at least two complete comparable Scenario fingerprints
  differ.
- `DUPLICATE_UNCLASSIFIED`: at least two occurrences and at least one outcome
  is authoritative `UNAVAILABLE` with an approved V1 reason.

A singleton unavailable occurrence cannot form a V1 identity-group
fingerprint. Caller-supplied state is rejected. A state inconsistent with its
verified occurrence outcomes is an integrity failure.

Every duplicate state is rejecting. There is no winner, first/last or file
precedence, newest preference, majority, content-similarity fallback, authority
increase, evidential-strength increase, corroboration, confidence increase, or
completeness increase. Equivalent occurrences remain distinct retained
occurrences.

## Canonical occurrence ordering

The caller supplies occurrences in this exact order:

1. normalized repository-relative member path using Unicode code-point order;
2. numeric zero-based Scenario array index extracted from the canonical
   structural location.

Numeric ordering means index `2` precedes index `10`. Equal ordering keys are
an integrity failure. The authoritative group boundary rejects noncanonical
caller order and does not silently sort. Extractor may create canonical order
before submitting the request, but Evidence Governance validates it.

Ordering exists only for deterministic encoding. It creates no evidential or
selection precedence.

## Canonical exclusions

The group fingerprint does not encode:

- title or normalized Scenario presentation;
- Step text;
- HTTP Operation-reference fields;
- Business Rule-reference fields;
- parsed JSON or generic JSON serialization;
- schema or human diagnostics;
- exception messages, classes, or stack traces;
- timestamps;
- Git, host, process, user, or operating-system metadata;
- authority qualification;
- resolved HTTP Operation or Business Rule identities.

Scenario meaning enters only through an authoritative
`ScenarioSemanticFingerprint`. Normalized source content retained for
attestation verification is not serialized into the group fingerprint.

## Golden and rejection vectors

Normative successful golden vectors must lock complete domain bytes, complete
canonical bytes, final prefixed fingerprints, and deterministic recalculation
for:

- one `UNIQUE` composed occurrence;
- equivalent duplicates;
- conflicting duplicates;
- unclassified duplicates for each unavailable reason;
- mixed composed/unavailable occurrences;
- multiple unavailable occurrences;
- relocation to another member;
- changed Scenario array index;
- numeric indexes `2` and `10`;
- Unicode path ordering;
- an unavailable-reason change.

Normative rejection vectors must cover:

- equal canonical ordering keys;
- noncanonical occurrence ordering;
- fingerprint substitution from another claimed identity;
- fingerprint substitution between occurrences of the same identity;
- fabricated equivalent or conflicting state;
- caller-supplied unavailable reason;
- group/occurrence claimed-identity mismatch;
- occurrence identity/parent-member mismatch;
- consistently relabeled foreign parent-member input;
- structural path/location mismatch;
- both optional outcome fields present or both absent;
- `UNIQUE` without a composed fingerprint;
- every invalid state/count/outcome combination;
- unsupported domain, encoding, digest, outcome, identity, occurrence,
  fingerprint, or canonicalization version.

Correspondence and attempt-boundary vectors additionally cover every row of
the correspondence table, including title mismatch; authored phase-text versus
normalized-Step disagreement; Step text, phase, ordinal, count, and position
mismatch; Operation method/path and ownership/profile mismatch; Business Rule
authority/key/scheme and authored-order/position mismatch; and fingerprint
substitution between same-identity occurrences.

Support vectors exercise every row of the complete V1 support table separately
and multiple unsupported rows together. Integrity vectors exercise every
finite integrity category above. A combined unsupported plus integrity defect
must produce `UNSUPPORTED_SEMANTIC_CONTRACT`. An injected unexpected failure at
each stage must propagate with no authoritative unavailable outcome. A
caller-selected unavailable reason must be rejected before group composition.

Golden construction must use the authoritative occurrence and composition
boundaries. Test-only naked fingerprints or caller-selected reasons are not
conformance vectors.

## ADR-015 compatibility

This contract introduces no contradiction with ADR-015. It expands the exact
nested occurrence encoding, freezes explicit optional tags, selects
reject-on-noncanonical-input as the validation point for ADR-015's required
deterministic order, and supplies the anti-substitution and unavailable-outcome
details ADR-015 delegates to subordinate contracts. This correction changes no
fingerprint identifier, canonical field, field order, optional encoding, or
canonical byte sequence defined above.
