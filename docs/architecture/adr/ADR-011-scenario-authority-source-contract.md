# ADR-011: Scenario Authority source contract

- Status: Accepted
- Date: 2026-08-16
- Decision owners: QAIP architecture

## Context

ADR-009 permits an authoritative `SCENARIO` and its specification
relationships only from an explicit, structured, qualified source declaration
or a separately accepted derivation profile. It requires stable scenario,
operation, and business-rule references, but does not select a concrete source
format. ADR-010 defines authority-qualified business-rule references and their
resolution to canonical `BUSINESS_RULE` identities. ADR-006 defines the
snapshot, qualification, integrity, and provenance obligations for evidence.

QAIP needs a first accepted source format that repository owners can author
without depending on a live external system or conflating test execution with
scenario specification. The format must be narrower than the Canonical Model:
it declares scenario intent and exact external references, not canonical graph
nodes, relationship IDs, repository-derived operation bodies, rule bodies,
tests, checks, or verification conclusions.

## Decision drivers

- Stable, human-authorable scenario identity.
- Exact operation and business-rule references without semantic guessing.
- Deterministic formation of `SCENARIO`, `SPECIFIED_BY`, and `COVERS` facts.
- Repository-local adoption and deterministic multi-file composition.
- ADR-006-qualified snapshots and replayable provenance.
- Independence between scenario specification and test evidence.
- Compatibility with future YAML and Gherkin adapters without making their
  syntax authoritative now.
- No Canonical Ontology, Runtime, Explorer, or Assertion Semantics change.

## Decision

The first accepted authoritative Scenario source format is the
**QAIP Scenario Authority Manifest v1**, identified by the exact format name:

```text
qaip-scenario-authority-manifest-v1
```

It is a repository-local UTF-8 JSON document governed by a versioned JSON
Schema. A source profile named
`qaip-scenario-authority-repository-json-v1` defines its capture,
normalization, identity, reference, composition, and qualification semantics.

The manifest is an authored specification source. It may author scenarios and
explicitly reference operations and business rules. It does not author tests,
checks, `VALIDATES`, canonical node IDs, canonical relationship IDs, operation
bodies, or business-rule bodies.

### Logical source contract

A logical Scenario Authority source consists of:

- the exact format identifier;
- one stable authority namespace;
- an authority identity-scheme version;
- one or more repository-local JSON manifest files;
- one immutable captured source snapshot containing the deterministic manifest
  membership; and
- zero or more scenario declarations.

Conceptually, each manifest contains only:

```text
format
authority
scenarioIdentityScheme
scenarios[]
```

Each scenario declaration contains only the source information necessary for
scenario specification:

```text
scenarioKey
title
given[]
when[]
then[]
operationRef
ruleRefs[]
```

This is a logical contract, not a committed JSON Schema or parser design. Field
spelling, schema publication location, diagnostics, and transport DTOs remain
implementation work, but an implementation must preserve these exact semantic
elements and constraints.

The manifest must not contain authored canonical node IDs, relationship IDs,
canonical graph edges, repository-derived controller/model details, copied
OpenAPI operation bodies, copied business-rule text, tests, checks, expected
assertion operands, or verification status.

### Authority namespace

`authority` is a stable logical Scenario Authority namespace satisfying
ADR-006. It is not a repository path, checkout, branch, file name, URL,
snapshot ID, or parser invocation. Moving or splitting manifests does not
change authority.

The authority declaration and source profile grant scenario-authoring
capability. They do not automatically grant business-rule-authoring capability
under ADR-010. A manifest may reference a rule owned by the same or a different
authority, but the rule reference does not transfer ownership to Scenario
Authority.

Two manifests with different authority values are different logical sources
even when their content or scenario keys match. Identical keys across
authorities do not imply identical scenarios.

### Scenario identity

The durable source-local scenario identity is:

```text
AuthorityQualifiedScenarioReference =
    authority
  + scenarioKey
  + scenarioIdentityScheme/version
```

The v1 scheme uses the exact version
`qaip-scenario-identity-v1`. `scenarioKey` is a non-blank, source-authored key
whose normalized form is defined by that scheme. It is compared exactly after
scheme-defined Unicode and whitespace validation; case folding, punctuation
removal, display-title normalization, and path-derived prefixes are forbidden.

Although the shorthand `authority + scenarioKey` identifies a scenario within
one v1 contract, the identity-scheme version is always retained for durable
interpretation and future compatibility.

