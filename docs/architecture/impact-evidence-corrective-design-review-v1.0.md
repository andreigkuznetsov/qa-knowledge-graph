# Impact Evidence Corrective Design Review v1.0

## 1. Status and verdict

**Status:** accepted corrective design  
**Verdict:** **READY FOR CORRECTIVE IMPLEMENTATION**

The implementation should be corrected, not redesigned or abandoned. Its
manifest, qualification, traversal, fingerprint, and result-algebra concepts
remain valid. The correction is limited to binding accepted changes to the
supported domain, making proof/conclusion construction authoritative, retaining
subject assertion evidence, and strengthening the evaluation tests.

## 2. Scope

This review covers only:

- authoritative `ArtifactCategory.NODE` / `NodeType.BUSINESS_RULE` extraction
  from `VerifiedChangeSet` declarations;
- consistency between accepted changes and manifest assertions;
- ownership and context binding of direct proofs, path proofs, and conclusions;
- unresolved-subject audit evidence;
- the explicit `REMOVED` manifest assumption;
- an executable test-only baseline and corrective adversarial tests;
- stronger canonicalization tests.

It does not reopen ADR-005 or ADR-006, change the `AFFECTED | UNKNOWN`
vocabulary, add non-impact, broaden evidence sources, or introduce storage,
services, frameworks, confidence, freshness, or general-purpose reasoning.

## 3. Confirmed review findings

| Finding | Classification | Corrective conclusion |
|---|---|---|
| Accepted change type is not bound to `BUSINESS_RULE` | **CONFIRMED** | Current direct matching and root extraction trust manifest type claims; genuine fixtures change `CHECK` nodes. This is merge-blocking. |
| Public conclusion/proof context can be detached | **CONFIRMED** | A public direct proof and public `ImpactConclusion` constructor can combine unrelated subject/snapshot context. This is high severity. |
| Unresolved assertion evidence is omitted | **CONFIRMED** | The output retains local subject and snapshot but not assertion ID, resolution reason, or the exact assertion. |
| Baseline is executable in tests | **CONFIRMED WITH ADJUSTED SEVERITY** | The evaluation describes a test-level baseline, but no baseline implementation or comparison assertions exist. The problem is evidence strength, not production semantics. |
| Corrective test coverage is incomplete | **CONFIRMED** | Sixteen tests pass but omit realistic business-rule changes, type/category collisions, public construction attacks, and several contract boundaries. |
| Runtime validation dependency is slice-local architecture failure | **DISCUSSION ONLY** | `qa-model-validation-core` is inherited at runtime from existing `qa-model-change`; production slice code does not import it. This correction must not alter that existing boundary. |
| Canonical byte tests are sufficient | **CONFIRMED WITH ADJUSTED SEVERITY** | The canonicalizer covers required fields, but tests assert only a final hash and one mutation. This is a test-strength issue, not a demonstrated integrity defect. |
| `REMOVED` is unsupported | **NOT CONFIRMED** | It is supported when the change-independent evidence manifest retains a resolved identity assertion. That assumption must be explicit and tested. |

## 4. Corrected semantic pipeline

The production pipeline shall be:

```text
request null/shape validation
    -> manifest structural, version, reference, and integrity validation
    -> kind-specific accepted-change domain extraction
    -> manifest/change compatibility validation
    -> subject assertion lookup and resolution
    -> direct proof selection
    -> relationship qualification
    -> deterministic traversal over supported changed roots
    -> analyzer-owned conclusion projection
```

Manifest validation remains pure and must not accept `VerifiedChangeSet`.
Change-domain and manifest/change checks form a separate request-level stage.
All compatibility failures occur before a conclusion is projected; they never
become `UNKNOWN`.

### Considered pipeline options

1. **Add change checks inside `ManifestValidator`.** Rejected because it would
   couple frozen-manifest integrity to an external accepted change.
2. **Resolve the subject and perform direct matching first.** Rejected because
   incompatible evidence could produce `AFFECTED` before compatibility is
   established.
