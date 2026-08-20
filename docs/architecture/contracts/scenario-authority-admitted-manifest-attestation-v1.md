# Scenario Authority Admitted Manifest Attestation V1 Contract

Contract identifier: `scenario-authority-admitted-manifest-attestation-v1`

Status: normative subordinate contract of ADR-014 and ADR-015. This document is
not an ADR and does not broaden either ADR.

Date: 2026-08-20

Owner: Evidence Governance

Normative basis:

- [ADR-014: Logical Source Authority Snapshot and Qualification Boundary](../adr/ADR-014-logical-source-authority-snapshot-and-qualification-boundary.md)
- [ADR-015: Logical Source Semantic Canonicalization and Fingerprint Contract](../adr/ADR-015-logical-source-semantic-canonicalization-and-fingerprint-contract.md)
- [Scenario Authority Manifest Semantic Outcome V1](scenario-authority-manifest-semantic-outcome-v1.md)
- [Scenario Authority Schema Diagnostic Mapping V1](scenario-authority-schema-diagnostic-mapping-v1.md)

## Purpose and authority principle

This contract defines the minimum positively verifiable cross-module proof that
one exact Repository Capture member was successfully parsed, safely attributed,
structurally admitted, and deterministically normalized as one exact Scenario
Authority Manifest.

A public caller may supply candidate verification material. Evidence Governance
independently recomputes every claim needed to mint the proof and terminates the
verification in `RepositoryCaptureAttestation`. Constructor secrecy protects the
result, but positive recomputation is the source of authority.

The boundary must not derive authority from a split package, public marker
interface, DTO invariants, caller-selected admission state, Extractor normalized
record, caller-supplied identity or Scenario list, or an unrecomputed
fingerprint.

## Module ownership

The normative dependency direction is:

```text
qa-model-extractor
    -> qa-evidence-governance-core
        -> qa-model-validation-core
```

`qa-model-validation-core` owns reusable deterministic JSON Schema mechanics,
the content-pinned Manifest V1 schema resource, its SHA-256 identity, and stable
validation-result mechanics. It does not interpret Scenario Authority,
attribute authority, normalize a Manifest, or mint evidence.

`qa-evidence-governance-core` owns admitted-Manifest verification, interpretation
of validation results, the authoritative normalized Manifest representation,
the single authoritative exact-byte parser, safe authority attribution,
deterministic normalizer, Scenario declaration derivation, and
`VerifiedAdmittedManifestV1` construction.

`qa-model-extractor` owns source orchestration, custody of captured raw bytes,
mapping to the public candidate request, and compatibility projections. It does
not supply authoritative admission or normalized data to this verifier. Its V1
parser, attributor, and normalizer compatibility APIs delegate to the Evidence
Governance primitives and may not retain independent normative implementations.

Evidence Governance has no dependency on Extractor. No authoritative
implementation package may be split across these modules.

## Fixed V1 identifiers

| Role | Exact identifier |
| --- | --- |
| Attestation contract | `scenario-authority-admitted-manifest-attestation-v1` |
| Verification request | `scenario-authority-admitted-manifest-verification-request-v1` |
| Verification operation | `scenario-authority-admitted-manifest-verifier-v1` |
| Parser contract | `scenario-authority-json-parser-v1` |
| Attribution contract | `scenario-authority-attribution-v1` |
| Schema contract | `qaip-scenario-authority-manifest-schema-v1` |
| Schema content identity | `sha256:7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1` |
| Schema diagnostic mapping | `scenario-authority-schema-diagnostic-mapping-v1` |
| Source normalization | `scenario-authority-source-normalization-v1` |
| Manifest format | `qaip-scenario-authority-manifest-v1` |
| Manifest schema version | `1.0` |
| Manifest occurrence identity | `qaip-scenario-manifest-occurrence-identity-v1` |
| Scenario declaration occurrence identity | `qaip-scenario-declaration-occurrence-identity-v1` |
| Scenario identity scheme | `qaip-scenario-identity-v1` |
| Structural location | `rfc-6901-json-pointer-v1` |

The implementation must use the approved Step, HTTP Operation-reference, and
Business Rule-reference V1 identity/profile identifiers when deriving their
normalized values. No identifier is defaulted or inferred from a caller's
unsupported value.

