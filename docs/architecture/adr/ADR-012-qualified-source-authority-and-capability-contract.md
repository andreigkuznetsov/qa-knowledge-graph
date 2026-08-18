# ADR-012: Qualified Source Authority and Capability Contract

- Status: Accepted
- Date: 2026-08-18
- Decision owners: QAIP architecture

## Context

[ADR-006](ADR-006-qualified-engineering-data-and-provenance-contract.md)
requires every evidence source to have a stable identity, declared capabilities,
an authority/trust-policy reference, immutable snapshots, and replayable
provenance. It intentionally leaves the authority and capability vocabulary to
a later decision.

[ADR-008](ADR-008-rule-verification-proof-contract.md) consumes only qualified
facts when forming a rule-verification witness. [ADR-009](ADR-009-scenario-evidence-formation.md)
requires an accepted scenario-authoring authority. [ADR-010](ADR-010-qualified-business-rule-identity-and-reconciliation.md)
adopts federated, authority-qualified Business Rule identity without universal
source precedence. [ADR-011](ADR-011-scenario-authority-source-contract.md)
defines the first authored Scenario source profile but does not define how an
authority becomes accepted.

Consequently, a well-formed source can name an `authority` and still lack any
independent basis for QAIP to accept that namespace or the engineering claims
made under it. Inferring acceptance from source content, repository location,
or schema validity would allow authored data to authorize itself. A global
source-type hierarchy would also contradict the federated model and prevent
conservative handling of conflicts.

This ADR answers:

> How does QAIP determine which source authorities are accepted and which
> engineering claims they are permitted to make?

It defines the conceptual contracts and qualification outcomes. It does not
select a registry, storage format, serialization, digest, or implementation.

## Decision drivers

- Independent, explicit, and replayable authority qualification.
- Exact source and profile binding rather than descriptive inference.
- Capability-specific permission without implied completeness or precedence.
- Conservative, evidence-retaining conflict semantics.
- Repository-only operation without a mandatory external service.
- Compatibility with federated Business Rule and Scenario authorities.
- Extensibility to BA/SA, OpenAPI, and new repository evidence profiles.
- A strict distinction between ordinary qualification and processing failure.

## Decision

QAIP adopts a **frozen, policy-qualified authority declaration model**.

An engineering claim is authority-qualified only when one immutable authority
declaration in the frozen qualification context exactly matches the claim's
authority namespace, binds the claim's exact source identity and source
profile, declares the required capability, and is accepted by the referenced
trust policy. Source evidence and authority declarations are independent
inputs. Neither one can create, amend, or accept the other.

Qualification is evaluated against a single immutable context version. The
result retains the declaration, policy, source, context, and provenance
references used. No live registry, mutable repository state, current default,
or wall clock is consulted during qualification or replay.

Authority qualification governs the evidential standing of engineering
claims. It is **evidence governance**, not authentication, access control,
tenant isolation, operating-system permission, or user/security authorization.

## Authority, source, and capability model

The following concepts are distinct and must not be collapsed.

### Authority namespace

An authority namespace is a stable, exact, opaque identifier for one logical
claim-authoring authority. It scopes source-local engineering identities such
as ADR-010 Business Rule keys and ADR-011 Scenario keys.

Equality is exact equality of the complete normalized identifier under the
declared identifier syntax version. Lookup does not use prefixes, case folding,
aliases, substring matching, hierarchy, path ancestry, or a "closest" entry.
An authority namespace is not a repository, organization display name, source
category, file path, transport endpoint, or security principal.

### Source identity

`sourceId` is the stable ADR-006 identity of the logical source that supplies
the claims. It distinguishes custody and capture lineage from the authority
namespace that scopes claim identity. A source may be moved without changing
its identity when its accepted source contract permits that move. A source ID
must never be reused for another logical source.

An authority declaration binds one exact authority namespace to one exact
source ID. Possession of the namespace in authored source data does not prove
that binding.

### Source profile

