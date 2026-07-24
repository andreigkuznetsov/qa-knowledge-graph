# Import Contract Review — Pre-Implementation Gate

**Status:** Independent architectural review  
**Scope:** `project.json` → import → domain verification → `AnalyzeProject`  
**Required verdict:** **APPROVED AFTER MINOR CONTRACT ADJUSTMENTS**

## 1. Executive Summary

The import boundary can be implemented without violating the existing domain
contracts, but only if “imported” is not treated as “verified.” The approved
architecture must preserve two distinct handoffs:

```text
project.json bytes
  → ProjectImporter
  → ParsedProject (untrusted, immutable data)
  → existing domain validation and change-verification pipeline
  → VerifiedChangeSet + manifest candidate + subject + context
  → ImpactEvidenceAnalyzer (manifest validation, identity/relationship use,
     qualification, traversal, impact)
```

The recommended mapping is **Option B: JSON → Import DTO → Domain Verification
→ Domain Objects**. It is the only option that makes the trust transition
explicit and preserves domain ownership.

The minor contract adjustment is terminological and structural: the importer
must return `ParsedProject`, not `ImportedProject` described or typed as
containing “verified domain inputs.” It may mechanically rehydrate immutable
domain-shaped candidate values where their constructors explicitly represent
untrusted declarations, but it must never return `VerifiedChangeSet`, a
qualified relationship, an impact proof, or any value whose type asserts a
completed domain decision.

This conclusion is grounded in existing invariants:

- `DeclaredChange` is documented as an **untrusted declaration**.
- `DeclaredChangeSet` is documented as an input-order-preserving aggregate of
  **untrusted change declarations**.
- `ArtifactState` construction explicitly does **not** imply full-model
  validation or change verification.
- `VerifiedChangeSet` has a package-private constructor documented as owned by
  `FinalChangeSetVerifier`.
- the canonical-change pipeline requires exact retained evidence through all
  stages; independently rebuilding a later-stage value is invalid.
- `ImpactEvidenceAnalyzer` invokes its manifest validator before compatibility,
  identity use, relationship qualification, traversal, and impact projection.

No domain contract redesign is required. The import API must expose data, not a
claim of trust.

## 2. Import Boundary

### Where import begins

Import begins after an input adapter has successfully acquired an immutable
sequence of project bytes and a non-semantic source label. File existence,
permissions, size policy, and read failures belong to the input adapter, not to
the importer.

```text
ProjectSource(sourceName, bytes)
                  ↑
          importer begins here
```

The importer owns only:

1. decoding the supported serialization format;
2. detecting malformed JSON;
3. binding JSON members to a lossless, immutable import representation;
4. reporting missing/wrong JSON shapes needed for binding;
5. preserving declared values and input order exactly.

“Binding” is structural decoding. It is not JSON Schema validation. For
example, the importer may report that `changes` is not an array because it
cannot bind the document; it may not decide that a schema-valid array violates
a domain cardinality or semantic rule.

### Where import ends

Import ends when it returns either:

- `ProjectParsed(ParsedProject)`, containing immutable but untrusted data; or
- `ProjectParseFailed(ProjectParseFailure)`, containing syntax/binding
  diagnostics.

It ends **before** JSON Schema validation, semantic validation, change
verification, manifest validation, identity decisions, relationship
qualification, or impact analysis.

### What the importer may construct

The importer may construct:

- import-owned DTOs containing strings, booleans, numbers, lists, and
  immutable JSON subtrees;
- `ProjectSource` and parse diagnostics;
- `ParsedProject` and its nested import records;
- domain value objects explicitly defined as untrusted/candidate values, but
  only as a mechanical convenience after successful binding:
  `CanonicalQaModelVersion`, `CanonicalIdentity`, `NodeArtifactState`,
  `RelationshipArtifactState`, `DeclaredChange`, and `DeclaredChangeSet`.

The preferred contract keeps raw strings and JSON subtrees in import DTOs and
lets the domain-verification adapter construct the candidate domain values.
This keeps constructor rejection distinct from malformed JSON and ensures that
canonical identifier/version decisions remain attributed to the domain.

