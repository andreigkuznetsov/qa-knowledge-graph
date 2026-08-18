# ADR-014: Logical Source Authority Snapshot and Qualification Boundary

- Status: Accepted
- Date: 2026-08-18
- Decision owners: QAIP architecture

## Context

[ADR-006](ADR-006-qualified-engineering-data-and-provenance-contract.md)
requires immutable source snapshots, deterministic datum identity, semantic
fingerprints, retained rejected evidence, and replayable provenance.
[ADR-009](ADR-009-scenario-evidence-formation.md) permits Scenario facts only
from qualified specification evidence. [ADR-010](ADR-010-qualified-business-rule-identity-and-reconciliation.md)
requires authority-qualified Business Rule references without transferring
rule-authoring authority to a referring source.

[ADR-011](ADR-011-scenario-authority-source-contract.md) defines the Scenario
Authority Manifest v1 and requires all valid and rejected attributable data to
be grouped by exact authority before authority-wide duplicate validation.
[ADR-012](ADR-012-qualified-source-authority-and-capability-contract.md) makes
authority declarations and frozen qualification contexts independent of
authored source data. [ADR-013](ADR-013-immutable-source-snapshot-and-fingerprint-contract.md)
separates Repository Capture Snapshots from Logical Source Authority Snapshots
and defines immutable parent capture identity.

The approved Repository Capture capability now provides an immutable,
untrusted candidate containing ordered membership, exact bytes, raw member
fingerprints, unsupported matching entries, mutation-detection version, and a
repository-capture content fingerprint. The current downstream Scenario
pipeline predates that capability: it parses an older byte-capture batch,
returns one batch-level failure on the first malformed member, validates only
the members that survived parsing, and reduces schema-invalid members to
excluded paths during identity processing.

That behavior cannot satisfy the evidence-retention contract. It also leaves a
boundary ambiguity. ADR-013 places one authority-declaration identity and
fingerprint inside logical source content, while ADR-012 permits no matching
declaration, rejected declarations, missing capabilities, and multiple
conflicting declarations. Selecting a declaration while constructing source
content would allow qualification context to change source identity and would
make an `UNKNOWN_AUTHORITY` source impossible to snapshot.

This ADR answers:

> How does QAIP construct an authority-attributed Logical Source Snapshot while
> preserving rejected evidence and keeping authority qualification independent
> from source content?

It defines architecture contracts only. It does not implement parsing,
normalization, logical fingerprinting, authority qualification, canonical
Scenario formation, or cross-source reference resolution.

## Decision drivers

- Every captured member receives a deterministic outcome independently.
- Malformed or rejected source evidence never disappears because another
  member succeeded or failed.
- Authority attribution records what content safely claims; it does not accept
  that claim.
- Authored content cannot provide or select its own authority declaration.
- One immutable logical content snapshot can be requalified under different
  frozen contexts without changing source content identity.
- Logical records remain transitively bound to exact captured bytes.
- Duplicate declarations remain visible without processing-order precedence.
- Source-native references remain distinct from later cross-source resolution.
- One authoritative implementation owns each semantic and logical fingerprint
  contract.

## Decision

QAIP adopts a declaration-independent **Logical Source Authority Snapshot**.
It records the immutable content attributable to one exact claimed authority
namespace. It is an untrusted source-content observation, not an authority
acceptance result.

The processing order is:

```text
immutable Repository Capture Snapshot candidate
  -> verify parent and member integrity
  -> process every captured member independently
  -> record parse outcome
  -> extract a safe claimed-authority envelope when possible
  -> record format, schema, and source-semantic outcomes
  -> retain unattributable outcomes at repository derivation scope
  -> partition attributable accepted and rejected members by exact authority
  -> normalize structurally admitted source data
  -> validate authority-wide Scenario identities and duplicate groups
  -> construct immutable untrusted Logical Source Authority Snapshot candidate
  -> perform independent authority-declaration lookup and binding
  -> perform ADR-012 qualification under one frozen context
  -> consider qualified data in a later accepted composite snapshot
```

Authority-declaration identity, declaration fingerprint, trust-policy content,
qualification-context identity, and qualification state are not logical source
content and do not participate in its content fingerprint.

