# ADR-015: Logical Source Semantic Canonicalization and Fingerprint Contract

- Status: Accepted
- Date: 2026-08-18
- Amended: 2026-08-20 (Manifest semantic availability)
- Decision owners: QAIP architecture

## Context

[ADR-006](ADR-006-qualified-engineering-data-and-provenance-contract.md)
requires immutable snapshot and datum identities, semantic content fingerprints,
versioned canonicalization, retained rejected evidence, and replayable
provenance. [ADR-011](ADR-011-scenario-authority-source-contract.md) defines the
Scenario Authority source-native identities and content. [ADR-013](ADR-013-immutable-source-snapshot-and-fingerprint-contract.md)
separates exact repository capture from logical source meaning. [ADR-014](ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
separates claimed authority content from later authority qualification and
reserves the declaration-independent Logical Source Authority Snapshot V2
domain.

The completed source-adapter pipeline now produces, without rediscovery:

```text
Repository Capture Snapshot candidate
  -> independent parse and safe authority attribution
  -> per-member schema admission
  -> source-native manifest, Scenario, Step, and unresolved-reference records
  -> authority-wide exact claimed-Scenario-identity groups
```

It deliberately does not yet compute semantic fingerprints. Raw fingerprints
distinguish exact bytes, but cannot answer whether two differently formatted
documents claim the same engineering content. Conversely, generic JSON
serialization can preserve or erase accidental syntax and library behavior in
ways that are not engineering semantics. Duplicate Scenario groups therefore
remain `DUPLICATE_UNCLASSIFIED`, and the Logical Source Authority Snapshot V2
cannot yet be constructed.

This ADR answers:

> How does QAIP canonically fingerprint Scenario Authority logical source
> content without confusing source occurrence, syntax, provenance, or
> qualification with engineering meaning?

## Decision drivers

- Exact source bytes remain available and fingerprinted under ADR-013.
- Source occurrence identity and semantic content identity remain distinct.
- The same Scenario meaning at another path or array index is comparable.
- Rejected and unattributable evidence remains content-bound and auditable.
- Duplicate classification uses comparable semantic fingerprints, never prose
  heuristics or processing order.
- Canonicalization is portable across JSON and validator implementations.
- Qualification and cross-source resolution cannot rewrite logical content.
- Evidence Governance owns exactly one implementation for each fingerprint
  domain.
- The contract introduces no fingerprint domain that is not independently
  referenced, compared, or required as an aggregate integrity boundary.

## Decision

QAIP adopts ten domain-separated semantic and aggregate fingerprints for
Scenario Authority Logical Source V2:

1. attributed-member outcome;
2. repository derivation report;
3. manifest semantic content;
4. Scenario semantic content;
5. Step semantic content;
6. unresolved HTTP Operation-reference content;
7. unresolved Business Rule-reference content;
8. Scenario identity group;
9. semantic provenance record; and
10. Logical Source Authority Snapshot V2 aggregate.

A Scenario declaration occurrence has an immutable occurrence identity and
references a Scenario semantic fingerprint. It has no separate semantic
fingerprint in V2. This is intentional: occurrence identifies where an
authored declaration appeared; the Scenario fingerprint identifies what that
declaration means under the source contract.

An unattributable member outcome is encoded inside the repository derivation
report. It receives no separate fingerprint unless a future public contract
makes it an independently referenced ADR-006 datum. This does not weaken its
binding because the report embeds its complete parent-member reference and
terminal outcome.

## Fingerprint domains and identifiers

Every serialized fingerprint value is its value identifier, one ASCII colon,
and 64 lowercase hexadecimal SHA-256 digits.

| Semantic object | Encoding identifier | Value identifier | Domain string |
| --- | --- | --- | --- |
| Attributed member outcome | `scenario-authority-attributed-member-outcome-c14n-v1` | `scenario-authority-attributed-member-outcome-v1` | `QAIP\u0000SCENARIO_AUTHORITY_ATTRIBUTED_MEMBER_OUTCOME\u0000V1` |
| Repository derivation report | `scenario-authority-repository-derivation-c14n-v1` | `scenario-authority-repository-derivation-v1` | `QAIP\u0000SCENARIO_AUTHORITY_REPOSITORY_DERIVATION\u0000V1` |
| Manifest semantic content | `scenario-authority-manifest-semantic-c14n-v1` | `scenario-authority-manifest-semantic-v1` | `QAIP\u0000SCENARIO_AUTHORITY_MANIFEST_SEMANTIC\u0000V1` |
| Scenario semantic content | `scenario-authority-scenario-semantic-c14n-v1` | `scenario-authority-scenario-semantic-v1` | `QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_SEMANTIC\u0000V1` |
| Step semantic content | `scenario-authority-step-semantic-c14n-v1` | `scenario-authority-step-semantic-v1` | `QAIP\u0000SCENARIO_AUTHORITY_STEP_SEMANTIC\u0000V1` |
| HTTP Operation-reference content | `scenario-authority-http-operation-reference-semantic-c14n-v1` | `scenario-authority-http-operation-reference-semantic-v1` | `QAIP\u0000SCENARIO_AUTHORITY_HTTP_OPERATION_REFERENCE_SEMANTIC\u0000V1` |
| Business Rule-reference content | `scenario-authority-business-rule-reference-semantic-c14n-v1` | `scenario-authority-business-rule-reference-semantic-v1` | `QAIP\u0000SCENARIO_AUTHORITY_BUSINESS_RULE_REFERENCE_SEMANTIC\u0000V1` |
| Scenario identity group | `scenario-authority-scenario-identity-group-c14n-v1` | `scenario-authority-scenario-identity-group-v1` | `QAIP\u0000SCENARIO_AUTHORITY_SCENARIO_IDENTITY_GROUP\u0000V1` |
| Semantic provenance record | `scenario-authority-semantic-provenance-c14n-v1` | `scenario-authority-semantic-provenance-v1` | `QAIP\u0000SCENARIO_AUTHORITY_SEMANTIC_PROVENANCE\u0000V1` |
| Logical Source Authority Snapshot | `scenario-authority-logical-source-c14n-v2` | `scenario-authority-logical-source-v2` | `QAIP\u0000SCENARIO_AUTHORITY_LOGICAL_SOURCE\u0000V2` |

All domains use digest identifier `sha-256-v1`. Unsupported domain, encoding,
digest, normalization, identity, outcome, or provenance versions are
processing/compatibility failures. An implementation never substitutes a
version.

In every domain string above, each written `\u0000` separator denotes exactly
one Unicode code point U+0000. Under the required strict UTF-8 encoding, that
code point is exactly one byte `0x00`. The written escape-notation characters
backslash, `u`, `0`, `0`, `0`, `0` are not canonical domain bytes. The two
characters backslash plus zero are likewise never a canonical representation.
Normative golden vectors include the complete length-prefixed domain bytes and
lock each U+0000 separator as one `00` byte.

## Common canonical binary encoding

Every domain uses one Evidence Governance implementation of these primitives:

- `text`: strict UTF-8 byte length as an unsigned 64-bit big-endian integer,
  followed by exactly those bytes;
- `bytes`: byte length in the same form, followed by exactly those bytes;
- `uint64`: one unsigned 64-bit big-endian integer;
- `enum`: its frozen case-sensitive ASCII identifier encoded as `text`;
- `fingerprintRef`: digest/value identifier as `text`, then the complete
  prefixed fingerprint value as `text`;
- `identityRef`: identity-contract identifier as `text`, followed by that
  identity contract's exact fields;
- `collection`: unsigned 64-bit count followed by the exact canonical encoding
  of every element; and
- `optional`: exactly one tag byte, `0x00` for absent or `0x01` followed by the
  canonical value for present.

No other optional tag is valid. Absent, present empty text, present zero, and
present empty collection are distinct. Mandatory fields cannot be represented
as absent or null.

The first fields of every fingerprint encoding are, in exact order:

1. domain string as `text`;
2. encoding identifier as `text`; and
3. digest identifier `sha-256-v1` as `text`.

The remaining fields are those specified by this ADR in their listed order.
SHA-256 is applied to the complete canonical byte sequence. Counts and lengths
have unsigned semantics from zero through `2^64 - 1`; implementations must
reject values they cannot represent, allocation overflow, truncation, and any
signed reinterpretation. Text encoders reject malformed Unicode scalar input,
including unpaired surrogates. They perform no trimming, case folding, Unicode
normalization, newline conversion, BOM handling, or locale-sensitive mapping.

JSON object field order is never semantic. Generic JSON serialization,
`JsonNode.toString()`, Jackson insertion order, pretty printing, validator
output serialization, and serializer defaults are prohibited canonical
encodings. Duplicate JSON object members remain a parse outcome; they are not
normalized.

Collections use the order defined for their containing record. A collection
defined as ordered is never silently sorted. A collection defined as a set is
sorted by the specified exact Unicode code-point tuple and rejects duplicate
keys. Human-readable rendering is never hashed.

## Stable outcome vocabulary

The following case-sensitive codes are owned by QAIP and frozen for V1.

### Parsing

- `PARSED`
- `INVALID_UTF8`
- `MALFORMED_JSON`
- `DUPLICATE_JSON_MEMBER`
- `TRAILING_JSON_CONTENT`

### Authority attribution

- `ATTRIBUTED`
- `NON_OBJECT_ROOT`
- `MISSING_AUTHORITY`
- `AUTHORITY_NOT_STRING`
- `INVALID_AUTHORITY`
- `PARSE_UNATTRIBUTABLE`

### Structural/schema admission

- `STRUCTURALLY_ADMITTED`
- `STRUCTURALLY_REJECTED`

### Scenario identity groups

- `UNIQUE`
- `DUPLICATE_EQUIVALENT`
- `DUPLICATE_CONFLICTING`
- `DUPLICATE_UNCLASSIFIED`

### Derivation

- `ATTRIBUTED_MEMBER_RETAINED`
- `UNATTRIBUTABLE_MEMBER_RETAINED`
- `UNSUPPORTED_MATCHING_ENTRY_RETAINED`
- `NORMALIZED_DATUM_DERIVED`
- `NO_NORMALIZED_DATUM`

These codes describe source processing and derivation. They are not ADR-012
authority qualification states. Adding, removing, merging, or changing the
meaning of a code requires a new outcome-contract version.

## Supporting V1 contract identifiers

The semantic encoders recognize these exact supporting identifiers:

| Purpose | Identifier |
| --- | --- |
| Source normalization | `scenario-authority-source-normalization-v1` |
| Processing/outcome vocabulary | `scenario-authority-processing-outcomes-v1` |
| Schema diagnostic contract | `scenario-authority-schema-diagnostic-v1` |
| Duplicate outcome contract | `scenario-authority-duplicate-outcomes-v1` |
| Repository derivation-report contract | `scenario-authority-repository-derivation-report-v1` |
| Semantic provenance contract | `scenario-authority-semantic-provenance-contract-v1` |
| JSON structural location | `rfc-6901-json-pointer-v1` |
| Parser | `scenario-authority-json-parser-v1` |
| Authority attribution | `scenario-authority-attribution-v1` |
| Manifest schema | `qaip-scenario-authority-manifest-schema-v1` |
| Manifest occurrence identity | `qaip-scenario-manifest-occurrence-identity-v1` |
| Scenario occurrence identity | `qaip-scenario-declaration-occurrence-identity-v1` |
| Claimed Scenario identity | `qaip-scenario-identity-v1` |
| Step identity | `qaip-scenario-step-identity-v1` |
| Operation-reference datum identity | `qaip-scenario-operation-reference-datum-identity-v1` |
| HTTP Operation target profile | `qaip-http-operation-reference-v1` |
| Business Rule-reference datum identity | `qaip-scenario-business-rule-reference-datum-identity-v1` |

Within each semantic field contract below, “semantic contract version” means
that domain's encoding identifier from the fingerprint-domain table. It is
encoded even though it also appears in the common prefix so a nested semantic
record remains explicitly self-describing and substitution checks can reject
inconsistent references.

## Schema diagnostic canonicalization

Fingerprint-relevant schema diagnostics use this QAIP-owned record:

```text
StableScenarioSchemaDiagnosticV1 {
  stableDiagnosticCode
  instanceLocation
  normativeSchemaKeyword
  schemaRuleId
  typedParameters[]
}
```

The initial `stableDiagnosticCode` is `SCHEMA_VIOLATION`. `instanceLocation`
is an exact RFC 6901 JSON Pointer; the empty string denotes the document root.
`normativeSchemaKeyword` is the exact JSON Schema 2020-12 keyword whose
contract rule failed, such as `required`, `const`, `type`, `pattern`,
`minItems`, `uniqueItems`, or `additionalProperties`.

`schemaRuleId` is a QAIP-owned identifier assigned by the manifest schema
contract, not by the validator. V1 uses:

```text
qaip-scenario-authority-manifest-schema-v1#<canonical schema JSON Pointer>
```

The pointer names the exact keyword-bearing node in the immutable QAIP schema
resource. Schema `$id`, retrieval URI, validator schema location, and library
codes are not schema-rule identifiers.

A typed parameter is encoded as parameter name, one type code, and the value.
The V1 type codes are `BOOLEAN`, `UINT64`, `TEXT`, and `ORDERED_TEXT_LIST`.
Parameter names use exact Unicode code-point order and are unique. Only values
defined by the QAIP schema-adapter contract for that keyword are admitted; for
example, a `required` violation carries the missing property name, while
`minItems` carries the normative unsigned minimum. Authored invalid values are
included only where the QAIP rule explicitly requires them; validator-rendered
fragments are never copied.

Diagnostics are sorted by this exact tuple:

1. instance location by Unicode code point;
2. normative keyword by Unicode code point;
3. schema-rule ID by Unicode code point;
4. stable diagnostic code by Unicode code point; and
5. canonical typed-parameter bytes lexicographically as unsigned bytes.

Exact duplicate diagnostic tuples are rejected. Validator emission order,
numeric codes, schema URIs, exception types, localized or human-readable
messages, and library-generated wording are excluded. The current validator
adapter must map library results into this vocabulary before rejected-member
fingerprinting; its existing machine detail is not automatically canonical.

## Semantic field contracts

### Step semantic content

After the common prefix, encode:

1. semantic contract version;
2. exact Step identity:
   - claimed Scenario identity;
   - phase `GIVEN`, `WHEN`, or `THEN`;
   - zero-based ordinal within that phase as `uint64`;
   - `qaip-scenario-step-identity-v1`;
3. exact decoded authored step text.

Step text is engineering content. No whitespace, newline, case, or Unicode
normalization is applied. The JSON spelling used to encode that string is not
semantic.

### Unresolved HTTP Operation-reference content

After the common prefix, encode:

1. semantic contract version;
2. exact datum identity:
   - claimed Scenario identity;
   - fixed role `OPERATION_REF`;
   - operation-reference datum identity version;
3. fixed target profile `qaip-http-operation-reference-v1`;
4. exact schema-admitted method; and
5. exact schema-admitted path.

No resolved Operation identity, OpenAPI snapshot, candidates, match outcome, or
resolution provenance enters this fingerprint.

### Unresolved Business Rule-reference content

After the common prefix, encode:

1. semantic contract version;
2. exact datum identity:
   - claimed Scenario identity;
   - referenced authority;
   - stableRuleKey;
   - identityScheme;
   - Business Rule-reference datum identity version;
3. exact referenced authority;
4. exact stableRuleKey; and
5. exact identityScheme.

The repeated identity-bearing values make the content contract self-checking;
inconsistency is rejected. Authored array position is excluded from the
individual reference fingerprint. No resolved Business Rule, candidate,
reconciliation, or verification result enters it.

### Scenario semantic content

After the common prefix, encode:

1. normalization version;
2. Scenario semantic contract version;
3. exact claimed Scenario identity:
   - exact authority;
   - exact scenarioKey;
   - `qaip-scenario-identity-v1`;
4. exact title;
5. ordered count and fingerprint references for `GIVEN` steps;
6. ordered count and fingerprint references for `WHEN` steps;
7. ordered count and fingerprint references for `THEN` steps;
8. exactly one unresolved HTTP Operation-reference fingerprint reference; and
9. ordered Business Rule-reference fingerprint references.

Title and exact decoded Step text are semantic content, never identity aliases.
Step order is semantic within each phase. The phase sequence is fixed as
`GIVEN`, `WHEN`, `THEN`.

Business Rule-reference authored order is Scenario content and provenance. It
therefore changes the Scenario semantic fingerprint, but grants no resolution
priority, precedence, authority, or evidential strength. Repeated references,
where a future schema admits them, remain repeated ordered elements.

Manifest path, Scenario array index, raw bytes, raw fingerprint, JSON object
order, source spans, and occurrence identity are excluded. These exclusions
make two occurrences comparable without collapsing the occurrences.

#### Mandatory Scenario composition anti-substitution boundary

Leaf semantic fingerprint values are opaque integrity values. A supported
value prefix proves the leaf fingerprint domain and version, but the digest
does not expose or independently prove the claimed Scenario identity, Step
phase or ordinal, or reference datum identity from which it was calculated.
Opaque leaf fingerprint values alone are therefore insufficient evidence that
a leaf belongs to the claimed Scenario into which it is composed.

`ScenarioSemanticFingerprintEncoder` remains the low-level pure canonical
encoder for the field sequence above. It encodes already-accepted leaf
fingerprint references and must not deserialize, inspect, reconstruct, or
independently canonicalize leaf semantic content. Direct construction from
naked leaf fingerprint values is not an accepted production Scenario
fingerprint path.

Production construction passes through an Evidence Governance composition
boundary. That boundary uses these immutable typed authoritative attestations:

```text
FingerprintStepAttestation {
  exact StepSemanticFingerprintInput
  authoritative StepSemanticFingerprint
}

FingerprintOperationReferenceAttestation {
  exact HttpOperationReferenceSemanticFingerprintInput
  authoritative HttpOperationReferenceSemanticFingerprint
}

FingerprintBusinessRuleReferenceAttestation {
  exact BusinessRuleReferenceSemanticFingerprintInput
  authoritative BusinessRuleReferenceSemanticFingerprint
}
```

An attestation is created or verified only by Evidence Governance using the
one authoritative encoder for that leaf domain. Verification recalculates the
fingerprint from the exact attested input and requires value equality. A
mismatched input/fingerprint pair, unsupported fingerprint type or version, or
unsupported input contract is an integrity/processing failure, not an ordinary
source outcome or qualification state.

Before constructing `ScenarioSemanticFingerprintInput`, the composition
boundary requires all of these invariants:

1. Every Step input's exact claimed Scenario identity equals the parent
   Scenario's exact claimed identity.
2. Every Step phase equals its containing `GIVEN`, `WHEN`, or `THEN`
   collection.
3. Step ordinals in each phase are zero-based, contiguous, unique, and equal
   to their collection positions.
4. Every Step fingerprint is bound by its attestation to that exact Step
   input.
5. The Operation-reference input's exact claimed Scenario identity equals the
   parent Scenario's exact claimed identity, and its fingerprint is bound by
   its attestation to that exact input.
6. Every Business Rule-reference input's exact claimed Scenario identity
   equals the parent Scenario's exact claimed identity, and every fingerprint
   is bound by its attestation to that exact input.
7. Business Rule-reference authored positions are zero-based, contiguous,
   unique, and preserve authored array order before position is discarded from
   the individual Business Rule-reference semantic fingerprint.
8. All identity, target-profile, fingerprint, normalization, and semantic
   contracts are supported V1 contracts.
9. Composition preserves the normalized authored order. It performs no
   sorting, first/last/majority winner selection, deduplication, or caller
   fingerprint substitution.

Failure of any invariant is a deterministic processing/integrity failure and
emits no Scenario semantic fingerprint. It does not emit a partial Scenario
fingerprint and cannot be repaired by later authority qualification.

ADR-014 normalized Scenario source records provide the source-native parent
and leaf identities, phases, ordinals, authored positions, and exact content
required for these checks. The Extractor may map those immutable normalized
records into a composition request, but it does not attest or accept the
fingerprints. Evidence Governance owns attestation creation/verification,
composition validation, construction of the accepted
`ScenarioSemanticFingerprintInput`, and invocation of the low-level encoder.

This boundary adds no canonical fields. The Scenario canonical byte sequence,
domain, encoding and value identifiers, fingerprint values, and normative
golden vectors defined by this ADR remain unchanged.

### Manifest semantic content

This ADR distinguishes three independent states:

1. **Structural admission** means that the authority-attributed Manifest
   satisfies the fixed Scenario Authority JSON Schema.
2. **Scenario semantic availability** means that an authored Scenario
   occurrence has an authoritative supported `ScenarioSemanticFingerprint`.
3. **Manifest semantic availability** means that every authored Scenario
   occurrence in the structurally admitted Manifest has an authoritative
   `ScenarioSemanticFingerprint`, so the `ManifestSemanticFingerprint` can be
   composed.

Structural admission does not imply Scenario semantic availability or Manifest
semantic availability.

After the common prefix, encode:

1. normalization version;
2. manifest semantic contract version;
3. exact claimed authority;
4. exact format;
5. exact schemaVersion;
6. exact scenarioIdentityScheme; and
7. ordered Scenario semantic fingerprint references in authored Scenario-array
   order.

Manifest occurrence identity, repository-relative path, parent identity, raw
bytes, raw fingerprint, and Scenario array indexes are excluded. The logical
snapshot separately binds one manifest occurrence identity to this fingerprint.
Moving a manifest therefore changes occurrence and repository identity without
misrepresenting unchanged manifest meaning.

`ManifestSemanticFingerprint` represents only a structurally admitted Manifest
whose complete authored Scenario semantic content is available. It requires
exactly one authoritative `ScenarioSemanticFingerprint` for every authored
Scenario, in exact authored Scenario-array order. If any authored Scenario is
semantically unavailable, no partial Manifest semantic fingerprint is emitted;
structural admission and authority attribution remain unchanged, and the
Manifest occurrence remains retained evidence.

An attributable rejected member remains fingerprint-bound through its
attributed-member outcome. A future contract may safely fingerprint a broader
rejected semantic envelope only under a new explicit version. This amendment
does not change the Manifest semantic fingerprint domain, encoding, canonical
sequence, or golden bytes.

#### Manifest semantic composition outcome V1

`ManifestSemanticCompositionOutcomeV1` is an authoritative outcome with exactly
two states:

- `COMPOSED` requires a structurally admitted Manifest, the exact normalized
  Manifest, an authoritative `COMPOSED` Scenario semantic outcome for every
  authored Scenario, and the authoritative `ManifestSemanticFingerprint`.
- `UNAVAILABLE` requires a structurally admitted Manifest, the exact normalized
  Manifest, at least one authoritative finite `UNAVAILABLE` Scenario semantic
  outcome, and no `ManifestSemanticFingerprint`.

The only Manifest-level unavailable reason is
`SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`. Exact child Scenario outcomes retain
whether the underlying reason is `UNSUPPORTED_SEMANTIC_CONTRACT`,
`SCENARIO_COMPOSITION_INTEGRITY_FAILURE`, or a mixture; those reasons are not
duplicated at Manifest level.

Only authoritative finite Scenario semantic-unavailable outcomes may produce
Manifest `UNAVAILABLE`. Caller-selected Manifest outcome or reason is
prohibited. Unexpected programming, infrastructure, runtime, or unclassified
failures remain processing failures: they produce neither Manifest
`UNAVAILABLE` nor a Manifest fingerprint.

## Attributed-member outcome fingerprint

After the common prefix, encode:

1. attributed-member outcome-contract version;
2. full `ParentCapturedMemberRef` in ADR-014 field order:
   - parent sourceId;
   - parent snapshotId;
   - parent repository-capture content fingerprint;
   - normalized repository-relative path;
   - raw byte length;
   - raw member fingerprint;
3. exact claimed authority;
4. parser contract identifier;
5. attribution contract identifier;
6. parsing outcome, necessarily `PARSED`;
7. attribution outcome, necessarily `ATTRIBUTED`;
8. optional attribution structural location;
9. schema contract identifier;
10. structural/schema admission outcome;
11. canonically ordered stable schema diagnostics;
12. optional manifest occurrence identity; and
13. optional admitted manifest semantic fingerprint reference.

`STRUCTURALLY_ADMITTED` requires no schema diagnostics, a present Manifest
occurrence identity, and a present authoritative
`ManifestSemanticCompositionOutcomeV1`. If that outcome is `COMPOSED`, the
Manifest semantic fingerprint must be present. If it is `UNAVAILABLE`, the
Manifest semantic fingerprint must be absent. `STRUCTURALLY_REJECTED` requires
at least one stable diagnostic and no admitted Manifest occurrence identity,
Manifest semantic composition outcome, or Manifest semantic fingerprint.

This activates the already encodable combination `STRUCTURALLY_ADMITTED` plus
present Manifest occurrence identity plus absent Manifest semantic fingerprint
only when an authoritative Manifest `UNAVAILABLE` proof backs it. The canonical
field order and optional encoding above do not change. Existing valid
attributed-member outcome fingerprint bytes therefore remain unchanged; the
newly admitted combination receives deterministic bytes through that existing
optional encoding. The parent reference binds rejected content to exact
captured bytes without treating the raw digest as a semantic digest.

The authoritative Manifest composition outcome is proof-bearing validation
input for constructing an admitted attributed-member fingerprint. It is not a
new canonical attributed-outcome field. Evidence Governance verifies that
outcome before constructing the canonical input above: the Manifest occurrence
identity is canonical, and the existing optional Manifest fingerprint is
present exactly for `COMPOSED` and absent exactly for `UNAVAILABLE`.

Parsed JSON trees, generic serialized JSON, human messages, validator metadata,
and qualification results are excluded.

## Repository derivation-report fingerprint

The repository report accounts for every parent captured regular member before
authority partitioning. After the common prefix, encode:

1. report contract version;
2. full parent Repository Capture Snapshot identity tuple;
3. parent repository-capture fingerprint;
4. parser contract identifier;
5. attribution contract identifier;
6. structural-location contract identifier;
7. parent regular-member count;
8. exactly one tagged outcome in exact parent-member order for each member:
   - tag `ATTRIBUTED_MEMBER_RETAINED`, full parent-member reference, exact
     claimed authority, and attributed-member outcome fingerprint reference;
     or
   - tag `UNATTRIBUTABLE_MEMBER_RETAINED`, full parent-member reference,
     parsing outcome, attribution outcome, and optional structural location;
9. parent unsupported-matching-entry count and each unchanged parent entry in
   canonical parent order as normalized path, stable entry kind, and stable
   diagnostic code; and
10. ordered semantic provenance fingerprint references.

Member outcomes must cover the parent regular membership exactly once. The
report does not assign authority to unattributable members. Unsupported entries
are retained as repository observations, not parsed manifests.

Absolute paths, exact bytes, decoded malformed text, human diagnostics,
timestamps, and Git or host metadata are excluded. Exact bytes remain
resolvable from the referenced parent capture.

## Scenario identity-group fingerprint and duplicate semantics

After the common prefix, encode:

1. duplicate-outcome contract version;
2. exact claimed Scenario identity;
3. group outcome;
4. occurrence count; and
5. every occurrence in deterministic order:
   - Scenario occurrence identity;
   - full `ParentCapturedMemberRef`;
   - RFC 6901 structural location;
   - optional supported Scenario semantic fingerprint reference;
   - optional stable non-comparability reason.

Occurrences are ordered by normalized repository-relative member path using
exact Unicode code-point order, then by numeric zero-based Scenario array
index. Equal ordering keys are an invariant failure. Groups are ordered in an
aggregate by exact authority, scenarioKey, and identity scheme, each using
Unicode code-point order.

The states are:

- `UNIQUE`: exactly one admitted occurrence with a supported Scenario semantic
  fingerprint;
- `DUPLICATE_EQUIVALENT`: two or more occurrences, all fingerprints use the
  same supported semantic contract, and every complete fingerprint is equal;
- `DUPLICATE_CONFLICTING`: two or more occurrences have comparable supported
  fingerprints under the same contract and at least one differs; and
- `DUPLICATE_UNCLASSIFIED`: two or more occurrences exist but complete
  deterministic comparability is unavailable.

Every duplicate state is rejecting. Equivalent duplication does not increase
authority, confidence, completeness, corroboration, or evidential strength.
There is no first, last, file-order, newest, majority, source-category, or
content-similarity winner. Title, Steps, references, JSON trees, and authored
text are never compared directly by grouping; only complete supported Scenario
semantic fingerprints are compared.

`DUPLICATE_UNCLASSIFIED` remains persistent direct evidence. An occurrence with
an authoritative semantic `UNAVAILABLE` outcome neither revokes its parent
Manifest's structural admission, removes that Manifest from its authority
partition, nor removes the occurrence from group evidence. Its parent Manifest
has Manifest semantic outcome `UNAVAILABLE` and no Manifest semantic
fingerprint.

`DUPLICATE_EQUIVALENT` and `DUPLICATE_CONFLICTING` may coexist with a parent
Manifest semantic outcome of `COMPOSED` when every authored Scenario occurrence
has a Scenario semantic fingerprint. Duplicate classification controls Scenario
admissibility, not Manifest semantic availability.

## Semantic provenance fingerprint

Semantic provenance records the reproducible derivation edge without importing
operational metadata. After the common prefix, encode:

1. provenance contract/version;
2. stable provenance identity;
3. stable activity kind;
4. activity/normalization version;
5. output datum identity;
6. output semantic fingerprint reference;
7. ordered parent count and parent references, each containing the parent datum
   or captured-member identity and integrity fingerprint reference; and
8. stable derivation outcome.

Parent order is canonical identity order unless the named activity contract
explicitly declares sequence semantic. Parent references are encoded as opaque
integrity-bound references; the complete referred provenance document is not
recursively serialized.

Semantic provenance follows a strict acyclic dependency rule:

- a semantic provenance record may reference as parents and name as its output
  only already-identified and already-fingerprinted child data or outcomes;
- provenance embedded by fingerprint reference in an aggregate must not name
  that containing aggregate's fingerprint as its output;
- no fingerprint dependency may directly or transitively depend on itself;
- provenance edges follow the fingerprint dependency graph from completed
  child fingerprints toward later aggregate fingerprints; and
- an encoder rejects any forward reference, self-reference, back edge, or
  dependency cycle as an integrity/processing failure.

The allowed output kinds for provenance used in each context are:

| Embedding or association context | Allowed output kinds | Prohibited output kind |
| --- | --- | --- |
| Repository Derivation Report | `ATTRIBUTED_MEMBER_OUTCOME`; an independently fingerprinted captured-member or child processing datum defined by its supported contract | the containing `REPOSITORY_DERIVATION_REPORT` |
| Manifest semantic content context | `STEP_SEMANTIC_CONTENT`, `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT`, `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT`, `SCENARIO_SEMANTIC_CONTENT`, or `MANIFEST_SEMANTIC_CONTENT` after that output fingerprint exists | no provenance is embedded in the Manifest semantic fingerprint itself |
| Scenario semantic content context | `STEP_SEMANTIC_CONTENT`, `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT`, `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT`, or `SCENARIO_SEMANTIC_CONTENT` after that output fingerprint exists | no provenance is embedded in the Scenario semantic fingerprint itself |
| Logical Source Authority Snapshot V2 | `ATTRIBUTED_MEMBER_OUTCOME`, `MANIFEST_SEMANTIC_CONTENT`, `STEP_SEMANTIC_CONTENT`, `HTTP_OPERATION_REFERENCE_SEMANTIC_CONTENT`, `BUSINESS_RULE_REFERENCE_SEMANTIC_CONTENT`, `SCENARIO_SEMANTIC_CONTENT`, or `SCENARIO_IDENTITY_GROUP` | the containing `LOGICAL_SOURCE_AUTHORITY_SNAPSHOT_V2` |

Manifest and Scenario semantic encodings contain child fingerprint references,
not semantic provenance references. “Context” in the table means provenance
associated with those completed semantic outputs and included only by a later
aggregate. Repository-level unattributable outcomes remain embedded and bound
by the Repository Derivation Report; because they have no independent V1
fingerprint, they cannot be named as the output of a semantic provenance record.

If aggregate-level provenance is later required for a Repository Derivation
Report or Logical Source Authority Snapshot, it must exist outside the content
fingerprint of the aggregate it describes, or inside a separately versioned
non-cyclic envelope constructed after that aggregate fingerprint exists. It
cannot be inserted back into that aggregate's canonical content.

Unresolved references, duplicate parent identities, output-fingerprint
mismatch, incompatible versions, and all dependency cycles are
integrity/processing failures.

Capture or processing timestamps, Git revision/branch/tag/remote, absolute
paths, host, process, user, operating system, stack traces, and human messages
are excluded. A normalized repository-relative path appears only where a
captured-member identity explicitly requires it; it is not imported merely
because operational provenance also contains a locator.

## Logical Source Authority Snapshot V2 aggregate

The aggregate uses:

```text
encoding identifier: scenario-authority-logical-source-c14n-v2
digest identifier:   sha-256-v1
value format:        scenario-authority-logical-source-v2:<64 lowercase hex>
domain string:       QAIP\u0000SCENARIO_AUTHORITY_LOGICAL_SOURCE\u0000V2
```

After the common prefix, encode in this exact order:

1. sourceId;
2. exact claimed authority namespace;
3. source profile;
4. source-contract version;
5. full parent Repository Capture Snapshot identity tuple;
6. parent repository-capture fingerprint;
7. format, schema, parser, attribution, structural-location, normalization,
   manifest-identity, Scenario-occurrence-identity, Scenario-identity, Step,
   Operation-reference-identity, Business Rule-reference-identity,
   semantic-canonicalization, semantic-fingerprint, outcome, diagnostic, and
   provenance contract identifiers in that order;
8. attributed-member count and outcome fingerprint references in exact parent
   member order for members attributed to this exact authority;
9. structurally admitted Manifest occurrence count and, in normalized
   member-path order, each canonical Manifest evidence entry containing exactly:
   the exact Manifest occurrence identity, Manifest semantic availability state,
   explicit optional Manifest semantic fingerprint reference, and explicit
   optional Manifest unavailable reason; rejected attributed members remain
   represented by item 8 and do not fabricate a normalized Manifest occurrence
   or Manifest semantic composition outcome;
10. Scenario identity-group count and group fingerprint references in exact
    claimed-identity order;
11. normalized datum count and each datum kind, datum identity, and semantic
    fingerprint reference, ordered first by frozen datum-kind order
    `MANIFEST`, `SCENARIO`, `STEP`, `OPERATION_REFERENCE`,
    `BUSINESS_RULE_REFERENCE`, then by that kind's exact identity tuple; and
12. semantic provenance fingerprint-reference count and references ordered by
    stable provenance identity.

Counts, membership, and references must agree. A datum appears exactly once in
the normalized datum section. A duplicate Scenario group publishes occurrence
data and semantic fingerprints for comparison but publishes no unique accepted
Scenario, Step, or reference datum membership. The group remains visible and
fingerprinted.

Every structurally admitted Manifest remains direct authority evidence through
its occurrence identity and authoritative Manifest semantic composition
outcome. A normalized `MANIFEST` datum appears in item 11 only for `COMPOSED`.
For `UNAVAILABLE`, no normalized `MANIFEST` datum or Manifest semantic
fingerprint is published, but the Manifest occurrence/outcome remains direct
evidence. Scenario identity-group evidence is retained independently.

The Manifest evidence-entry invariants are exact:

- `COMPOSED` means state `COMPOSED`, present Manifest semantic fingerprint, and
  absent unavailable reason.
- `UNAVAILABLE` means state `UNAVAILABLE`, absent Manifest semantic fingerprint,
  and present unavailable reason exactly
  `SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`.

Both optionals present, both absent, or either optional inconsistent with the
state is an aggregate-integrity failure. The complete proof-bearing
`ManifestSemanticCompositionOutcomeV1` is verified during construction but is
not serialized verbatim into Logical V2 canonical bytes. Its normalized
Manifest object, complete child Scenario composition outcomes, leaf
attestations, recomputation intermediates, and human or debug diagnostics are
proof-only validation inputs and do not enter Logical V2 canonical bytes
through the Manifest entry. They may contribute independently through another
already-approved direct Logical V2 member where that member's contract requires
them.

The snapshot identity remains exactly:

```text
sourceId + snapshotId + contentFingerprint
```

`snapshotId` is externally supplied and is excluded from content encoding.
Equal content fingerprints do not collapse distinct snapshot observations.
Reuse of one `sourceId + snapshotId` with another fingerprint is a hard
substitution failure.

Repository derivation reports are repository-scope artifacts and are referenced
from provenance or a containing evidence bundle; unattributable members are not
copied into an authority snapshot. Their absence from an authority partition
does not erase them from the parent capture or derivation report.

## Semantic versus non-semantic ordering

- JSON object field order is never semantic.
- Manifest Scenario-array order is semantic manifest content and also defines
  occurrence position; it grants no evidential precedence.
- Phase order is fixed `GIVEN`, `WHEN`, `THEN`; Step order within a phase is
  semantic.
- Business Rule-reference authored order is Scenario content and provenance,
  but not resolution priority.
- Parent captured-member and manifest order is normalized path Unicode
  code-point order.
- Scenario groups use authority, scenarioKey, identity-scheme Unicode
  code-point order.
- Occurrences use member path then numeric Scenario index.
- Diagnostics use the tuple defined by this ADR, never validator emission
  order.
- Semantic provenance uses stable provenance identity order unless an activity
  explicitly defines semantic parent sequence.

Authored ordering retained as content never establishes a first/last winner or
increases evidential strength.

## Exclusions from every semantic and logical fingerprint

Unless an exact normalized repository-relative path is expressly required by
an occurrence or parent-member identity, the following never enter a semantic
or logical fingerprint:

- absolute checkout paths or live filesystem locators;
- timestamps;
- Git revision, branch, tag, remote, or worktree status;
- host, process, user, or operating-system metadata;
- human-readable diagnostics, exception messages, or stack traces;
- JSON formatting, object-member insertion order, or serializer settings;
- authority declarations or declaration fingerprints;
- trust policies;
- qualification contexts or qualification states;
- resolved Operation or Business Rule targets and resolution outcomes;
- canonical nodes, canonical relationships, or downstream analysis; and
- `SPECIFIED_BY`, `COVERS`, or `VALIDATES` assertions.

Raw member byte fingerprints remain mandatory parent integrity inputs, but
never substitute for manifest, Scenario, Step, reference, group, provenance,
or logical semantic fingerprints.

## Fingerprint dependency graph

```text
Step fingerprints -----------+
Operation-reference fp ------+--> Scenario semantic fingerprint
Business Rule-reference fps -+

Authoritative Scenario occurrence composition outcomes
  |
  +-- all authored outcomes COMPOSED
  |     + ordered authoritative Scenario semantic fingerprints
  |     + exact normalized Manifest
  |         |
  |         +--> Manifest semantic fingerprint
  |                  |
  |                  +--> Manifest semantic composition outcome COMPOSED
  |                            |
  |                            +--> admitted/COMPOSED attributed-member outcome fp
  |
  +-- at least one authoritative outcome UNAVAILABLE
        + exact normalized Manifest
            |
            +--> Manifest semantic composition outcome UNAVAILABLE
                     |
                     +--> no Manifest semantic fingerprint
                     |
                     +--> admitted/UNAVAILABLE attributed-member outcome fp

Parent member ref + stable outcomes --------+--> Attributed-member outcome fp

Completed child datum/outcome fingerprint
  + activity and already-completed parent refs --> Semantic provenance fp

Parent capture + attributed outcome refs
  + embedded unattributable outcomes
  + unsupported entries
  + provenance for allowed completed children ---> Repository derivation report fp

Scenario occurrence identities
  + parent-member refs
  + Scenario semantic fps ----------------------> Scenario identity-group fp

Attributed outcomes + manifest occurrences/outcomes
  + Scenario groups + normalized data
  + provenance for allowed completed children ---> Logical Source Snapshot V2 fp

Repository Derivation Report fp ----------------> optional later external envelope
Logical Source Snapshot V2 fp ------------------> optional later external envelope
```

Every dependency edge points from already-established authoritative input or
evidence to a value that depends on it. Dependencies may originate from
normalized authoritative input, authoritative composition outcomes, completed
semantic fingerprints, or other already-established proof-bearing evidence. No
dependency may point from a value to one of its own required inputs. No
provenance arrow points backward from a child to an aggregate that contains it.
An aggregate embeds fingerprint references rather than reimplementing a child's
canonical field serialization. Aggregate-level provenance, when present, is
downstream in an external non-cyclic envelope and cannot alter the completed
aggregate fingerprint.

## Golden vectors

Evidence Governance publishes input models, exact canonical bytes in lowercase
hexadecimal, and expected prefixed fingerprints for every domain. Required
vectors include:

- empty and smallest valid collections where the domain permits them;
- one admitted manifest and one Scenario;
- every parse, attribution, structural-admission, derivation, and duplicate
  code;
- one attributable schema-rejected member with one and multiple diagnostics;
- one unattributable member for every terminal outcome;
- diagnostic reordering, library-message changes, and duplicate-diagnostic
  rejection;
- absent versus present-empty optional values;
- ASCII, multibyte Unicode, supplementary code points, delimiter-like text,
  embedded NUL, and newline text;
- JSON inputs with different whitespace, escape spellings, and object order
  that produce equal semantic but different raw fingerprints;
- title-only, Step-text, Step-phase, and Step-ordinal changes;
- Operation method/path changes;
- Business Rule authority/key/scheme changes and authored-reference reorder;
- same admitted Scenario meaning at different member paths and Scenario array
  indexes: equal Scenario semantic fingerprints and different occurrences;
- equivalent, conflicting, and unclassified duplicates with all occurrences
  retained;
- parent-member path, raw length, and raw fingerprint changes affecting outcome
  and aggregate fingerprints without changing child semantic meaning;
- provenance-only timestamp, Git, absolute-path, host, process, user, OS, and
  human-message changes proving semantic fingerprint stability;
- collection-order changes wherever order is semantic;
- noncanonical set ordering, duplicate keys, invalid UTF-8, unpaired surrogate,
  count/length overflow, unsupported version, and invalid optional-tag
  rejection;
- exact domain bytes for all ten domains, proving every written `\u0000`
  separator contributes exactly one `00` byte and no backslash notation bytes;
  and
- change of every fingerprint-relevant contract/version identifier.

Golden vectors are normative interoperability artifacts. An independent test
reader may verify them, but production adapters do not implement a second
encoder.

The Manifest semantic-availability amendment additionally requires future
golden or rejection vectors for:

- admitted Manifest with all Scenario outcomes composed;
- admitted Manifest with an unsupported Scenario;
- admitted Manifest with an integrity-unavailable Scenario;
- mixed child unavailable reasons;
- all child fingerprints available with `DUPLICATE_EQUIVALENT`;
- all child fingerprints available with `DUPLICATE_CONFLICTING`;
- rejection of a caller-selected Manifest unavailable reason;
- proof that an unexpected child processing failure does not become Manifest
  `UNAVAILABLE`;
- admitted-unavailable attributed-member outcome bytes; and
- existing composed and rejected attributed-member outcome byte compatibility.

## Ownership

The Scenario source adapter / Extractor owns source-specific parsing,
attribution, schema-to-QAIP-diagnostic mapping, normalization, occurrence
construction, mapping normalized records into composition requests, and
untrusted candidate assembly. It calls Evidence Governance fingerprint
services and must not treat naked leaf fingerprints as accepted Scenario
composition input.

Evidence Governance owns the common binary primitives, all domain encoders,
digest/value types, supported-version registry, canonical diagnostic contract,
typed leaf attestations, attestation verification, Scenario composition
acceptance, anti-substitution validation, semantic provenance invariants,
Logical V2 aggregate encoding, and golden vectors. There is exactly one
authoritative production implementation per domain. Source adapters must not
copy, translate, or reproduce canonical serialization.

Runtime, Coverage, Explorer, Canonical Ontology, Operation/Rule resolution, and
relationship formation own none of these encodings and are unchanged by this
ADR.

## Compatibility consequences

### ADR-006

The contract provides integrity-bound semantic fingerprints for every
independently referenced normalized datum, deterministic derived provenance,
retained rejection outcomes, and immutable aggregate membership. Occurrence-
distinct but content-equivalent observations remain provenance-distinct.

### ADR-011

The exact Scenario identity remains authority + scenarioKey +
`qaip-scenario-identity-v1`. Title, Steps, references, paths, and array indexes
remain content or occurrence, not identity. Authored ordering does not become
precedence. Duplicate declarations remain rejecting and retain every
occurrence.

### ADR-013

Repository capture continues to answer what exact bytes and membership were
captured. Raw and repository-capture fingerprints remain separate domains and
are referenced for integrity rather than reused as semantic fingerprints. Git
and operational metadata remain provenance-only.

### ADR-014

This ADR freezes the semantic prerequisite and exact Logical V2 boundary left
open by ADR-014. Authority declaration lookup, trust policy, capability, frozen
qualification context, and qualification state remain later independent
results. One logical snapshot can receive different qualification results
without changing content identity.

`COMPOSE_MANIFEST_SEMANTIC_CONTENT` provenance applies only to successfully
`COMPOSED` Manifest semantic content. Under semantic provenance V1, Manifest
`UNAVAILABLE` creates no semantic provenance fingerprint. No additional
provenance activity is activated by this amendment.

No Canonical Ontology change is required. No canonical `SCENARIO`, resolved
reference, `SPECIFIED_BY`, `COVERS`, or `VALIDATES` assertion is created.

## Consequences

### Positive

- Semantic equivalence is no longer confused with byte or occurrence equality.
- Formatting-only JSON changes remain visible in raw capture without changing
  engineering semantic fingerprints.
- Duplicate equivalent/conflicting classification becomes deterministic.
- Rejected and unattributable evidence remains integrity-bound.
- Validator wording and implementation order cannot perturb content identity.
- Domain separation prevents one fingerprint kind from substituting for
  another.
- Logical snapshots remain requalifiable under different frozen contexts.
- Structural admission remains retained when complete Manifest semantics cannot
  be composed.

### Negative

- Ten fingerprint domains and normative golden vectors add contract and test
  surface.
- Validator results require an explicit QAIP schema-rule adapter.
- Source ordering that is semantic must be retained even where it grants no
  precedence.
- Fingerprint version changes can invalidate comparison with older semantic
  fingerprints until an explicit compatibility policy exists.

### Manifest semantic-availability compatibility

Existing `ScenarioSemanticFingerprint`, `ManifestSemanticFingerprint`,
`ScenarioIdentityGroupFingerprint`, and semantic provenance bytes are
unchanged. No partial Manifest fingerprint is introduced. Existing valid
`AttributedMemberOutcomeFingerprint` bytes are unchanged. The newly permitted
structurally-admitted, Manifest-unavailable combination receives deterministic
bytes using the already defined optional encoding. No existing semantic
fingerprint domain requires a version change.

Logical V2 has no production fingerprints, so its corrected canonical Manifest
entry requires no migration or compatibility version. The Manifest semantic
fingerprint itself remains available only after all authored Scenario outcomes
are `COMPOSED`; its canonical input remains exactly the authored-order sequence
of complete Scenario semantic fingerprints. No unavailable marker or partial
child content enters that fingerprint domain.

This amendment requires five follow-up deliverables before the corrected path
is implemented and gated:

1. a subordinate Manifest semantic-outcome V1 contract;
2. the corresponding Attributed Member Outcome invariant and construction
   correction;
3. a Logical V2 subordinate-contract correction;
4. a Semantic Provenance V1 wording and DAG compatibility correction, satisfied
   by the accompanying documentation correction, which introduces no new
   provenance activity, fingerprint version, or byte change; and
5. the production Manifest composition-attempt implementation and capability
   gate.

## Rejected alternatives

### Generic canonical JSON for all semantic content

Rejected because object layout is not the engineering contract, diagnostics
and derived records are not all JSON, and generic canonical JSON would not
define datum-specific ordering, optional, identity, or version semantics.

### Raw member fingerprint as semantic fingerprint

Rejected because whitespace, escaping, object order, and other syntax change
raw bytes without necessarily changing engineering meaning.

### One fingerprint for occurrence and Scenario meaning

Rejected because paths and array positions would make equivalent duplicate
content incomparable. Occurrence identity remains separately bound.

### Fingerprint every Java record

Rejected because it creates unnecessary domains and substitution opportunities.
Scenario occurrence and unattributable outcome records are embedded in their
independently fingerprinted parents.

### Include qualification or resolved references

Rejected because qualification policy and available cross-source snapshots may
change without changing authored logical content.

### Compare titles, Steps, or JSON directly during duplicate grouping

Rejected because grouping would own a second semantic canonicalization and
could drift from the authoritative Scenario fingerprint.

### Hash complete provenance documents recursively

Rejected because timestamps, hosts, paths, and human diagnostics could enter
semantic identity transitively, and recursive provenance graphs complicate
cycle safety. Semantic provenance uses explicit immutable references.

## Unresolved questions

- What concrete neutral Evidence Governance API and package expose these ten
  encoders without introducing source-adapter dependency inversion?
- What stable snapshot-ID allocation policy identifies Logical Source
  observations? Snapshot ID remains outside content fingerprinting.
- Which storage and access policy governs exact bytes and operational
  provenance excluded from semantic fingerprints?
- Does a future schema version admit repeated identical Business Rule
  references, and if so which source-semantic rejection outcome accompanies
  them? V1 ordering and no-precedence rules remain unchanged.
- What compatibility artifact permits comparison or migration between future
  semantic canonicalization versions? Cross-version fingerprints are not
  comparable by default.
- Which public API exposes repository derivation reports, semantic provenance,
  and duplicate evidence without presenting claimed authority as accepted
  authority?
- Does a future evidence-envelope contract carry aggregate-level provenance for
  Repository Derivation Reports and Logical Source Authority Snapshots? Such an
  envelope must remain separately versioned and acyclic.

These questions do not block the first semantic fingerprint implementation.
The first implementation must begin with the shared primitives, stable
diagnostic adapter, leaf Step/reference encoders, and their golden vectors
before constructing Scenario, group, provenance, or Logical V2 aggregates.

## Conformance requirements

An implementation conforms only if:

- it implements exactly the ten domains and identifiers defined here;
- it does not introduce a separate Scenario occurrence semantic fingerprint in
  V2;
- every domain separator is the single U+0000 code point encoded as byte
  `0x00`, never backslash notation;
- it uses the common binary encoding and explicit optional tags;
- every production domain encoder is owned authoritatively by Evidence
  Governance;
- production Scenario fingerprint construction uses Evidence Governance typed
  leaf attestations and the mandatory composition anti-substitution boundary;
- every attested leaf fingerprint equals the authoritative fingerprint of its
  exact attested input, and every leaf belongs structurally and by exact
  claimed Scenario identity to its parent Scenario;
- failure of a Scenario composition invariant emits no Scenario semantic
  fingerprint;
- structural Manifest admission does not imply Scenario or Manifest semantic
  availability;
- Manifest `COMPOSED` is derived only from authoritative `COMPOSED` outcomes
  for every authored Scenario, and emits exactly one complete Manifest semantic
  fingerprint;
- Manifest `UNAVAILABLE` is derived only from at least one authoritative finite
  Scenario `UNAVAILABLE` outcome, uses only
  `SCENARIO_SEMANTIC_CONTENT_UNAVAILABLE`, and emits no Manifest semantic
  fingerprint;
- caller-selected Manifest outcomes and conversion of unexpected failures into
  Manifest `UNAVAILABLE` are rejected;
- every structurally admitted Manifest remains direct Logical V2 evidence, but
  only a `COMPOSED` Manifest contributes a normalized `MANIFEST` datum;
- source adapters never reproduce canonical serialization;
- raw byte fingerprints never substitute for semantic fingerprints;
- JSON object order and validator-library behavior never define semantic
  identity;
- rejected and unattributable evidence remains fingerprint-bound;
- duplicate equivalence/conflict uses only comparable supported Scenario
  semantic fingerprints and every duplicate remains rejecting;
- occurrence, provenance, qualification, and engineering meaning remain
  distinct;
- semantic provenance refers only to completed child fingerprints, conforms to
  the allowed-output-kind table, and creates no direct or transitive cycle;
- no provenance embedded in an aggregate names that aggregate as its output;
- Logical V2 excludes snapshotId, authority declarations, qualification,
  operational provenance, resolution, and canonical facts;
- snapshot identity remains `sourceId + snapshotId + contentFingerprint`;
- all noncanonical input and unsupported versions fail explicitly; and
- every domain passes its normative golden vectors.

## Scope exclusions

This ADR does not implement:

- canonical encoders, fingerprint value types, or golden-vector files;
- Logical Source Authority Snapshot construction;
- authority qualification;
- Operation or Business Rule resolution;
- canonical `SCENARIO` nodes;
- `SPECIFIED_BY`, `COVERS`, or `VALIDATES` relationships;
- Runtime, Coverage, or Explorer behavior;
- Canonical Ontology changes; or
- signing, encryption, retention, or access-control policy.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-011: Scenario Authority Source Contract](ADR-011-scenario-authority-source-contract.md)
- [ADR-013: Immutable Source Snapshot and Fingerprint Contract](ADR-013-immutable-source-snapshot-and-fingerprint-contract.md)
- [ADR-014: Logical Source Authority Snapshot and Qualification Boundary](ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
