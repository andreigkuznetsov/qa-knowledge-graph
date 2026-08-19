# Scenario Authority Semantic Provenance V1 Contract

Contract identifier: `scenario-authority-semantic-provenance-v1`

Status: normative subordinate contract of ADR-015. This document is not an ADR
and does not broaden the scope of ADR-006, ADR-014, or ADR-015.

Normative basis:

- [ADR-006](../adr/ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-014](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)

## Purpose and scope

This contract freezes the smallest Scenario-specific provenance model needed
to bind completed fingerprint-producing derivations. It is not a general
provenance framework. A failed derivation produces no semantic-provenance
record or fingerprint.

The active V1 activities produce only already-supported Scenario Authority
fingerprint values. Parsing, attribution, schema admission, normalization as
an unfingerprinted intermediate, qualification, and reference resolution are
not separately fingerprinted provenance activities.

## Fingerprint domain

The authoritative identifiers are:

```text
domain:   QAIP\u0000SCENARIO_AUTHORITY_SEMANTIC_PROVENANCE\u0000V1
encoding: scenario-authority-semantic-provenance-c14n-v1
digest:   sha-256-v1
value:    scenario-authority-semantic-provenance-v1:<64 lowercase hex>
```

Each `\u0000` in the displayed domain denotes one actual U+0000 code point.
The literal six-character escape spelling is never hashed. Canonical binary
encoding uses the shared ADR-015 primitives: strict UTF-8 length-prefixed
text, big-endian `uint64`, explicit optional tags, and counted collections.

The provenance semantic canonicalization version is
`scenario-authority-semantic-provenance-c14n-v1`. The supporting provenance
contract identifier is `scenario-authority-semantic-provenance-contract-v1`;
it constrains accepted records but is not an additional canonical field.

## Provenance identity

The identity contract/version is
`scenario-authority-semantic-provenance-identity-v1`.

`SemanticProvenanceIdentityV1` is a structured canonical identity, not a
digest and not a delimiter-joined string. Its equality and ordering tuple is,
in this exact order:

1. identity contract/version as `text`;
2. activity kind as `text`;
3. output kind as `text`; and
4. the exact typed output datum identity defined below.

The complete output fingerprint is not part of provenance identity. It is a
separate integrity binding. Two records with the same provenance identity but
different output fingerprints are a substitution conflict, not two distinct
provenance records.

Provenance identities are ordered by their complete canonical identity bytes,
lexicographically as unsigned bytes. Exact duplicate identity bytes are an
integrity failure.

## Active activities and versions

Only these activity/version pairs are admitted:

| Activity kind | Activity/version identifier | Output kind |
| --- | --- | --- |
| `DERIVE_ATTRIBUTED_MEMBER_OUTCOME` | `scenario-authority-derive-attributed-member-outcome-v1` | `ATTRIBUTED_MEMBER_OUTCOME` |
| `FINGERPRINT_STEP_SEMANTIC_CONTENT` | `scenario-authority-fingerprint-step-semantic-content-v1` | `STEP_SEMANTIC_CONTENT` |
| `FINGERPRINT_HTTP_OPERATION_REFERENCE_CONTENT` | `scenario-authority-fingerprint-http-operation-reference-content-v1` | `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT` |
| `FINGERPRINT_BUSINESS_RULE_REFERENCE_CONTENT` | `scenario-authority-fingerprint-business-rule-reference-content-v1` | `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT` |
| `COMPOSE_SCENARIO_SEMANTIC_CONTENT` | `scenario-authority-compose-scenario-semantic-content-v1` | `SCENARIO_SEMANTIC_CONTENT` |
| `COMPOSE_MANIFEST_SEMANTIC_CONTENT` | `scenario-authority-compose-manifest-semantic-content-v1` | `MANIFEST_SEMANTIC_CONTENT` |

`CLASSIFY_SCENARIO_IDENTITY_GROUP` and output kind
`SCENARIO_IDENTITY_GROUP` are reserved names. V1 construction must reject
them until the authoritative Scenario identity-group fingerprint domain is
implemented and a compatible revision of this contract activates them.

The only successful derivation outcome is `DERIVED`. No failure, partial,
unknown, rejected, or exception outcome is admitted by this contract.

## Exact output datum identities

Every output identity is encoded as its output-kind tag followed by the fields
below. This tag is part of the typed output identity; it is not an additional
top-level provenance field.

### `ATTRIBUTED_MEMBER_OUTCOME`

Encode the full `ParentCapturedMemberRef`:

1. parent source ID;
2. parent snapshot ID;
3. Repository Capture fingerprint value identifier and complete value;
4. normalized repository-relative path;
5. raw byte length as `uint64`; and
6. raw-member digest identifier and complete fingerprint value.

### `STEP_SEMANTIC_CONTENT`

Encode the accepted normalized Step identity:

1. claimed Scenario authority;
2. Scenario key;
3. Scenario identity scheme `qaip-scenario-identity-v1`;
4. phase `GIVEN`, `WHEN`, or `THEN`;
5. zero-based ordinal within that phase as `uint64`; and
6. Step identity version `qaip-scenario-step-identity-v1`.

