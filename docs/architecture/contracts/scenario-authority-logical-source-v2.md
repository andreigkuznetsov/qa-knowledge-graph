# Scenario Authority Logical Source V2 Contract

Contract identifier: `scenario-authority-logical-source-contract-v2`

Status: normative subordinate construction contract of ADR-014 and ADR-015.
This document is not an ADR and does not broaden either ADR.

Normative basis:

- [ADR-014](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)
- [Scenario Authority Manifest Semantic Outcome V1](scenario-authority-manifest-semantic-outcome-v1.md)
- [Scenario Authority Admitted Manifest Attestation V1](scenario-authority-admitted-manifest-attestation-v1.md)
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

- one `VerifiedScenarioAuthorityPartitionSourceV2` as defined below;
- the fixed contract identifiers above;
- ordered attributed-member outcome attestations;
- ordered authoritative `ManifestSemanticCompositionOutcomeV1` values for
  admitted members;
- ordered `VerifiedScenarioIdentityGroupV1` values;
- ordered normalized semantic datum attestations derived from admitted
  Manifests and `UNIQUE` groups; and
- ordered `DERIVE_ATTRIBUTED_MEMBER_OUTCOME` semantic-provenance attestations.

Every attestation contains the complete accepted fingerprint input, its
authoritative fingerprint, and enough source-normalization or child proof to
recompute it. Naked child fingerprints are never sufficient. Construction
revalidates every proof before encoding. The request contains neither state
nor a caller-supplied Logical V2 fingerprint.

### `VerifiedScenarioAuthorityPartitionSourceV2`

The mandatory upstream enumeration proof is a factory-only verified handoff
that binds together:

1. the exact `RepositoryCaptureAttestation`;
2. the complete `ScenarioSourceNormalizationResult` for that capture;
3. the exact parent Repository Capture identity;
4. all parent-member processing and normalization outcomes in exact parent
   order;
5. the exact safely attributed members and their claimed authorities;
6. the exact claimed authority selected for this partition;
7. the derived admitted and rejected attributed-member membership;
8. the derived admitted Manifest membership; and
9. every Scenario declaration and occurrence binding in those Manifests.

The complete normalization result, not the supplied child-attestation set, is
the authoritative enumeration source. Its parent identity and every retained
parent-member reference must be positively verified against the same capture
attestation. Its ordered outcomes must cover the attested regular-member
collection exactly once in exact parent order. Capture A normalization cannot
be combined with Capture B attestation, including through coordinated
relabeling.

The handoff derives expected partition membership by filtering the complete
ordered result to safely attributed members whose exact claimed authority
equals the selected authority. It retains admitted/rejected state, admitted
Manifest content, and declaration occurrences without filesystem discovery,
recapture, inferred membership, or defaulted identifiers. A missing, extra,
duplicated, relabeled, or reordered parent outcome prevents creation of the
handoff.

Evidence Governance derives expected aggregate membership from this verified
handoff and compares all supplied child evidence against it. Child attestations
prove the correctness of supplied evidence; the handoff proves completeness.

## Attributed-member membership

The attributed-member collection contains exactly every safely attributed
regular captured member claiming the partition authority, whether
structurally admitted or structurally rejected. It excludes unattributable
members and members attributed to another authority.

The collection must be non-empty. A partition exists only for an authority
actually claimed by at least one safely attributed member in the verified
capture; a caller cannot manufacture an empty authority partition.

One or more structurally rejected attributed members claiming the authority
are sufficient to form a valid non-empty partition even when it contains no
admitted Manifest. If the complete normalization result contains no safely
attributed member for the selected authority, no Logical V2 partition exists
and construction is rejected.

Each entry binds:

1. its complete `ParentCapturedMemberRef`;
2. the exact partition authority;
3. the complete accepted `AttributedMemberOutcomeFingerprint` input; and
4. the authoritative attributed-member outcome fingerprint.

Expected entries are derived from the complete verified handoff. Entries
preserve exact parent regular-member order after filtering that order to the
partition authority. The constructor validates this canonical order;
it does not silently sort caller input. A foreign capture or authority,
missing or extra attributable member, duplicate member, substituted outcome,
or reordered entry is an aggregate-membership failure.

## Admitted Manifest evidence and availability

