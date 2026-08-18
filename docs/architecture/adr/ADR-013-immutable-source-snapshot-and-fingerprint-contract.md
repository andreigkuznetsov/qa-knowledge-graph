# ADR-013: Immutable Source Snapshot and Fingerprint Contract

- Status: Accepted
- Date: 2026-08-18
- Decision owners: QAIP architecture

## Context

[ADR-006](ADR-006-qualified-engineering-data-and-provenance-contract.md)
requires immutable source snapshots, deterministic datum membership, content
fingerprints, versioned canonicalization, provenance, and anti-substitution
checks. It defines the generic evidence boundary but does not select a concrete
repository capture or fingerprint encoding.

[ADR-011](ADR-011-scenario-authority-source-contract.md) defines deterministic
Scenario Authority repository discovery, exact-byte capture, logical authority
grouping, rejected-member retention, and separate repository-capture and
logical-source obligations. It intentionally leaves canonical serialization,
digest algorithms, mutation detection, and construction ownership unresolved.

[ADR-012](ADR-012-qualified-source-authority-and-capability-contract.md) binds
an accepted authority and capability to an exact source identity, source
profile, authority declaration, and frozen qualification context. Authority
qualification must therefore refer to an immutable source observation rather
than mutable repository state.

The current Scenario Authority pipeline discovers normalized repository paths
in exact Unicode code-point order, diagnoses matching symbolic links, captures
member bytes without following links, parses only captured bytes, and retains
defensive byte copies. It does not yet construct a source snapshot, compute
member or snapshot fingerprints, retain a batch containing malformed members,
or detect every observable mutation between discovery and capture.

Existing QAIP fingerprints have narrower meanings. The Simulation model
fingerprint canonicalizes Canonical QA Model JSON. The Impact Evidence manifest
fingerprint uses a deterministic length-prefixed binary encoding of normalized
evidence. The repository analyzer also has an internal project-graph hash.
None is the source identity of a repository filesystem capture, and silently
reusing any of them would create a second or misleading fingerprint truth.

This ADR answers:

> How does QAIP construct immutable, reproducible, fingerprint-bound source
> snapshots from repository evidence?

It specifies contracts and ownership only. It does not implement them.

## Decision drivers

- Exact source bytes remain available for replay and audit.
- Repository membership includes ignored, untracked, modified, and malformed
  matching files under ADR-011.
- Raw source identity is not replaced by semantic JSON canonicalization.
- File movement changes capture membership without changing authored Scenario
  identity.
- Mutation during a non-atomic filesystem scan is detected conservatively.
- Repository capture and authority-specific normalization remain distinct.
- Fingerprint encoding is deterministic across processes and platforms.
- One implementation owns canonical fingerprint serialization.
- Git metadata is never a substitute for captured filesystem content.

## Decision

QAIP adopts two immutable, fingerprint-bound snapshot layers:

```text
repository filesystem view
  -> Repository Capture Snapshot
  -> Logical Source Authority Snapshot candidate(s)
  -> authority/capability qualification under ADR-012
  -> qualified normalized evidence under ADR-006
```

The **Repository Capture Snapshot** binds exactly what the repository discovery
profile observed and captured, including raw bytes and unsupported matching
entries. The **Logical Source Authority Snapshot** binds the authority-specific
interpretation of attributed captured members and normalized data while
retaining an immutable reference to the parent repository capture.

Neither layer queries live repository state after publication. Corrections or
new captures create new snapshot observations. Snapshot contents, membership,
outcomes, provenance references, and fingerprints are immutable.

## Snapshot identity

Both layers use the ADR-006 snapshot identity tuple:

```text
sourceId + snapshotId + contentFingerprint
```

- `sourceId` identifies the stable logical source.
- `snapshotId` identifies one capture or logical-source observation and cannot
  be reused for different content.
- `contentFingerprint` binds the exact content governed by the applicable
  snapshot fingerprint contract.

The tuple, not any one field, is the durable reference. Reusing one
`sourceId + snapshotId` with a different content fingerprint is a hard
substitution failure.

Equal content fingerprints do not collapse distinct observations. Two captures
of unchanged bytes may have different snapshot IDs, capture times, custody
records, and provenance while sharing a content fingerprint. Their normalized
data remain provenance-distinct under ADR-006.