3. **Separate request-level compatibility stage after pure manifest validation.**
   Accepted because it preserves current responsibilities and is the smallest
   trustworthy ordering.

## 5. Accepted change domain contract

### Decision

The authoritative category and node type come only from the retained states in
the accepted `DeclaredChange`; a manifest assertion can confirm but cannot
define or override them.

| Change kind | Required authoritative state | Supported condition |
|---|---|---|
| `ADDED` | `afterState` | category is `NODE`, state is `NodeArtifactState`, state snapshot `type` resolves to `BUSINESS_RULE` |
| `MODIFIED` | both `beforeState` and `afterState` | both states are `NODE`/`NodeArtifactState`, retain the declaration identity, and both resolve to `BUSINESS_RULE` |
| `REMOVED` | `beforeState` | category is `NODE`, state is `NodeArtifactState`, state snapshot `type` resolves to `BUSINESS_RULE` |

`NodeArtifactState` has no public `nodeType()` accessor. The correction shall
use one fixed package-private extraction function based on its validated
snapshot `type` and `NodeType.from(...)`. It shall not introduce an interface,
strategy, registry, taxonomy, or generic type mapper.

For `MODIFIED`, transitions into or out of `BUSINESS_RULE` are outside the
slice. If either side is `BUSINESS_RULE` and the other is not, analysis fails
with `INCOMPATIBLE_CHANGE_DOMAIN`. Both-state validation is necessary because
using only the after state would silently admit an entry transition, while
using only the before state would admit an exit transition.

Relationship-category declarations never become roots or direct business-rule
proofs.

### Accepted incompatible-change policy: Policy 3 — Hybrid

- A declaration wholly unrelated to the slice is excluded: relationship
  declarations and node changes whose applicable state(s) are consistently
  non-`BUSINESS_RULE` do not become roots.
- A type transition involving `BUSINESS_RULE` fails.
- A relationship or non-business-rule declaration whose canonical identity
  collides with a manifest assertion claiming `BUSINESS_RULE` fails.
- Every supported changed business rule must have exactly one resolved
  manifest assertion with the same canonical identity and `BUSINESS_RULE`
  type. Missing or contradictory assertions fail.
- Compatible declarations continue in accepted declaration order. Unrelated
  declarations neither affect root order nor produce diagnostics.

### Rationale

A `VerifiedChangeSet` may legitimately contain mixed changes, so failing on
every unrelated declaration would make the narrow slice unusable. Ignoring a
contradiction, however, would permit manifest relabeling. The hybrid policy
preserves mixed-set use while failing only identity-relevant ambiguity.

### Rejected alternatives

- **Manifest assertion authoritative:** rejected because it caused the current
  defect and duplicates neither accepted category nor retained state truth.
- **Kind-specific after state only for `MODIFIED`:** rejected because type entry
  and exit would receive asymmetric, undocumented semantics.
- **Fail on every non-business-rule declaration:** rejected because unrelated
  changes are valid members of a mixed accepted change set.

### Production impact

Add a fixed package-private change-domain extraction/compatibility helper or
equivalent private analyzer functions. `ImpactEvidenceAnalyzer` consumes its
ordered supported-change result for direct selection and traversal roots.

### Test impact

Replace `CHECK` fixtures with genuine business-rule fixtures and add mixed,
transition, category-collision, and mismatch tests.

### Compatibility impact

Previously accepted but semantically invalid requests will fail. This is an
intentional correction before the module's first merge.

## 6. Manifest/change compatibility contract

### Decision

Compatibility uses exact `CanonicalIdentity` equality after manifest integrity
validation and supported-change extraction.

For each supported business-rule declaration:

1. exactly one manifest assertion must resolve to the declaration identity;
2. that assertion must declare `NodeType.BUSINESS_RULE`;
3. its source and snapshot must already satisfy manifest validation;
4. duplicate canonical mappings to the same changed identity are a mismatch,
   even if local IDs differ;
5. the accepted category must be `NODE` and its authoritative state type must
   be `BUSINESS_RULE`.