Expected Manifest membership is derived from the complete normalization result
in the verified handoff. Exactly one Manifest evidence entry exists for every
structurally admitted attributed member, whether its authoritative Manifest
semantic outcome is `COMPOSED` or `UNAVAILABLE`. No entry exists for a
structurally rejected member. Structural admission and Manifest semantic
availability are distinct states; fingerprint presence is not a prerequisite
for authority-partition membership.

The canonical Manifest entry contains exactly, in this order:

1. the exact `ManifestOccurrenceIdentity`;
2. Manifest semantic availability state, exactly `COMPOSED` or `UNAVAILABLE`;
3. an explicit optional `ManifestSemanticFingerprint` reference; and
4. an explicit optional Manifest unavailable reason.

The authoritative `ManifestSemanticCompositionOutcomeV1` is proof-bearing
construction input and is not serialized verbatim. For `COMPOSED`, the entry
has the exact occurrence and fingerprint bound by that outcome and has no
unavailable reason. For `UNAVAILABLE`, the entry has the exact occurrence, no
fingerprint, and reason exactly `SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`.
No child Scenario unavailable reason is copied into the Manifest entry.

These are the only legal combinations. Construction rejects `COMPOSED`
without a fingerprint, `COMPOSED` with a reason, `UNAVAILABLE` with a
fingerprint, `UNAVAILABLE` without the exact reason, and every unknown state or
reason. There is no `OTHER`, `UNKNOWN`, partial-fingerprint, or fallback form.

The complete `VerifiedAdmittedManifestV1`, its exact normalized Manifest,
complete child Scenario composition outcomes, parser/attribution/schema
proofs, leaf attestations, recomputation intermediates, and human/debug
diagnostics remain proof-only through this entry. They may enter another
already-approved Logical V2 member only where that member independently
requires them.

The matching authoritative attributed-member composition must agree exactly:

| Manifest outcome | Attributed-member occurrence/fingerprint | Logical V2 Manifest entry |
| --- | --- | --- |
| `COMPOSED` | same occurrence; exact fingerprint present | `COMPOSED`; same occurrence and fingerprint; reason absent |
| `UNAVAILABLE` | same occurrence; fingerprint absent | `UNAVAILABLE`; same occurrence; fingerprint absent; exact reason present |

Any disagreement is an aggregate-integrity failure. A caller cannot derive the
Manifest entry from the attributed-member optional fingerprint alone; both
authoritative proofs are revalidated and compared.

Manifest entries use normalized repository-relative member-path Unicode
code-point order. Missing, extra, duplicated, foreign, reordered, or
substituted Manifest evidence is rejected. Availability state does not alter
this occurrence order and does not sort `COMPOSED` before or after
`UNAVAILABLE`. A rejected attributed member never fabricates a Manifest
occurrence, availability entry, Manifest semantic fingerprint, or normalized
`MANIFEST` datum.

Under fixed format `qaip-scenario-authority-manifest-v1`, a structurally
admitted Manifest contains at least one Scenario because the fixed schema
requires `scenarios.minItems = 1`. An admitted empty Manifest is impossible:
such source input must have been structurally rejected before Logical V2
construction. Supplied admitted-empty-Manifest evidence is therefore an
integrity/closure failure.

## Scenario identity-group membership

The group collection accepts only authoritative
`VerifiedScenarioIdentityGroupV1` values. It never accepts the Extractor's
legacy presentation `ScenarioIdentityGroup` as evidence.

The complete declaration membership of every admitted Manifest is derived
from the verified normalization result. Every such Scenario declaration
belongs to exactly one verified group. Every occurrence in every group must
bind to the same parent
capture, an admitted member and Manifest occurrence in this partition, and the
exact partition authority. No declaration may disappear, occur in two groups,
or be introduced from another Manifest, authority, member, or capture.

Groups are in canonical claimed-identity order: authority, `scenarioKey`, and
identity scheme, each compared by Unicode code points. The constructor
validates rather than sorts caller input. It revalidates each complete group
proof and fingerprint before aggregate membership is accepted.

Group evidence is retained independently of parent Manifest semantic
availability. Every declaration from both `COMPOSED` and `UNAVAILABLE`
Manifests remains in exactly one group. In particular, a Scenario semantic
`UNAVAILABLE` occurrence and a `DUPLICATE_UNCLASSIFIED` group do not disappear
because their parent Manifest has no semantic fingerprint.

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

