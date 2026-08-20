# Scenario Authority Logical Source V2 Contract

Contract identifier: `scenario-authority-logical-source-contract-v2`

Status: normative subordinate construction contract of ADR-014 and ADR-015.
This document is not an ADR and does not broaden either ADR.

Normative basis:

- [ADR-014](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)
- [Scenario Authority Semantic Provenance V1](scenario-authority-semantic-provenance-v1.md)
- [Scenario Authority Scenario Identity Group V1](scenario-authority-scenario-identity-group-v1.md)

## Purpose and scope

This contract freezes the smallest proof-bearing construction boundary for
the first conformant Logical Source Authority Snapshot V2. One snapshot
partitions the attributable evidence from exactly one verified Repository
Capture by one exact claimed authority. Evidence Governance alone validates
membership, derives accepted normalized membership, encodes canonical bytes,
and emits the Logical V2 fingerprint. A source adapter maps already-produced
evidence and does not reproduce these decisions.

This contract does not activate additional semantic-provenance activities,
qualify authority, resolve references, form canonical facts or relationships,
or change Runtime, Coverage, or Explorer behavior.

## Fingerprint namespace and fixed identifiers

The authoritative identifiers are:

```text
domain:                         QAIP\u0000SCENARIO_AUTHORITY_LOGICAL_SOURCE\u0000V2
encoding:                       scenario-authority-logical-source-c14n-v2
digest:                         sha-256-v1
value:                          scenario-authority-logical-source-v2:<64 lowercase hex>
Logical V2 contract:            scenario-authority-logical-source-contract-v2
source contract:                qaip-source-snapshot-contract-v1
source profile:                 qaip-scenario-authority-repository-json-v1
format:                         qaip-scenario-authority-manifest-v1
schema:                         qaip-scenario-authority-manifest-schema-v1
parser:                         scenario-authority-json-parser-v1
attribution:                    scenario-authority-attribution-v1
structural location:            rfc-6901-json-pointer-v1
normalization:                  scenario-authority-source-normalization-v1
Manifest identity:              qaip-scenario-manifest-occurrence-identity-v1
Scenario occurrence identity:   qaip-scenario-declaration-occurrence-identity-v1
Scenario identity:              qaip-scenario-identity-v1
Step identity:                  qaip-scenario-step-identity-v1
Operation-reference identity:   qaip-scenario-operation-reference-datum-identity-v1
Business Rule-reference identity:
                                qaip-scenario-business-rule-reference-datum-identity-v1
semantic canonicalization:      scenario-authority-logical-source-c14n-v2
semantic fingerprint algorithm: sha-256-v1
processing outcomes:            scenario-authority-processing-outcomes-v1
schema diagnostics:             scenario-authority-schema-diagnostic-v1
semantic provenance:            scenario-authority-semantic-provenance-contract-v1
```

Each displayed `\u0000` denotes one U+0000 code point and one `00` byte under
strict UTF-8. Escape-notation characters are never canonical domain bytes.
Canonical encoding uses ADR-015 length-prefixed strict UTF-8 text, big-endian
unsigned 64-bit counts and lengths, explicit optional tags, and counted
ordered collections.

The Logical V2 contract identifier constrains construction but is not an
additional canonical field. ADR-015 does not encode it separately. The
semantic-canonicalization field is the Logical V2 encoding identifier because
that identifier governs the aggregate's semantic reference encoding.

## Authority partition identity

One construction request is identified outside canonical content by:

1. `sourceId`;
2. externally supplied `snapshotId`;
3. exact claimed authority;
4. exact source contract and source profile; and
5. one authoritative `RepositoryCaptureAttestation` containing the complete
   parent Repository Capture identity, fingerprint, regular membership, and
   unsupported matching entries.

The canonical content includes `sourceId`, claimed authority, source profile,
source contract, the full parent Repository Capture identity tuple, and its
fingerprint. The externally supplied Logical V2 `snapshotId` is excluded from
canonical content. The parent-capture tuple independently includes the parent
capture's snapshot ID. Those two snapshot IDs are distinct identity fields and
need not be equal. No request may aggregate across captures or authorities,
and the Logical `sourceId` must equal the attested parent capture `sourceId`.

Naked capture identities, capture fingerprints, or caller-described member
collections are not authoritative inputs.

## Proof-bearing construction input

The authoritative constructor consumes one immutable request containing:

- the authority-partition identity and `RepositoryCaptureAttestation`;
- the fixed contract identifiers above;
- ordered attributed-member outcome attestations;
- ordered admitted Manifest composition attestations;
- ordered `VerifiedScenarioIdentityGroupV1` values;
- ordered normalized semantic datum attestations derived from admitted
  Manifests and `UNIQUE` groups; and