A timestamp, UUID, Git commit, branch, path, or fingerprint alone is not a
complete snapshot identity.

## Repository Capture Snapshot

A Repository Capture Snapshot contains, at minimum:

```text
RepositoryCaptureSnapshot {
  sourceId
  snapshotId
  sourceProfile
  sourceContractVersion
  discoveryProfileVersion
  discoveryAnchorContract
  pathNormalizationVersion
  orderingVersion
  memberFingerprintAlgorithm
  repositoryFingerprintEncodingVersion
  repositoryFingerprintDigestAlgorithm
  members[]
  unsupportedMatchingEntries[]
  captureOutcome
  capturedAt
  repositoryRevision?
  provenanceRef
  repositoryCaptureFingerprint
}
```

The snapshot binds:

- one stable repository `sourceId` satisfying ADR-006 and ADR-012;
- one non-reused `snapshotId` for the capture observation;
- exact source-contract, source-profile, and discovery-profile versions;
- the fixed discovery-anchor interpretation;
- the complete, unique, normalized repository-relative membership;
- exact captured bytes for every member, either embedded or reachable through
  an immutable integrity-bound custody reference;
- raw byte length and raw member fingerprint for every captured member;
- every matching unsupported entry and its stable structural outcome;
- the successful stable-capture outcome and mutation-detection version;
- capture and repository provenance; and
- a repository-capture content fingerprint computed by this ADR.

Exact member bytes are mandatory audit material. A custody reference is valid
only when it resolves to immutable bytes whose length and raw member
fingerprint match the member record. A live path or URI without immutable
custody and integrity binding is insufficient. Normalized JSON cannot replace
the exact bytes.

`capturedAt` records when the capture was frozen. It is required provenance but
does not enter the repository-capture content fingerprint.

### Captured member contract

Each regular discovered member contains:

```text
CapturedMember {
  normalizedRepositoryRelativePath
  rawByteLength
  rawMemberFingerprint
  embeddedBytes | immutableCustodyRef
  memberCaptureOutcome
  provenanceRef
}
```

The path is normalized with `/` separators, remains contained beneath the
normalized repository root and discovery anchor, and is unique within the
capture. The contract does not Unicode-normalize or case-fold path text beyond
the explicit ADR-011 path-normalization profile.

`rawByteLength` is the non-negative count of captured octets. It is checked
against the retained bytes independently of the digest.

`memberCaptureOutcome` is successful only when the member remained a regular
non-symbolic-link entry through the required capture checks. An unreadable,
replaced, or observably mutating member prevents an accepted repository capture;
it is not silently omitted.

### Unsupported matching entries

Every path matching the discovery suffix but having an unsupported entry kind,
including an ADR-011 matching symbolic link, is represented by:

```text
UnsupportedMatchingEntry {
  normalizedRepositoryRelativePath
  entryKind
  stableDiagnosticCode
}
```

Unsupported entries have no captured target bytes and no raw member
fingerprint. Their normalized path, entry kind, and stable code participate in
the repository-capture fingerprint. Human-readable diagnostic text does not.

A structurally invalid discovery anchor or other top-level discovery invariant
failure produces no accepted Repository Capture Snapshot. Matching unsupported
entries that ADR-011 permits as explicit diagnostics remain fingerprinted
members of an otherwise stable capture outcome.

### Malformed and rejected members

Discovery establishes membership before UTF-8 decoding, JSON parsing, schema
validation, semantic validation, authority attribution, or qualification.
Consequently every successfully captured regular member remains in the
Repository Capture Snapshot even when it later has:

- invalid UTF-8;
- malformed JSON or trailing JSON content;
- duplicate JSON object members;
- an unknown format or schema version;
- schema or semantic validation failures;
- inconsistent or unreadable authority content; or
- duplicate/conflicting authored identities.

Such outcomes cannot retroactively turn a discovered member into a non-member.
The exact bytes, path, length, raw fingerprint, outcome, and provenance remain
available. A parser or validator may reject a member without rejecting the
integrity of the already completed repository capture.

## Raw member fingerprint

The initial raw member fingerprint contract is:

```text
algorithm identifier: sha-256-v1
digest input:          exact captured byte sequence, with no transformation
digest algorithm:      SHA-256
digest encoding:       exactly 64 lowercase hexadecimal characters
serialized value:      sha-256-v1:<64 lowercase hexadecimal characters>
```

