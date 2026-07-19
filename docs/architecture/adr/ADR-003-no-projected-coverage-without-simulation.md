# ADR-003: No projected coverage without simulation

- Status: Accepted
- Date: 2026-07-19
- Decision owners: QAIP architecture

## Context

A remediation task records intended work. It does not provide the identity,
content, validity, or relationships of a completed future QA model.

## Decision

Do not publish projected coverage from Roadmap, Execution Planner, or Impact
Analysis. MVP 0.8 reports only deterministic expected structural change and a
conditional resolution expectation.

## Rationale

Coverage is computed from an actual validated graph. A task count cannot prove
that future nodes and relationships will be created correctly. Coverage delta
requires an explicitly defined future-model simulation followed by validation
and re-analysis.

## Consequences

Impact reports contain no coverage-before, coverage-after, or coverage-delta
fields. Actual finding resolution requires valid task execution, an updated
model, and subsequent Validation, Coverage, and Findings analysis.

## Rejected alternatives

- Direct arithmetic coverage projection from task counts.
- Treating every planned task as a resolved finding.
- Creating implicit virtual nodes or relationships inside Impact Analysis.

## Follow-up

Define simulation semantics and validation boundaries before considering any
future projected-coverage contract.