The importer may also bind a **manifest candidate representation**. If the
existing public evidence records must be instantiated to call the analyzer,
the adapter may mechanically rehydrate `FrozenEvidenceManifest`,
`EvidenceSnapshotRef`, `ArtifactIdentityAssertion`, `RelationshipEvidence`,
`ProvenanceRef`, `ResolvedIdentity`, and `UnresolvedIdentity`. Those instances
remain untrusted manifest claims until accepted by the analyzer's manifest
validation. Immutability does not elevate their trust level.

### What the importer is forbidden to construct

The importer must never construct, synthesize, or claim to return:

- `VerifiedChangeSet`;
- any intermediate success evidence from the canonical-change pipeline as a
  substitute for invoking its owning stage;
- schema-valid or semantically-valid evidence;
- a validated or accepted manifest wrapper unless produced by a domain-owned
  public validator result;
- `QualifiedRelationship`, `RejectedRelationship`, `QualifiedPathStep`,
  `DirectChangeProof`, `RelationshipPathProof`, `ImpactConclusion`, or any
  impact result;
- a resolved canonical identity derived by matching, lookup, normalization, or
  inference;
- repaired, normalized, defaulted, reordered, or deduplicated input.

Several forbidden evidence/proof constructors are already inaccessible. That
visibility is an invariant to preserve, not a reason to use reflection,
serialization bypasses, package co-location, or copied constructors.

### Objects that may exist only after domain verification

- `VerifiedChangeSet`: only after
  `FinalChangeSetVerifier.verify(CompleteProposedRootValidationResult)` returns
  its verified variant.
- Complete schema/semantic success evidence: only after
  `CompleteProposedRootValidator` invokes the authoritative validation core.
- Qualified/rejected relationship outcomes: only inside/after the existing
  impact analysis qualification step.
- Impact proofs and conclusions: only after `ImpactEvidenceAnalyzer` completes.

The current impact contract does not expose a separately typed
`ValidatedFrozenEvidenceManifest`. Consequently no upstream layer may claim
that a manifest is validated before calling `ImpactEvidenceAnalyzer`.

### Objects safe before verification

The following can exist before verification when labelled as untrusted:

- JSON bytes/tree and import DTOs;
- canonical base-model JSON snapshot supplied by the project;
- change declaration fields and before/after artifact snapshots;
- candidate version and canonical-identity value objects whose constructors
  enforce only local shape;
- `DeclaredChange` and `DeclaredChangeSet`;
- manifest assertion, provenance, snapshot, relationship, and fingerprint
  claims;
- `SubjectArtifactRef` and `SliceAnalysisContext` candidates.

Their constructors provide local immutability or lexical well-formedness only.
They do not establish cross-object consistency, truth, qualification, or impact.

## 3. Trust Boundary

### Trust levels

| Object | Classification on creation | Trust transition | Classification after transition |
|---|---|---|---|
| project bytes | raw external data | successful JSON parse only | still untrusted parsed content |
| JSON tree/tokens | parsed representation | import binding | still untrusted `ParsedProject` |
| import DTO fields | parsed representation | relevant domain constructor/stage | candidate or validated according to that stage's explicit result |
| base QA-model snapshot | raw/parsed external data | canonical-change extraction, indexing, reconstruction, schema and semantic stages | retained validation evidence, only within successful exact-instance chain |
| candidate version string | parsed representation | `CanonicalQaModelVersion` plus owning version checks | locally valid value; supported only after domain result says so |
| candidate identity string | parsed representation | `CanonicalIdentity` constructor | locally well-formed immutable value, not resolved identity evidence |
| `ArtifactState` | immutable untrusted domain value | intrinsic/base/aggregate/complete verification chain | retained verified evidence only as part of `VerifiedChangeSet` |
| `DeclaredChange` | untrusted domain representation | full canonical-change pipeline | retained declaration inside `VerifiedChangeSet` |
| `DeclaredChangeSet` | immutable aggregate of untrusted declarations | full canonical-change pipeline | retained set inside `VerifiedChangeSet` |
| `VerifiedChangeSet` | cannot be external/import-created | `FinalChangeSetVerifier` verified variant | verified, immutable trusted domain object |
| manifest DTO | parsed representation | mechanical construction of manifest candidate | immutable but untrusted domain-shaped input |
| `FrozenEvidenceManifest` instance | immutable candidate/claim | internal manifest validation in `ImpactEvidenceAnalyzer` | accepted for that analysis invocation; no separate public validated type |
| identity assertion with `ResolvedIdentity` | external frozen-evidence claim | manifest integrity/compatibility checks and analyzer semantics | usable evidence within the analyzer; importer never performs resolution |
| relationship evidence | external frozen-evidence claim | analyzer qualification | `QualifiedRelationship` or `RejectedRelationship` |
| `SubjectArtifactRef` | immutable request value | analyzer subject-declaration/type checks | accepted subject for that analysis or declared failure |
| `SliceAnalysisContext` | immutable request value | manifest/analyzer supported-version checks | accepted execution semantics for that invocation |
| `ImpactEvidenceRequest` | assembled request, not proof of validity | `ImpactEvidenceAnalyzer.analyze` | completed or failed domain result |
| `ImpactConclusion` / proof | verified analysis output | constructed only by analyzer-owned flow | immutable trusted domain output |