## Public candidate request

Evidence Governance owns the immutable candidate type:

```text
AdmittedManifestVerificationRequestV1 {
  requestVersion
  RepositoryCaptureAttestation
  ParentCapturedMemberReference
  exact rawMemberBytes
  parserContractIdentifier
  attributionContractIdentifier
  schemaContractIdentifier
  schemaContentIdentity
  schemaDiagnosticMappingContractIdentifier
  sourceNormalizationVersion
  required V1 identity/profile identifiers
}
```

The request defensively copies the raw bytes and immutable collections. It is
candidate material, not evidence.

The request contains no structural-admission state,
`AttributedMemberSchemaAdmissionOutcome`, `NormalizedManifestDatum`, claimed
authority, Manifest occurrence identity, authored Scenario declarations,
normalized Scenario occurrences, or semantic fingerprint. Those values are
derived only by the verifier.

## Verification operation and result

Evidence Governance exposes exactly:

```text
AdmittedManifestVerifierV1.verifyAdmittedManifestV1(
  AdmittedManifestVerificationRequestV1 request
) -> VerifiedAdmittedManifestV1
```

The operation returns `VerifiedAdmittedManifestV1` only on success. Expected
non-admitted cases throw `AdmittedManifestVerificationRejectionV1` with one
finite code. This success-only model avoids a second rejected-member evidence
representation: rejected-member persistence remains owned by Attributed Member
Outcome and Repository Derivation Report.

An unexpected programming, infrastructure, library, or runtime failure is not a
finite rejection and propagates. No broad catch may convert it to admission,
semantic unavailability, or another authoritative outcome.

## Validation precedence

The exact precedence is:

1. **Request compatibility.** Validate every fixed contract, profile, and
   identity identifier.
2. **Capture and raw-member binding.** Verify the selected parent and supplied
   bytes against `RepositoryCaptureAttestation`.
3. **Parsing.** Parse only the verified bytes using the V1 parser semantics.
4. **Safe attribution.** Derive authority only from the parsed source.
5. **Schema validation.** Validate the exact parsed source with the pinned V1
   schema and canonical diagnostic mapping.
6. **Normalization.** Deterministically derive the authoritative normalized
   Manifest value.
7. **Occurrence and declaration derivation.** Derive Manifest identity and the
   complete authored Scenario sequence.
8. **Proof minting.** Construct `VerifiedAdmittedManifestV1` internally.

An earlier-stage failure wins. Selection among multiple human/debug diagnostics
inside one stage is non-authoritative unless already frozen by the schema
diagnostic contract. A later stage must not run after an earlier rejection.

## Stage 2: capture and raw-byte binding

Before parsing, the verifier proves:

- the parent source ID, snapshot ID, and Repository Capture fingerprint equal
  the attestation identity;
- the normalized repository-relative path identifies the exact ordered regular
  member in the attestation;
- raw length recomputed from the supplied bytes equals the captured length;
- `RawSourceMemberFingerprint` recomputed from those bytes equals the captured
  raw-member fingerprint; and
- the complete parent reference equals that attested member at its exact
  position.

Caller-supplied length or raw fingerprint is never trusted. Foreign capture,
foreign member, changed bytes, changed length, changed raw fingerprint,
unattested membership, and coordinated relabeling are rejected.

## Stage 3: parsing

Evidence Governance owns the single authoritative
`ScenarioAuthorityExactJsonParserV1` primitive. Extractor delegates its V1
compatibility parsing to that primitive; Validation Core does not own parsing.
The parser applies `scenario-authority-json-parser-v1` only to the
capture-verified bytes using this exact algorithm:

1. Decode with strict UTF-8. Malformed and unmappable byte sequences are
   rejected; replacement-character recovery is prohibited.
2. Parse with strict duplicate-object-member detection. A duplicate member name
   at any object depth is rejected.
3. Preserve JSON integers and decimals with arbitrary precision. Narrowing to a
   bounded integer or binary floating point is prohibited.
4. Require exactly one complete JSON value. JSON whitespace after the value is
   permitted; any trailing non-whitespace token or content is rejected.