Conceptually:

```text
rawMemberFingerprint =
  "sha-256-v1:" + lowercaseHex(SHA-256(exactCapturedBytes))
```

No byte-order mark removal, UTF-8 decoding, newline conversion, whitespace
normalization, JSON parsing, object-key sorting, numeric normalization, or
Unicode normalization occurs before hashing.

The normalized repository-relative path is **not** part of the raw byte digest.
It participates separately in snapshot membership. Therefore:

- identical bytes at two paths have equal raw fingerprints but remain two
  distinct members;
- moving or renaming a file changes repository snapshot membership and the
  repository-capture fingerprint;
- moving a manifest need not change its ADR-011 authored Scenario identity;
  and
- formatting-only byte changes change the raw fingerprint even when later
  semantic normalization considers the documents equivalent.

The `sha-256-v1` identifier fixes both SHA-256 and lowercase hexadecimal
serialization. A bare 64-character value or differently prefixed value is not
silently treated as this algorithm.

## Repository-capture fingerprint contract

The initial repository-capture fingerprint uses:

```text
encoding identifier: scenario-authority-repository-capture-c14n-v1
digest identifier:   sha-256-v1
serialized value:    scenario-authority-repository-capture-v1:
                     <64 lowercase hexadecimal characters>
```

The digest input is a dedicated, domain-separated, deterministic
length-prefixed binary serialization. It is not JSON and is not the
`qamodel-c14n-v1` or `impact-evidence-canonical-v1` serialization.

### Length-prefixed binary encoding

`scenario-authority-repository-capture-c14n-v1` defines:

- text as strict UTF-8 bytes preceded by an unsigned 64-bit byte length;
- byte strings preceded by an unsigned 64-bit byte length;
- collection counts as unsigned 64-bit integers;
- integers in unsigned big-endian network byte order;
- enums and algorithm identifiers as their exact case-sensitive ASCII names
  encoded through the text rule;
- no null values; an optional value is encoded as a one-byte presence marker
  followed by its value when present; and
- no locale-sensitive transformation or platform-native separator.

The encoding begins with the exact domain string:

```text
QAIP\u0000SCENARIO_AUTHORITY_REPOSITORY_CAPTURE\u0000V1
```

encoded through the text rule. This prevents bytes encoded for another QAIP
fingerprint purpose from being interpreted as a repository capture.

### Fingerprinted repository-capture fields

The encoding contains, in this order:

1. domain string;
2. encoding identifier;
3. digest identifier;
4. source ID;
5. source-contract version;
6. source-profile identifier/version;
7. discovery-profile version;
8. discovery-anchor contract/version and exact relative anchor;
9. path-normalization version;
10. ordering version;
11. member-fingerprint algorithm identifier;
12. mutation-detection version;
13. successful stable capture-outcome code;
14. member count;
15. each captured member record;
16. unsupported-entry count; and
17. each unsupported matching-entry record.

Each member record encodes, in order:

1. normalized repository-relative path;
2. raw byte length;
3. raw member-fingerprint algorithm identifier; and
4. raw member fingerprint value.

The fingerprint binds exact bytes transitively through the validated raw
fingerprint and byte length. Exact bytes remain independently retained and
verifiable; embedding the full byte sequence again in the aggregate encoding is
not required.

Each unsupported-entry record encodes, in order:

1. normalized repository-relative path;
2. stable entry-kind identifier; and
3. stable diagnostic code.

Captured members are encoded in ascending exact Unicode code-point lexical
order of normalized repository-relative path. Unsupported entries use the same
path ordering, then entry kind, then stable diagnostic code. Duplicate
normalized paths are an invariant failure; enumeration order never breaks a
tie.

The repository-capture fingerprint is:

```text
"scenario-authority-repository-capture-v1:"
  + lowercaseHex(SHA-256(encodedRepositoryCaptureFields))
```

### Excluded content

The following do not enter the repository-capture content fingerprint:

- absolute checkout or host filesystem path;
- capture, file, commit, or wall-clock timestamps;
- branch or tag name;
- Git commit/revision;
- remote repository URL;
- human-readable diagnostic messages;
- host, user, process, thread, invocation, or operating-system metadata;
- file-system timestamps, permissions, or file keys used only for mutation
  detection; and
- custody locator text, provided that retained bytes are independently bound by
  length and raw fingerprint.