`sourceProfile` is the exact, versioned semantic profile under which the source
is captured, normalized, identifies artifacts, and forms claims. It is more
specific than a descriptive source category. For example,
`qaip-scenario-authority-repository-json-v1` is a source profile; "repository"
is not.

Qualification requires an exact profile match. Compatibility between two
profiles exists only when an accepted policy explicitly states it; a qualifier
does not silently substitute a newer, older, or similar profile.

### Source contract version

`sourceContractVersion` identifies the version of the generic source contract
implemented by the declaration and source envelope. It governs the meaning and
required fields of that binding. It is distinct from the source profile
version, manifest/schema version, identity-scheme version, normalization
version, and qualification-context version.

Unsupported contract versions are processing/compatibility failures, not
ordinary rejection of an authority.

### Source capability

A source capability is a stable semantic permission to originate a defined
kind of engineering claim under a source profile. It answers what the source is
permitted to assert, not whether any particular assertion is true, resolved,
fresh, conflict-free, applicable, or complete.

The initial capability is:

```text
AUTHOR_SCENARIO_SPECIFICATION
```

It permits qualified source data to originate authored Scenario specification
claims governed by ADR-009 and ADR-011, including the Scenario datum and the
explicit operation and Business Rule references needed to form `SPECIFIED_BY`
and `COVERS` when those references independently qualify. It does not permit
the source to author Business Rules, OpenAPI operations, test verification,
`VALIDATES`, execution results, completeness, precedence, or security policy.

New capabilities require a separately accepted, versioned semantic definition
that states:

- the exact claim kinds and source profiles to which it applies;
- formation, identity, qualification, conflict, and provenance obligations;
- relationships or derived facts it may enable and those it may not enable;
- compatibility and migration rules; and
- whether any separately modeled scope or completeness assertion is allowed.

Unknown capabilities are never approximated by a known capability. Capability
names have no prefix, inheritance, implication, wildcard, or superset semantics
unless a later accepted contract explicitly defines them. Adding a capability
does not change the meaning of an existing capability. Multiple capabilities
are a set of independent grants, not evidence that the source is complete.

### Trust-policy reference

`trustPolicyRef` is an immutable reference to the accepted policy that decides
whether the declaration is usable in a particular qualification context. It
may constrain deployment, project, evidence purpose, source custody, or other
explicit dimensions. It does not silently confer additional capabilities or
source-profile compatibility.

The policy content and version are resolved and integrity-bound by the frozen
qualification context. A label that cannot be resolved to exact policy content
is insufficient.

### Immutable authority declaration

An immutable authority declaration is independently governed evidence binding
one authority namespace, logical source, source profile, source-contract
version, capability set, and trust policy. Published semantic content cannot be
mutated. A change creates a new declaration identity and fingerprint; it does
not retroactively alter prior qualifications.

The declaration is necessary but not alone sufficient: the frozen context must
accept it, the source and profile must match, the capability must be granted,
and all source datum, snapshot, integrity, identity, and provenance obligations
must independently succeed.

### Frozen qualification context

A frozen qualification context is the complete immutable input that determines
which authority declarations and trust policies are considered for one
qualification run. It binds exact content rather than mutable names.

The context is distinct from an ADR-006 evidence snapshot, but an accepted
composite evidence snapshot binds the exact qualification context used for its
authority outcomes. Replaying that composite snapshot uses the same context;
changing the context creates a new qualification run and new outcomes even if
source bytes are unchanged.

## Authority declaration contract

Conceptually, every authority declaration contains:

```text
AuthorityDeclaration {
  declarationId
  authority
  sourceId
  sourceProfile
  sourceContractVersion
  capabilities
  trustPolicyRef
  declarationFingerprint
  provenanceRef
}
```

The fields have these obligations:

- `declarationId` is a stable identifier for this published declaration. It
  cannot be reused with different semantic content.
- `authority` is one exact authority namespace.
- `sourceId` is one exact ADR-006 logical source identity.
- `sourceProfile` is one exact versioned source profile.
- `sourceContractVersion` is one supported generic contract version.
- `capabilities` is a non-duplicated, deterministically ordered semantic set of
  explicitly granted capabilities. Absence means no grant.