The canonical `SCENARIO` node ID is deterministically formed or selected by
the extractor from this qualified identity and a supported canonicalization
version. Authors never supply it. The source-local tuple remains auditable and
is not replaced by the derived canonical ID.

Scenario identity does not include title, steps, manifest path, array index,
operation reference, rule references, content fingerprint, or source
coordinates. Changing those values updates scenario content or provenance; it
does not silently replace source identity while `scenarioKey` is retained.

### Minimum scenario content

Every scenario declaration contains:

- one non-blank `title` used for human presentation;
- one non-empty ordered `given` sequence;
- one non-empty ordered `when` sequence;
- one non-empty ordered `then` sequence;
- exactly one explicit HTTP operation reference; and
- zero or more explicit authority-qualified business-rule references.

Each Given, When, and Then item is a non-blank authored step text. The manifest
does not interpret step prose as operation, rule, test, check, or assertion
evidence. Step text cannot repair a missing or unresolved explicit reference.

The title and steps are scenario content, never identity aliases. Matching or
similar titles and steps do not reconcile scenarios.

### Deterministic step identity

Each step receives an extractor-derived source-local identity under
`qaip-scenario-step-identity-v1`:

```text
scenario qualified identity
  + phase (`GIVEN`, `WHEN`, or `THEN`)
  + zero-based ordinal within that phase
  + step identity-scheme version
```

Authors do not supply step IDs. Step text is not an identity input. Rewording a
step preserves identity at the same phase and ordinal while changing its
content fingerprint. Insertion, removal, phase movement, or reordering changes
the affected ordinal identities and is treated as a structural content change.
No cross-snapshot step continuity is inferred from matching text.

This bounded ordinal identity is sufficient for v1 because the manifest does
not expose independent step references. A future format requiring durable
independent step references needs a new identity scheme and compatibility
decision.

## Reference contracts

### HTTP operation reference

Each scenario declares exactly one HTTP operation reference:

```text
HttpOperationReference =
    operationIdentityScheme/version
  + method
  + path
```

The v1 scheme is `qaip-http-operation-reference-v1`. It identifies an HTTP
operation by exact normalized method and path. It contains no canonical
`BUSINESS_OPERATION` ID, operation name, controller, method symbol, request
model, response model, copied OpenAPI operation, or endpoint description.

### HTTP operation normalization

Under `qaip-http-operation-reference-v1`:

1. `method` is trimmed of surrounding ASCII whitespace, converted to uppercase
   using locale-independent ASCII rules, and must be one of `GET`, `POST`,
   `PUT`, `PATCH`, or `DELETE`.
2. `path` is trimmed of surrounding ASCII whitespace.
3. If absent, one leading `/` is added.
4. Each run of two or more `/` characters is replaced by one `/`.
5. One trailing `/` is removed unless the result is `/`.
6. Query and fragment components are forbidden.
7. Dot segments `.` and `..`, control characters, backslashes, and URI scheme
   or authority components are forbidden.
8. Path-template names and case remain exact. Percent-encoded octets are not
   decoded for identity matching.

The normalized tuple is compared exactly with canonical operation identity
evidence produced under a compatible operation profile. If no candidate, more
than one candidate, or only incompatible-profile candidates exist, the
reference does not resolve. Method/path text in the title or steps is ignored.

This profile deliberately aligns its basic slash and trailing-slash behavior
with current repository REST operation normalization while making unsupported
URI components and ambiguity explicit.

### Business-rule references

`ruleRefs` is an ordered, duplicate-free collection of zero or more
`AuthorityQualifiedBusinessRuleReference` values from ADR-010:

```text
authority
stableRuleKey
identityScheme/version
```

The manifest contains no canonical `BUSINESS_RULE` ID, rule name, field name,
constraint value, rule body, expression, or copied repository/OpenAPI rule
content. A rule owned by any accepted authority may be referenced.

Each reference resolves independently through qualified ADR-010 identity
assertions in the accepted composite snapshot. Collection order is retained as
authored content but has no resolution or evidential priority. Duplicate
normalized references in one scenario are a source-schema or semantic
validation failure; they are not counted twice.

Zero rule references is valid. Such a scenario may specify an operation but
claims no rule coverage.

### Prohibited reference resolution

Operation, rule, and scenario identity must not be resolved or reconciled from:

- title or step text;
- field, property, class, method, controller, or request-model names;
- endpoint mentions outside the explicit operation reference;
- rule values, messages, expressions, or content equality;
- common operation membership;
- shared files, repositories, tests, checks, or implementations;
- graph proximity, reachability, or shared neighbors; or
- fuzzy, ranked, probabilistic, first-match, or display-name matching.