Manifest availability is not duplicate classification. A Manifest is
`COMPOSED` whenever every authoritative child Scenario semantic fingerprint
exists, even when groups independently classify those occurrences as
`UNIQUE`, `DUPLICATE_EQUIVALENT`, or `DUPLICATE_CONFLICTING`. If at least one
child fingerprint is unavailable, the Manifest is `UNAVAILABLE` and its group
may independently be `DUPLICATE_UNCLASSIFIED`. Logical V2 retains both the
Manifest availability entry and group fingerprint; neither state derives from
the other.

## Accepted normalized datum membership

The datum-kind order is exactly:

1. `MANIFEST`;
2. `SCENARIO`;
3. `STEP`;
4. `OPERATION_REFERENCE`;
5. `BUSINESS_RULE_REFERENCE`.

Every admitted Manifest with authoritative outcome `COMPOSED` contributes
exactly one `MANIFEST` datum containing its exact occurrence identity and exact
Manifest semantic fingerprint. An admitted Manifest with outcome `UNAVAILABLE`
contributes no normalized `MANIFEST` datum. This absence removes neither its
direct occurrence/availability entry, authority-partition membership, nor its
Scenario identity-group evidence.

The normalized `MANIFEST` datum for `COMPOSED` is derived from the exact
authoritative normalized Manifest bound by `VerifiedAdmittedManifestV1` and
the authoritative `ManifestSemanticCompositionOutcomeV1.Composed`. An
independently reconstructed Extractor `NormalizedManifestDatum` is not
authoritative. A compatibility projection is permitted only after positive
field-for-field equivalence and identity binding to that Evidence Governance
value.

Every `UNIQUE` group contributes exactly:

- one `SCENARIO` datum with its occurrence identity and authoritative
  Scenario semantic fingerprint;
- its `GIVEN`, `WHEN`, and `THEN` Step data in phase order and increasing
  numeric ordinal;
- exactly one unresolved `OPERATION_REFERENCE` datum; and
- every unresolved `BUSINESS_RULE_REFERENCE` datum.

The global aggregate comparator first uses the fixed datum-kind order above,
then the following exact per-kind identity comparator. Every text field uses
Unicode code-point order, fingerprint values use their complete ASCII value,
and indexes and ordinals use unsigned numeric order:

- `MANIFEST`: parent source ID, parent capture snapshot ID, complete parent
  Repository Capture fingerprint, normalized repository-relative member path,
  Manifest-occurrence identity version;
- `SCENARIO`: its complete `MANIFEST` identity tuple, numeric Scenario array
  index from the canonical structural path, Scenario-occurrence identity
  version;
- `STEP`: claimed Scenario authority, `scenarioKey`, Scenario identity scheme,
  phase in fixed order `GIVEN`, `WHEN`, `THEN`, numeric ordinal within that
  phase, Step identity version;
- `OPERATION_REFERENCE`: claimed Scenario authority, `scenarioKey`, Scenario
  identity scheme, fixed role `OPERATION_REF`, Operation-reference datum
  identity version; and
- `BUSINESS_RULE_REFERENCE`: claimed Scenario authority, `scenarioKey`,
  Scenario identity scheme, referenced authority, stable Rule key, Business
  Rule identity scheme, Business Rule-reference datum identity version.

The supplied aggregate collection must already be in this global order and is
validated rather than silently sorted. Equal complete identity tuples are
duplicates and are rejected.

Scenario semantic composition separately preserves Business Rule references
in exact authored array order. That authored order is revalidated against the
authoritative Scenario composition request and proof, but it is not the global
Logical V2 normalized-datum ordering key. The aggregate serializes Business
Rule-reference data by the canonical identity comparator above. Manifest
Scenario-array order, Step phase/ordinal order, and Business Rule authored
order remain sequence-semantic proof properties even where the global
aggregate comparator uses another identity ordering.

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
- admitted Manifest occurrence, availability state, optional fingerprint, and
  optional unavailable-reason entries;
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

A `COMPOSED` Manifest fingerprint may have independently valid
`COMPOSE_MANIFEST_SEMANTIC_CONTENT` provenance, but that provenance remains
outside this minimum Logical V2 profile. An `UNAVAILABLE` Manifest has no
Manifest fingerprint and no Manifest semantic provenance. Recording its direct
availability evidence invents neither provenance nor a new activity/version.

## Total authority-partition closure

Starting from the attested parent capture and exact authority, construction
must prove all of the following simultaneously:

1. every safely attributed member claiming this authority appears exactly once
   in attributed-member outcomes;