These values may be retained in provenance. Excluding them allows the same
captured membership and bytes under the same source/profile contract to produce
the same content fingerprint across machines and capture observations.

## Repository revision and source identity

Git revision is provenance or an effective-context reference when available.
It is not source identity, snapshot identity, repository-capture fingerprint
input, or evidence that the working tree equals the commit.

A revision change without a change to matching filesystem membership or bytes
does not change the repository-capture fingerprint. A matching ignored,
untracked, staged, unstaged, or otherwise modified file does change the
fingerprint even when the Git revision is unchanged. Git commit alone can never
serve as the Repository Capture Snapshot.

The absolute checkout location is also provenance only. Stable `sourceId` comes
from the accepted ADR-012 source binding and cannot be inferred from the
checkout directory or remote name.

## Logical Source Authority Snapshot

After capture, parsing and source-specific validation partition attributable
data by exact authority. Each authority produces a distinct immutable Logical
Source Authority Snapshot candidate.

It contains, at minimum:

```text
LogicalSourceAuthoritySnapshot {
  sourceId
  snapshotId
  authority
  sourceProfile
  sourceContractVersion
  authorityDeclarationId
  authorityDeclarationFingerprint
  parentRepositoryCaptureRef
  parentRepositoryCaptureFingerprint
  formatVersion
  schemaVersion
  parserVersion
  normalizationVersion
  identitySchemeVersion
  semanticCanonicalizationVersion
  semanticFingerprintAlgorithm
  attributedMembers[]
  normalizedData[]
  provenance[]
  logicalSnapshotFingerprint
}
```

The authority declaration identity and fingerprint bind the candidate to the
independently governed declaration considered under ADR-012. Inclusion does not
itself mean `ACCEPTED`; Evidence Governance still validates the declaration,
frozen qualification context, source/profile binding, capability, integrity,
and provenance.

### Parent capture binding

Every logical snapshot references exactly one parent Repository Capture
Snapshot by its full ADR-006 identity tuple and separately retains the parent
repository-capture fingerprint. Every attributed member resolves to exactly one
captured member in that parent with the same normalized path, raw byte length,
and raw member fingerprint.

The logical snapshot does not rediscover files, recapture bytes, redefine path
normalization, omit parent unsupported entries, or independently reinterpret
raw repository membership. The parent capture is the single truth for what was
present. Authority attribution and semantic normalization are a derived layer.

Malformed or otherwise unreadable members that cannot be attributed to an
authority remain visible through the parent capture. They are not assigned to
an authority by file name, path, neighboring manifests, first/last processing,
or repository defaults.

### Attributed member records and outcomes

An attributed member record contains:

- parent captured-member reference;
- exact authority read from the captured content;
- parse, format, schema, semantic, duplicate, and conflict outcomes using
  stable versioned codes;
- source spans or structural locations when available;
- semantic manifest fingerprint when one can be formed; and
- captured-to-normalized provenance references.

Accepted and rejected attributed members both participate in the logical
snapshot fingerprint. Rejection does not erase captured bytes or membership.
Human-readable diagnostics remain outside fingerprints; stable outcome codes
and structural locations participate when defined by the applicable versioned
contract.

### Normalized datum membership

Normalized data include deterministic identities and semantic fingerprint
references for every formed manifest datum, Scenario, Step, explicit operation
reference, Business Rule reference, resolution assertion, and source-formed
relationship allowed by ADR-011.

Semantic fingerprints use a separately versioned source-semantic
canonicalization. They do not replace member raw fingerprints and cannot prove
which bytes were captured. Formatting-equivalent JSON may produce equal
semantic fingerprints while retaining distinct raw fingerprints and capture
provenance.

Datum ordering follows its contract-defined deterministic identity. Authored
Scenario step order remains semantic and is preserved. File order controls
deterministic processing and serialization only; it never establishes
authority, duplicate precedence, or composition semantics.

## Logical-source fingerprint contract

The logical snapshot has a separate fingerprint domain:

```text
encoding identifier: scenario-authority-logical-source-c14n-v1
digest identifier:   sha-256-v1
serialized value:    scenario-authority-logical-source-v1:
                     <64 lowercase hexadecimal characters>
domain string:       QAIP\u0000SCENARIO_AUTHORITY_LOGICAL_SOURCE\u0000V1
```

