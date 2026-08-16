# ADR-010: Qualified business-rule identity and reconciliation

- Status: Accepted
- Date: 2026-08-16
- Decision owners: QAIP architecture

## Context

QAIP can receive business-rule knowledge from heterogeneous sources. Current
Story Input declares documented rules with source-local rule codes, while
repository analysis creates canonical `BUSINESS_RULE` nodes from supported
Bean Validation constraints. Future sources may include BA/SA specifications,
Scenario Authority manifests, OpenAPI descriptions, implementation
constraints, and verification evidence.

The Canonical Model gives every `BUSINESS_RULE` a canonical node ID and permits
source references, but structural validity does not establish which source
owns a rule, whether two source objects identify the same rule, or how
conflicting assertions remain visible. Current repository-generated
`BR-VALIDATION-<hash>` IDs include implementation and source-location facts;
current Story Input canonical `BR-n` IDs are allocation-order identities.
Neither is a universal cross-source reference contract.

ADR-006 requires qualified source namespaces, immutable snapshots, provenance,
and explicit artifact-identity assertions. ADR-008 consumes known canonical
rules without deciding their source authority. ADR-009 requires Scenario
Authority to use authoritative stable rule references but deliberately leaves
their identity scheme unresolved. This ADR supplies that missing rule-identity
and reconciliation decision.

The decision must preserve useful repository-only analysis. It cannot assume
that an independent BA/SA artifact, OpenAPI document, or other specification
always exists. Conversely, implementation constraints cannot silently become
the universal owner of business truth merely because they are extractable.

## Decision drivers

- Stable, human-authorable rule references for Scenario Authority.
- Repository-only operation without synthetic external specifications.
- Explicit authority, provenance, ambiguity, and conflict semantics.
- Deterministic cross-source resolution without heuristic identity inference.
- Compatibility with the existing Canonical Model and downstream engines.
- Refactoring behavior that separates enduring artifact identity from content
  and source location.
- Incremental migration from Story Input codes and Bean Validation hashes.

## Decision

QAIP adopts a **federated qualified-authority model** for business-rule
identity. No source category is the universal authority for every
`BUSINESS_RULE`. An accepted source authority owns source-local rule identities
under a versioned source profile. Qualified identity assertions resolve those
identities to canonical `BUSINESS_RULE` identities.

The canonical `BUSINESS_RULE` remains the resolution target used by the
Canonical Model and downstream capabilities. This decision does not change the
Canonical Ontology.

### Authority-qualified reference

The source-facing reference is conceptually:

```text
AuthorityQualifiedBusinessRuleReference =
    authority
  + stableRuleKey
  + identityScheme/version
```

All three fields are semantic:

- `authority` identifies one logical source authority and its identity
  namespace;
- `stableRuleKey` identifies one rule in that namespace; and
- `identityScheme/version` defines normalization, comparison, and compatibility
  rules for the reference.

The tuple, not any display rendering or digest of it, is the auditable source
identity. A deterministic compact ID may be derived for storage, but it does
not replace the tuple.

### Source authority namespace

An authority namespace identifies a logical rule-identifying source, not a
file, URL, repository checkout, branch, parser invocation, or mutable snapshot.
It must not be reused for another logical authority. Its declaration is bound
to a source contract and the ADR-006 source identity, capability, trust-policy,
and integrity requirements.

Authority names are exact and namespace-qualified. Identical `stableRuleKey`
values under different authorities do not imply identical rules. Moving a
source artifact or changing its transport locator does not change authority
when the logical source remains the same.

### Source profiles and rule-authoring capability

Every accepted source uses a versioned source profile. The profile declares
whether it may:

- originate business-rule identities;
- assert identity correspondence to existing rules;
- provide corroborating rule evidence;
- reference rules without authoring them; or
- provide verification evidence only.

A capability declaration is not a completeness or precedence claim. A source
that may author rules does not thereby own all rules in a domain. A scenario
manifest that references a rule does not own that rule unless its accepted
profile separately grants rule-authoring capability. Tests and verification
observations do not author rules merely by exercising or checking matching
behavior.

No universal preference is assigned to BA/SA, Scenario Authority, OpenAPI, or
repository sources. Applicability and trust policy may qualify an assertion,
but source-category labels alone never select an identity winner.

### Source-local and canonical identity

An authority-qualified reference is a source-local artifact identity. A
canonical node ID is an identity in the Canonical QA namespace. They are
different identity domains.

An explicit, provenance-retained identity assertion relates a source-local
reference to zero, one, or multiple canonical candidates. Canonical
relationships such as `GOVERNED_BY` and `COVERS` use canonical IDs only after
the relevant source identities resolve uniquely.