For every manifest assertion claiming a changed identity:

- agreement with the applicable accepted node state is required;
- collision with a relationship declaration or consistently non-business-rule
  node declaration is a mismatch.

Every supported root requires a manifest assertion because traversal and
subject comparison use the source-to-canonical mapping. Direct detection still
occurs only after subject assertion lookup; the manifest cannot be bypassed for
a removed or directly changed subject.

Compatibility accumulates all mismatches and returns diagnostics ordered by
declaration index, then diagnostic code, then canonical/local identity. It does
not mutate or reverify the accepted change.

### Options and rationale

- **Validate only the requested subject:** smaller, but leaves root relabeling
  possible and makes output depend on which subject is asked about. Rejected.
- **Require agreement for supported roots and all colliding assertions:**
  accepted; it closes both direct and traversal paths without requiring the
  manifest to represent unrelated changes.

### Production impact

Request-level compatibility validation is added between `ManifestValidator`
and subject resolution. Pure fingerprint validation remains unchanged.

### Test impact

Test missing changed assertion, duplicate canonical mappings, type mismatch,
and relationship-category collision independently.

### Compatibility impact

No valid designed input changes meaning. Inconsistent provisional inputs now
fail instead of producing `AFFECTED` or `UNKNOWN`.

## 7. Proof ownership model

### Decision

Proofs remain public readable domain types but their construction is
package-private and analyzer-owned. No public proof factory is added.

#### Direct proof

`DirectChangeProof` shall become a public final immutable class rather than a
publicly constructible record. Its package-private constructor receives only:

- the exact `VerifiedChangeSet` instance;
- declaration index;
- the already validated resolved subject assertion, or the equivalent internal
  subject binding.

Identity, category, change kind, and supported node type are derived from the
accepted declaration/domain extraction. Caller-supplied `nodeType` is removed.
Construction verifies index range, exact declaration identity, `NODE`, and the
kind-specific `BUSINESS_RULE` state contract. It does not re-run
`FinalChangeSetVerifier`.

#### Relationship path proof

`RelationshipPathProof` retains:

- changed root identity;
- proved subject identity;
- one `EvidenceSnapshotRef`;
- ordered non-empty `QualifiedPathStep` values.

Its package-private constructor verifies:

- non-empty, contiguous path;
- first propagation source equals changed root;
- final target equals proved subject;
- every step's relationship source/snapshot equals the proof snapshot;
- no mixed snapshot.

Only `QualifiedRelationship` can be converted internally to a
`QualifiedPathStep`; package-private construction plus constructor checks
provide both ownership and invariant enforcement.

### Rationale

Public consumers need to inspect and pattern-match proofs, not manufacture
authoritative ones. Non-public construction avoids reproducing all analyzer
context in public constructors.

### Rejected alternatives

- **Public constructors with complete validation:** rejected because a direct
  proof would need request compatibility context and a path proof would need to
  re-run qualification.
- **Generic proof factory/engine:** rejected as unnecessary platform scope.
- **Move all conclusion context into proofs:** rejected because `UNKNOWN` has no
  proof and still needs the same subject/snapshot context.

### Production impact

Change `DirectChangeProof`; strengthen `RelationshipPathProof` and possibly
`QualifiedPathStep`; keep the sealed `ImpactProof` variants unchanged.

### Test impact

Use reflection/visibility tests plus package-level negative constructor tests
to show arbitrary node types, discontinuity, wrong subject, and mixed snapshots
cannot form authoritative proofs.

### Compatibility impact

The provisional public direct-proof constructor is removed. This is acceptable
before first merge.

## 8. Conclusion ownership model

### Decision

`ImpactConclusion` becomes a public immutable readable final class with
package-private constructors/factories. `ImpactEvidenceAnalyzer` remains the
only public production entry point that creates conclusions.

The exact validated `ArtifactIdentityAssertion` is the conclusion context
anchor. The conclusion derives:

