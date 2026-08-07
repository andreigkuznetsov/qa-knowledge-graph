# ADR-007: Runtime-owned Operation Overview aggregation

- Status: Accepted
- Date: 2026-08-07
- Decision owners: QAIP architecture

## Context

Explorer 0.5 requires one Operation Overview containing operation identity,
available implementation information, an event path when applicable,
verification status, and qualified tests and checks. Runtime already owns the
specialized Operation Details, Event Path, and Operation Tests queries. Those
queries interpret the project graph, resolve engineering paths, qualify tests
and checks, and derive verification semantics. Explorer currently adapts their
typed results into presentation models and HTTP responses.

An overview must preserve partial knowledge. An incomplete implementation path,
a non-event-driven operation, or unavailable verification evidence must not
hide the operation or invalidate independent sections. Overview values must
remain equal to values produced by the specialized Runtime queries, and the
existing specialized queries and Explorer endpoints must remain valid.

## Decision

Runtime owns Operation Overview aggregation. Explorer owns presentation of the
Runtime result only.

The architectural principle is:

> Runtime owns cross-capability engineering interpretation.
>
> Explorer owns presentation of that interpretation.

The overview is resolved from one Runtime project snapshot. Runtime reuses the
existing engineering semantics behind Operation Details, Event Path, and
Operation Tests; it must not copy or independently recalculate their traversal,
qualification, ordering, count, or verification rules. Explorer must not
traverse the graph, qualify evidence, derive verification, or reconcile
conflicting specialized results.

Project absence and operation absence are top-level query failures. When the
operation exists, the overview exists:

> Operation Overview exists if the operation exists. Missing engineering
> knowledge changes section state, not overview existence.

Implementation, event path, and verification are independent sections with
explicit states. Unavailable knowledge in one section does not invalidate any
other section. In particular:

- an incomplete or ambiguous implementation changes only the implementation
  section state;
- `NOT_EVENT_DRIVEN` from the specialized Event Path semantics becomes the
  non-fatal `NOT_APPLICABLE` state in the overview event-path section;
- an incomplete or ambiguous event path changes only the event-path section
  state;
- no qualified tests is available verification knowledge represented as
  `UNVERIFIED`, `0` tests, `0` checks, and an empty qualified-test collection;
- ambiguous verification evidence changes only the verification section state.

The Runtime Operation Overview query is additive. Existing Runtime specialized
queries and existing Explorer endpoints retain their contracts and behavior.

## Rationale

The overview combines engineering interpretations, not merely presentation
fields. Runtime already owns the project snapshot, graph access, path
resolution, evidence qualification, deterministic ordering, and verification
semantics. Keeping their composition in Runtime provides one reusable contract
for Explorer and future non-HTTP consumers.

A single project snapshot prevents an overview from mixing sections computed
from different project states. Reusing the same internal resolvers as the
specialized queries makes semantic equality enforceable without introducing a
second source of truth.

This decision is consistent with
[ADR-004](ADR-004-downstream-ownership-and-no-recalculation.md): each owning
capability retains its derived semantics, while aggregation consumes those
semantics without recalculating them. Explorer is downstream of Runtime and
validates or maps only presentation-boundary concerns.

## Consequences

- Runtime exposes an additive Operation Overview query and typed result.
- Runtime must acquire one project snapshot before resolving overview sections.
- Shared internal resolvers may be extracted so overview and specialized
  queries use identical semantics without duplication.
- Section contracts represent availability, non-applicability, incompleteness,
  and ambiguity explicitly rather than with null values or whole-overview
  failure.
- Explorer maps Runtime section states to view models and HTTP representation;
  it does not reinterpret them.
- Deterministic ordering and values are inherited from Runtime-owned semantics.
- New engineering sections should follow the same ownership and independent
  section-state model.
- Existing specialized Runtime queries and Explorer endpoints remain
  unchanged.

## Rejected alternatives

- **Explorer-owned aggregation.** Rejected because Explorer would own the
  cross-query failure matrix and engineering-aware rules, require multiple
  Runtime calls, risk mixed project states, and force future non-HTTP consumers
  to duplicate orchestration.
- **A new shared orchestration module.** Rejected because all participating
  semantics and project access already belong to Runtime. A separate module
  would add a boundary and dependency surface without introducing an
  independent responsibility.

## Follow-up

Define the Runtime Operation Overview result contract and section states before
implementation. Contract tests must demonstrate equality with specialized
query values, independence of section failures, deterministic output, and use
of one project snapshot.
