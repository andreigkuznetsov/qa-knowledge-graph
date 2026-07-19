# ADR-001: Modular deterministic pipeline

- Status: Accepted
- Date: 2026-07-19
- Decision owners: QAIP architecture

## Context

QAIP derives validation, structural evidence, remediation tasks, execution
waves, and expected impact. Combining these semantics in one service would
couple releases, obscure ownership, and make deterministic behavior harder to
test.

## Decision

Keep capabilities in separately owned modules and exchange immutable reports.
Dependencies point downstream to upstream contracts. Deterministic ordering
and identifiers are part of each owning module's contract.

## Rationale

Separate modules make semantic ownership explicit, allow pure Java testing,
and prevent framework or API concerns from entering Roadmap, Execution, and
Impact domains.

## Consequences

Consumers sometimes need direct Gradle dependencies because non-transitive
module declarations expose contract types explicitly. More boundary validation
is required, but individual algorithms remain small and testable.

## Rejected alternatives

- One monolithic analysis service.
- Internal HTTP calls between capability endpoints.
- A generic workflow engine or event bus for the current synchronous pipeline.

## Follow-up

Review dependency direction whenever a new capability or public report is
introduced.