- `trustPolicyRef` resolves within the frozen context to one exact,
  integrity-bound policy version.
- `declarationFingerprint` binds all semantic fields using an identified
  canonicalization and digest version.
- `provenanceRef` resolves to immutable provenance identifying who or what
  governance process issued the declaration, its custody/origin, and its
  integrity lineage.

Descriptive names, owners, repository URLs, comments, and operational locators
may be retained as metadata but have no authority semantics unless a later
contract explicitly makes a field semantic.

The authority namespace may also appear in a manifest such as ADR-011, but the
manifest occurrence is only a claimed identity used for lookup. It cannot be
the authority declaration, supply its trust policy, add capabilities, or
establish its own acceptance. Manifest authority is necessary for an
authority-qualified authored claim but insufficient to authorize it.

## Qualification-context contract

The frozen qualification context binds, at minimum:

- a stable `qualificationContextId` and explicit context-contract version;
- a content fingerprint and its canonicalization/digest versions;
- the complete deterministic set of authority declaration references and
  fingerprints considered by the run;
- the complete deterministic set of resolved trust-policy references,
  versions, and fingerprints;
- supported authority-identifier, source-contract, source-profile, capability,
  and policy-evaluation versions;
- explicit applicability inputs required by the policies;
- creation/capture provenance and immutable custody reference; and
- deterministic ordering and duplicate/conflict diagnostics.

Context identity alone is not integrity. Reuse of one context ID with different
fingerprinted semantic content is a hard substitution failure. Published
context content is immutable; additions, removals, policy changes, declaration
changes, or applicability changes create a new context version/identity and
fingerprint.

The **qualification-context provider** owns construction, validation,
versioning, freezing, and delivery of the context to the qualification
boundary. Source adapters may capture declaration candidates but cannot accept
their own source. Authored manifests do not own the context. Runtime, Coverage,
and Explorer consume qualified results and do not reconstruct authority from
raw source metadata. This ADR does not choose the provider's module, process,
registry, file format, or deployment topology.

Every authority result binds the exact qualification context, authority
declaration candidate(s), resolved trust policy, required capability, source
identity/profile, reason codes, and provenance. The binding is immutable and
retained with any emitted evidence or relationship that depends on it.

## Qualification algorithm and states

For one claim and one required capability, qualification proceeds
deterministically:

1. Validate the context, declarations, policy references, versions,
   fingerprints, and required invariants. Fail processing if they are invalid.
2. Select declaration candidates by exact authority-namespace equality.
3. If there is no candidate, return `UNKNOWN_AUTHORITY`.
4. Detect conflicting declarations for that authority before selecting a
   winner. If conflicts exist, return
   `CONFLICTING_AUTHORITY_DECLARATIONS`.
5. Evaluate the candidate under its exact trust policy in the frozen context.
   If policy explicitly refuses it, return `REJECTED_AUTHORITY`.
6. Require exact `sourceId` and `sourceProfile` binding. A mismatch is
   `REJECTED_AUTHORITY`; it is not repaired from path, repository, or content.
7. Require the exact requested capability in the declaration. If it is absent,
   return `CAPABILITY_NOT_PERMITTED`.
8. Otherwise return `ACCEPTED` for that authority/source/profile/capability
   tuple. Continue with independent source snapshot, datum, reference,
   conflict, and provenance qualification.

The ordinary authority qualification states are:

- `ACCEPTED`: exactly one non-conflicting declaration is policy-accepted and
  exactly binds the source, profile, and required capability.
- `UNKNOWN_AUTHORITY`: no declaration for the exact authority namespace exists
  in the frozen context. Missing knowledge is not policy rejection.
- `REJECTED_AUTHORITY`: a declaration exists, but the referenced trust policy
  explicitly rejects it or its source/profile binding does not match the
  candidate claim.
- `CAPABILITY_NOT_PERMITTED`: the authority and binding are accepted, but the
  exact required capability is not granted.
