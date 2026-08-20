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
Scenario declaration derivation, and `VerifiedAdmittedManifestV1` construction.

`qa-model-extractor` owns source orchestration, custody of captured raw bytes,
mapping to the public candidate request, and compatibility projections. It does
not supply authoritative admission or normalized data to this verifier.

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

The verifier applies `scenario-authority-json-parser-v1` to only the
capture-verified bytes. V1 finite parse causes retain their existing detail:

- `INVALID_UTF8`;
- `MALFORMED_JSON`;
- `DUPLICATE_JSON_MEMBER`;
- `TRAILING_JSON_CONTENT`.

At this attestation boundary they map exhaustively to the single finite
`PARSE_REJECTED` code while the exact existing parse cause may remain
non-authoritative diagnostic evidence. Parse rejection produces no admission
proof, normalized Manifest, occurrence, or Scenario enumeration. Unexpected
parser failures propagate.

## Stage 4: authority attribution

Authority is derived from the exact parsed, capture-bound root using
`scenario-authority-attribution-v1`. The source must have an object root and an
exact, valid authority string. No partial, guessed, defaulted, or caller-supplied
authority is permitted.

The existing finite causes `NON_OBJECT_ROOT`, `MISSING_AUTHORITY`,
`AUTHORITY_NOT_STRING`, `INVALID_AUTHORITY`, and `PARSE_UNATTRIBUTABLE` map
exhaustively to `AUTHORITY_ATTRIBUTION_UNAVAILABLE`. The last cause is reachable
only through the preceding parse rejection and cannot override its precedence.
Attribution rejection produces no proof.

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

Normalization is a pure deterministic function of the admitted parsed source
and pinned contracts. A caller cannot supply or replace the normalized value.
Failure of an enumerated normalization invariant produces
`NORMALIZATION_INTEGRITY_FAILURE`; unexpected failures propagate.

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
Scenario collection is accepted. An enumerated correspondence or identity
failure produces `OCCURRENCE_DERIVATION_INTEGRITY_FAILURE`; unexpected failures
propagate.

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

## Finite rejection taxonomy

`AdmittedManifestVerificationRejectionV1.Code` contains exactly:

| Code | Stage | Classification |
| --- | --- | --- |
| `UNSUPPORTED_VERIFICATION_CONTRACT` | 1 | expected finite compatibility rejection |
| `CAPTURE_MEMBER_RAW_BYTES_MISMATCH` | 2 | expected finite verification rejection |
| `PARSE_REJECTED` | 3 | expected finite source rejection |
| `AUTHORITY_ATTRIBUTION_UNAVAILABLE` | 4 | expected finite source rejection |
| `STRUCTURAL_SCHEMA_REJECTED` | 5 | expected finite source rejection |
| `NORMALIZATION_INTEGRITY_FAILURE` | 6 | finite processing/integrity failure |
| `OCCURRENCE_DERIVATION_INTEGRITY_FAILURE` | 7 | finite processing/integrity failure |

The mapping from existing finite parse and attribution causes is exhaustive and
has no default. There is no `OTHER`, `UNKNOWN`, generic, exception-derived, or
caller-defined code. Future source-processing codes require an explicit contract
decision before compilation/conformance can succeed.

All seven codes produce no `VerifiedAdmittedManifestV1`, no Manifest semantic
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

Parsing and attribution vectors must cover malformed JSON, every existing V1
parse code, missing/invalid/unsafe authority, exhaustive cause mapping, and an
unexpected parser failure that propagates.

Schema vectors must cover structural rejection, schema content-identity
mismatch, canonical diagnostic compatibility, and unexpected validator failure.

Normalization vectors must cover deterministic repeated normalization, absence
of caller normalized input, and an equal reconstructed Extractor value being
ignored as authority.

Derivation vectors must freeze Manifest occurrence identity, Scenario indexes,
structural paths, authored ordering, duplicate occurrence preservation, and
occurrence substitution rejection.

Architecture vectors must prove no split Evidence Governance implementation
package, no reverse module dependency, no public internal proof factory, and that
only the Evidence Governance verifier mints `VerifiedAdmittedManifestV1`.

Compatibility vectors must compare Extractor and Evidence Governance validation
and normalization results over the same approved valid and rejected fixtures and
must preserve every existing semantic fingerprint golden.

## Follow-up implementation sequence

After approval of this contract:

1. expose the pinned validation primitive and schema resource through
   `qa-model-validation-core`;
2. implement the Evidence Governance request, normalization model, verifier,
   finite rejection, and internally minted proof;
3. migrate Extractor to the public verifier and remove the split-package bridge;
4. align Manifest Semantic Outcome V1 construction with the new verifier without
   changing its outcome or fingerprint semantics; and
5. rerun the complete Manifest Semantic Outcome capability gate.

No ADR amendment follows from this contract unless review identifies a semantic
contradiction. Attributed Member Outcome, Logical V2, provenance, qualification,
resolution, Runtime, Coverage, and Explorer remain outside this contract.