## Parent Repository Capture binding

Every logical record derived from a captured regular member contains one exact
parent-member reference:

```text
ParentCapturedMemberRef {
  parentSourceId
  parentSnapshotId
  parentContentFingerprint
  normalizedRepositoryRelativePath
  rawByteLength
  rawMemberFingerprint
}
```

The first three fields are the full ADR-006 identity of exactly one parent
Repository Capture Snapshot. `parentContentFingerprint` is also retained as the
explicit ADR-013 parent repository-capture fingerprint. The remaining fields
resolve to exactly one member in that parent.

Construction fails integrity validation when:

- the parent identity does not resolve to the supplied immutable parent;
- the path is absent, duplicated, or not byte-for-byte equal to the parent's
  normalized path text;
- the length or raw fingerprint differs;
- the referenced member has no immutable exact-byte custody;
- a logical record refers to a live path instead of the captured member; or
- one record attempts to combine members from different parent captures.

Logical processing reads only the exact bytes retained by the parent or an
immutable custody reference already bound by the parent's length and raw
fingerprint. It never rediscovers, reopens, or recaptures repository files.
Path normalization and repository membership are not reinterpreted.

One logical snapshot has exactly one parent Repository Capture Snapshot. Parent
unsupported matching entries remain visible through that immutable parent and
are not reclassified as authority-attributed manifests.

For the Scenario repository profile v1, the logical snapshot `sourceId` equals
the parent repository capture `sourceId`. The exact claimed `authority`
partitions logical observations under that source. A future profile needing a
different source-ID relationship requires an explicit versioned mapping; it
must not derive source identity from repository names, paths, remotes, or the
claimed authority string.

## Independent per-member processing

Every captured regular member produces exactly one terminal member-processing
record for the selected parser/schema/normalization versions. Processing
continues after any member-local failure. There is no batch-level first-failure
result and no partial list whose missing suffix is ambiguous.

Member processing records, in parent member order:

- the parent-member reference;
- parser and attribution contract versions;
- one stable parse outcome;
- an optional safely extracted claimed authority;
- format and schema versions when readable;
- stable format, schema, and source-semantic outcome codes;
- deterministic structural locations when the applicable contract defines
  them;
- optional semantic manifest fingerprint when it can be formed; and
- captured-to-processed provenance references.

Human-readable messages, exception text, library wording, stack traces, and
locale-dependent descriptions are diagnostic metadata. They do not determine
membership, attribution, ordering, duplicate behavior, or fingerprints.

An unsupported parser, schema, attribution, normalization, canonicalization,
or mandatory identity version is a processing or compatibility failure. It is
not an ordinary ADR-012 authority state.

## Safe authority attribution

Attribution means only:

> this exact captured document safely and unambiguously claims this exact
> authority namespace.

It does not mean the authority exists, is accepted, has a declaration, owns the
repository, or may author Scenario specifications.

A v1 captured member is safely attributable only when all of these conditions
hold:

1. Its exact bytes decode as strict UTF-8 without BOM removal, replacement
   characters, newline normalization, or other byte transformation.
2. The decoded content contains exactly one complete JSON value and no trailing
   JSON content.
3. JSON parsing succeeds with duplicate-object-member detection enabled for
   the complete document.
4. The root value is an object.
5. The root contains exactly one field named exactly `authority`.
6. The `authority` value is a JSON string.
7. The string satisfies the exact Scenario Authority Manifest v1 authority
   lexical contract and is retained without case folding, trimming, Unicode
   normalization, aliasing, or path-derived prefixes.

Full manifest schema admission is deliberately not required for attribution.
A parseable document may therefore be attributable while having a missing or
wrong format discriminator, unsupported schema version, unknown properties,
invalid Scenario declarations, or other schema/semantic failures. Those
failures are retained; attribution does not repair them.

If any attribution precondition fails, the member is **unattributable**. QAIP
does not infer authority from:

- repository, directory, or file names;
- source ID, Git metadata, branch, remote, or project defaults;
- another manifest in the same capture;
- adjacent or majority authority values;
- Scenario, Operation, or Business Rule text;
- processing order; or
- an authority declaration that was not safely claimed by the content.