- `SubjectArtifactRef` from its local artifact ID;
- snapshot from the assertion;
- resolved canonical subject when present.

It does not accept an independently caller-selected subject or snapshot.

Package-private construction enforces:

- `AFFECTED` has one proof and no unknown reasons;
- `UNKNOWN` has no proof and at least one reason;
- direct proof identity equals the assertion's resolved canonical identity;
- path proof subject equals that resolved identity;
- path proof snapshot equals assertion snapshot;
- unresolved assertion can produce only `UNKNOWN`;
- rejected evidence remains immutable and deterministically ordered.

`ImpactEvidenceCompleted` may remain publicly constructible because callers
cannot construct an authoritative `ImpactConclusion`; making its constructor
package-private is also acceptable for a consistently analyzer-owned result.

### Options and rationale

- **Public constructor with full validation:** rejected because it would expose
  internal compatibility/qualification responsibilities.
- **Derive all context from proof:** rejected because it does not cover
  `UNKNOWN`.
- **Analyzer-owned conclusion anchored by subject assertion:** accepted as the
  smallest model shared by positive and uncertain outcomes.

### Production and test impact

Change `ImpactConclusion` and analyzer projection. Add public-construction and
cross-subject/snapshot tests.

### Compatibility impact

The public record constructor is removed. Read accessors and semantic equality
shall remain available.

## 9. Unknown evidence model

### Decision

Do not introduce a generic `UnknownEvidence` hierarchy. Retain the exact
validated `ArtifactIdentityAssertion` in every conclusion as described above.

For unresolved subject output, callers can therefore read:

- assertion ID;
- local identity;
- `UnresolvedIdentity.reasonCode`;
- assertion source/snapshot;
- assertion content and provenance references.

For `NO_QUALIFIED_IMPACT_PROOF`, the resolved subject assertion and existing
sorted `RejectedEvidenceReference` list provide the minimal audit evidence.

### Rejected alternatives

- **New sealed unknown-evidence algebra:** explicit but duplicates current
  reason and rejection structures.
- **Put subject assertion into rejected relationships:** rejected because an
  unresolved subject is not relationship rejection.

### Impact

`ImpactConclusion` gains an assertion accessor; unresolved analyzer projection
and tests are updated. This is a provisional public API correction.

## 10. `REMOVED` assumption

### Decision

Adopt **Assumption C — change-independent identity manifest**.

The frozen manifest is an evidence/identity snapshot and is not required to be
the post-change active artifact set. A removed business rule remains directly
addressable when the supplied manifest retains a resolved assertion for its
canonical identity. This requires no tombstone infrastructure.

If the requested removed subject has no assertion, the invocation remains
structurally unsupported and returns `INVALID_REQUEST / SUBJECT_NOT_DECLARED`.
If a supported removed business-rule root lacks its required assertion, request
compatibility fails with `CHANGE_MANIFEST_MISMATCH`.

### Alternatives

- **Pre-change manifest:** workable but unnecessarily constrains other evidence.
- **Tombstone assertion contract:** explicit but introduces semantics and
  infrastructure absent from current values.

### Impact

No new production type. Add documentation/Javadocs and realistic direct removed
and missing-assertion tests.

## 11. Failure taxonomy updates

### Decision

Add two stable `FailureCode` values:

- `INCOMPATIBLE_CHANGE_DOMAIN` — an accepted declaration crosses into/out of
  `BUSINESS_RULE`, or its retained kind-specific state violates the supported
  declaration domain in a way relevant to the slice;
- `CHANGE_MANIFEST_MISMATCH` — a supported change lacks a required assertion,
  assertion type/identity contradicts accepted evidence, canonical mappings are
  ambiguous, or a relationship/non-business declaration collides with a
  business-rule assertion.

Unrelated declarations excluded by the hybrid policy produce no failure or
diagnostic. Multiple relevant incompatibilities are accumulated in stable
declaration-index/code/identity order. Diagnostics include declaration index,
canonical identity, applicable state/kind, assertion/local ID when available,
and expected versus actual category/type.