- ordered `DERIVE_ATTRIBUTED_MEMBER_OUTCOME` semantic-provenance attestations.

Every attestation contains the complete accepted fingerprint input, its
authoritative fingerprint, and enough source-normalization or child proof to
recompute it. Naked child fingerprints are never sufficient. Construction
revalidates every proof before encoding. The request contains neither state
nor a caller-supplied Logical V2 fingerprint.

## Attributed-member membership

The attributed-member collection contains exactly every safely attributed
regular captured member claiming the partition authority, whether
structurally admitted or structurally rejected. It excludes unattributable
members and members attributed to another authority.

The collection must be non-empty. A partition exists only for an authority
actually claimed by at least one safely attributed member in the verified
capture; a caller cannot manufacture an empty authority partition.

Each entry binds:

1. its complete `ParentCapturedMemberRef`;
2. the exact partition authority;
3. the complete accepted `AttributedMemberOutcomeFingerprint` input; and
4. the authoritative attributed-member outcome fingerprint.

Entries preserve exact parent regular-member order after filtering that order
to the partition authority. The constructor validates this canonical order;
it does not silently sort caller input. A foreign capture or authority,
missing or extra attributable member, duplicate member, substituted outcome,
or reordered entry is an aggregate-membership failure.

## Admitted Manifest evidence

Exactly one Manifest evidence entry exists for every structurally admitted
attributed member, and none exists for a structurally rejected member. Each
entry binds the exact `ManifestOccurrenceIdentity`, accepted Manifest semantic
composition input, authoritative `ManifestSemanticFingerprint`, and the
matching admitted attributed-member outcome.

Manifest entries use normalized repository-relative member-path Unicode
code-point order. Missing, extra, duplicated, foreign, reordered, or
substituted Manifest evidence is rejected. A rejected attributed member never
fabricates a Manifest occurrence or Manifest semantic fingerprint.

## Scenario identity-group membership

The group collection accepts only authoritative
`VerifiedScenarioIdentityGroupV1` values. It never accepts the Extractor's
legacy presentation `ScenarioIdentityGroup` as evidence.

Every Scenario declaration in every admitted Manifest belongs to exactly one
verified group. Every occurrence in every group must bind to the same parent
capture, an admitted member and Manifest occurrence in this partition, and the
exact partition authority. No declaration may disappear, occur in two groups,
or be introduced from another Manifest, authority, member, or capture.

Groups are in canonical claimed-identity order: authority, `scenarioKey`, and
identity scheme, each compared by Unicode code points. The constructor
validates rather than sorts caller input. It revalidates each complete group
proof and fingerprint before aggregate membership is accepted.

## Duplicate and admission semantics

- `UNIQUE` contains one authoritative composed occurrence and publishes one
  admissible normalized Scenario claim and its exact semantic children.
- `DUPLICATE_EQUIVALENT`, `DUPLICATE_CONFLICTING`, and
  `DUPLICATE_UNCLASSIFIED` remain direct fingerprinted group evidence and
  publish no accepted Scenario, Step, Operation-reference, or Business
  Rule-reference normalized datum.

Every duplicate state rejects Scenario admission. Equivalent duplication adds
no winner, corroboration, confidence, precedence, completeness, or evidential
strength. Ordering confers no evidential precedence.

## Accepted normalized datum membership

The datum-kind order is exactly:

1. `MANIFEST`;
2. `SCENARIO`;
3. `STEP`;
4. `OPERATION_REFERENCE`;
5. `BUSINESS_RULE_REFERENCE`.

Every admitted Manifest contributes exactly one `MANIFEST` datum containing
its exact occurrence identity and Manifest semantic fingerprint.

Every `UNIQUE` group contributes exactly:

- one `SCENARIO` datum with its occurrence identity and authoritative
  Scenario semantic fingerprint;
- its `GIVEN`, `WHEN`, and `THEN` Step data in phase order and increasing
  numeric ordinal;
- exactly one unresolved `OPERATION_REFERENCE` datum; and
- unresolved `BUSINESS_RULE_REFERENCE` data in authored array order.

After kind grouping, data use the exact ADR-015 identity tuple order for that
kind. The supplied aggregate collection must already be in this order and is
validated rather than silently sorted. The sequence-semantic Step and Business
Rule parent order is additionally revalidated against the authoritative
Scenario composition proof.

Every datum identity and fingerprint must equal its admitted Manifest or
`UNIQUE` group proof. A datum appears exactly once. Duplicate-group semantic
children are forbidden even when their fingerprints are available. Missing,
extra, duplicate, foreign, reordered, or substituted data are
aggregate-membership failures.