## V1 attribution cardinality

One manifest member claims at most one Scenario authority because v1 has one
root `authority` field. Every attributable member belongs to exactly one
authority partition. A member is never copied into several authority snapshots.

Authorities inside `ruleRefs` identify referenced Business Rule sources under
ADR-010. They do not attribute the manifest, transfer rule-authoring capability,
or create additional Logical Source Authority Snapshots. The HTTP Operation
reference likewise does not attribute the manifest to an implementation or
OpenAPI authority.

Different members in one Repository Capture Snapshot may safely claim different
authorities. Each exact authority produces a distinct logical snapshot
candidate with the same immutable parent capture.

## Rejected-evidence model

### Repository-level unattributable-member outcomes

Members for which no authority can safely be extracted remain present in the
parent capture and in an immutable repository-level derivation report:

```text
RepositoryLogicalDerivationReport {
  reportContractVersion
  parentRepositoryCaptureIdentity
  parserVersion
  attributionVersion
  memberOutcomes[]
  provenance[]
}

UnattributableMemberOutcome {
  parentMemberRef
  stableOutcomeCode
  structuralLocation?
  provenanceRef
}
```

The report has exactly one outcome for every parent captured regular member:
`ATTRIBUTED` with its exact claimed authority, or one terminal unattributable
code. Initial unattributable categories include invalid UTF-8, malformed JSON,
duplicate JSON member, trailing JSON content, non-object root, missing
authority, non-string authority, and invalid authority namespace.

The report is ordered by the parent's exact member order. Stable codes and
contract-defined structural locations are semantic; human messages are not.
The report also retains parent unsupported entries by reference rather than
pretending they were parsed members.

The report prevents evidence loss from being inferred only by comparing the
parent with a set of authority snapshots. It is derived evidence with a
separately versioned identity/fingerprint and provenance contract; it is not a
Logical Source Authority Snapshot and cannot authorize anything.

### Attributable rejected members

Every safely attributed member belongs to its authority snapshot even when
format, schema, source-semantic, identity, or duplicate validation rejects all
or part of its content. Its attributed-member record retains:

```text
AttributedMemberRecord {
  parentMemberRef
  exactClaimedAuthority
  parseOutcome
  formatOutcome
  schemaOutcomes[]
  semanticOutcomes[]
  identityAndConflictOutcomes[]
  structuralLocations[]
  semanticManifestFingerprint?
  provenanceRefs[]
}
```

Accepted and rejected attributed-member records both participate in logical
snapshot content and its fingerprint. Schema-invalid content forms no admitted
Scenario, Step, Operation-reference, or Business Rule-reference datum, but its
member outcome remains fingerprinted and auditable.

An outcome is not erased when authority qualification later returns unknown,
rejected, incapable, or conflicting. Conversely, accepted authority cannot
repair a malformed, schema-invalid, duplicate, or semantically invalid datum.

## Datum identities

Logical content distinguishes a source occurrence from the engineering identity
claimed by that occurrence.

### Manifest occurrence

```text
ManifestOccurrenceIdentity =
  parent Repository Capture identity
  + normalized repository-relative path
  + manifest-occurrence identity version
```

The raw member fingerprint is an integrity binding and content input, not a
replacement for occurrence identity. Moving a manifest changes its occurrence
identity and parent membership.

### Scenario occurrence

```text
ScenarioOccurrenceIdentity =
  ManifestOccurrenceIdentity
  + `/scenarios/<zero-based array index>`
  + scenario-occurrence identity version
```

This identity retains every declaration location, including duplicate and
rejected declarations. It does not make duplicate declarations distinct
authoritative Scenarios.

### Claimed Scenario identity

```text
ClaimedScenarioIdentity =
  exact claimed authority
  + normalized scenarioKey
  + scenario identity-scheme version
```

For v1 the identity-scheme version is `qaip-scenario-identity-v1`. This is the
authority-qualified source identity defined by ADR-011. It is not a canonical
`SCENARIO` node ID.

### Step identity

For a structurally admitted unique Scenario declaration:

```text
StepIdentity =
  ClaimedScenarioIdentity
  + phase (`GIVEN`, `WHEN`, or `THEN`)
  + zero-based ordinal within that phase
  + `qaip-scenario-step-identity-v1`
```

Rejected duplicate occurrences retain their steps beneath their
`ScenarioOccurrenceIdentity` for diagnostics and semantic comparison; they do
not publish colliding authoritative Step identities.

### Operation-reference datum identity

```text
OperationReferenceDatumIdentity =
  ClaimedScenarioIdentity
  + fixed role `OPERATION_REF`
  + operation-reference-datum identity version
```

Its content contains the exact supported operation-reference scheme and the
ADR-011 normalized method/path target. It remains an unresolved source-native
reference.

### Business Rule-reference datum identity

```text
BusinessRuleReferenceDatumIdentity =
  ClaimedScenarioIdentity
  + referenced authority
  + normalized stableRuleKey
  + rule identity-scheme version
  + rule-reference-datum identity version
```

Authored array order remains semantic content and provenance but creates no
resolution priority. Duplicate normalized references are retained as rejected
occurrences and do not become multiple accepted reference data.

## Duplicate Scenario identity groups

After partitioning by exact authority, all structurally admitted Scenario
occurrences are grouped by `ClaimedScenarioIdentity` across every attributed
manifest in the parent capture.

```text
ScenarioIdentityGroup {
  claimedScenarioIdentity
  occurrences[]
  outcome
}
```

Occurrences are ordered deterministically by parent member path and then source
array index. No file, declaration, lexical, newest, first, last, or majority
winner exists.

The outcome is:

- `UNIQUE` when exactly one admitted occurrence exists;
- `DUPLICATE_EQUIVALENT` when two or more occurrences have semantic Scenario
  fingerprints produced by the same supported canonicalization version and
  every fingerprint is equal;
- `DUPLICATE_CONFLICTING` when comparable supported semantic fingerprints
  differ; or
- `DUPLICATE_UNCLASSIFIED` when duplication is known but deterministic semantic
  comparability is unavailable.

Every duplicate outcome is rejecting. Equivalent repetition does not increase
authority or evidential strength. Conflicting and unclassified groups preserve
all occurrences, fingerprints that could be formed, stable reasons, locations,
and provenance. No accepted Scenario, Step, or reference datum is published
from a duplicate group.

## Logical Source Authority Snapshot candidate

The immutable candidate contains conceptually:

```text
LogicalSourceAuthoritySnapshotCandidate {
  sourceId
  snapshotId
  contentFingerprint

  authority
  sourceProfile
  sourceContractVersion

  parentRepositoryCaptureIdentity
  parentRepositoryCaptureFingerprint

  formatVersion
  schemaVersion
  parserVersion
  attributionVersion
  normalizationVersion
  manifestIdentityVersion
  scenarioOccurrenceIdentityVersion
  scenarioIdentitySchemeVersion
  stepIdentityVersion
  operationReferenceIdentityVersion
  businessRuleReferenceIdentityVersion
  semanticCanonicalizationVersion
  semanticFingerprintAlgorithm
  logicalEncodingVersion
  logicalDigestAlgorithm

  attributedMembers[]
  manifestOccurrences[]
  scenarioIdentityGroups[]
  normalizedData[]
  provenance[]
}
```

`snapshotId` is externally supplied and is not derived from content. The
ADR-006 snapshot identity is exactly:

```text
sourceId + snapshotId + contentFingerprint
```

Equal content fingerprints do not collapse different observations or snapshot
IDs. Reusing one `sourceId + snapshotId` with a different fingerprint is a hard
substitution failure.

The candidate is untrusted. Construction does not establish authority,
capability, provenance acceptance, canonical Scenario identity, reference
resolution, relationship formation, or completeness.

## Logical content fingerprint boundary

Because this ADR removes declaration fields from ADR-013's field set, it does
not reuse the `scenario-authority-logical-source-c14n-v1` identifier for
different semantics. V1 is retired before production implementation. The first
implementable declaration-independent contract is:

```text
encoding identifier: scenario-authority-logical-source-c14n-v2
digest identifier:   sha-256-v1
serialized value:    scenario-authority-logical-source-v2:
                     <64 lowercase hexadecimal characters>
domain string:       QAIP\u0000SCENARIO_AUTHORITY_LOGICAL_SOURCE\u0000V2
```