### Exact trust transitions

There is no single global “trusted” bit. Each authority establishes a narrower
fact:

1. Parser: the bytes are syntactically JSON and structurally bindable.
2. Local domain value constructor: one value satisfies its lexical/shape
   invariant.
3. Canonical-change stages: declarations, base truth, materialization,
   aggregate transition, reconstructed root, schema, semantics, and final
   evidence are coherent and exact-instance-bound.
4. `FinalChangeSetVerifier`: the change set is verified.
5. `ImpactEvidenceAnalyzer` manifest validation: the manifest is structurally,
   versionally, and integrity consistent for this request.
6. Analyzer qualification: each relationship is qualified or rejected.
7. Analyzer completion: the conclusion/proof is authoritative for the supplied
   request and context.

The importer owns only transition 1. It may invoke local constructors through a
separate mapping step, but that does not grant transitions 3–7.

## 4. Mapping Recommendation

### Decision: Option B

```text
JSON bytes
  ↓ parse and lossless bind
Import DTO (`ParsedProject`)
  ↓ mechanical candidate mapping
Existing Domain Verification
  ↓ only successful domain result variants
VerifiedChangeSet + accepted request values
  ↓
ImpactEvidenceAnalyzer
```

### Why Option A is rejected

Direct `JSON → Domain Objects` obscures the difference between a constructor
accepting local shape and a domain authority verifying a claim. It creates four
specific hazards:

- Jackson or another binder could attempt to materialize a trusted result type;
- constructor failures would be misreported as malformed JSON rather than
  domain/version/declaration failures;
- annotations or permissive binding could introduce defaults, aliases, enum
  coercion, or unknown-field loss outside domain policy;
- a `FrozenEvidenceManifest` object could be incorrectly described as already
  validated merely because it is immutable.

Most importantly, `VerifiedChangeSet` cannot legitimately be deserialized. Its
package-private constructor and exact retained-evidence invariants intentionally
require the complete domain pipeline.

### Why no Option C is needed

A generic anti-corruption layer, schema registry, versioned importer factory,
or workflow abstraction adds no protection beyond the explicit DTO and domain
result boundaries. The project has one JSON format and one supported semantic
path. Option B is both the smallest and the most explicit architecture.

### Mapping rules

- Preserve list order and every supplied scalar exactly.
- Do not trim strings, canonicalize case, normalize identifiers, sort,
  deduplicate, interpolate, or fill missing values.
- Do not silently ignore unknown project fields. Preserve them for the
  authoritative project JSON Schema decision, or retain the original root JSON
  alongside typed DTOs so validation sees the exact document.
- Do not coerce strings to numbers/booleans or accept enum aliases.
- A missing optional field remains explicitly absent. A missing required field
  is a binding failure only when no DTO can represent it; otherwise it remains
  present-as-missing for authoritative schema/domain validation.
- Candidate mapping exceptions from domain value constructors are returned as
  domain-mapping/validation outcomes, never repaired and never called syntax
  errors.

## 5. Import Contract

### Input

```java
public record ProjectSource(String sourceName, byte[] content) {}
```

Contract requirements:

- `sourceName` is diagnostic metadata only and must not affect semantics;
- `content` is defensively copied on construction and access;
- character encoding is fixed by the project serialization contract (UTF-8 for
  JSON); the importer does not guess encodings;
- the input adapter, not the importer, owns file I/O.

### Entry point and result