Matching content may support diagnostics only. It cannot emit a canonical node
or relationship.

## Relationship formation

### Scenario node formation

One canonical `SCENARIO` observation may be emitted only when:

- the manifest and declaration pass source-schema and semantic validation;
- the authority and source profile are accepted and scenario-authoring capable;
- the source snapshot, manifest datum, scenario datum, and provenance satisfy
  ADR-006;
- the qualified scenario identity is unique in the logical source snapshot;
- the identity/canonicalization versions are supported; and
- no relevant integrity, duplicate, or identity conflict prevents
  qualification.

Operation or rule reference failure does not erase an otherwise authoritative
scenario declaration. The scenario may remain qualified as scenario knowledge
while individual relationships are withheld with retained diagnostics and
resolution outcomes.

### SPECIFIED_BY

Exactly one

```text
BUSINESS_OPERATION --SPECIFIED_BY--> SCENARIO
```

fact may be emitted for a declaration only when:

- its canonical `SCENARIO` identity is qualified;
- its explicit HTTP operation reference resolves to exactly one canonical
  `BUSINESS_OPERATION` in the same accepted composite evidence snapshot;
- the operation identity profile is compatible;
- the endpoint types and direction satisfy the Canonical Model; and
- the relationship datum has its own deterministic identity, source snapshot,
  content fingerprint, and provenance.

An unresolved, ambiguous, rejected, or incompatible operation reference emits
no `SPECIFIED_BY`. Candidates, reasons, and provenance remain visible. No rule
reference and no scenario prose can substitute for operation resolution.

### COVERS

For each distinct declared rule reference, exactly one

```text
SCENARIO --COVERS--> BUSINESS_RULE
```

fact may be emitted only when:

- the canonical `SCENARIO` identity is qualified;
- that exact rule reference is qualified and `RESOLVED` under ADR-010 to one
  canonical `BUSINESS_RULE` in the same accepted composite snapshot;
- endpoint types and direction satisfy the Canonical Model; and
- the relationship datum has its own deterministic identity, source snapshot,
  content fingerprint, and provenance.

Rule references are independent. One unresolved or ambiguous rule reference
does not suppress qualified `COVERS` facts for other references. It emits no
edge for itself and retains its outcome, candidates, reasons, and provenance.
An empty `ruleRefs` collection emits no `COVERS` facts and is not an error.

`COVERS` formation does not require `SPECIFIED_BY` formation to succeed. Both
facts are qualified independently from their exact declarations. ADR-008 may
still be unable to form a complete verification witness when either required
fact is absent.

### VALIDATES exclusion

The manifest never creates
`TEST_IMPLEMENTATION --VALIDATES--> SCENARIO`. It contains no test identity or
test binding. Test evidence must independently identify a test and explicitly
reference the scenario under a separately accepted source contract, as
required by ADR-009.

Scenario specification and test verification remain semantically independent
even when their physical files are stored in the same repository. No test,
check, assertion, execution result, or operation exercise is interpreted by
this contract. Assertion Semantics is outside scope.

## Validation, qualification, and provenance

### Source-schema validation boundary

The versioned JSON Schema owns JSON shape, required fields, primitive types,
closed objects, non-blank constraints that can be expressed structurally, and
the exact format discriminator. Unknown fields and unsupported format versions
are rejected. Parsers do not coerce scalar types, apply aliases, fill omitted
semantic fields, or silently upgrade versions.

Semantic validation owns cross-entry uniqueness, normalized reference
duplicates, authority consistency, identity-scheme support, HTTP normalization
constraints, file-composition invariants, and internal reference rules.

Schema-valid input is only a well-formed source candidate. It is not
automatically qualified evidence and cannot by itself emit canonical facts.
Authority capability, snapshot integrity, identity resolution, provenance,
compatibility, and conflict checks occur after schema validation.

### Snapshot and fingerprint

One logical-source snapshot deterministically binds:

- the authority and source-profile declaration;
- repository revision or other stable capture context;
- the lexically ordered set of normalized repository-relative manifest paths;
- the exact bytes or canonical semantic content of every manifest;
- format, schema, normalization, identity, and canonicalization versions;
- deterministic scenario and relationship datum membership; and
- a versioned snapshot content fingerprint.