- `CONFLICTING_AUTHORITY_DECLARATIONS`: two or more declarations considered for
  the exact authority cannot coexist under context invariants, including reuse
  of a declaration identity with different content or incompatible bindings,
  policies, or capability grants.

All candidates, fingerprints, policy evaluations, and deterministic reasons
remain visible for conflict and rejection outcomes. Declaration order, file
order, load order, recency, lexical order, source category, vote count, and
duplicate repetition never select a winner. There is no first-wins,
last-wins, newest-wins, or majority-wins rule. Identical duplicate declarations
cannot increase authority and are diagnosed/de-duplicated according to context
invariants.

These states describe only authority/capability qualification. Malformed
contracts, unsupported mandatory versions, unresolved policy content, missing
required integrity fields, fingerprint mismatch, ID/content substitution,
broken provenance references, nondeterministic duplicate identity, and
internal invariant violations are processing, integrity, or compatibility
failures. They must not be converted to `UNKNOWN_AUTHORITY`,
`REJECTED_AUTHORITY`, or any other ordinary qualification state.

`ACCEPTED` is necessary but insufficient for emitting engineering facts. It
does not override unresolved identities, ambiguous references, duplicate
source data, stale or incompatible snapshots, conflicting evidence, malformed
data, or other ADR-006 outcomes.

## Repository-only bootstrap

Repository-only operation remains supported through an **explicit accepted
qualification policy**, not by repository inference.

A repository-only deployment may ship or be invoked with a frozen bootstrap
qualification context that contains an independently provisioned authority
declaration and trust policy for the exact repository source ID, exact source
profile, exact authority namespace, and explicitly granted capabilities. That
context may be stored alongside the repository for offline operation, provided
its declaration, policy, integrity, provenance, and acceptance are governed
independently from the authored source data they qualify.

For ADR-011, the bootstrap policy may grant
`AUTHOR_SCENARIO_SPECIFICATION` to an exact
`qaip-scenario-authority-repository-json-v1` source binding. The scenario
manifest still cannot grant that capability to itself. If the bootstrap
context is absent, the manifest authority is `UNKNOWN_AUTHORITY`; QAIP does not
promote it merely to preserve convenience.

The same pattern permits a future repository evidence profile to originate
Business Rules or other claims only after a separately defined capability and
an explicit accepted bootstrap declaration grant it. Repository-only does not
mean repository-universal.

## Cross-source reuse and composition

An authority declaration is reusable across captures of the same logical
`sourceId` and exact `sourceProfile` when the declaration, trust policy, and
qualification context remain applicable and are immutably bound to each
capture. It is not automatically reusable for another source ID, profile,
authority namespace, capability, project, or policy scope.

One source may receive multiple capabilities only through explicit grants. One
authority namespace used across multiple physical source IDs requires
non-conflicting declarations that explicitly model those bindings under an
accepted policy; shared naming is insufficient. One source containing claims
that reference another authority does not acquire that authority's capability.

Cross-source facts, such as an ADR-011 Scenario referring to an ADR-010
Business Rule or a future OpenAPI operation, qualify only in an accepted
composite evidence snapshot. That snapshot binds every participating source
snapshot, each source's authority result and capability, the frozen
qualification context, compatibility decisions, resolved references, and
provenance. Acceptance of one participant cannot repair an unknown, rejected,
incapable, conflicting, or integrity-failed participant.

Corroboration by multiple sources preserves separate provenance. Repetition
does not transfer authority, establish completeness, or create majority
precedence.

## Prohibited inference and precedence

QAIP must not infer authority, acceptance, source/profile binding, capability,
trust, or precedence from:

- repository name, owner, remote, revision, root, directory, or file path;
- manifest location, discovery membership, file name, format, or schema
  validity;
- source category such as repository, BA/SA, OpenAPI, test, or runtime;
- organization/display names, URLs, package names, tags, labels, or free text;
- canonical node IDs, source-local keys, content equality, naming similarity,
  graph proximity, or shared referenced artifacts;
- successful parsing, extraction, normalization, validation, compilation, test
  execution, or deployment;