### `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT`

Encode the accepted unresolved Operation-reference datum identity:

1. claimed Scenario authority;
2. Scenario key;
3. Scenario identity scheme `qaip-scenario-identity-v1`;
4. fixed role `OPERATION_REF`; and
5. identity version
   `qaip-scenario-operation-reference-datum-identity-v1`.

### `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT`

Encode the accepted unresolved Business Rule-reference datum identity:

1. claimed Scenario authority;
2. Scenario key;
3. Scenario identity scheme `qaip-scenario-identity-v1`;
4. referenced authority;
5. stable rule key;
6. Business Rule identity scheme; and
7. identity version
   `qaip-scenario-business-rule-reference-datum-identity-v1`.

### `SCENARIO_SEMANTIC_CONTENT`

Encode the occurrence-scoped `ScenarioDeclarationOccurrenceIdentity`:

1. its complete `ManifestOccurrenceIdentity`;
2. exact RFC 6901 Scenario structural path `/scenarios/<canonical index>`;
3. identity version `qaip-scenario-declaration-occurrence-identity-v1`.

### `MANIFEST_SEMANTIC_CONTENT`

Encode `ManifestOccurrenceIdentity`:

1. identity version `qaip-scenario-manifest-occurrence-identity-v1`;
2. parent source ID;
3. parent snapshot ID;
4. Repository Capture fingerprint value identifier and complete value; and
5. normalized repository-relative member path.

The output fingerprint reference immediately following an output identity
contains the output kind, the exact fingerprint value identifier required for
that kind, and the complete prefixed fingerprint value. Its kind, identifier,
and value must agree with the activity and output identity.

## Typed parent references

V1 admits exactly three parent-reference variants. Every variant begins with
its tag.

### `CAPTURED_MEMBER`

Contains one full `ParentCapturedMemberRef` in the field order defined above.

### `ATTRIBUTED_MEMBER_OUTCOME`

Contains:

1. the exact `ATTRIBUTED_MEMBER_OUTCOME` output identity; and
2. value identifier and complete `AttributedMemberOutcomeFingerprint`.

### `SEMANTIC_DATUM`

Contains:

1. active semantic datum/output kind;
2. exact typed datum identity for that kind;
3. exact fingerprint value identifier; and
4. complete prefixed semantic fingerprint value.

Parent provenance records are never embedded or referenced as parents. An
unfingerprinted normalized object cannot be represented as a parent. For leaf
activities its normalized identity is the output identity and its complete
semantic binding is the output fingerprint; the captured-to-normalized origin
is supplied by the attributed-member parent.

## Per-activity parent requirements

### `DERIVE_ATTRIBUTED_MEMBER_OUTCOME`

- Exactly one `CAPTURED_MEMBER` parent.
- It must equal the full parent reference embedded in the output identity and
  in the `AttributedMemberOutcomeFingerprint` input.

### Leaf fingerprint activities

The Step, HTTP Operation-reference, and Business Rule-reference fingerprint
activities each require exactly one `ATTRIBUTED_MEMBER_OUTCOME` parent for the
structurally admitted Manifest from which the normalized leaf was derived.
The output identity is the exact normalized leaf identity and the output
fingerprint binds its complete supported semantic content. This avoids both an
unfingerprinted-parent variant and a self-referential `SEMANTIC_DATUM` parent.

### `COMPOSE_SCENARIO_SEMANTIC_CONTENT`

Parents are all `SEMANTIC_DATUM` references in this sequence-semantic order:

1. `GIVEN` Step references by increasing ordinal;
2. `WHEN` Step references by increasing ordinal;
3. `THEN` Step references by increasing ordinal;
4. exactly one HTTP Operation-reference;
5. Business Rule-reference parents in exact authored array order.

Their identities and fingerprints must be exactly those accepted by the
authoritative Scenario semantic composition request. Missing, extra,
duplicated, or reordered parents are integrity failures.

### `COMPOSE_MANIFEST_SEMANTIC_CONTENT`

Parents are sequence-semantic in this exact order:

1. exactly one `ATTRIBUTED_MEMBER_OUTCOME` parent for the admitted Manifest;
2. `SCENARIO_SEMANTIC_CONTENT` parents in exact authored Manifest order.

The attributed outcome must bind the same Manifest occurrence identity and
Manifest semantic fingerprint as the provenance output. Every Scenario parent
must correspond to exactly one declaration occurrence in that Manifest.

Where a future admitted activity does not declare sequence semantics, its
parents must use canonical identity-byte order. No active V1 activity relies
on locale, map iteration, validator emission order, or human diagnostics.

## Authoritative top-level field order

ADR-015 specifies eight fields after the common three-field prefix. Therefore
V1 encodes this exact sequence:

1. domain;
2. encoding identifier;
3. digest identifier;
4. provenance semantic canonicalization version;
5. stable structured provenance identity;
6. activity kind;
7. activity/version identifier;
8. exact typed output datum identity;
9. typed output semantic fingerprint reference;
10. ordered parent count and typed parent references;
11. stable derivation outcome `DERIVED`.

