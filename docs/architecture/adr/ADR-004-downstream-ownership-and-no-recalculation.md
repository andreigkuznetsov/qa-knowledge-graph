# ADR-004: Downstream ownership and no recalculation

- Status: Accepted
- Date: 2026-07-19
- Decision owners: QAIP architecture

## Context

Downstream capabilities need confidence that reports belong to the same
request, but recalculating upstream algorithms creates competing definitions.
Report schema versions also describe distinct contracts.

## Decision

Each module owns its derived semantics. Downstream modules consume those
results and validate only boundary assumptions. Execution Planner owns wave
assignment and `ExecutionPlanSummary`; Impact Analysis preserves waves and
reads planner-owned summary values without recomputing them. Schema versions of
different report types are independent and need not be equal.

## Rationale

Boundary consistency asks whether task IDs are known, unique, assigned once,
and tied to the expected source report. Duplicated semantic computation asks a
downstream module to independently reproduce an upstream algorithm. The former
protects a handoff; the latter creates semantic drift.

## Consequences

Impact Analysis validates Roadmap/Execution task correspondence, wave-number
shape, source Roadmap summary, and catalog semantics. It does not recalculate
topological ordering or the execution summary. Cross-contract compatibility is
not inferred from schema-version string equality.

## Rejected alternatives

- Recomputing every upstream summary at every boundary.
- Requiring all report contracts to share one version string.
- Trusting unknown or duplicate task assignments without validation.

## Follow-up

New boundary checks must identify the owning semantic before implementation.