```java
public final class JsonProjectImporter {
    public ProjectImportResult parse(ProjectSource source);
}

public sealed interface ProjectImportResult
        permits ProjectParsed, ProjectParseFailed {}

public record ProjectParsed(ParsedProject project)
        implements ProjectImportResult {}

public record ProjectParseFailed(ProjectParseFailure failure)
        implements ProjectImportResult {}
```

`parse`, not `importVerified`, is the deliberate verb. The result does not
contain `VerifiedChangeSet`.

### Parsed representation

```java
public record ParsedProject(
        JsonNode originalDocument,
        String declaredProjectVersion,
        ParsedBaseModel baseModel,
        List<ParsedChange> changes,
        ParsedManifest manifest,
        String subjectLocalArtifactId,
        ParsedAnalysisContext context) {}
```

Nested DTOs are import-owned records with only lossless scalar/list/JSON fields.
`originalDocument` is a defensive deep copy and ensures the authoritative JSON
Schema validator receives exactly the parsed document, including unknown
members. Lists are input-order-preserving immutable copies. No DTO method
computes identity, fingerprint, qualification, validity, or defaults.

The exact JSON member names are a separate project-file serialization contract;
this review does not invent them. That contract must be frozen before coding
and must contain all inputs needed by the existing change pipeline: canonical
base model, declared changes with before/after snapshots, frozen evidence
manifest, one subject, and explicit supported context/version values.

### Failure

```java
public record ProjectParseFailure(
        String code,
        String message,
        Optional<String> path,
        Optional<Integer> line,
        Optional<Integer> column) {}
```

Allowed codes are serialization-owned and small, for example:
`MALFORMED_JSON`, `INVALID_UTF8`, and `UNBINDABLE_PROJECT_SHAPE`. They describe
why a parsed representation could not be created. They must not include
`SCHEMA_INVALID`, `SEMANTICALLY_INVALID`, `UNSUPPORTED_VERSION`,
`INVALID_MANIFEST`, or any change/impact classification.

### Ownership and immutability

The import module owns `ProjectSource`, DTOs, parse result variants, and parse
diagnostics. The domain owns all candidate domain types and verification
results. The application owns the fixed call sequence between them.

Every import value is immutable:

- byte arrays are copied in and out;
- JSON nodes are deep-copied in and out;
- lists are `List.copyOf` values with null members rejected;
- no DTO exposes a mutable mapper, stream, input handle, or lazy parser;
- parse results retain no filesystem resource.

No importer interface is required in MVP unless the already-approved
application constructor needs it. A single concrete `JsonProjectImporter` is
sufficient; future formats do not justify a framework now.

## 6. Ownership Matrix

| Condition/decision | Owner | Importer's permitted action |
|---|---|---|
| file cannot be read | input/CLI adapter | none; importer is not called |
| malformed JSON / invalid UTF-8 | JSON importer | return parse failure |
| JSON cannot be represented by lossless import DTO | JSON importer | return binding failure without repair/default |
| project JSON Schema mismatch | existing JSON Schema validator | preserve exact JSON and relay its result |
| canonical QA-model schema mismatch | existing validation core through canonical-change pipeline | no independent check |
| semantic mismatch | existing semantic validation | no independent check |
| locally malformed canonical identity | `CanonicalIdentity` domain value | relay constructor/domain-mapping failure; do not normalize |
| unsupported canonical/project/domain version | owning domain validator/verification stage | retain declared version; do not choose fallback |
| invalid intrinsic change declaration | `IntrinsicChangeValidator` | no rule or inference |
| base mismatch/ambiguity | `BaseChangeVerifier` | no lookup reinterpretation |
| invalid materialization/aggregate/root | owning canonical-change stage | no reconstruction substitute |
| invalid complete schema/semantics | `CompleteProposedRootValidator` | no duplicated validation |
| invalid/finally inconsistent change set | `FinalChangeSetVerifier` | never produce `VerifiedChangeSet` |
| manifest structural/version/integrity mismatch | manifest validation inside `ImpactEvidenceAnalyzer` | bind the claim exactly; do not pre-accept or repair |
| change/manifest incompatibility | `ImpactEvidenceAnalyzer` | no cross-object compatibility rule |
| identity resolved/unresolved meaning | existing evidence-generation/identity domain semantics; consumed by analyzer | rehydrate the frozen assertion only; never perform resolution |
| relationship qualified/rejected | `ImpactEvidenceAnalyzer` | preserve relationship evidence only |
| subject compatibility | `ImpactEvidenceAnalyzer` | preserve the selected local ID only |
| impact classification/proof | `ImpactEvidenceAnalyzer` | no classification or traversal |