2. every admitted attributed member contributes exactly one admitted Manifest
   availability entry and rejected attributed members contribute none;
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
12. admitted Manifest occurrence count and ordered entries, each encoding the
    exact occurrence identity, availability state, optional Manifest semantic
    fingerprint reference, and optional unavailable reason;
13. Scenario identity-group count and ordered fingerprint references;
14. normalized datum count and ordered kind/identity/fingerprint references;
15. semantic-provenance count and ordered fingerprint references.

The apparent numbering difference from ADR-015 is only that this list includes
the common three-field prefix. No field is added, removed, or reordered.
The explicit availability state and two optionals are the amended ADR-015 item
9 fields, not an additional aggregate field. The optionals use the existing
ADR-015 `0x00` absent and `0x01` present encoding. Proof objects, child
outcomes, normalized Manifest content through the entry, parser/schema proofs,
`snapshotId` as a separate Logical field, and the Logical V2 contract
identifier are not additional canonical fields.

The state is encoded as the exact frozen enum text. The unavailable reason is
encoded as exact frozen enum text only inside its present optional. Thus
`COMPOSED`/present-fingerprint and `UNAVAILABLE`/present-reason cannot share a
canonical representation.

## Validation precedence and failure categories

Construction uses this deterministic precedence:

1. **Partition enumeration:** verify the authority partition, parent capture,
   member enumeration, source/snapshot binding, fixed compatibility
   identifiers, and absence of cross-capture input.
2. **Attributed members:** revalidate every attributed-member outcome proof
   and structural-admission state in exact partition order.
3. **Manifest outcomes:** for each admitted member, revalidate its authoritative
   Manifest semantic outcome proof and exact occurrence/member/capture/
   authority binding.
4. **Outcome consistency:** positively verify attributed-member occurrence and
   optional fingerprint against the Manifest outcome.
5. **Direct Manifest entries:** derive the four canonical fields and validate
   the finite state/optional invariants.
6. **Normalized Manifest membership:** include a normalized `MANIFEST` datum
   only for `COMPOSED`, from the exact proof-bound normalized Manifest.
7. **Scenario groups:** independently revalidate complete occurrence closure,
   all group states, duplicate admission semantics, and partition binding.
8. **Remaining aggregate closure:** derive and verify other `UNIQUE`-only data,
   the finite provenance profile, all canonical orders, then encode the
   already-verified canonical input and emit the fingerprint.

The authoritative failure semantics are stage-level only. Stages are evaluated
in the numbered order above. All applicable finite checks within the current
stage are evaluated as required for complete validation. If one or more fail,
that stage fails; individual same-stage diagnostic selection and check order
are non-authoritative. Human or debug diagnostics may report multiple defects,
but do not enter the Logical V2 fingerprint or authoritative evidence, and the
stage result cannot depend on which same-stage check executed first.

If defects exist in more than one stage, the earliest failing stage is the
authoritative aggregate failure category and later stages do not manufacture
another result. This contract defines no persistent fine-grained Logical V2
failure taxonomy.

Invalid or substituted attestations, identity conflicts, and inability to
establish the exact parent partition are processing/integrity failures.
Unsupported fixed identifiers are unsupported-contract failures. Missing,
extra, duplicated, foreign, or noncanonical members are aggregate-membership
failures. None is authoritative source evidence or an alternate aggregate
state. No broad exception catch converts unexpected failures into a declared
failure category.

Unexpected programming, infrastructure, or runtime exceptions remain
processing failures. They are not converted into compatibility or
aggregate-membership outcomes, and no generic exception catch-all
classification is permitted.

## Anti-substitution requirements

Before emitting canonical bytes, the constructor must reject:

- foreign or consistently relabeled Repository Captures;
- foreign authority partitions;
- attributed-outcome, Manifest, group, normalized-datum, or provenance proof
  substitution;
- a foreign, cross-capture, cross-member, or cross-authority Manifest outcome;
- a substituted Manifest occurrence or `COMPOSED` fingerprint;
- fabricated Manifest availability state or unavailable reason;
- disagreement between the attributed-member outcome and Manifest outcome;
- an independently reconstructed or foreign normalized `MANIFEST` datum;
- Scenario group evidence from another authority partition;
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

Complete Logical V2 capability integration must provide an atomic
observation-acceptance operation over exactly
`(sourceId, logicalSnapshotId, completedContentFingerprint)`. The first binding
is accepted. An identical replay may be accepted idempotently according to the
later storage contract. The same source ID and Logical snapshot ID with a
different completed fingerprint is rejected atomically. This custody operation
occurs only after successful composition, is not part of the verified
authority-partition handoff slice, and adds no mutable state to fingerprint
content.

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
- a `COMPOSED` Manifest entry with occurrence, present exact fingerprint,
  absent reason, and included normalized `MANIFEST` datum;