It uses the same primitive length/count/optional-value rules defined for the
repository encoding, but its field sequence is independently versioned. It
encodes, in order:

1. logical-source domain string;
2. logical encoding and digest identifiers;
3. source ID;
4. exact authority namespace;
5. source profile and source-contract version;
6. authority-declaration ID and fingerprint;
7. full parent Repository Capture Snapshot identity tuple;
8. parent repository-capture fingerprint;
9. format, schema, parser, normalization, identity-scheme, semantic
   canonicalization, and semantic-fingerprint algorithm versions;
10. deterministically ordered attributed-member records and stable outcomes;
11. deterministically ordered normalized datum identities and semantic
    fingerprint references; and
12. deterministically ordered semantic provenance references required to
    reproduce the captured-to-normalized mapping.

The logical fingerprint is:

```text
"scenario-authority-logical-source-v1:"
  + lowercaseHex(SHA-256(encodedLogicalSourceFields))
```

The frozen ADR-012 qualification-context identity is not part of source content
because the same immutable source may be qualified under different contexts.
Qualification results bind the logical snapshot and exact frozen context in a
later accepted composite evidence snapshot. Changing qualification policy does
not mutate the source snapshot.

## Mutation-detection contract

Ordinary repository filesystems do not provide a portable atomic multi-file
read transaction. The baseline capture protocol detects observable instability
and refuses partial acceptance:

```text
discovery D1
  -> open each D1 member without following links
  -> record pre-read attributes
  -> capture exact bytes and compute raw length/fingerprint
  -> record post-read attributes
  -> discovery D2
  -> compare D1 and D2 membership and unsupported entries exactly
  -> publish complete stable capture or fail atomically
```

### Required checks

1. **Discovery D1** records the complete ordered members and unsupported
   matching entries under the exact discovery profile.
2. Each member is opened with no-follow semantics and verified as a regular
   non-symbolic-link entry.
3. Pre-read attributes record, when the filesystem supplies them, entry type,
   file key/identity, byte size, and last-modified value.
4. Exact bytes are read from the opened member, retained, counted, and hashed
   with `sha-256-v1`.
5. Post-read attributes are collected and compared with the pre-read values.
   Entry-type, file-key, size, or timestamp change is mutation evidence.
6. **Discovery D2** repeats the same membership and unsupported-entry
   discovery after all member reads.
7. D1 and D2 normalized member paths and unsupported-entry records must be
   exactly equal, including order, entry kind, and stable code.
8. Implementations may perform an additional full byte re-read/hash pass or
   use a stronger immutable filesystem facility. If performed by the selected
   mutation-detection version, every repeated byte length and fingerprint must
   equal the captured result.

Any observed addition, removal, rename, type change, symlink substitution,
replacement, read failure, attribute instability, byte instability, or D1/D2
membership difference produces the deterministic processing failure:

```text
CONCURRENT_SOURCE_MUTATION
```

The failure retains stable diagnostics and capture-attempt provenance. It
produces no accepted Repository Capture Snapshot, no accepted logical snapshot,
and no partial member subset. A retry is a new capture attempt, not continuation
of the failed one.

### Atomicity limit

The baseline detects mutations visible through its observations but cannot
prove that an arbitrary ordinary filesystem never changed and reverted between
checks, nor that all files existed simultaneously at one atomic instant.
File-size and timestamp comparison alone is not a cryptographic guarantee.

Where a point-in-time repository view is required, the source adapter must
capture from a separately established immutable filesystem/storage snapshot or
equivalent transactional view and retain that facility's provenance. A Git
commit does not supply this guarantee for ADR-011 discovery because matching
ignored, untracked, and working-tree files remain members.

The mutation-detection version states whether the baseline attribute protocol,
a repeated-byte pass, or a stronger immutable-view mechanism was used.

## Provenance contract

Repository capture provenance retains, at minimum:

- source ID and source declaration reference;
- repository and discovery-profile references;
- capture attempt and successful snapshot ID;
- capture time;
- absolute root and operational locator when audit policy permits;
- Git revision, branch, remote, and worktree status when available;
- D1/D2 observations and mutation-detection version;
- byte custody activity and immutable custody references;
- exact member paths, lengths, and raw fingerprints;
- unsupported-entry observations; and
- capture tool/adapter version.