The v2 canonical content encoding contains, in order:

1. logical-source domain string;
2. logical encoding and digest identifiers;
3. source ID;
4. exact claimed authority namespace;
5. source profile and source-contract version;
6. full parent Repository Capture Snapshot identity tuple;
7. parent repository-capture fingerprint;
8. format, schema, parser, attribution, normalization, identity,
   semantic-canonicalization, semantic-fingerprint, and outcome-contract
   versions;
9. deterministically ordered attributed-member records and stable outcomes;
10. deterministically ordered manifest occurrences;
11. deterministically ordered Scenario identity groups and occurrence
    references;
12. deterministically ordered normalized datum identities and semantic
    fingerprint references; and
13. deterministically ordered semantic provenance references needed to replay
    the captured-to-normalized mapping.

The encoding reuses ADR-013's strict UTF-8, unsigned 64-bit count/length,
big-endian, length-prefixed primitive rules. Its exact record field sequences,
optional-value encoding, bounds, stable code vocabulary, and golden vectors
must be frozen before production implementation.

It excludes:

- authority declarations and their fingerprints;
- trust policies and qualification-context identity;
- qualification state and policy evaluation;
- snapshot ID;
- repository revision, branch, timestamps, and absolute paths;
- human-readable diagnostics;
- host, process, user, and operating-system metadata;
- Operation or Business Rule resolution results; and
- canonical nodes or relationships.

Changing declaration or qualification policy therefore cannot change the
fingerprint of unchanged logical source content.

## Required semantic fingerprint contracts

Logical snapshot implementation must not begin until Evidence Governance owns
one explicit, versioned canonicalization and fingerprint implementation for
each semantic object used by the logical encoder:

- attributed-member outcome records;
- admitted manifest semantic content;
- Scenario semantic content;
- Step semantic content;
- unresolved HTTP Operation-reference content;
- unresolved authority-qualified Business Rule-reference content;
- duplicate Scenario identity groups;
- repository-level derivation outcomes; and
- semantic provenance references included for replay.

Every contract must define:

- a dedicated domain string and encoding identifier;
- the exact field set and field order;
- strict text, integer, collection, optional, and enum encoding;
- deterministic ordering and duplicate rejection;
- normalization rules and normalization version;
- stable outcome and structural-location vocabulary;
- digest algorithm and serialized value identifier;
- bounds and unsupported-version behavior; and
- golden vectors including accepted, schema-rejected, semantically rejected,
  equivalent-duplicate, conflicting-duplicate, and Unicode cases.

The initial digest primitive is `sha-256-v1`, but raw member fingerprints do
not substitute for semantic fingerprints. JSON object insertion order,
serializer defaults, library diagnostic text, `JsonNode.toString()`, generic
JSON pretty printing, and the Repository Capture encoder are not semantic
canonicalization contracts.

Semantic manifest fingerprints are optional only when safe semantic content
cannot be formed. A stable rejected outcome still participates in the logical
fingerprint. Duplicate equivalence or conflict is asserted only when all
compared occurrences have fingerprints under the same supported semantic
contract; otherwise the outcome is `DUPLICATE_UNCLASSIFIED`.

## Authority declaration and qualification boundary

After an immutable logical source snapshot candidate exists, Evidence
Governance performs exact ADR-012 declaration lookup using:

- the snapshot's claimed authority;
- its exact `sourceId`;
- its exact source profile and source-contract version; and
- required capability `AUTHOR_SCENARIO_SPECIFICATION`.

Declaration lookup/binding produces separate immutable evidence that references
the full logical snapshot identity. Qualification then binds that lookup result,
all declaration candidates and fingerprints, resolved trust policies, required
capability, frozen qualification context, stable reasons, and provenance.

Conceptually:

```text
AuthorityQualificationResult {
  logicalSnapshotIdentity
  qualificationContextIdentity
  requiredCapability
  consideredAuthorityDeclarations[]
  selectedDeclarationBinding?
  trustPolicyEvaluations[]
  state
  stableReasons[]
  provenance[]
}
```