- an `UNAVAILABLE` Manifest entry with occurrence, absent fingerprint, exact
  reason, and absent normalized `MANIFEST` datum;
- attributed-member/Manifest consistency for both availability states;
- mixed `COMPOSED` and `UNAVAILABLE` entries preserving occurrence order;
- `UNIQUE` together with each duplicate group state;
- parent Manifest `UNAVAILABLE` together with retained
  `DUPLICATE_UNCLASSIFIED` group evidence;
- duplicate-group child-data exclusion;
- multiple `UNIQUE` groups;
- multiple authorities proving partition isolation;
- mandatory provenance completeness;
- ordering boundaries including Unicode paths and numeric indexes;
- an omitted attributed member while every supplied child proof is
  individually valid;
- Capture A normalization combined with Capture B attestation;
- admitted-empty-Manifest rejection under `scenarios.minItems = 1`;
- Business Rule authored order differing from Logical aggregate identity
  order;
- exact comparator boundaries for every normalized datum kind;
- simultaneous defects in two stages proving earliest-stage precedence;
- multiple defects within one stage proving the same stage-level result
  independently of check order;
- a valid rejected-only authority partition;
- absence of safely attributed members proving that no partition exists;
- every fixed identifier/version change; and
- deterministic recalculation.

Rejection vectors must independently cover missing, extra, duplicate, foreign,
reordered, and substituted attributed outcomes, Manifests, groups, normalized
data, and provenance; foreign capture; foreign authority; coordinated capture
relabeling; an empty or fabricated authority partition; duplicate child-data
admission; unsupported contracts, profiles, and versions; and reuse of one
`sourceId + snapshotId` with a conflicting content fingerprint at the
later observation-acceptance boundary.

They must also cover `COMPOSED` without a fingerprint, `COMPOSED` with a
reason, `UNAVAILABLE` with a fingerprint, `UNAVAILABLE` without the exact
reason, foreign/cross-capture Manifest outcome, occurrence substitution,
attributed-outcome disagreement, and normalized Manifest substitution. Every
non-Logical-V2 fingerprint golden remains byte-for-byte unchanged.

## Dependency graph and implementation readiness

The construction dependencies are:

```text
Repository Capture / Admitted Manifest Verifier V1
    -> ManifestSemanticCompositionOutcomeV1
    -> authoritative AttributedMemberOutcome composition
    -> Logical V2 Manifest entry

Scenario occurrence composition outcomes
    -> VerifiedScenarioIdentityGroupV1
    -> Logical V2 Scenario-group evidence

Logical V2 Manifest entry ----+
                              +-> Logical Source V2 aggregate/fingerprint
Logical V2 group evidence ----+
```

Manifest availability and Scenario duplicate classification converge as
independent direct evidence in Logical V2; neither is derived from the other.

This contract is sufficient for
`VerifiedScenarioAuthorityPartitionSourceV2` to drive Logical Source V2
aggregate construction without inventing availability semantics. Production
implementation will benefit from a Logical-V2-only immutable Manifest entry
value type carrying the four canonical fields and enforcing their finite
combinations. That type is an implementation detail and introduces no new
semantic state. It must be factory-produced from the two revalidated proofs,
not accepted as caller authority.

## ADR compatibility

This contract introduces no contradiction with ADR-014 or ADR-015. It selects
the minimum provenance profile those ADRs permit: exact attributed-outcome
provenance needed for captured-to-attributed replay, with no arbitrary subset
and no provenance for reserved Scenario-group classification. It preserves the
ADR-015 canonical sequence and treats construction proofs as non-canonical
verification inputs.

Logical V2 has no released production fingerprint bytes. The amended Manifest
entry is therefore adopted directly in the existing V2 contract and does not
create Logical Source V3. No Scenario, Manifest, Attributed Member Outcome,
Scenario Identity Group, Semantic Provenance, or Repository Derivation Report
domain, input, encoder, or fingerprint byte changes under this correction.

Any future addition of provenance kinds, canonical fields, datum kinds,
identity forms, profile versions, aggregate states, or admission semantics
requires an explicit compatible contract and, whenever canonical bytes or
accepted meanings change, a new canonicalization/version identifier.