### Error projection rule

The application may copy an owner's code/message/path into the unified report,
but neither importer nor application may rename a domain failure into an import
failure. Unsupported version, manifest mismatch, and invalid change set remain
domain validation/analysis results even though the triggering text originated
in `project.json`.

## 7. Risk Analysis

| Risk | Bypassed invariant | Required control |
|---|---|---|
| deserializing `VerifiedChangeSet` | only `FinalChangeSetVerifier` constructs final exact-instance evidence | do not register creators/reflection; import result type cannot contain it |
| constructing late-stage success evidence from JSON | each canonical-change success retains the exact preceding evidence instance | invoke all production entry points in documented order; branch on every result variant |
| treating `ArtifactState` as validated | its contract says construction does not imply full validation/change verification | label it candidate/untrusted until retained in `VerifiedChangeSet` |
| calling a lexical `CanonicalIdentity` “resolved” | identifier syntax is not evidence resolution | keep external identity assertions as frozen claims; importer performs no lookup/matching |
| binding directly to `ResolvedIdentity` and trusting it | resolution claim could be mistaken for importer-owned truth | document candidate status; pass through existing manifest/analyzer authority only |
| prequalifying relationship evidence | qualification requires snapshot, endpoint, provenance, type, direction, and version rules | importer creates no qualification result and no traversal edge |
| computing or correcting manifest fingerprint | manifest integrity/canonicalization is domain-owned | preserve supplied fingerprints; analyzer validates them |
| accepting manifest because record constructor succeeded | constructor/local invariants are weaker than manifest validation | only analyzer result establishes acceptance for an invocation |
| independently validating manifest before analyzer | duplicate rules could drift; current validator is internal to analyzer | do not add importer/application manifest semantics |
| defaulting missing versions/context | changes replay semantics and may turn unsupported input into accepted input | require explicit values or let owning domain reject absence |
| importer dispatches to closest supported version | silently migrates semantics | dispatch only on exact serialization version after domain compatibility decision; MVP has one parser |
| ignoring unknown JSON members during DTO binding | could hide authoritative schema violations | retain original document for schema validation; never validate a reconstructed subset |
| Jackson enum coercion/case-insensitivity | introduces aliases not owned by domain | bind exact strings and let exact mapping/domain constructor decide |
| trimming or normalizing identifiers | bypasses canonical identity constraints and changes fingerprints | preserve exact text |
| sorting/deduplicating lists | breaks input-order and diagnostic/proof determinism | immutable input-order copies only |
| continuing after a failed domain stage | later stages would consume evidence they are not allowed to receive | exhaustive result-variant branching; stop verification immediately |
| rebuilding equivalent evidence after a stage | violates provenance-sensitive exact-instance binding | pass the exact returned object instance to the next stage |
| mapper class placed in domain package | could access package-private trusted constructors | mapper stays in import/application package; no reflective access |
| “helpful” repair of before/after states | changes declared change semantics | reject/relay; never synthesize state or change kind |

### Most important residual risk

The manifest format contains identity-resolution assertions as evidence. The
importer may deserialize the assertion but cannot independently prove the
resolution. This is safe only under the existing slice contract: the frozen
manifest, its fingerprints/provenance, and analyzer validation are the
authoritative evidence input. Calling the imported assertion “identity resolved
by the importer” would violate the boundary. No new resolver should be added to
the importer.

## 8. Required Preconditions

The following must be true before implementation starts:

1. **Freeze the project JSON serialization contract.** It must name one exact
   project format version and contain, without inferred defaults, the canonical
   base model, declared changes, manifest, one subject, and analysis context.
2. **Keep the original JSON tree.** Authoritative JSON Schema validation must
   see the external document, not a DTO-to-JSON reconstruction.
3. **Define the application/domain mapping handoff.** It must return candidate
   inputs and stage result variants; it must not be named or typed as a verified
   importer.
