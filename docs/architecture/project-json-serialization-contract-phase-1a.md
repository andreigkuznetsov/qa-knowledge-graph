# Project JSON Serialization Contracts — Phase 1A

## 1. Scope

This document freezes independent external JSON subcontracts used by the future
`qaip-project-v1` root schema. It does not define that root schema or runtime
import behavior.

## 2. Approved serialization decisions

The future root uses `projectContractVersion` with the exact value
`qaip-project-v1` and members `baseModel`, `declaredChanges`,
`evidenceManifest`, `subject`, and `analysisContext`. Declared changes use the
declaration-level `artifactCategory` discriminator. Identity resolution uses
the nested `resolution.status` discriminator with `RESOLVED` and `UNRESOLVED`.

## 3. Domain-to-JSON traceability

| Contract | Domain source | Normative external mapping |
|---|---|---|
| analysis context | `SliceAnalysisContext` and `ImpactEvidenceVersions` | `qualificationVersion`, `influenceVersion`, and `algorithmVersion`, all exact strings |
| subject candidate | `SubjectArtifactRef` | one closed object containing non-blank `localArtifactId` |
| evidence manifest | `FrozenEvidenceManifest` | its nine constructor inputs, mapped by the exact approved member names |
| snapshot | `EvidenceSnapshotRef` | `sourceId`, `snapshotId`, `contentFingerprint` |
| identity assertion | `ArtifactIdentityAssertion` | its evidence fields plus the approved nested resolution union |
| resolved identity | `ResolvedIdentity(CanonicalIdentity)` | `status: RESOLVED` plus `canonicalIdentity` using the domain lexical constraint |
| unresolved identity | `UnresolvedIdentity` | `status: UNRESOLVED` plus open non-blank `reasonCode` |
| relationship evidence | `RelationshipEvidence` | exact constructor inputs and authoritative `RelationshipType` names |
| provenance | `ProvenanceRef` | exact constructor inputs |

Declared changes map `category` to the normative `artifactCategory`, `identity`
to the normative scalar `canonicalIdentity`, `kind` to `changeKind`, and the
domain's canonical-model `schemaVersion` directly. `canonicalIdentity` uses the
same exact lexical contract as the manifest. `beforeState` and `afterState`
reuse the canonical model's external node or relationship definition selected
by `artifactCategory`; no state-level discriminator is added.

## 4. Exact schema identities

- `https://example.local/schemas/impact-evidence-manifest-v1.schema.json`
- `https://example.local/schemas/impact-analysis-context-v1.schema.json`
- `https://example.local/schemas/qaip-project-subject-candidate-v1.schema.json`
- `https://example.local/schemas/qaip-declared-changes-v1.schema.json`

All use JSON Schema Draft 2020-12. No root project schema exists in this phase.

## 5. Null and absence policy

Required members are present and non-null. Optional members use absence only.
Explicit `null`, null array elements, defaults, aliases, fallback, and implicit
version selection are forbidden.

## 6. Ordering and duplicate policy

Declared-change order will be semantically significant. Manifest collection
order is retained in the parsed representation for diagnostics. The domain
constructor later canonicalizes assertions by `assertionId`, relationships by
`datumId`, and provenance by `provenanceId`. Schemas do not sort, deduplicate,
or use `uniqueItems`; duplicate authority remains with domain validation.

## 7. Frozen candidate semantics

The evidence schema describes immutable candidate claims. It does not assert
validation, qualification, trust, or impact and contains no computed or repair
fields. Manifest fingerprints are supplied claims, not importer computations.

## 8. Local `$ref` resolution policy

Public schema identities are stable absolute `https://example.local/schemas/`
URIs. NetworkNT 1.5.8 `SchemaLoaders.Builder.schemas` supplies an exact-map
loader whose values are classpath resource contents. The map contains only the
five approved schema IDs. Unknown IDs fail before factory resolution; there is
no prefix mapping, filesystem guessing, HTTP loader, or network fallback.
Tests load the declared-change schema by its absolute ID and resolve its nested
canonical node and relationship references through the same loader.

## 9. `baseModel` compatibility conclusion

The future `baseModel` member can directly reference
`https://example.local/schemas/qa-model-v0.1.schema.json`. That Draft 2020-12
schema governs the complete canonical root, closes its objects, and exposes
canonical node and relationship definitions without requiring a wrapper,
normalization, sorting, or deduplication.

## 10. Fixtures and tests

Valid fixtures cover context, subject, minimal manifest, resolved identity,
unresolved identity, non-canonical external assertion order, and representative
manifest content. Focused tests mutate defensive copies to cover missing,
altered, null, and unknown members, invalid resolution branches, missing
collections, null elements, and structurally admissible duplicates.

## 11. Remaining blockers

None. Root composition remains a separately authorized task.

## 12. Root schema readiness

**READY.** Every independent subcontract is frozen and tested, canonical state
definitions resolve through deterministic local absolute references, and the
root schema remains intentionally absent pending separate authorization.