V1 finite parse causes retain their existing detail:

- `INVALID_UTF8`;
- `MALFORMED_JSON`;
- `DUPLICATE_JSON_MEMBER`;
- `TRAILING_JSON_CONTENT`.

The exact exception mapping is:

| Authoritative parser condition | Finite cause |
| --- | --- |
| strict UTF-8 decoder reports malformed or unmappable input | `INVALID_UTF8` |
| strict duplicate-field exception before a complete value is read | `DUPLICATE_JSON_MEMBER` |
| no first token, a `JsonParseException` before the first complete value (other than the duplicate case), or any other `IOException` | `MALFORMED_JSON` |
| another token exists after the first value, or a `JsonParseException` occurs after the first complete value | `TRAILING_JSON_CONTENT` |

The selected Jackson adapter may recognize the approved strict-duplicate
exception's exact `Duplicate field '` message prefix when no typed distinction
is exposed. It must not classify unrelated messages as duplicates.

These four internal codes map exhaustively, with no default, to public
`PARSE_REJECTED`. Parse rejection produces no admission proof, normalized
Manifest, occurrence, or Scenario enumeration. Any exception not identified by
the table, including an unclassified `RuntimeException`, propagates as a
processing failure. There is no broad parser fallback.

## Stage 4: authority attribution

Evidence Governance owns the single authoritative `ScenarioAuthorityAttributorV1`
primitive. Extractor delegates V1 compatibility attribution to that primitive
and does not reimplement its grammar or finite classification.

Authority is derived from the exact parsed, capture-bound value using
`scenario-authority-attribution-v1` in this order:

1. The root must be an object; otherwise emit `NON_OBJECT_ROOT` at `""`.
2. `/authority` must exist; otherwise emit `MISSING_AUTHORITY` at `/authority`.
3. `/authority` must be textual; otherwise emit `AUTHORITY_NOT_STRING` at
   `/authority`.
4. Its Java `String.length()` (UTF-16 code units, matching the approved V1
   implementation) must be from 1 through 200 inclusive and it must
   match exactly `^[A-Za-z0-9][A-Za-z0-9._:/-]*$`; otherwise emit
   `INVALID_AUTHORITY` at `/authority`.

No trimming, case conversion, Unicode normalization, repair, inference,
fallback, or defaulting is permitted. The exact decoded string is retained.

The existing finite causes `NON_OBJECT_ROOT`, `MISSING_AUTHORITY`,
`AUTHORITY_NOT_STRING`, `INVALID_AUTHORITY`, and `PARSE_UNATTRIBUTABLE` map
exhaustively, with no default, to `AUTHORITY_ATTRIBUTION_UNAVAILABLE`. The last cause is reachable
only through the preceding parse rejection and cannot override its precedence.
Attribution rejection produces no proof. Unknown or unclassified attribution
failures propagate.

## Stage 5: schema validation

`qa-model-validation-core` validates the exact parsed source against the pinned
schema bytes whose SHA-256 is
`7fdac4321c125cadec4afe734460920afc3dfcaf6ea8bb1f49cee43b49a901f1`.
The primitive verifies the schema resource identity before use and returns a
deterministic validation result. Canonical structural diagnostics remain
governed by `scenario-authority-schema-diagnostic-mapping-v1`.

Evidence Governance interprets an empty canonical structural-diagnostic
collection as structural admission. Any structural diagnostic produces
`STRUCTURAL_SCHEMA_REJECTED` and no proof. A schema resource identity mismatch,
unsupported diagnostic mapping, schema-engine failure, or other unexpected
validation failure is a processing/integrity failure and propagates; it is not a
structural rejection.

The validation-core boundary owns no claimed authority, occurrence identity,
normalized Scenario model, or `VerifiedAdmittedManifestV1`.

## Stage 6: authoritative normalization

Only an admitted parsed source is normalized. Evidence Governance derives an
immutable `EvidenceGovernanceNormalizedManifestV1` containing exactly:

```text
EvidenceGovernanceNormalizedManifestV1 {
  format
  schemaVersion
  scenarioIdentityScheme
  claimedAuthority
  authoredScenarios[] {
    scenarioKey
    exact title
    authored GIVEN texts[]
    authored WHEN texts[]
    authored THEN texts[]
    normalized Steps[] { phase, zero-based ordinal, exact authored text }
    unresolved HTTP Operation reference
    unresolved Business Rule references[] in authored order
  }
}
```

Every identity/profile/version required by downstream Scenario occurrence
composition is fixed by this contract and retained in the derived representation
or its derived identities. Qualification, resolution, duplicate classification,
semantic fingerprints, and provenance are excluded.

Evidence Governance owns the single authoritative
`ScenarioAuthorityNormalizerV1`. Normalization is a pure deterministic function
of the admitted parsed source and pinned contracts. A caller cannot supply or
replace the normalized value. Extractor compatibility records are projections
from this result: they may change Java representation but preserve every value,
identity, and order and do not independently normalize parsed JSON.

### Exact Manifest and Scenario mapping

All source fields below are required and non-null under the pinned schema.
Authored strings are copied exactly after JSON decoding: no trimming, case
conversion, Unicode normalization, or whitespace rewriting occurs.

| Exact JSON source | Target | Preservation or transformation |
| --- | --- | --- |
| `/format` | Manifest `format` | exact `qaip-scenario-authority-manifest-v1` |
| `/schemaVersion` | Manifest `schemaVersion` | exact `1.0` |
| `/authority` | Manifest `claimedAuthority` | exact safely attributed string |
| `/scenarioIdentityScheme` | Manifest `scenarioIdentityScheme` | exact `qaip-scenario-identity-v1` |
| `/scenarios` | `authoredScenarios` | source array order; no sorting or deduplication |
| `/scenarios/i/scenarioKey` | Scenario key and claimed-identity key | exact string |
| `/scenarios/i/title` | `exactTitle` | exact string |
| `/scenarios/i/given` | `authoredGiven` | exact strings in array order |
| `/scenarios/i/when` | `authoredWhen` | exact strings in array order |
| `/scenarios/i/then` | `authoredThen` | exact strings in array order |
| `/scenarios/i/operationRef` | unresolved Operation reference | mapping below |
| `/scenarios/i/ruleRefs` | unresolved Business Rule references | authored order |

For `/scenarios/i`, `i` is its zero-based source position and its structural
path is exactly `/scenarios/<i>`. Claimed Scenario identity consists of derived
Manifest authority, exact `scenarioKey`, and `qaip-scenario-identity-v1`.
Duplicate claimed identities remain distinct occurrence identities.

### Exact Step mapping

`given`, `when`, and `then` are required, non-null, and contain at least one
nonblank string. Absence, null, or an empty phase array is structurally rejected
before normalization.

GIVEN Steps preserve `/given` order, use phase `GIVEN`, and have ordinals from
zero within GIVEN. WHEN preserves `/when` order and resets its phase ordinal to
zero. THEN preserves `/then` order and resets its phase ordinal to zero. A
combined Step projection is ordered GIVEN, then WHEN, then THEN, with numeric
ordinal inside each phase. Every Step is owned by the exact claimed Scenario
identity and retains its exact decoded array string as authored text.

### Exact unresolved Operation-reference mapping

`operationRef` and each child are required and non-null:

| Source or constant | Derived value |
| --- | --- |
| owning Scenario | exact claimed Scenario identity |
| constant | role `OPERATION_REF` |
| constant | datum identity `qaip-scenario-operation-reference-datum-identity-v1` |
| `/scenarios/i/operationRef/identityScheme` | target profile `qaip-http-operation-reference-v1` |
| `/scenarios/i/operationRef/method` | exact admitted method |
| `/scenarios/i/operationRef/path` | exact admitted path |
| constant | semantic version `scenario-authority-http-operation-reference-semantic-c14n-v1` |

Method and path undergo no case conversion, path normalization, resolution,
trimming, or whitespace rewriting.

### Exact unresolved Business Rule-reference mapping

For `/scenarios/i/ruleRefs/j`, in source array order, derive:

| Source or constant | Derived value |
| --- | --- |
| `j` | zero-based authored position |
| owning Scenario | exact claimed Scenario identity |
| `/authority` | exact referenced authority |
| `/stableRuleKey` | exact stable Rule key |
| `/identityScheme` | exact Business Rule identity scheme |
| constant | datum identity `qaip-scenario-business-rule-reference-datum-identity-v1` |
| constant | semantic version `scenario-authority-business-rule-reference-semantic-c14n-v1` |

Rule references are not sorted, deduplicated, trimmed, normalized, qualified, or
resolved. `ruleRefs` may be empty but not absent or null; its schema items are
unique. All strings are copied exactly.

### Numeric, null, absent, and empty semantics

No JSON numeric value can enter a V1 normalized Manifest, Scenario, Step,
Operation-reference, or Business Rule-reference semantic field. Scenario
indexes, Step ordinals, and Rule positions are derived non-negative integers,
not JSON numeric inputs. Arbitrary-precision number retention remains required
parser compatibility behavior for documents subsequently rejected by schema.

`scenarios` and each `ruleRefs` may be empty and map to exact empty immutable
collections. No projected property is optional. Explicit null, an absent
required property, or an empty string is schema rejected. Phase arrays also
reject empty arrays. No absent, null, empty, or default state is conflated.

### Fixed normalized identifiers

The normalizer attaches only these frozen identifiers:

| Role | Identifier |
| --- | --- |
| parser | `scenario-authority-json-parser-v1` |
| attribution | `scenario-authority-attribution-v1` |
| schema | `qaip-scenario-authority-manifest-schema-v1` |
| schema diagnostic mapping | `scenario-authority-schema-diagnostic-mapping-v1` |
| source normalization | `scenario-authority-source-normalization-v1` |
| Manifest format | `qaip-scenario-authority-manifest-v1` |
| Manifest schema | `1.0` |
| Manifest occurrence identity | `qaip-scenario-manifest-occurrence-identity-v1` |
| Scenario declaration identity | `qaip-scenario-declaration-occurrence-identity-v1` |
| Scenario identity | `qaip-scenario-identity-v1` |
| Scenario semantic | `scenario-authority-scenario-semantic-c14n-v1` |
| Step identity | `qaip-scenario-step-identity-v1` |
| Step semantic | `scenario-authority-step-semantic-c14n-v1` |
| Operation role | `OPERATION_REF` |
| Operation target profile | `qaip-http-operation-reference-v1` |
| Operation datum identity | `qaip-scenario-operation-reference-datum-identity-v1` |
| Operation semantic | `scenario-authority-http-operation-reference-semantic-c14n-v1` |
| Business Rule identity | exact admitted `/identityScheme`; semantic composition supports `qaip-business-rule-identity-v1` |
| Business Rule datum identity | `qaip-scenario-business-rule-reference-datum-identity-v1` |
| Business Rule semantic | `scenario-authority-business-rule-reference-semantic-c14n-v1` |

The schema admits a syntactically valid non-fixed Business Rule identity scheme.
Normalization preserves it; the later Scenario semantic support decision does
not authorize replacement during normalization.

### Normalization integrity disposition

After pinned-schema admission, every required normalization field, type,
nullability rule, and array relationship is closed by construction. There is no
active finite `NORMALIZATION_INTEGRITY_FAILURE` outcome in V1. A contradiction
between the admitted validation result and parsed tree, unexpected missing or
type-invalid field, constructor failure, `NullPointerException`,
`IllegalArgumentException`, or other unclassified failure is a programming or
processing failure and propagates. No generic normalization catch is permitted.

## Stage 7: occurrence and Scenario derivation

The Manifest occurrence identity is derived from the verified parent source ID,
snapshot ID, Repository Capture fingerprint, normalized member path, and
`qaip-scenario-manifest-occurrence-identity-v1`.

The authored Scenario sequence is derived only from the authoritative normalized
Manifest. For each authored array position `i`, Evidence Governance derives:

- zero-based index `i`;
- structural path `/scenarios/<i>`;
- a `ScenarioDeclarationOccurrenceIdentity` bound to the Manifest occurrence;
- claimed Scenario identity from derived Manifest authority and exact
  `scenarioKey`;
- the complete `NormalizedScenarioOccurrenceInputV1`, including Step and
  unresolved reference inputs.

Authored array order is preserved. Duplicate declarations remain separate
occurrences with different indexes and structural paths. No independent caller
Scenario collection is accepted.

Manifest and Scenario occurrence components are direct deterministic functions
of the verified parent and normalized array position. There is no caller
occurrence candidate to compare and no active finite
`OCCURRENCE_DERIVATION_INTEGRITY_FAILURE` outcome in V1. Parent/capture mismatch
is decided at Stage 2 and unsupported identity contracts at Stage 1. Any
contradiction or construction failure at Stage 7 is therefore an unexpected
programming or processing failure and propagates.

## Verified proof

`VerifiedAdmittedManifestV1` is immutable and binds:

- its proof version;
- exact `RepositoryCaptureAttestation`;
- exact attested parent member and position;
- recomputed raw-member length and fingerprint;
- pinned parser, attribution, schema, diagnostic, normalization, identity, and
  profile identifiers;
- derived claimed authority;
- derived Manifest occurrence identity;
- authoritative `EvidenceGovernanceNormalizedManifestV1`;
- complete derived `NormalizedScenarioOccurrenceInputV1` sequence.

Its constructor and internal factory are non-public in the Evidence Governance
owning module. No public constructor or factory accepts the derived fields.
Only `verifyAdmittedManifestV1(request)` may mint it after all stages succeed.

Java object-instance identity is not evidential custody. Custody is the proven
deterministic relationship:

```text
exact capture-bound raw bytes
+ pinned processing contracts
    -> deterministic normalized Manifest
    -> deterministic Manifest occurrence
    -> deterministic authored Scenario enumeration