The states affect use, never source content:

- `UNKNOWN_AUTHORITY`: no exact declaration candidate exists. The logical
  snapshot remains unchanged and visible; no Scenario-authoring claim is
  accepted.
- `REJECTED_AUTHORITY`: a declaration exists but policy rejects it or its exact
  source/profile binding fails. The logical snapshot remains unchanged.
- `CAPABILITY_NOT_PERMITTED`: authority and source binding may be accepted, but
  `AUTHOR_SCENARIO_SPECIFICATION` is absent. The logical snapshot remains
  unchanged.
- `CONFLICTING_AUTHORITY_DECLARATIONS`: all conflicting declaration candidates
  remain in the qualification result. No winner is selected and the logical
  snapshot remains unchanged.
- `ACCEPTED`: exactly one non-conflicting, policy-accepted declaration binds the
  source/profile and grants the required capability. This is necessary but not
  sufficient for later Scenario fact formation.

Requalifying the same logical snapshot under another frozen context creates a
new qualification result, not a new logical snapshot. Qualification results
cannot erase rejected members, repair duplicate identities, mutate semantic
fingerprints, or add source data.

Malformed declarations, broken fingerprints, unresolved trust-policy content,
invalid contexts, and provenance/integrity failures remain processing or
integrity failures rather than ordinary qualification states.

## Cross-source resolution boundary

The Logical Source Authority Snapshot contains unresolved source-native
Operation and Business Rule references only. It does not query or bind OpenAPI,
repository implementation, Business Rule, test, or verification snapshots.

Operation resolution, Business Rule resolution, compatibility decisions,
resolution assertions, canonical `SCENARIO` formation, `SPECIFIED_BY`, `COVERS`,
and `VALIDATES` belong to later qualification/composite/derivation stages under
ADR-009 through ADR-012. Their identities, fingerprints, outcomes, and
provenance do not enter logical source content.

An unresolved reference never causes authority inference. Acceptance of the
Scenario source cannot grant authority to the referenced source, and acceptance
of a referenced source cannot repair an unknown or rejected Scenario source.

## Ownership

### Extractor / source adapter

The Scenario source adapter owns:

- reading exact bytes only from the immutable parent candidate;
- strict per-member parsing without batch fail-fast loss;
- safe claimed-authority extraction;
- format, schema, and source-semantic validation;
- source-native normalization;
- authority partitioning;
- occurrence construction and authority-wide duplicate grouping;
- repository-level derivation-outcome construction;
- captured-to-normalized provenance production; and
- construction of untrusted logical-content candidates.

It does not accept an authority declaration, evaluate trust policy, qualify a
capability, resolve cross-source references, or create canonical facts.

### Evidence Governance

Evidence Governance owns:

- logical snapshot and derivation-report invariant contracts;
- parent/member anti-substitution validation;
- semantic canonicalization and fingerprint algorithms;
- logical snapshot canonical encoding and content fingerprint;
- immutable provenance validation and acceptance;
- authority-declaration lookup and binding;
- frozen qualification-context evaluation; and
- immutable qualification results.

There is exactly one authoritative implementation for every semantic and
logical fingerprint version. Source adapters call it and do not reproduce its
serialization.

### Downstream components

Composite evidence processing owns later compatible-source binding and
reference resolution. Runtime, Coverage, Explorer, and the Canonical Ontology
own none of the parsing, attribution, source fingerprinting, declaration
binding, or authority qualification defined here.

## ADR-013 amendment

This ADR normatively amends ADR-013's Logical Source Authority Snapshot section.

The following ADR-013 requirements are superseded:

1. `authorityDeclarationId` and `authorityDeclarationFingerprint` are removed
   from Logical Source Authority Snapshot content and from the logical content
   fingerprint field sequence.
2. Authority declaration binding is a separate immutable Evidence Governance
   result referencing the already immutable logical snapshot.
3. Resolution assertions and source-formed relationships are not normalized
   logical source membership. The logical snapshot contains unresolved authored
   references; resolution and relationships belong to later composite derived
   evidence.