Consistent with ADR-015's definition of semantic contract version, field 4 is
`scenario-authority-semantic-provenance-c14n-v1`, repeated even though it is
also the common-prefix encoding identifier in field 2. The supporting
`scenario-authority-semantic-provenance-contract-v1` identifier is validated
by the V1 factory but is not separately encoded. Output kind is encoded inside
fields 5, 8, and 9, not as an additional top-level field. This preserves
ADR-015's normative sequence while retaining explicit type checking.

## Integrity and anti-substitution rules

Construction must fail before emitting a fingerprint unless all of these hold:

- the output fingerprint already exists under its active authoritative domain;
- every fingerprinted parent already exists under its supported domain;
- activity kind, activity version, output kind, output identity, and output
  fingerprint type are the single allowed combination;
- the output identity is exactly the identity of the fingerprint-producing
  datum or outcome;
- parent identities and fingerprints equal the exact derivation inputs;
- parent count, variants, membership, and order match the activity contract;
- no parent is duplicated where the activity forbids duplicates;
- the same provenance identity is not reused with different content;
- no parent is a provenance record;
- no direct or transitive fingerprint dependency reaches the output
  fingerprint from itself;
- the output is not a containing Repository Derivation Report or Logical
  Source Authority Snapshot fingerprint; and
- no forward reference, self-reference, back edge, or dependency cycle exists.

Integrity checking follows the fingerprint dependency graph, not textual
identity resemblance. A mismatch is a processing/integrity failure and emits
no semantic-provenance fingerprint.

## Repository Derivation Report use

A conforming Repository Derivation Report contains exactly one provenance
fingerprint reference produced by `DERIVE_ATTRIBUTED_MEMBER_OUTCOME` for every
`ATTRIBUTED_MEMBER_RETAINED` entry. The provenance output identity and
fingerprint must equal that retained entry.

An `UNATTRIBUTABLE_MEMBER_RETAINED` entry has no independent V1 semantic
fingerprint and therefore has no semantic-provenance record. It remains
directly and completely bound inside the Repository Derivation Report by its
full parent-member reference, stable parse outcome, stable attribution
outcome, and optional structural location.

Repository-report provenance cannot name the containing report as output.

## Logical Source Authority Snapshot V2 use

Logical V2 may reference provenance for these completed output kinds:

- `ATTRIBUTED_MEMBER_OUTCOME`;
- `STEP_SEMANTIC_CONTENT`;
- `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT`;
- `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT`;
- `SCENARIO_SEMANTIC_CONTENT`; and
- `MANIFEST_SEMANTIC_CONTENT`.

References are ordered by complete stable provenance identity bytes. Counts,
output identities, output fingerprints, and normalized-datum membership must
agree. `SCENARIO_IDENTITY_GROUP` remains prohibited until activated. No
provenance embedded in Logical V2 may name that containing Logical V2
fingerprint as output.

## Explicit exclusions

V1 defines no independent provenance record for:

- raw capture fingerprinting;
- parsing;
- authority attribution;
- schema admission or rejection;
- individual schema diagnostics;
- normalization as an unfingerprinted intermediate;
- unattributable outcomes;
- authority qualification;
- resolved Operation or Rule identities; or
- Operation or Rule resolution.

Timestamps, Git metadata, absolute paths, host, process, user, operating
system, decoded source text, human messages, exceptions, and stack traces are
not fields, identities, parents, ordering keys, or fingerprint inputs.
Repository-relative occurrence paths appear only inside an authoritative
captured-member or occurrence identity.

## Normative golden-vector requirements

Before production activation, Evidence Governance must publish complete domain
bytes, complete canonical bytes, and final prefixed fingerprints for:

- one valid record for every active activity;
- every active output kind and fingerprint value type;
- all three parent-reference variants;
- absent and present optional structural locations as exercised through an
  attributed-member outcome;
- zero, one, and multiple Scenario leaf parents where the activity permits;
- `GIVEN`/`WHEN`/`THEN` ordinal ordering;
- the fixed HTTP parent position;
- authored Business Rule-reference order;
- attributed parent first and authored Scenario order for Manifest composition;
- canonical identity ordering for provenance collections;
- deterministic repeated calculation;
- output fingerprint changes that leave provenance identity unchanged but
  produce a substitution conflict;
- output identity, output kind, activity, activity version, parent identity,
  parent fingerprint, parent count, parent variant, and parent-order changes;
- duplicate parent rejection;
- unsupported and reserved activity/output rejection;
- unsupported contract/version and fingerprint-value rejection;
- malformed identity and RFC 6901 rejection;
- direct self-dependency rejection;
- transitive cycle, forward reference, and back-edge rejection;
- provenance-as-parent rejection; and
- containing-aggregate-as-output rejection.

Golden vectors must prove that excluded operational metadata cannot alter the
canonical bytes or fingerprint.

## Compatibility

Any change to an active activity's meaning, version, parent variants, parent
order, output identity, outcome vocabulary, canonical field sequence, or
dependency rules requires a new explicit contract/canonicalization version.
Reserved names do not become active through implementation convenience.
