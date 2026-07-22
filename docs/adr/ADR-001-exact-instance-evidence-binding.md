# ADR-001: Exact-instance evidence binding

**Status:** Accepted for v0.1

## Context

Canonical Change is an in-process pipeline with no persistence, resume,
serialization, or distributed execution contract. Content-equivalent evidence
can still be stale or substituted relative to a particular verification run.

## Decision

Each later successful stage binds to the exact earlier evidence instance. Base
indexes, Base root context, materialization, reconstruction, complete validation,
and finalization preserve and check that provenance.

## Rationale

Identity checks cheaply prevent a caller from mixing separately extracted Base
data or substituting stale Phase 4/5 evidence. Independently reconstructed but
content-equivalent evidence is rejected because content equality does not prove
that it belongs to the same run.

## Consequences

The contract is deterministic and safe inside one process, but it cannot resume
after serialization or process restart. Java equality is provenance-sensitive
and is not a business semantic-equivalence API.

This is acceptable because v0.1 has no persistence or distribution boundary.

## Future trigger

Persistence, messaging, distributed execution, or restart/resume requires a
stable provenance mechanism such as a canonical fingerprint, persisted evidence
ID, content hash, or signed provenance token. This ADR does not choose or
implement that future mechanism.

## Alternatives considered

- **Value equality:** rejected because it cannot prove run provenance.
- **Canonical fingerprint:** deferred until a cross-process contract exists.
- **Persisted evidence ID:** deferred because there is no persistence contract.
- **No binding:** rejected because stale/substituted evidence could advance.