4. The unimplemented `scenario-authority-logical-source-c14n-v1` encoding and
   `scenario-authority-logical-source-v1` value contract are retired. No
   production snapshot may claim those identifiers. The first implementable
   declaration-independent versions are
   `scenario-authority-logical-source-c14n-v2` and
   `scenario-authority-logical-source-v2` with the V2 domain defined here.

ADR-013 remains unchanged for Repository Capture Snapshots, raw member
fingerprints, repository-capture fingerprints, mutation detection, exact-byte
custody, parent binding, snapshot identity, provenance principles, and the
separate logical fingerprint domain.

This amendment resolves the no-declaration and conflicting-declaration
cardinality problem without encoding absence sentinels or arbitrarily selecting
one declaration. It also aligns logical source identity with ADR-012's rule that
qualification-context changes do not mutate source content.

## Compatibility consequences

### ADR-006

The decision preserves immutable snapshot identity, per-datum identities,
semantic fingerprints, rejected evidence, parent lineage, deterministic
outcomes, and the separation of processing failures from qualification states.

### ADR-009

Logical content alone creates no canonical Scenario or relationship. Only a
later qualified and otherwise valid Scenario claim may participate in Scenario
evidence formation.

### ADR-010

Business Rule references retain exact authority-qualified source identity but
remain unresolved. Referencing another authority neither attributes the
manifest to it nor transfers rule-authoring capability.

### ADR-011

The decision implements deterministic multi-file composition, exact authority
partitioning, per-member rejected-data retention, authority-wide duplicate
handling, step identity, and the prohibition on file-order winners.

### ADR-012

Authored authority is only a lookup claim. Declaration binding, trust policy,
capability, and qualification context remain independent evidence. Every
ordinary qualification outcome preserves the same logical snapshot.

### Future source profiles

Future BA/SA, OpenAPI, repository, test, or verification sources may reuse the
parent-binding, unattributable-report, attributed-rejection, and post-snapshot
qualification pattern. Each requires its own attribution, identity,
normalization, semantic canonicalization, and capability contracts. Similar
syntax or repository colocation grants no compatibility or authority.

## Consequences

### Positive

- One malformed member cannot suppress other member outcomes.
- Schema-invalid but safely attributable manifests remain visible under their
  claimed authority.
- Unattributable evidence remains auditable without heuristic attribution.
- Qualification-policy changes do not rewrite source content fingerprints.
- Unknown and conflicting authorities can retain immutable source observations.
- Duplicate Scenario occurrences remain visible without precedence.
- Exact bytes remain bound through one parent repository truth.
- Cross-source resolution cannot leak into source-content identity.

### Negative

- Repository derivation reports add a third retained artifact beside repository
  and authority-specific snapshots.
- Occurrence identities and claimed engineering identities increase model
  complexity.
- Rejected members and duplicates increase storage and fingerprint payloads.
- Multiple separately versioned semantic fingerprint contracts are required.
- A repository containing many authorities produces multiple logical snapshots
  plus one derivation report.

## Rejected alternatives

### Put the selected authority declaration in logical content

Rejected because unknown and conflicting authorities have no single valid
selection, and context changes would change source content identity.

### Use an absent or conflict sentinel as the declaration fingerprint

Rejected because declaration availability is qualification-context evidence,
not authored source content. Encoding it would preserve the same coupling under
a different representation.

### Require full schema admission before authority attribution

Rejected because it would remove safely attributable rejected evidence from the
authority snapshot and contradict ADR-011 and ADR-013 retention requirements.

### Extract authority from malformed JSON or paths

Rejected because partial parsing and contextual inference are ambiguous and
allow repository placement or neighboring content to manufacture authority.

### Fail the entire parse batch on one member

Rejected because it hides deterministic outcomes for other parent members and
makes retained membership depend on processing order.

### Give every duplicate occurrence a different claimed Scenario identity

Rejected because source location is occurrence identity, not authored Scenario
identity. Doing so would conceal the collision ADR-011 requires QAIP to reject.

### Include resolution and relationships in the logical source snapshot

Rejected because their meaning depends on other source snapshots,
qualification outcomes, compatibility, and composite context. They are derived
evidence rather than Scenario source content.

### Reuse raw byte fingerprints as semantic fingerprints