- declaration order, timestamps, number of declarations, or number of sources;
  or
- the fact that another capability or another authority was accepted.

No source category receives universal precedence. Any future precedence or
conflict-resolution policy must be explicit, versioned, context-scoped,
provenance-retaining, and separately accepted. It cannot erase losing evidence
or retroactively turn conflicting declarations into a silent winner.

Schema validity establishes structure only. Authority acceptance establishes a
capability-specific permission only. Neither establishes truth, identity
resolution, applicability, freshness, scope, completeness, or proof.

## Compatibility consequences

### ADR-006 evidence qualification

This ADR supplies ADR-006's authority/capability qualification specialization.
The authority result is one input to the broader evidence-bearing result
algebra. ADR-006 snapshot, datum, fingerprint, provenance, replay, conflict,
and processing-failure obligations remain unchanged. A source declaration in
ADR-006 is not accepted until it is bound through this ADR's frozen context.

### ADR-008 rule verification

ADR-008 witnesses may consume relationships whose originating claims have the
required accepted capabilities. Authority acceptance does not itself form a
witness, establish `VALIDATES`, or change Runtime ownership of verification
interpretation. An authority limitation remains visible and can contribute to
conservative `UNKNOWN` or `AMBIGUOUS` outcomes under the applicable proof
contract.

### ADR-009 scenario evidence formation

The phrase "declared and accepted" in ADR-009 means `ACCEPTED` for the exact
scenario source/profile and `AUTHOR_SCENARIO_SPECIFICATION` in the frozen
context. The capability permits authored Scenario specification; it does not
permit test-declared verification or inference from graph structure.

### ADR-010 multi-authority Business Rule identity

ADR-010's authority-qualified references retain their federated identity.
This ADR qualifies the authority and capability behind a rule-originating or
rule-referencing claim; it does not merge authority namespaces or choose a
canonical Business Rule by source type. A future Business Rule authoring
capability must be defined separately. Repository-only rule origination remains
possible through an explicit accepted profile and bootstrap policy.

### ADR-011 Scenario Authority

ADR-011's manifest `authority` is an exact lookup key, not an authorization
declaration. Its source profile is eligible for
`AUTHOR_SCENARIO_SPECIFICATION` only when the frozen context explicitly grants
that capability to its exact source binding. All ADR-011 discovery, identity,
duplicate, snapshot, relationship, and provenance rules continue to apply.

### Future BA/SA sources

BA/SA tools or documents receive no inherent precedence. Each adapter needs a
versioned source profile, exact source and authority binding, independently
accepted declaration, explicit capabilities, immutable capture, and
provenance. They may originate or reference engineering artifacts only under
capabilities whose semantics are separately accepted.

### Future OpenAPI sources

OpenAPI syntax or schema validity does not establish operation authority.
Future OpenAPI profiles require exact source/authority binding and explicit
capabilities defining whether they may originate operation identity,
specification, constraints, or other claims. A Scenario reference to an
OpenAPI operation does not transfer either source's authority to the other.

### Future repository evidence profiles

Each new repository profile is independently versioned and capability-scoped.
Repository colocation, common Git history, or reuse of a discovery root does
not make profiles compatible or share capabilities. Offline acceptance remains
available through explicit frozen bootstrap contexts.

## Consequences

### Positive

- Authored data cannot authorize itself.
- Authority decisions are deterministic, immutable, replayable, and auditable.
- Capability grants remain narrow and extensible without implicit semantics.
- Missing, rejected, incapable, and conflicting authority remain distinct.
- Federated sources compose without a universal precedence hierarchy.
- Repository-only deployments do not require a live central registry.
- Cross-source claims retain every participating authority and provenance
  boundary.

### Negative

- Every accepted source requires independently governed declaration and policy
  material in addition to its data contract.
- Qualification bundles and diagnostics become larger.
- Context rotation can produce new qualification outcomes for unchanged source
  bytes and therefore requires explicit rebinding and replay identity.
- Deployments must manage bootstrap custody without allowing source manifests
  to mutate their own authority grants.