### Alternatives

- **One generic invalid-request code:** too opaque for correcting engineering
  evidence.
- **One code per mismatch:** excessive taxonomy without present consumers.

### Impact

Update `FailureCode`, request compatibility output, and deterministic diagnostic
tests. Failures remain outside conclusion algebra.

## 12. Executable baseline design

### Decision

Add one package-private test helper, for example `StructuralPathBaseline`, under
the module's test sources only. It is a fixture function, not a reusable graph
abstraction.

Input is the same accepted `VerifiedChangeSet`, manifest, and subject used by
the analyzer test. It:

1. resolves local endpoint assertions only as needed to form structural edges;
2. traverses all structurally recognizable relationships without qualification
   gating;
3. returns a test-local value containing direct/path/no-path plus warning datum
   IDs/reasons;
4. never maps no-path to a production classification.

Required same-fixture comparisons:

| Scenario | Baseline assertion | Classifier assertion |
|---|---|---|
| Qualified path | path exists | `AFFECTED` with evidence-bearing path |
| No path | no path | `UNKNOWN`, not a negative conclusion |
| Unresolved subject | no path plus identity warning | `UNKNOWN` retaining assertion and reason |
| Wrong snapshot | structural path plus warning | relationship rejected; no positive proof |
| Missing provenance | structural path plus warning | relationship rejected; no positive proof |
| Direct plus path | both candidates visible, no baseline precedence | direct proof deterministically selected |

The tests explicitly assert identical fixture object inputs and demonstrate the
semantic difference rather than merely describing it.

### Rejected alternative

A second production analyzer or baseline result API is forbidden and
unnecessary.

### Impact

Add only test helper/test files and revise the evaluation document after tests
exist. No production dependency or API changes.

## 13. Corrective test matrix

| Correction | Mandatory tests |
|---|---|
| Genuine domain binding | accepted `BUSINESS_RULE` `ADDED`, `MODIFIED`, and `REMOVED` through the real verification pipeline |
| Relabeling defense | accepted `CHECK` plus manifest `BUSINESS_RULE` fails; relationship declaration with colliding ID fails |
| Modified boundaries | non-rule -> rule and rule -> non-rule both fail with `INCOMPATIBLE_CHANGE_DOMAIN`; rule -> rule succeeds |
| Hybrid policy | mixed verified set retains supported roots and ignores wholly unrelated changes |
| Root/direct determinism | multiple compatible declarations retain declaration precedence; category collisions cannot win |
| Manifest compatibility | changed identity missing assertion; type mismatch; duplicate canonical mapping; colliding local identity |
| Direct proof ownership | no public constructor/factory; arbitrary node type cannot be supplied; exact change instance/index retained |
| Conclusion ownership | cross-subject and cross-snapshot construction unavailable; affected/no-proof and unknown/proof impossible |
| Path ownership | wrong terminal subject, discontinuity, rejected step, unresolved endpoint, and mixed snapshot cannot produce proof |
| Unresolved audit | output retains exact assertion ID, local ID, reason code, and snapshot |
| Removed assumption | resolved change-independent assertion succeeds; absent subject assertion is invalid request; missing supported-root assertion is mismatch |
| Baseline | six executable comparison scenarios from section 12 |
| Canonicalization | independent golden digest/bytes for ASCII and Unicode; multibyte and delimiter-like/control characters; shuffled order remains identical |

Focused additions are preferred; existing valid tests need not be rewritten
except where their `CHECK` fixtures falsely represent business rules.

## 14. Exact implementation impact

### Production files expected to change

- `ImpactEvidenceAnalyzer.java` — corrected pipeline and supported roots;
- `DirectChangeProof.java` — analyzer-owned construction and derived fields;
- `RelationshipPathProof.java` — explicit snapshot and step checks;
- `QualifiedPathStep.java` — only if needed to strengthen qualified ownership;
- `ImpactConclusion.java` — analyzer-owned, assertion-anchored construction;
- `ImpactEvidenceCompleted.java` — optionally close constructor ownership;
- `FailureCode.java` — two compatibility failure codes;
- `AnalysisDiagnostic.java` — only if its existing fields cannot express stable
  declaration/assertion references.