Rejected because formatting-equivalent content may have different raw bytes,
while exact-byte identity still must remain independently auditable.

## Unresolved questions

- What exact Java/module boundary exposes the logical snapshot, derivation
  report, semantic fingerprint, and qualification-result contracts?
- What exact stable code vocabulary and structural-location representation is
  frozen for parser, format, schema, semantic, and duplicate outcomes?
- What are the exact record field encodings and golden vectors for
  `scenario-authority-logical-source-c14n-v2`?
- What domain/value identifiers are selected for manifest, Scenario, Step,
  Operation-reference, Business Rule-reference, duplicate-group, and
  derivation-report semantic fingerprints?
- Is repository derivation-report `snapshotId` externally allocated, or is the
  report identified as a deterministic derived datum within the parent
  snapshot?
- Which schema diagnostics are promoted to stable semantic outcome codes rather
  than library-specific human messages?
- Which safely parsed but unsupported format/schema documents permit a semantic
  manifest fingerprint, and which retain only stable rejected outcomes?
- What retention, confidentiality, and access policy governs exact parent bytes
  and rejected authored content?
- Which source-span representation is portable enough to fingerprint when byte
  offsets or JSON pointers are available?
- How are qualification results and repository derivation reports exposed to
  users without conflating claimed authority with accepted authority?

These questions block production logical snapshot fingerprinting where noted,
but they do not change the declaration-independent snapshot boundary,
per-member retention, safe attribution, parent binding, or qualification order
decided here.

## Conformance requirements

An implementation conforms only if:

- every captured regular member receives one deterministic processing outcome;
- one malformed member cannot suppress any other member outcome;
- every logical/member outcome binds one exact parent captured member;
- no live file is rediscovered, reopened, or recaptured during logical
  construction;
- authority is attributed only under the safe exact-content rules in this ADR;
- unattributable members remain in the parent and repository derivation report;
- schema- and semantic-rejected attributable members remain in their authority
  snapshot and logical fingerprint;
- one v1 manifest is attributed to at most one Scenario authority;
- RuleRef authorities never cause manifest attribution;
- occurrence identities remain distinct from claimed Scenario identity;
- every duplicate occurrence is retained and no winner is selected;
- equivalent/conflicting duplicate classification occurs only with comparable
  deterministic semantic fingerprints;
- logical source content contains no authority declaration, trust policy,
  qualification context, or qualification state;
- every qualification result references the immutable logical snapshot and
  frozen context without mutating source content;
- unknown, rejected, incapable, and conflicting authority results erase no
  source evidence;
- semantic and logical fingerprints have one authoritative Evidence Governance
  implementation per version;
- exact parent bytes remain available through integrity-bound custody;
- no authority is inferred from path, repository, defaults, colocation, order,
  or majority;
- no canonical `SCENARIO`, Operation/Rule resolution, `SPECIFIED_BY`, `COVERS`,
  or `VALIDATES` is created by this capability; and
- Runtime, Coverage, Explorer, and Canonical Ontology remain unchanged.

## Scope exclusions

This ADR does not implement or select:

- logical snapshot, derivation-report, or qualification code;
- exact semantic or logical fingerprint serialization;
- snapshot-ID allocation;
- authority registry or qualification-context storage;
- source retention or access-control technology;
- canonical Scenario formation;
- Operation or Business Rule resolution;
- `SPECIFIED_BY`, `COVERS`, or `VALIDATES` formation;
- Runtime, Coverage, or Explorer behavior; or
- Canonical Ontology changes.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-009: Scenario Evidence Formation](ADR-009-scenario-evidence-formation.md)
- [ADR-010: Qualified Business Rule Identity and Reconciliation](ADR-010-qualified-business-rule-identity-and-reconciliation.md)
- [ADR-011: Scenario Authority Source Contract](ADR-011-scenario-authority-source-contract.md)
- [ADR-012: Qualified Source Authority and Capability Contract](ADR-012-qualified-source-authority-and-capability-contract.md)
- [ADR-013: Immutable Source Snapshot and Fingerprint Contract](ADR-013-immutable-source-snapshot-and-fingerprint-contract.md)