Logical-source provenance retains parent capture references, attributed member
references, parser/schema/normalization activities and versions, semantic
fingerprints, derivation parents, stable outcomes, and authority declaration
reference. Provenance forms the immutable captured/normalized DAG required by
ADR-006.

Operational metadata excluded from content fingerprints remains auditable
through provenance. Exclusion from content identity does not permit omission
when another ADR or custody policy requires retention.

## Algorithm and contract identifiers

The initial contract recognizes these exact identifiers:

| Purpose | Identifier |
| --- | --- |
| Generic source snapshot contract | `qaip-source-snapshot-contract-v1` |
| Scenario source profile | `qaip-scenario-authority-repository-json-v1` |
| Scenario manifest format | `qaip-scenario-authority-manifest-v1` |
| Repository discovery | `scenario-authority-repository-discovery-v1` |
| Path normalization | `scenario-authority-repository-path-v1` |
| Member ordering | `unicode-code-point-order-v1` |
| Raw member bytes | `scenario-authority-member-bytes-v1` |
| Digest and hex encoding | `sha-256-v1` |
| Mutation detection | `scenario-authority-capture-stability-v1` |
| Repository encoding | `scenario-authority-repository-capture-c14n-v1` |
| Repository fingerprint value | `scenario-authority-repository-capture-v1` |
| Logical-source encoding | `scenario-authority-logical-source-c14n-v1` |
| Logical fingerprint value | `scenario-authority-logical-source-v1` |
| Capture provenance | `scenario-authority-capture-provenance-v1` |

The exact manifest schema, parser, Scenario identity scheme, normalization, and
datum semantic-canonicalization identifiers must also be present in logical
snapshots. They remain separately versioned because changing one must not be
misrepresented as a change to raw byte hashing.

Unsupported identifiers are processing/compatibility failures. An
implementation never silently substitutes an algorithm, encoding, profile, or
newer/older version.

## Ownership

### Source adapter / Extractor

The source adapter, currently the Scenario Authority pipeline in
`qa-model-extractor`, owns:

- filesystem discovery under the source profile;
- safe no-follow member access;
- mutation detection and stable-capture retry boundary;
- exact byte capture and immutable byte custody;
- source-native parsing and source-specific validation;
- construction of untrusted Repository Capture Snapshot and Logical Source
  Authority Snapshot candidates; and
- captured-to-normalized provenance production.

It does not accept its own candidates as authoritative, grant capabilities, or
own the canonical fingerprint algorithm implementation.

### Evidence Governance

Evidence Governance owns:

- the snapshot contracts and public invariant definitions;
- canonical field encoding and fingerprint algorithms;
- algorithm and contract version vocabulary;
- integrity, anti-substitution, reference, and provenance validation;
- authority declaration and frozen qualification-context binding under
  ADR-012; and
- acceptance or rejection of snapshot candidates.

The closest current implementation precedent is `qa-impact-evidence-core`, but
this ADR does not force Scenario source contracts into an Impact-specific
package. A neutral shared evidence-contract boundary may be introduced by a
separate implementation design if required by dependency direction.

### Runtime, Coverage, and Explorer

Runtime, Coverage, and Explorer own no discovery, byte capture, snapshot
construction, canonical encoding, fingerprint computation, authority binding,
or source qualification. They consume accepted immutable results and must not
recalculate source identity.

## One authoritative fingerprint implementation

There must be exactly one authoritative implementation of each versioned
repository and logical-source canonical encoding and fingerprint algorithm.
Source adapters call that Evidence Governance implementation to construct or
verify candidates. They do not copy, fork, translate, or independently
reimplement fingerprint serialization.

Golden vectors must cover empty membership, Unicode paths, delimiter-like text,
raw binary bytes, unsupported entries, ordering, formatting-only changes,
member rename, rejected members, version changes, and semantic mutation. An
independent test implementation may verify published vectors, but it is not a
second production authority.

The existing `ManifestCanonicalizer` length-prefix technique is a reusable
implementation pattern, not the Scenario repository serialization itself. The
Simulation model canonicalizer and repository project-graph hash are not
reusable source-snapshot algorithms.

## Compatibility consequences

### ADR-006

This ADR specializes ADR-006 with concrete raw-capture and logical-source
snapshot layers, SHA-256 member integrity, versioned deterministic encodings,
provenance, replay, and anti-substitution behavior. The full snapshot identity
tuple and distinction between processing failure and qualification outcome are
preserved.

