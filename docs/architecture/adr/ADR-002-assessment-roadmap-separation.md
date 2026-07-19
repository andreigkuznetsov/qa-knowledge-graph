# ADR-002: Assessment and Roadmap separation

- Status: Accepted
- Date: 2026-07-19
- Decision owners: QAIP architecture

## Context

Assessment summarizes current validation, coverage, findings, and API health.
Roadmap converts supported findings into remediation tasks. They use related
evidence but answer different questions.

## Decision

Keep Assessment and Roadmap semantically separate. The current assessment is
an application-level `RegisteredModelAssessmentResponse`; it is not an input to
`RoadmapService`. Roadmap consumes `FindingsReport` directly.

## Rationale

Quality state is not an execution order. Keeping the capabilities separate
prevents health status or finding severity from silently becoming remediation
priority.

## Consequences

Registered-model Assessment and Roadmap requests independently orchestrate the
upstream evidence they require. Their responses may evolve independently.

## Rejected alternatives

- Passing the assessment REST response into Roadmap.
- Deriving task priority directly from assessment health or severity.
- Embedding Roadmap tasks in the Assessment response without explicit review.

## Follow-up

Any future combined API must preserve the two ownership boundaries and avoid
duplicate upstream computation within one request.