Manifest path is membership and provenance, not scenario identity. Adding,
removing, or changing a member manifest creates a new snapshot. Reusing one
source/snapshot identity with a different fingerprint is a hard failure under
ADR-006.

Every manifest, scenario declaration, derived step, explicit reference,
resolution assertion, and emitted relationship has a deterministic datum
identity and semantic content fingerprint. Provenance retains the repository
snapshot, repository-relative file, integrity-bound location, source span when
available, normalization activity/version, and parent evidence. Live mutable
repository state is not consulted during qualification or replay.

Cross-source operation and rule resolution occurs only within an accepted
composite evidence snapshot that binds the Scenario Authority snapshot to the
referenced authority snapshots and records compatibility.

### Duplicate scenario keys

Within one authority and scenario identity scheme, a normalized `scenarioKey`
may occur exactly once in one logical-source snapshot.

- Repetition in one file or across files is a duplicate identity failure.
- Byte- or content-identical repetition is still rejected and cannot increase
  evidential strength.
- Different content under one key is a conflicting duplicate and is rejected;
  file order never selects a winner.
- The conflicting declarations and locations remain in diagnostics and
  provenance, but no canonical scenario or relationships are emitted for that
  key.
- The same key under a different authority is a different source-local
  identity and is not a duplicate.

V1 defines no multi-declaration composition for one scenario identity.

### File splitting and multiple manifests

An authority may split scenarios across any number of manifest files. All
member files must declare the same exact format, authority, and compatible
identity-scheme versions. File boundaries and lexical file order have no
semantic effect on scenario identity or relationship formation.

The captured set is composed by validating each file, sorting normalized
repository-relative paths lexically, combining scenario declarations, then
applying authority-wide duplicate and semantic validation. Files are never
processed with last-wins, first-wins, or directory precedence.

One repository may contain multiple Scenario Authorities. Each is captured as
a distinct logical source snapshot and may participate in an explicitly bound
composite snapshot. Merely sharing a repository does not reconcile authorities
or scenarios.

## Compatibility with future source adapters

Future YAML or Gherkin support requires a separately accepted, versioned source
profile. Such an adapter may produce evidence compatible with this contract
only when it preserves:

- an explicit stable authority and scenario key;
- title and ordered Given/When/Then content;
- one exact HTTP operation reference;
- zero or more exact ADR-010 rule references;
- independent scenario and test declarations;
- equivalent duplicate, ambiguity, qualification, snapshot, fingerprint, and
  provenance semantics; and
- a deterministic, versioned normalization mapping to the same logical datum
  model.

File names, feature titles, scenario titles, tags, natural-language endpoint
mentions, step text, generated line numbers, and test bindings cannot be used
as substitute identity or references. A YAML or Gherkin adapter is not accepted
merely because it can serialize similar fields. Cross-format declarations
resolve to one scenario only through explicit qualified identity evidence or an
accepted lossless normalization profile; content similarity never merges them.

This ADR does not approve a YAML syntax, Gherkin dialect, tag convention,
executable-specification binding, parser, or extractor.

## Compatibility with existing architecture

### ADR-006

The logical source, immutable snapshot, datum fingerprints, qualification
outcomes, composite snapshot, and provenance obligations specialize ADR-006
without weakening it. Schema validity never substitutes for qualification.

### ADR-009

This manifest is the first explicit authored-scenario source class accepted by
ADR-009. It supplies stable scenario identity and exact operation/rule
references. It preserves independent specification and verification evidence,
and it never emits `VALIDATES`.

### ADR-010

Every business-rule reference is an ADR-010
`AuthorityQualifiedBusinessRuleReference`. The manifest neither authors a
canonical rule ID nor reconciles rule identity from content. Unresolved,
ambiguous, rejected, and conflicting rule evidence remains outside simplistic
canonical merging.

### Canonical Model

The existing `SCENARIO`, `STEP`, `BUSINESS_OPERATION`, and `BUSINESS_RULE`
nodes and `SPECIFIED_BY` and `COVERS` relationships are sufficient. Authors
declare no canonical IDs or relationship IDs. No Canonical Ontology change is
required.

### ADR-008, Runtime, Coverage, and Explorer

Qualified facts produced under this contract may later participate in the
existing ADR-008 witness. Runtime continues to own verification interpretation;
Coverage continues to consume exact canonical `COVERS` relationships; Explorer
continues to present results. None owns source parsing, reference repair, or
reconciliation, and this ADR modifies none of them.

## Consequences

### Positive

- Repository owners have one concrete, versioned authoritative scenario
  format.