The canonical node may retain multiple compatible source references. Such
references improve explanation but do not replace ADR-006 snapshot, datum,
identity-resolution, and provenance evidence for audit-grade or cross-source
claims.

### Identity assertion and resolution states

Every assertion retains its authority-qualified reference, source snapshot,
source-local datum identity, content fingerprint, source profile and identity
scheme versions, provenance, resolution evidence, and categorical outcome.

The required outcomes are:

- **RESOLVED:** exactly one canonical `BUSINESS_RULE` is justified. The
  assertion retains that target and the exact mapping evidence.
- **UNRESOLVED:** no canonical target is justified. The assertion retains a
  stable reason; absence of a candidate is not evidence that the rule does not
  exist.
- **AMBIGUOUS:** two or more distinct canonical candidates remain viable and no
  winner is selected. Candidates are retained in deterministic order.
- **REJECTED:** the assertion cannot participate because its contract,
  authority, integrity, semantics, or provenance fails qualification.

Conflicting identity assertions are preserved as distinct qualified evidence.
They are not overwritten or collapsed into a preferred assertion. For example,
two sources resolving apparently related local rules to incompatible canonical
targets remain a conflict until an accepted policy and additional explicit
evidence resolve it.

Conflicting rule content is also distinct from identity conflict. Multiple
sources may resolve to the same canonical rule while asserting incompatible
parameters, applicability, status, or meaning. That content conflict remains
provenance-distinct and must not silently split or merge identity.

### Origination

An accepted rule-authoring source may originate a canonical `BUSINESS_RULE`
when its authority-qualified reference is valid and no existing canonical
target has been explicitly established. Origination deterministically creates
or selects one canonical identity according to the source profile and retains
the originating identity assertion and provenance.

Repository sources may have rule-authoring capability. Therefore a supported
repository constraint can originate a rule in a repository-only evidence
snapshot. Repository-only projects do not require a synthetic BA/SA or OpenAPI
artifact.

Origination is scoped authority, not universal truth. If another authority is
introduced later, its local rule is not automatically the same rule. It must be
resolved explicitly.

### Reconciliation and corroboration

Reconciliation occurs only through an explicit identity assertion or a
separately accepted deterministic identity-resolution profile with equivalent
provenance obligations. A resolved assertion may map a later source-local rule
to a canonical rule previously originated by another source.

After unique resolution, compatible observations from additional sources are
corroborating evidence for the canonical rule. Each observation retains its
own authority, snapshot, content, and provenance. Corroboration does not
increase authority merely by repetition, and duplicate evidence cannot be
counted as independent support.

Matching content may be reported as a diagnostic or candidate-generation aid
for human review. It is never resolution evidence by itself.

### Duplicate handling

Within one authority snapshot, one normalized authority-qualified reference
may identify only one source-local rule object unless the source profile
defines deterministic, semantically compatible composition.

- Exact repeated evidence is deterministically de-duplicated or rejected with
  a duplicate diagnostic and cannot increase evidential strength.
- Reuse of one local identity with different content in the same snapshot is an
  integrity or duplicate-identity failure.
- Multiple incompatible declarations under one stable key are ambiguous or
  rejected; declaration order never selects a winner.
- Distinct keys in one or multiple authorities remain distinct until explicit
  reconciliation establishes otherwise.

### Precedence and non-precedence

There is no global source-type precedence. In particular:

- BA/SA does not universally override repository or OpenAPI evidence;
- OpenAPI does not universally define business truth;
- repository implementation does not universally define business truth; and
- majority agreement does not determine identity or content truth.

A later accepted context-specific trust or precedence policy may qualify which
assertions are usable for a bounded decision. It must be explicit, versioned,
provenance-retained, and conservative under conflicts. It may not erase losing
evidence or mutate source identities. This ADR does not define such a policy.

### Snapshot and provenance requirements

Every authoritative assertion and observation satisfies ADR-006. At minimum it
retains:

- stable source and authority declarations;
- immutable source snapshot identity and content fingerprint;
- source-local datum identity;
- source profile, parser/normalization, canonicalization, and identity-scheme
  versions;
- stable origin locator plus integrity binding;
- semantic content fingerprint;
- exact identity-resolution evidence and outcome; and
- provenance lineage for captured, normalized, or derived facts.

Cross-source reconciliation is evaluated only within one accepted composite
evidence snapshot that explicitly binds its constituent snapshots and their
compatibility. Live mutable sources are not queried during resolution or
downstream interpretation.

### Refactoring and evolution

Ordinary file movement, line changes, formatting, annotation ordering, and
unrelated edits do not change an authority-qualified reference. Locations and
content fingerprints change as provenance.