Exact source bytes satisfy the strongest raw-source audit mode. Normalized
datum fingerprints remain separate and continue to follow ADR-006 semantic
canonicalization obligations.

### ADR-011

ADR-011 discovery membership, no-follow behavior, ignored/untracked inclusion,
exact Unicode code-point ordering, malformed-member retention, multi-authority
grouping, duplicate semantics, and exact-byte obligations are unchanged.
This ADR resolves its snapshot/fingerprint and mutation-detection follow-up.

One repository capture may parent multiple logical authority snapshots. File
paths affect capture membership and provenance but do not replace
`authority + scenarioKey + identity-scheme version` Scenario identity.

### ADR-012

Logical snapshots bind authority declaration identity/fingerprint without
self-authorizing. Evidence Governance applies the exact frozen qualification
context after source snapshot construction. Source bytes do not create a trust
policy, capability, or accepted authority.

The qualification context remains distinct from source content. Requalifying
one logical snapshot under a new frozen context creates a new qualification
result, not a new source snapshot.

### Impact Evidence manifests

`EvidenceSnapshotRef` can reference an accepted snapshot using
`sourceId + snapshotId + contentFingerprint`. `FrozenEvidenceManifest` may bind
normalized evidence to that reference. Its manifest fingerprint remains a
separate normalized-evidence fingerprint and must not replace either source
snapshot fingerprint.

### Canonical Model and downstream capabilities

No Canonical Ontology change is required. Snapshot records and rejected source
members remain in the evidence/provenance layer rather than becoming canonical
nodes or relationships. Runtime, Coverage, Explorer, validation, and simulation
ownership remain unchanged.

### Future repository evidence profiles

Future BA/SA, OpenAPI, test, verification, or repository profiles may reuse the
generic two-layer pattern and `sha-256-v1` raw member contract. They require
their own explicit discovery, path, semantic normalization, logical encoding,
capability, and provenance versions. Similar files or shared repositories do
not make profiles interchangeable.

## Consequences

### Positive

- Exact repository source bytes are reproducible and auditable.
- Raw source differences cannot be erased by JSON normalization.
- Repository membership and authority-specific semantics remain distinct.
- Malformed and rejected files cannot disappear from capture history.
- File movement has correct capture effects without replacing Scenario
  identity.
- Git metadata cannot conceal ignored, untracked, or working-tree evidence.
- Observable concurrent mutation fails explicitly without partial acceptance.
- Fingerprint behavior is portable, domain-separated, versioned, and governed
  by one implementation.
- Existing immutable evidence references can point to the richer snapshot.

### Negative

- Exact-byte custody and duplicate capture/logical metadata require storage.
- Stable capture may require repeated filesystem reads and retries.
- Ordinary filesystems cannot guarantee a globally atomic observation.
- Two snapshot layers and multiple fingerprint domains increase contract
  complexity.
- Evidence Governance needs a reusable contract boundary callable by source
  adapters.
- Changing any fingerprint-relevant contract version changes the content
  fingerprint even when source bytes are unchanged.

## Rejected alternatives

### Canonical JSON as the only source fingerprint

Rejected because canonicalization can erase source-byte differences and cannot
prove which exact source was captured. Canonical semantic fingerprints remain
useful only alongside raw byte fingerprints.

### Raw byte digest includes the path

Rejected because it conflates content integrity with membership identity and
prevents comparison of identical content at different locations. Path and raw
digest are separately bound by the aggregate snapshot.

### Git commit as the repository snapshot

Rejected because ADR-011 includes matching ignored, untracked, staged,
unstaged, and working-tree files. A commit neither captures nor fingerprints
that filesystem membership.

### Absolute checkout path in the content fingerprint

Rejected because host placement is provenance, not semantic repository
content, and would prevent cross-machine replay.

### Ignore malformed or rejected members

Rejected because discovery precedes parsing and validation. Dropping them would
make the fingerprint depend on acceptance and hide captured evidence.

### Best-effort partial capture

Rejected because a missing or changing member would silently create a snapshot
of a set that discovery never observed. Concurrent mutation is a processing
failure for the whole attempt.

### Every adapter implements the serialization itself

Rejected because separate implementations would drift and create multiple
fingerprint truths. Evidence Governance owns one implementation per version.

