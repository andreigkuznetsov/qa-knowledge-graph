# QAIP Pipeline Contracts

## Contract handoffs

| Producer | Output | Consumer | Fields relied upon | Recalculation | Current registered-model API |
|---|---|---|---|---|---|
| Validation Core | `QaModelValidationResult` | `CoverageService`, validation API | `valid`, `schemaVersion`, summary and issues | Coverage invokes validation for a raw model; it does not reinterpret validity | `POST /api/v1/qa-model/validate` and embedded validation responses |
| Coverage | `CoverageReport` | `FindingsService`, registered-model mappers/services | `analyzed`, `schemaVersion`, metrics, problems, validation | Findings maps existing problems and reuses validation | `GET /api/v1/models/{modelId}/coverage` |
| Findings | `FindingsReport` | Assessment application service, `RoadmapService` | `analyzed`, `schemaVersion`, summary, findings, validation | Roadmap maps findings; Assessment maps the same report into its own response | `GET .../findings`; Assessment and Roadmap endpoints consume it independently |
| Assessment application capability | `RegisteredModelAssessmentResponse` | REST client | health, summary, coverage, findings, validation | It composes current evidence; it is not Roadmap input | `GET /api/v1/models/{modelId}/assessment` |
| Roadmap | `RoadmapReport` | `ExecutionPlanner`, Roadmap REST mapper, Impact Analysis | `planned`, schema version, summary, tasks, source findings summary | Planner does not regenerate tasks | `GET /api/v1/models/{modelId}/roadmap` |
| Execution Planner | `ExecutionPlan` | Execution Plan REST mapper, Impact Analysis | `planned`, contract version, summary, waves, source roadmap summary | Impact consumes planner summary semantics and does not recalculate them | `GET /api/v1/models/{modelId}/execution-plan` |
| Impact Analysis | `ImpactReport` | Domain client only | analyzed flag, contract version, impact summary, ordered task impacts | Final capability in MVP 0.8 | No REST endpoint |

## Validation → Coverage

`CoverageService.analyze(JsonNode)` invokes `QaModelValidationEngine`. Invalid
models produce an unanalyzed `CoverageReport` carrying the validation result.
For registered-model requests, the application layer turns this state into the
existing 422 validation response.

## Coverage → Findings

`FindingsService.analyze(CoverageReport)` is the reuse contract. It relies on
coverage problems and preserves the report's validation. Registered Roadmap,
Assessment, and Execution Plan services use this overload so Coverage runs
once per request. The alternative `analyze(JsonNode)` overload is useful as a
standalone entry point but performs Coverage internally.

## Findings → Assessment and Roadmap

Assessment and Roadmap are siblings, not a serial handoff:

- `RegisteredModelAssessmentService` derives API health and summary from
  Coverage, Findings, and validation evidence.
- `RoadmapService` consumes `FindingsReport.findings`, validates supported
  target node types, and creates deterministic remediation tasks.

Assessment health or finding severity is not converted into remediation
priority. No `AssessmentReport` domain type exists in this pipeline; the actual
assessment contract is `RegisteredModelAssessmentResponse` in the REST module.

## Roadmap → Execution Planner

`ExecutionPlanner` relies on stable task IDs, task type, target node ID, and
explicit `dependsOn` lists. It rejects duplicate IDs, unknown dependencies,
self-dependencies, and cycles. It owns:

- deterministic earliest-valid wave grouping;
- one-based contiguous wave numbers;
- deterministic task order within waves;
- all `ExecutionPlanSummary` fields.

`ExecutionPlan.sourceRoadmapSummary` preserves the source summary. Consumers
must not rerun topological sorting or reinterpret planner summary fields.

## Roadmap + Execution Plan → Impact Analysis

`ImpactAnalyzer` needs both reports. `FindingsReport` is unnecessary because a
`RemediationTask` already carries its `sourceFindingCode`, task type, target
node identity/type, and dependencies.

Impact Analysis traverses task IDs in execution-wave order, verifies that each
roadmap task is assigned exactly once, rejects unknown assignments, checks the
source Roadmap summary and impact-catalog semantics, and preserves planner wave
assignments. It reads `ExecutionPlanSummary.parallelizableTasks` without
recalculating execution-summary semantics.

Each `TaskImpact` describes a structural change expected after valid
completion. It does not assert that the finding is resolved, mutate the model,
invent future node IDs, or project future coverage.

## Registered-model orchestration

The `qa-model-validator` API loads a defensive copy from
`InMemoryQaModelRepository` for each request. Coverage, Findings, Roadmap, and
Execution Plan endpoint services invoke the required domain components
directly. The API does not call its own HTTP endpoints. Impact Analysis is not
yet part of this application orchestration.

## Contract evolution rules

- A schema-version field belongs to its own report contract.
- Different report types are not required to have identical schema-version
  strings.
- Compatibility across unrelated contracts must not be inferred from version
  string equality.
- Public contract changes require explicit architecture and compatibility
  review.
- Downstream modules must fail explicitly when a new enum value lacks owned
  semantics; silent generic fallbacks are forbidden.
- Downstream boundary validation may verify identity, assignment, or structural
  consistency, but must not duplicate semantic computation owned upstream.