A rule-content modification retains identity when the owning authority states
that the same rule continues under the same stable key. Examples may include a
threshold, message, validation group, wording, or implementation-constraint
change. The new snapshot records changed content and may expose cross-source
drift or conflict.

A replacement receives a new stable key when the authority declares a new
rule rather than continuation. If both canonical rules exist, the existing
`BUSINESS_RULE --SUPERSEDES--> BUSINESS_RULE` relationship may represent an
explicit, qualified supersession fact. Identity is never changed into
replacement solely because content differs.

Property, type, endpoint, or implementation renames preserve identity only
when the authority retains the stable key or supplies an explicit migration
assertion. QAIP does not infer continuity from similar old and new content.

## Prohibited identity inference

None of the following, alone or in combination, may establish that two rule
objects have the same identity:

- rule name, description, message, or other text similarity;
- field, property, parameter, class, method, or type name;
- equal threshold, regex, enum, schema keyword, annotation attribute, or other
  value;
- membership in or governance of the same operation;
- common scenario, test, check, implementation, file, module, or repository;
- graph proximity, reachability, or shared neighbors;
- matching status, rule type, tags, or display metadata; or
- identical content fingerprints from different authority-local objects.

These facts may support diagnostics and review workflows only. No confidence
score, ranking, first match, or hidden preferred candidate converts them into
identity evidence.

## Compatibility and migration

### Canonical Model

The existing canonical `BUSINESS_RULE` node remains the resolution target and
canonical relationships retain their current meanings. No Canonical Ontology
change is required by this decision.

Unresolved, ambiguous, rejected, and conflicting source assertions remain in
the qualified evidence/provenance layer rather than being represented by
simplistic canonical merging. Only uniquely resolved facts may form canonical
relationships consumed as authoritative facts.

### Story Input rule codes

Existing `rule.code` values may become `stableRuleKey` values only under an
explicit Story Input authority, source profile, scope rule, and identity-scheme
version. The current operation-local code map and sequential `BR-n` allocation
are not by themselves cross-source identity guarantees.

Migration retains old canonical IDs for their historical snapshots and emits
explicit identity assertions from qualified Story Input references to current
canonical targets. Duplicate codes are no longer resolved by order; they are
diagnosed according to authority scope and duplicate rules.

### Bean Validation hashes

Existing `BR-VALIDATION-<hash>` IDs remain valid canonical identities in
historical snapshots and may remain deterministic declaration/content
identifiers during migration. Because their inputs include attributes and
source coordinates, they are not future stable rule keys.

A repository source profile supplies or resolves an authority-qualified stable
key. Migration evidence maps that reference to the applicable canonical rule.
No migration may infer correspondence merely from a field, annotation type,
attribute value, message, operation, or matching content.

### ADR-009 Scenario Authority

The “authoritative stable rule key” required by ADR-009 means an
`AuthorityQualifiedBusinessRuleReference` under this ADR. A Scenario Authority
source may reference such a rule without owning it. `COVERS` is emitted only
after the reference resolves to exactly one canonical `BUSINESS_RULE` in the
accepted snapshot. Unresolved or ambiguous references retain evidence and emit
no fabricated edge.

This ADR does not define a Scenario Authority schema.

### Bean Validation extraction

Bean Validation remains capable of repository-only rule origination under an
accepted repository rule-authoring profile. When a constraint resolves to an
independently originated canonical rule, it contributes provenance-retained
implementation or corroborating evidence instead of automatically creating a
second rule.

Parameter changes, moves, and formatting changes update observation content or
provenance without necessarily replacing rule identity. Extraction mechanics
and source syntax are follow-up design work and are not implemented here.

### Future BA/SA and OpenAPI sources

BA/SA and OpenAPI adapters use their own declared authorities and versioned
profiles. Either may originate rules when granted that capability or may assert
correspondence to existing rules. Neither receives universal precedence.

OpenAPI schema equality and BA/SA wording similarity are not identity evidence.
Disagreement with repository observations remains explicit conflict or drift,
not an automatic overwrite.

### ADR-008 Rule Verification

ADR-008 continues to evaluate one canonical rule in one accepted evidence
snapshot. Identity reconciliation precedes proof formation. A scenario witness
cannot verify an unresolved or ambiguously referenced rule. Relevant qualified
identity or content conflicts remain visible and may yield `AMBIGUOUS` or
`UNKNOWN` according to the proof contract; Runtime does not repair identity.

### Coverage Engine

Coverage continues to count canonical `BUSINESS_RULE` nodes and exact `COVERS`
relationships. It does not reconcile rules, merge duplicates, or infer
coverage. Upstream resolution prevents compatible multi-source observations
from becoming duplicate canonical rules. Unresolved source assertions do not
silently enlarge or reduce the canonical rule denominator, and Coverage makes
no completeness claim about omitted unresolved rules.