## Direct, reachable, and excluded evidence

Direct canonical Logical V2 content consists only of:

- partition scalar identity and contract fields;
- parent Repository Capture identity and fingerprint;
- attributed-member outcome fingerprint references;
- admitted Manifest occurrence and fingerprint references;
- Scenario identity-group fingerprint references;
- admitted Manifest and `UNIQUE`-only normalized datum references; and
- the mandatory semantic-provenance fingerprint references below.

Duplicate occurrences and their available Scenario fingerprints are reachable
only through group evidence. Canonical schema diagnostics are reachable only
through attributed-member outcomes. Raw bytes remain reachable through the
parent Repository Capture custody boundary.

Repository Derivation Reports, unattributable members, authority declaration
or qualification evidence, resolved references, canonical facts and
relationships, and operational metadata are excluded.

## Minimum semantic-provenance profile

The finite first-V2 completeness profile admits and requires only
`DERIVE_ATTRIBUTED_MEMBER_OUTCOME` provenance. Exactly one authoritative
provenance attestation is required for every attributed-member outcome in the
partition and no other provenance record is admitted.

Each record must verify:

- provenance identity contract and stable identity;
- activity kind `DERIVE_ATTRIBUTED_MEMBER_OUTCOME` and version
  `scenario-authority-derive-attributed-member-outcome-v1`;
- output kind `ATTRIBUTED_MEMBER_OUTCOME`;
- exact output parent-member identity and attributed-outcome fingerprint;
- exactly one matching `CAPTURED_MEMBER` parent;
- partition membership and uniqueness; and
- canonical stable provenance-identity order.

The following otherwise active V1 provenance kinds are not part of this
minimum profile: Step, HTTP Operation-reference, Business Rule-reference,
Scenario composition, and Manifest composition. An arbitrary caller-selected
subset is forbidden. `CLASSIFY_SCENARIO_IDENTITY_GROUP` remains reserved and
prohibited and `SCENARIO_IDENTITY_GROUP` provenance cannot enter V2 under this
contract.

## Total authority-partition closure

Starting from the attested parent capture and exact authority, construction
must prove all of the following simultaneously:

1. every safely attributed member claiming this authority appears exactly once
   in attributed-member outcomes;
2. every admitted attributed member contributes exactly one admitted Manifest
   and rejected attributed members contribute none;
3. every Scenario declaration in those Manifests appears exactly once in one
   verified Scenario identity group;
4. every `UNIQUE` group contributes exactly its complete accepted normalized
   semantic data;
5. duplicate groups contribute no accepted semantic child data; and
6. every attributed outcome has exactly one matching mandatory provenance
   record.

Closure is derived and verified by Evidence Governance. It is not a
caller-selected member list or state.

## Canonical top-level sequence

The encoder writes exactly the ADR-015 sequence:

1. domain;
2. encoding identifier;
3. digest identifier;
4. `sourceId`;
5. exact claimed authority namespace;
6. source profile;
7. source-contract version;
8. full parent Repository Capture Snapshot identity tuple;
9. parent Repository Capture fingerprint reference;
10. the format, schema, parser, attribution, structural-location,
    normalization, Manifest-identity, Scenario-occurrence-identity,
    Scenario-identity, Step-identity, Operation-reference-identity, Business
    Rule-reference-identity, semantic-canonicalization,
    semantic-fingerprint-algorithm, outcome, diagnostic, and provenance
    identifiers, in exactly that order;
11. attributed-member count and ordered outcome fingerprint references;
12. admitted Manifest occurrence count and ordered identity/fingerprint
    references;
13. Scenario identity-group count and ordered fingerprint references;
14. normalized datum count and ordered kind/identity/fingerprint references;
15. semantic-provenance count and ordered fingerprint references.

The apparent numbering difference from ADR-015 is only that this list includes
the common three-field prefix. No field is added, removed, or reordered.
Proof objects, `snapshotId` as a separate Logical field, derived state, and the
Logical V2 contract identifier are not additional canonical fields.

## Validation precedence and failure categories

Construction uses this deterministic precedence:

1. **Parent/partition identity:** validate the capture attestation, source and
   snapshot binding, claimed authority, and absence of cross-capture input.
2. **Compatibility:** validate every fixed domain, profile, contract, identity,
   canonicalization, digest, outcome, and diagnostic identifier.
3. **Attributed-member/Manifest closure:** revalidate outcome proofs and exact
   admitted/rejected Manifest membership.