4. **Use the complete canonical-change pipeline in its documented order:**
   `IntrinsicChangeValidator` → `BaseChangeVerifier` →
   `ProposedModelMaterializer` → `AggregateTransitionValidator` →
   `ProposedCanonicalRootReconstructor` →
   `CompleteProposedRootValidator` → `FinalChangeSetVerifier`.
5. **Preserve exact instances.** Each successful stage output is passed directly
   to the next stage. No serialization round-trip or independent reconstruction
   is allowed between stages.
6. **Do not require a prevalidated manifest type.** None is publicly exposed.
   Pass the immutable manifest candidate in `ImpactEvidenceRequest`; treat
   `ImpactEvidenceAnalyzer` failure as authoritative.
7. **Separate parse diagnostics from domain diagnostics.** The report keeps each
   owner's original category, code, path, and ordering.
8. **Make enum/version mapping exact and exhaustive.** Unknown strings are
   retained for owning version/schema failure where representable, or reported
   as binding failure when the DTO cannot represent them; never map to a default.
9. **Confirm module availability.** The impact-evidence classes must be consumed
   from a normal source/binary module dependency with stable public contracts,
   not inferred from local build output during implementation.

No precondition asks for a new domain rule or future-format abstraction.

## 9. Approved Import Architecture

### Approved call flow

```text
CLI/input adapter
  read bytes
  │
  ▼
JsonProjectImporter.parse(ProjectSource)
  ├─ ProjectParseFailed ───────────────► parse report
  └─ ProjectParsed(ParsedProject)
       │
       ▼
application-owned straight-line orchestration
  authoritative project/schema validation
  mechanical candidate mapping
  complete existing canonical-change pipeline
  ├─ any domain failure ───────────────► validation report
  └─ VerifiedChangeSet
       + manifest candidate
       + SubjectArtifactRef candidate
       + SliceAnalysisContext candidate
       │
       ▼
ImpactEvidenceAnalyzer.analyze(ImpactEvidenceRequest)
  ├─ ImpactEvidenceFailed ─────────────► analysis-failed report
  └─ ImpactEvidenceCompleted ──────────► analyzed report
```

### Approved responsibility split

- **Input adapter:** filesystem only.
- **JSON importer:** syntax and lossless binding only.
- **Application orchestration:** stage order and result branching only.
- **Canonical-change domain:** all change invariants and creation of
  `VerifiedChangeSet`.
- **Impact-evidence domain:** manifest validation, compatibility, identity
  evidence use, relationship qualification, traversal, and conclusion.
- **Report projection:** lossless copying of owner results only.

### Required adjustment to the approved application document

Where the application design says import produces `ImportedProject`, implement
that type as `ParsedProject` (or rename it equivalently) and state explicitly
that it contains untrusted candidate data. Where it says validation yields
“verified domain inputs,” interpret that phrase narrowly:

- the change input is a genuine `VerifiedChangeSet` returned by its finalizer;
- the manifest remains an immutable candidate validated inside the analyzer;
- subject/context remain request candidates until the analyzer accepts them.

This adjustment changes no workflow, use case, error policy, domain contract, or
report contract. It prevents an over-broad trust claim at the import handoff.

## 10. Implementation Readiness Verdict

### APPROVED AFTER MINOR CONTRACT ADJUSTMENTS

Implementation is approved after the import result is explicitly defined as an
immutable **untrusted parsed representation**, not a verified project or a bag
of trusted domain objects.

The adjustment is required by existing invariants, not future flexibility:

- `VerifiedChangeSet` construction belongs exclusively to
  `FinalChangeSetVerifier` after the exact-instance canonical-change chain.
- manifest validation belongs inside `ImpactEvidenceAnalyzer` and has no public
  prevalidated-manifest result type.
- identity resolution assertions and relationships in the manifest are evidence
  claims; importer construction cannot make them resolved or qualified facts.

Once those labels and result types are corrected, Option B provides a safe,
minimal boundary. It requires no production-domain changes and permits
`AnalyzeProject` to receive the exact verified change object plus unchanged
manifest/subject/context candidates required by the stable analyzer contract.

Any implementation that returns `VerifiedChangeSet` directly from the importer,
omits a canonical-change stage, reconstructs evidence between stages, validates
a DTO reconstruction instead of the original JSON, prequalifies relationships,
or applies defaults is **not** covered by this approval.