```

An equal reconstructed Extractor record has no authority and is neither accepted
nor consulted.

## Typed internal and public rejection taxonomy

The authoritative parser has exactly the internal codes `INVALID_UTF8`,
`MALFORMED_JSON`, `DUPLICATE_JSON_MEMBER`, and `TRAILING_JSON_CONTENT`. The
attributor has exactly `NON_OBJECT_ROOT`, `MISSING_AUTHORITY`,
`AUTHORITY_NOT_STRING`, `INVALID_AUTHORITY`, and precedence-only
`PARSE_UNATTRIBUTABLE`. Request compatibility, capture binding, and structural
schema interpretation use the corresponding public typed rejection below.
Normalization and occurrence derivation have no finite internal rejection codes
in V1 because their proposed finite conditions are closed by construction.

Mappings from internal finite enums are exhaustive switches with no default.
Adding an internal code must fail compilation or conformance until its mapping
is explicitly approved. An exception not carrying an enumerated typed rejection
propagates.

`AdmittedManifestVerificationRejectionV1.Code` contains exactly:

| Code | Stage | Classification |
| --- | --- | --- |
| `UNSUPPORTED_VERIFICATION_CONTRACT` | 1 | expected finite compatibility rejection |
| `CAPTURE_MEMBER_RAW_BYTES_MISMATCH` | 2 | expected finite verification rejection |
| `PARSE_REJECTED` | 3 | expected finite source rejection |
| `AUTHORITY_ATTRIBUTION_UNAVAILABLE` | 4 | expected finite source rejection |
| `STRUCTURAL_SCHEMA_REJECTED` | 5 | expected finite source rejection |

The active public V1 taxonomy contains five codes. The draft names
`NORMALIZATION_INTEGRITY_FAILURE` and
`OCCURRENCE_DERIVATION_INTEGRITY_FAILURE` have
`CLOSED_BY_CONSTRUCTION` disposition and are not active public enum values.

The mapping from existing finite parse and attribution causes is exhaustive and
has no default. There is no `OTHER`, `UNKNOWN`, generic, exception-derived, or
caller-defined code. Future source-processing codes require an explicit contract
decision before compilation/conformance can succeed.

All five codes produce no `VerifiedAdmittedManifestV1`, no Manifest semantic
outcome, and no semantic fingerprint. Detailed rejected-member and structural
diagnostic evidence continues through existing Attributed Member Outcome and
Repository Derivation Report processing; this verifier does not duplicate it.

## Anti-substitution rules

The verifier rejects:

- Capture A attestation paired with Capture B member bytes;
- a correct member reference paired with changed bytes or length;
- a changed or foreign raw-member fingerprint;
- a foreign or unattested member;
- a real captured member paired with fabricated authority, normalized Manifest,
  occurrence identity, or Scenario collection;
- coordinated relabeling of capture, member, Manifest, and authority;
- occurrence or authored-index substitution; and
- an equal reconstructed Extractor normalized value presented as custody.

The last five values are not request authority fields. Their rejection is
structural: the verifier derives them internally from the capture-bound bytes.

## Extractor compatibility and migration

Extractor may temporarily retain its existing processing and normalized records
as compatibility projections. For authoritative Manifest semantic processing it
must pass only capture attestation, member reference, raw bytes, and fixed
candidate contract identifiers to Evidence Governance and consume the verified
proof.

Extractor must not pass `NormalizedManifestDatum` or structural-admission state
as authority, independently mint equivalent evidence, or use a split package.
Validation results for the same valid and rejected fixtures must remain
compatible while the shared validation primitive becomes the single normative
schema engine.

## Fingerprint compatibility

This contract creates no fingerprint domain. `VerifiedAdmittedManifestV1` is a
proof-bearing attestation, not a persistent canonical fingerprint.

The contract changes no bytes, identifiers, domains, encodings, digests,
canonical sequences, or golden values for Repository Capture, Scenario semantic,
Manifest semantic, Scenario Identity Group, Attributed Member Outcome, or
Semantic Provenance fingerprints.

## Normative conformance corpus

Successful vectors must cover:

- one valid captured and admitted Manifest;
- multiple authored Scenarios; and
- duplicate authored declarations retained as distinct occurrences.

Capture vectors must cover changed bytes, changed length, wrong raw fingerprint,
foreign member, foreign capture, unattested membership, and coordinated
relabeling.

Parsing and attribution vectors must cover valid JSON, malformed UTF-8,
duplicate names, arbitrary-precision integers and decimals, trailing tokens,
every finite parse code, non-object root, missing authority, non-text authority,
empty and over-200-character authority, grammar violations, valid authority,
exact structural locations, exhaustive mappings, and unexpected parser and
attributor failures that propagate.

Schema vectors must cover structural rejection, schema content-identity
mismatch, canonical diagnostic compatibility, and unexpected validator failure.

Normalization vectors must cover deterministic repeated normalization, every
exact field mapping, Step phase order and ordinal resets, Operation-reference
preservation, Business Rule position/order, multiple and duplicate Scenarios,
absent/null/empty behavior, all fixed identifiers, absence of caller normalized
input, and an equal reconstructed Extractor value being ignored as authority.

Derivation vectors must freeze Manifest occurrence identity, Scenario indexes,
structural paths, authored ordering, duplicate occurrence preservation, and
occurrence substitution rejection.

Architecture vectors must prove no split Evidence Governance implementation
package, no reverse module dependency, no public internal proof factory, and that
only the Evidence Governance verifier mints `VerifiedAdmittedManifestV1`. They
also prove Evidence Governance owns the only normative parser, attributor, and
normalizer and that Extractor delegates or projects without duplicate logic.

Compatibility vectors must compare Extractor and Evidence Governance validation
and normalization results over the same approved valid and rejected fixtures and
must preserve every existing semantic fingerprint golden.

## Follow-up implementation sequence

After approval of this contract:

1. expose the pinned validation primitive and schema resource through
   `qa-model-validation-core`;
2. migrate the exact-byte parser and safe attributor to Evidence Governance and
   make Extractor delegate its compatibility APIs;
3. implement the Evidence Governance request, authoritative normalizer,
   verifier, five-code finite rejection, and internally minted proof, then make
   Extractor normalization records projections of that result;
4. migrate Extractor to the public verifier and remove the split-package bridge;
5. align Manifest Semantic Outcome V1 construction with the new verifier without
   changing its outcome or fingerprint semantics; and
6. rerun the complete Manifest Semantic Outcome capability gate.

No ADR amendment follows from this contract unless review identifies a semantic
contradiction. Attributed Member Outcome, Logical V2, provenance, qualification,
resolution, Runtime, Coverage, and Explorer remain outside this contract.