4. **Scenario-group closure:** revalidate groups, occurrence closure, and
   duplicate admission semantics.
5. **Normalized-datum closure:** derive and verify Manifest and `UNIQUE`-only
   datum membership and sequence-semantic correspondence.
6. **Provenance closure:** revalidate the exact finite minimum profile,
   membership, uniqueness, and canonical order.
7. **Aggregate encoding:** encode the already-verified canonical input and emit
   the fingerprint.

Invalid or substituted attestations, identity conflicts, and inability to
establish the exact parent partition are processing/integrity failures.
Unsupported fixed identifiers are unsupported-contract failures. Missing,
extra, duplicated, foreign, or noncanonical members are aggregate-membership
failures. None is authoritative source evidence or an alternate aggregate
state. No broad exception catch converts unexpected failures into a declared
failure category.

## Anti-substitution requirements

Before emitting canonical bytes, the constructor must reject:

- foreign or consistently relabeled Repository Captures;
- foreign authority partitions;
- attributed-outcome, Manifest, group, normalized-datum, or provenance proof
  substitution;
- same-identity proof reuse for another occurrence or member;
- duplicate child data entering accepted membership;
- caller-selected membership, duplicate state, or normalized admission;
- naked child fingerprint references without authoritative proof;
- a caller-supplied aggregate fingerprint; and
- any claimed proof whose authoritative recomputation differs.

The returned aggregate is factory-only and contains the derived canonical
membership and fingerprint. Re-encoding revalidates the aggregate rather than
trusting its stored fields.

## Snapshot-observation binding

The Logical snapshot identity is exactly:

```text
sourceId + snapshotId + contentFingerprint
```

The constructor receives the externally allocated Logical `snapshotId` and
returns a completed immutable observation containing that identity and the
authoritative content fingerprint. It verifies that `sourceId` equals the
parent capture's source ID. The Logical `snapshotId` does not have to equal the
parent capture's independently encoded snapshot ID and is not hashed as an
extra canonical field.

Uniqueness of `sourceId + snapshotId` across persisted observations cannot be
proved by a pure content encoder. The repository or custody boundary that
accepts a completed observation must atomically reject an existing identical
pair associated with a different Logical V2 content fingerprint. That boundary
accepts only the completed factory result; it contributes no registry state,
timestamp, or operational metadata to canonical content.

## Canonical exclusions

Logical V2 canonical content excludes Repository Derivation Reports,
unattributable members, raw bytes, parsed JSON trees, human diagnostics,
exceptions and stack traces, timestamps, Git data, absolute paths except where
a normalized repository-relative member identity requires a path, host,
process, user and operating-system data, authority declarations, trust policy,
qualification state, resolved Operation or Rule identities, canonical
`SCENARIO`, `SPECIFIED_BY`, `COVERS`, `VALIDATES`, and Runtime, Coverage, or
Explorer state.

## Normative golden and rejection vectors

The conformance corpus must provide independent literal complete domain bytes,
complete canonical bytes, and final prefixed fingerprints, without generating
expected bytes through the production encoder, for at least:

- one admitted member, one Manifest, and one `UNIQUE` Scenario;
- one rejected attributed member;
- mixed admitted and rejected attributed members;
- multiple admitted Manifests;
- `UNIQUE` together with each duplicate group state;
- duplicate-group child-data exclusion;
- multiple `UNIQUE` groups;
- multiple authorities proving partition isolation;
- mandatory provenance completeness;
- ordering boundaries including Unicode paths and numeric indexes;
- every fixed identifier/version change; and
- deterministic recalculation.

Rejection vectors must independently cover missing, extra, duplicate, foreign,
reordered, and substituted attributed outcomes, Manifests, groups, normalized
data, and provenance; foreign capture; foreign authority; coordinated capture
relabeling; an empty or fabricated authority partition; duplicate child-data
admission; unsupported contracts, profiles, and versions; and reuse of one
`sourceId + snapshotId` with a conflicting content fingerprint at the
observation-acceptance boundary.

## ADR compatibility

This contract introduces no contradiction with ADR-014 or ADR-015. It selects
the minimum provenance profile those ADRs permit: exact attributed-outcome
provenance needed for captured-to-attributed replay, with no arbitrary subset
and no provenance for reserved Scenario-group classification. It preserves the
ADR-015 canonical sequence and treats construction proofs as non-canonical
verification inputs.

Any future addition of provenance kinds, canonical fields, datum kinds,
identity forms, profile versions, aggregate states, or admission semantics
requires an explicit compatible contract and, whenever canonical bytes or
accepted meanings change, a new canonicalization/version identifier.