One small package-private production helper may be added for kind-specific
change extraction and compatibility. It must be concrete and slice-specific.

### Tests expected to change

- `EvidenceTestFixtures.java` — genuine `BUSINESS_RULE` JSON/states;
- `ImpactEvidenceAnalyzerTest.java` — corrected direct and precedence scenarios;
- `ManifestAndContractsTest.java` — canonical byte/Unicode coverage;
- `QualificationAndTraversalTest.java` — retain existing qualification tests and
  add compatibility boundaries where appropriate.

### New test files expected

- one focused change-domain/compatibility test class;
- one public-contract invariant test class;
- one test-only structural baseline helper and comparison test, which may be
  combined when clarity is preserved.

### Documentation expected to change during implementation

- `impact-evidence-minimal-vertical-slice-evaluation.md` only after executable
  baseline evidence exists;
- concise Javadocs describing change-independent manifest semantics.

### Files explicitly not to change

- ADR-005 and ADR-006;
- the architecture challenge and original slice design;
- `qa-model-change`, `qa-model`, `VerifiedChangeSet`, `NodeArtifactState`, and
  their accepted verification pipeline;
- Gradle module/dependency declarations unless a test-only adjustment is proven
  necessary;
- any REST, Spring, persistence, deployment, or unrelated module files.

## 15. Public API compatibility

Before first merge, correctness takes precedence over provisional compatibility.

- `DirectChangeProof` remains publicly readable but loses public construction;
  caller-supplied `nodeType` is removed and type is derived.
- `RelationshipPathProof` remains publicly readable with package-private
  construction and gains explicit snapshot binding.
- `ImpactConclusion` remains publicly readable but loses its public record
  constructor; subject and snapshot are derived from the retained assertion.
- `ImpactEvidenceAnalyzer.analyze(ImpactEvidenceRequest)` remains the single
  public production creation path.
- Result, classification, reason, evidence, and accessor vocabulary remain
  recognizable; no serialization compatibility is promised or introduced.

## 16. Guardrail check

This corrective design introduces none of the forbidden architecture:

- no non-impact value or completeness proof;
- no generic proof, rule, graph, change-type, taxonomy, or result framework;
- no Evidence Graph or provenance subsystem;
- no second production analyzer or public factory;
- no Spring, REST, persistence, messaging, deployment, or source adapter;
- no multiple sources, confidence, freshness, or speculative extension points.

The only new production responsibility is fixed request-level compatibility
validation for the already supported business-rule slice.

## 17. Corrective implementation acceptance criteria

Corrective implementation is complete only when:

1. accepted category/type is derived from kind-specific retained change state;
2. only `NODE` business-rule changes become direct candidates or roots;
3. modified type entry/exit and manifest relabeling fail deterministically;
4. mixed unrelated changes remain usable under the hybrid policy;
5. every supported changed root has one agreeing resolved assertion;
6. direct/path proofs cannot be publicly manufactured;
7. conclusions cannot combine unrelated proof, subject, or snapshot;
8. unresolved output retains exact assertion audit evidence;
9. direct `ADDED`, `MODIFIED`, and `REMOVED` use genuine verified business rules;
10. the six baseline comparisons execute against identical fixtures;
11. canonicalization has independent Unicode/boundary golden coverage;
12. all corrective and existing module tests pass from `cleanTest`;
13. full repository `test` and `build` pass;
14. dependency and forbidden-scope guardrails remain unchanged;
15. `git diff --check` passes and no unrelated files change.

## 18. Final recommendation

**PROCEED WITH NARROW CORRECTIVE IMPLEMENTATION**

The defects require contract corrections, not architectural rework. The
accepted approach keeps `VerifiedChangeSet` authoritative, preserves the
minimal evidence pipeline, and makes authoritative proof/conclusion context
coherent without adding a platform abstraction.