### Reuse Simulation or project-graph fingerprints

Rejected because those algorithms bind different semantic objects and do not
retain raw repository membership, unsupported entries, or exact source bytes.

## Unresolved questions

- What concrete artifact or module exposes the neutral Evidence Governance
  snapshot contracts and authoritative fingerprint implementation?
- What exact immutable byte-custody implementation is used for embedded and
  externally retained members?
- Does the first implementation perform a mandatory second byte-read pass, or
  use the baseline pre/post attribute checks plus D2 discovery?
- Which operating systems/filesystems can provide stronger immutable-view
  capture and how is that capability represented in provenance?
- What stable snapshot-ID allocation mechanism identifies capture observations
  without treating content equivalence as observation identity?
- What are the exact version identifiers and canonical field encodings for
  Scenario manifest semantic, Scenario, Step, reference, relationship, and
  provenance fingerprints?
- How are malformed members attributed when only a safely parsed envelope can
  reveal authority, and which failures remain parent-capture-only?
- What retention, confidentiality, size, and access policies govern exact
  source bytes?
- Which public diagnostic contract exposes mutation attempts, rejected members,
  unsupported entries, custody failures, and fingerprint mismatches?
- Should a future generic repository-capture profile extract the raw layer from
  Scenario Authority so other repository sources can share the same domain?

These questions affect implementation and later profiles but do not change the
two-layer identity, raw byte, aggregate fingerprint, mutation-failure, or
ownership decisions made here.

## Conformance requirements

An implementation conforms only if:

- it creates distinct Repository Capture and Logical Source Authority snapshot
  layers;
- both layers use `sourceId + snapshotId + contentFingerprint` identity;
- equal content fingerprints never collapse distinct snapshot observations;
- every raw member fingerprint is `sha-256-v1` over exact unmodified bytes and
  uses lowercase hexadecimal;
- path is excluded from the raw byte digest and included in aggregate
  membership;
- exact source bytes remain embedded or resolvable from immutable
  integrity-bound custody;
- malformed, rejected, and otherwise invalid captured members remain in the
  repository capture;
- matching unsupported entries and stable codes participate in the repository
  fingerprint;
- normalized paths are unique and members use exact Unicode code-point order;
- repository and logical snapshots use separate domain-separated,
  length-prefixed, versioned encodings;
- absolute paths, timestamps, branches, Git revisions, human messages, and
  host/process metadata do not enter source content fingerprints;
- Git revision is retained only as provenance/effective context and never
  substitutes for captured filesystem content;
- the logical snapshot references one parent repository capture and does not
  rediscover or reinterpret raw membership;
- authority declaration binding never self-authorizes the source;
- D1, no-follow open, pre/post attributes, exact byte capture/hash, D2, and
  exact membership/unsupported comparison form the baseline capture protocol;
- observed mutation produces `CONCURRENT_SOURCE_MUTATION` and no partial
  accepted snapshot;
- the limits of non-transactional filesystem capture remain explicit;
- source adapters construct untrusted candidates while Evidence Governance owns
  contracts, canonical encoding, integrity validation, authority binding, and
  acceptance;
- Runtime, Coverage, and Explorer do not reconstruct or fingerprint source
  snapshots;
- one authoritative production implementation exists for each fingerprint
  encoding version; and
- unsupported algorithms or versions fail explicitly and are never silently
  substituted.

## Scope exclusions

This ADR does not implement or select:

- snapshot, custody, canonicalization, fingerprint, provenance, or mutation
  detection code;
- a concrete Evidence Governance module or persistence technology;
- Scenario qualification;
- Runtime, Coverage, or Explorer behavior;
- Canonical Ontology changes;
- a Git integration requirement;
- signing, encryption, access control, or source-retention policy; or
- public transport or API representations.

## References

- [ADR-006: Qualified Engineering Data and Provenance Contract](ADR-006-qualified-engineering-data-and-provenance-contract.md)
- [ADR-011: Scenario Authority Source Contract](ADR-011-scenario-authority-source-contract.md)
- [ADR-012: Qualified Source Authority and Capability Contract](ADR-012-qualified-source-authority-and-capability-contract.md)
- [Impact Evidence Qualified Data and Provenance Decision Analysis](../impact-evidence-qualified-data-provenance-decision-analysis.md)