### Impact Evidence

ADR-006 identity assertions are the integration boundary from source-local
references to canonical rule identities. Impact Evidence consumes qualified
resolution evidence and preserves unresolved or rejected evidence; it does not
own source precedence or business-rule merging. Its implementation must evolve
separately if the full conceptual `AMBIGUOUS` and conflict states are not yet
represented in a deployed slice.

### Runtime and Explorer

This ADR requires no Runtime or Explorer modification. Runtime remains the
owner of engineering interpretation over qualified canonical facts, and
Explorer remains presentation-only.

## Consequences

### Positive

- Scenario references survive ordinary repository and formatting changes.
- Repository-only projects remain useful.
- BA/SA, OpenAPI, repository, and future sources can coexist without implicit
  universal authority.
- Duplicate, unresolved, ambiguous, and conflicting evidence remains
  auditable.
- Canonical consumers retain one exact identity namespace and unchanged graph
  vocabulary.
- Implementation drift can be distinguished from artifact replacement.

### Negative

- Every source adapter needs an authority declaration, profile, stable-key
  contract, and provenance-aware identity assertions.
- Composite snapshots and reconciliation evidence add contract and operational
  complexity.
- Repositories without stable rule keys require explicit adoption or migration
  decisions before Scenario Authority can reference their rules durably.
- Conservative unresolved and ambiguous results will be common while source
  mappings are incomplete.

## Alternatives considered

### Repository constraints universally own rule identity

Rejected as the global model. It preserves repository-only use but couples
business identity to implementation and makes independent specification and
drift difficult to represent. Repository authority remains allowed as one
qualified source profile.

### Independently authored rules universally own identity

Rejected as mandatory. It cleanly separates specification and implementation
but assumes an independent artifact always exists and would degrade or invent
authority for repository-only projects.

### Automatic semantic or structural matching

Rejected. Text, values, fields, operations, and graph structure cannot prove
author intent or identity and would hide ambiguity and conflicts.

### One global source-type precedence order

Rejected. Authority and applicability are contextual; a universal ordering
would discard legitimate evidence and silently encode governance policy into
identity resolution.

## Unresolved questions

- What concrete authority identifier syntax and registry will be adopted?
- Which first source profiles may originate rules, and what trust policies
  qualify them?
- How are stable keys declared for Bean Validation without coupling the core
  contract to Java syntax?
- What exact compatibility rules govern identity-scheme version upgrades?
- What contract creates and validates composite evidence snapshots?
- Which component owns interactive or reviewed reconciliation assertions?
- How are content conflicts classified independently from identity conflicts?
- Which diagnostics and public serialization expose candidates, conflicts,
  legacy mappings, and provenance?
- When does a rule modification require explicit `SUPERSEDES`, and which source
  may author that relationship?
- How long must historical Story Input and `BR-VALIDATION` identity mappings be
  retained?

## Conformance requirements

An implementation conforms only if:

- every source-facing rule reference is authority-qualified and versioned;
- canonical `BUSINESS_RULE` is the unique resolved target namespace;
- repository-only rule origination remains possible under an accepted profile;
- no source category receives implicit universal authority or precedence;
- resolution is explicit, deterministic, snapshot-bound, and
  provenance-retained;
- unresolved, ambiguous, rejected, duplicate, and conflicting evidence is not
  silently discarded or canonically merged;
- matching text, fields, values, operation membership, content, or graph
  proximity never establishes identity;
- ordinary location and formatting changes do not change a stable source-local
  rule identity;
- content modification and identity replacement remain separate decisions;
- only uniquely resolved rule references form authoritative canonical
  relationships; and
- Coverage, Runtime, Explorer, and Impact Evidence do not independently
  recreate source reconciliation.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-008: Rule Verification Proof Contract](ADR-008-rule-verification-proof-contract.md)
- [ADR-009: Scenario Evidence Formation](ADR-009-scenario-evidence-formation.md)
- [Canonical QA Model schema](../../../qa-model/src/main/resources/schema/qa-model-v0.1.schema.json)
- [Relationship matrix](../../relationship-matrix.md)
- [`StoryInputToQaModelMapper.java`](../../../qa-model-extractor/src/main/java/ru/kuznetsov/qagraph/extractor/mapping/StoryInputToQaModelMapper.java)
- [`OperationEvidenceGraphAssembler.java`](../../../qa-model-extractor/src/main/java/ru/kuznetsov/qagraph/extractor/assembly/OperationEvidenceGraphAssembler.java)
- [`ArtifactIdentityAssertion.java`](../../../qa-impact-evidence-core/src/main/java/ru/kuznetsov/qaip/evidence/ArtifactIdentityAssertion.java)