- New claim types require explicit capability decisions before use.

## Rejected alternatives

### Source manifests authorize their declared authority

Rejected because a source could grant itself any semantic role. Manifest
authority remains a necessary lookup key and insufficient proof of acceptance.

### Schema-valid means authoritative

Rejected because syntax validates shape, not governance, source binding,
integrity, provenance, or permission to make a claim.

### Infer authority from repository location or name

Rejected because locations are mutable provenance and deployment facts, not
stable governance declarations.

### Universal source-category precedence

Rejected because no category is authoritative for every artifact or project,
and hidden precedence would erase legitimate conflict.

### First, last, newest, or majority declaration wins

Rejected because processing order, time, and repetition are not evidence of
authority. Conflicts must remain explicit until an accepted governance action
produces a new non-conflicting context.

### Capabilities imply completeness

Rejected because permission to originate a class of claims says nothing about
whether every relevant claim is present. Completeness requires a separate,
explicit contract.

### Require a live central authority registry

Rejected as a universal dependency because deterministic replay and
repository-only operation must work offline. A future registry may provision
material, but the qualification run consumes a frozen context.

## Unresolved questions

- What concrete syntax and versioning rules identify authority namespaces,
  declaration IDs, context IDs, and policy references?
- What canonical serialization and digest algorithms fingerprint declarations
  and qualification contexts?
- Which component/module implements the qualification-context provider and
  authority qualifier?
- What signed attestation, approval workflow, or custody evidence is required
  for declaration provenance in each deployment class?
- What concrete repository-only bootstrap file/bundle profile is accepted, and
  how is it protected from self-authorization by repository contributors?
- Which capability first governs Business Rule origination under ADR-010?
- Which capabilities govern OpenAPI operation identity/specification and
  BA/SA-authored artifacts?
- Can one authority intentionally bind multiple source IDs, and what explicit
  policy and conflict rules govern that federation?
- How are declaration revocation, supersession, and temporal applicability
  represented without changing historical replay?
- What public diagnostics and serialization expose qualification candidates,
  conflicts, policies, and provenance?

## Conformance requirements

An implementation conforms only if:

- authority lookup uses exact namespace equality;
- every accepted result binds the exact source ID, source profile, source
  contract version, capability, trust policy, declaration, and frozen context;
- authored source data cannot create or accept its own authority declaration;
- manifest authority and schema validity are never sufficient for acceptance;
- missing declarations yield `UNKNOWN_AUTHORITY`, not `REJECTED_AUTHORITY`;
- absent capability yields `CAPABILITY_NOT_PERMITTED`;
- conflicting declarations yield
  `CONFLICTING_AUTHORITY_DECLARATIONS` and retain all candidates;
- no first, last, newest, majority, repository, or source-category winner is
  inferred;
- capabilities have no implicit inheritance, wildcard, truth, precedence, or
  completeness semantics;
- context and declaration identity reuse with different semantic fingerprints
  is a hard failure;
- processing and integrity failures remain distinct from ordinary
  qualification states;
- no repository name, path, discovery rule, or colocation establishes
  authority;
- cross-source use binds every source qualification and provenance in an
  accepted composite snapshot; and
- repository-only operation is possible only through an explicit accepted,
  immutable bootstrap qualification policy.

## Scope exclusions

This ADR does not implement or select:

- an authority registry;
- a qualification-context representation or provider;
- snapshot, canonicalization, or fingerprint code;
- Scenario qualification code;
- Runtime or Explorer behavior;
- a persistence, transport, signing, or security-authorization mechanism; or
- production-code changes.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-008: Rule Verification Proof Contract](ADR-008-rule-verification-proof-contract.md)
- [ADR-009: Scenario Evidence Formation](ADR-009-scenario-evidence-formation.md)
- [ADR-010: Qualified Business-Rule Identity and Reconciliation](ADR-010-qualified-business-rule-identity-and-reconciliation.md)
- [ADR-011: Scenario Authority Source Contract](ADR-011-scenario-authority-source-contract.md)