- Scenarios and steps remain stable across ordinary file movement and content
  edits according to explicit identity rules.
- Operation and rule relationships are formed only from exact qualified
  references.
- Multi-file repositories compose deterministically without file-order
  precedence.
- Repository-local specification remains independent of tests and execution.
- The Canonical Model and downstream ownership boundaries remain unchanged.

### Negative

- Authors must maintain stable scenario keys and qualified rule references.
- Rule and operation authority snapshots must be composed explicitly before
  cross-source relationships qualify.
- V1 ordinal step identity does not preserve independent step identity across
  insertion or reordering.
- Duplicate scenario declarations cannot be used for partial composition.
- Conservative unresolved outcomes are expected while referenced identities
  or composite snapshots are unavailable.

## Rejected alternatives

### Canonical QA Model JSON as the authored scenario format

Rejected because it would expose canonical node and relationship IDs, allow
authors to duplicate derived graph structure, and make a broad interchange
model the source-authoring contract.

### Scenario titles, file paths, or step text as identity

Rejected because display content and locations are refactoring-sensitive and
cannot establish durable author intent.

### Embed operation and business-rule bodies

Rejected because copied repository/OpenAPI operation details and rule text
would create duplicate truth and drift. The manifest carries exact references
only.

### Infer references from prose or graph context

Rejected because text, fields, values, operation membership, and proximity do
not establish identity or authority.

### Treat schema validity as qualification

Rejected because JSON shape does not prove source authority, snapshot
integrity, reference resolution, provenance, or compatibility.

### Create VALIDATES from colocated tests

Rejected because it would conflate independent specification and verification
evidence and could create a circular ADR-008 witness.

### One manifest file per authority

Rejected because file layout is an operational concern. Deterministic
authority-wide composition and duplicate detection preserve semantics across
file splits.

### Accept YAML or Gherkin as equivalent syntax in v1

Rejected because their structural, identity, tag, and executable-binding
semantics require explicit profiles and qualification decisions.

## Unresolved questions

- What repository-relative discovery convention selects manifest files without
  making paths part of scenario identity?
- What exact JSON Schema URI, publication lifecycle, and compatibility policy
  will govern the v1 format?
- What concrete syntax and registry identify Scenario Authority namespaces?
- Which repository revision/capture mechanisms satisfy the first source
  snapshot profile?
- What canonical serialization and digest algorithms fingerprint manifests,
  scenarios, steps, references, and relationships?
- Which component constructs and validates composite evidence snapshots?
- What diagnostics/public contract exposes duplicate declarations, unresolved
  operations, ambiguous rules, rejected evidence, and provenance?
- Should a future step-identity scheme introduce explicit durable step keys?
- Which future profile, if any, accepts Gherkin as both scenario specification
  and independently bound executable verification evidence?
- How are scenario replacement and supersession represented when an authority
  intentionally retires a `scenarioKey`?

## Conformance requirements

An implementation conforms only if:

- it accepts the exact v1 format and source-profile versions or rejects them
  explicitly;
- authority plus scenario key plus identity-scheme version is the durable
  source-local scenario identity;
- no authored canonical node or relationship ID is accepted;
- operation and rule bodies are not duplicated into the manifest;
- one explicit HTTP reference resolves exactly before `SPECIFIED_BY` forms;
- every `COVERS` edge comes from one independently resolved ADR-010 reference;
- zero rule references is valid and forms no `COVERS` edges;
- unresolved, ambiguous, rejected, duplicate, and conflicting evidence remains
  visible and never produces a guessed edge;
- schema validity alone never qualifies evidence;
- scenario specification and test evidence remain independent;
- the manifest never creates `VALIDATES`;
- file splitting cannot change logical identities or introduce processing-order
  precedence;
- no text, value, field, content, operation-membership, or graph-proximity
  matching establishes identity or reference resolution; and
- no Canonical Ontology, Runtime, Explorer, or Assertion Semantics change is
  required.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-008: Rule Verification Proof Contract](ADR-008-rule-verification-proof-contract.md)
- [ADR-009: Scenario Evidence Formation](ADR-009-scenario-evidence-formation.md)
- [ADR-010: Qualified Business-Rule Identity and Reconciliation](ADR-010-qualified-business-rule-identity-and-reconciliation.md)
- [Canonical QA Model schema](../../../qa-model/src/main/resources/schema/qa-model-v0.1.schema.json)
- [Relationship matrix](../../relationship-matrix.md)
