# QAIP Module Responsibilities

The repository contains the following Gradle projects. “Forbidden” identifies
an ownership boundary, not necessarily a compile-time enforcement mechanism.

## Module summary

| Module | Kind | Responsibility | Main input → output / entry point | Direct project dependencies |
|---|---|---|---|---|
| `qa-model` | Pure Java contract | Normalized node and relationship types plus JSON Schema resource | Model enums and schema | None |
| `qa-model-validation-core` | Pure Java core | JSON Schema and semantic graph validation | `JsonNode` → `QaModelValidationResult`; `QaModelValidationEngine` | None |
| `qa-coverage-engine` | Spring Boot + domain | Structural coverage metrics and problems | `JsonNode` → `CoverageReport`; `CoverageService` | `qa-model-validation-core` |
| `qa-findings-engine` | Java library | Convert coverage problems into actionable findings | `CoverageReport` → `FindingsReport`; `FindingsService` | `qa-coverage-engine`, `qa-model-validation-core` |
| `qa-roadmap-engine` | Pure Java domain | Convert supported findings into remediation tasks | `FindingsReport` → `RoadmapReport`; `RoadmapService` | `qa-findings-engine` |
| `qa-execution-planner` | Pure Java domain | Validate dependencies and create execution waves | `RoadmapReport` → `ExecutionPlan`; `ExecutionPlanner` | `qa-roadmap-engine` |
| `qa-impact-analysis` | Pure Java domain | Describe deterministic expected structural task impact | `RoadmapReport + ExecutionPlan` → `ImpactReport`; `ImpactAnalyzer` | `qa-execution-planner`, `qa-roadmap-engine`, `qa-findings-engine`, `qa-model` |
| `qa-model-validator` | Spring Boot REST/application | Registered-model storage, validation, trace, and API orchestration | `QaModelController`, registered-model services | model, validation, coverage, findings, roadmap, execution modules |
| `qa-model-extractor` | Spring Boot REST/application | Deterministic Story Input to normalized QA-model extraction and validation | `QaModelExtractionService`, extraction controllers | `qa-model`, `qa-model-validation-core` |
| `qa-story-analyzer` | Spring Boot REST/application | LLM-backed conversion of story text to structured Story Input | `StoryAnalyzer`, `StoryAnalysisController` | `qa-model-extractor` |
| `qaip-core` | Pure Java platform contracts | Generic analysis engine, assessment, finding, metric, and metadata contracts | `AnalysisEngine` and registry contracts | None |
| `qaip-analysis` | Java orchestration library | Execute registered generic analysis engines and adapt validation/coverage | `UnifiedAnalysisService`, `AnalysisOrchestrator`, `AnalysisExecutor` | `qaip-core`, `qa-coverage-engine`, `qa-model-validation-core` |

## Ownership boundaries

### `qa-model`

- Owns normalized QA model vocabulary and schema resources.
- May be consumed by validation, extraction, APIs, and Impact Analysis.
- Must not own validation results, analysis, remediation, or orchestration.

### `qa-model-validation-core`

- Owns schema-validity and semantic-validity rules.
- Produces `QaModelValidationResult` through `QaModelValidationEngine`.
- Executes semantic checks through the framework-independent
  `SemanticValidationEngine` and explicitly ordered `KnowledgeRule` instances;
  see [Semantic Validation Core](semantic-validation-core.md).
- Must not calculate coverage, findings, remediation, execution waves, or
  expected impact.

### `qa-coverage-engine`

- Owns coverage metric, coverage problem, and coverage summary semantics.
- `CoverageService` validates a model and produces `CoverageReport`.
- Also exposes standalone Spring REST endpoints, so it is not a pure domain
  module despite being consumed as an engine library elsewhere.
- Must not create Findings, remediation tasks, execution waves, or projected
  future coverage.

### `qa-findings-engine`

- Owns mapping from `CoverageProblem` to `Finding`, ordering, severity, and
  `FindingsSummary` semantics.
- Supports both `analyze(JsonNode)` and reuse-oriented
  `analyze(CoverageReport)`.
- Must not plan remediation or recalculate Coverage semantics.

### `qa-roadmap-engine`

- Owns supported finding-to-task mapping, deterministic task IDs, task order,
  and `RoadmapSummary`.
- Current tasks are `PLANNED` and Roadmap does not infer dependencies.
- Must not score Assessment, execute tasks, or calculate execution waves.

### `qa-execution-planner`

- Owns dependency validation, deterministic topological wave assignment, and
  every `ExecutionPlanSummary` semantic.
- Consumes explicit `RemediationTask.dependsOn`; it does not infer dependencies
  from task type.
- Must not regenerate Roadmap tasks, inspect the QA model, or calculate impact.

### `qa-impact-analysis`

- Owns the impact catalog, task-to-impact mapping, boundary consistency, and
  conditional expected structural-change semantics.
- Uses roadmap task data and planner-owned wave assignments and
  `parallelizableTasks`.
- Must not recalculate planner summary semantics, simulate a future model,
  project coverage, prioritize work, or claim verified resolution.

### `qa-model-validator`

- Owns the registered-model REST/application boundary and thread-safe
  in-memory repository.
- `RegisteredModelAssessmentService` separately composes Coverage and Findings
  into a REST assessment response.
- `RegisteredModelRoadmapService` composes Coverage → Findings → Roadmap.
- `RegisteredModelExecutionPlanService` extends that request-local flow with
  Execution Planner.
- It does not currently depend on or expose `qa-impact-analysis`.

### `qa-model-extractor`

- Owns deterministic mapping and validation of Story Input documents into QA
  models and exposes extraction REST endpoints.
- Must not own downstream coverage/remediation semantics.

### `qa-story-analyzer`

- Owns the separate LLM-backed Story Input analysis boundary.
- Depends on the extractor module and exposes `/api/v1/story/analyze`.
- Must not be treated as part of deterministic Impact Analysis.

### `qaip-core`

- Owns generic platform-level analysis contracts and registry infrastructure.
- Must remain independent of concrete analysis engines.

### `qaip-analysis`

- Owns generic engine execution and unified analysis aggregation for current
  validation and coverage adapters.
- This is distinct from `RegisteredModelAssessmentService` and does not
  currently orchestrate Roadmap, Execution Planner, or Impact Analysis.
- Must not absorb domain ownership from the engines it orchestrates.
