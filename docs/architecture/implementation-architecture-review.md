# Implementation Architecture Recovery Review

**System:** QA Knowledge Graph / QA Intelligence Platform (QAIP)  
**Review type:** as-built architecture recovery  
**Evidence baseline:** repository implementation at review time (Java 21, Gradle multi-project build)  
**Audience:** Architecture Review Board  

> This review describes the implementation that exists. Repository vision documents are context only; source code, build dependencies, schemas, and tests are the authority. “Domain” below therefore means concepts actually encoded in types or validation rules, not concepts that would be desirable in a future design.

## 1. Executive summary

This repository implements a deterministic QA knowledge-model platform. Its central input is a normalized, JSON-based Canonical QA Model v0.1 containing sources, typed nodes, and typed relationships. The platform validates that model, registers valid snapshots, traces paths through them, measures rule/scenario/check coverage, derives findings and remediation roadmaps, schedules remediation into dependency-safe waves, estimates expected structural impact, and simulates materialized future models. Adjacent capabilities convert structured story input into the canonical model, analyze free-form stories through an LLM port, verify declared canonical changes, and expose common analysis-engine contracts.

The problem solved is not generic graph storage. The implementation turns a QA artifact graph into reproducible engineering evidence: whether its JSON shape and cross-reference semantics are valid; whether business rules, scenarios, tests, and checks are connected; what gaps exist; what remediation tasks follow; in what order those tasks can run; and whether an explicitly materialized candidate remains valid. Evidence: the authoritative pipeline is `QaModelValidationEngine`; coverage is calculated by `CoverageService` and three `CoverageAnalyzer` implementations; downstream transformations are `FindingsService`, `RoadmapService`, `ExecutionPlanner`, `ImpactAnalyzer`, and `ModelSimulationEngine`.

The primary architectural style is a **modular monolith/source monorepo with a functional-core-and-adapter-shell structure**. Four independent Spring Boot applications provide REST adapters (`qa-model-validator`, `qa-model-extractor`, `qa-coverage-engine`, `qa-model-simulation-api`); `qa-story-analyzer` is a fifth Boot application with an LLM adapter. Framework-independent `java-library` modules hold most rules and result contracts. Within applications, the prevailing layering is controller → application service/facade → engine/domain service → immutable result records. Dependencies flow from transport/orchestration toward narrow deterministic libraries. The Gradle project graph is acyclic.

Major subsystems are:

- canonical contracts and schema (`qa-model`, `qa-model-validation-core`);
- model registration, tracing, and composed REST analysis (`qa-model-validator`);
- deterministic extraction (`qa-model-extractor`) and LLM-assisted story analysis (`qa-story-analyzer`);
- coverage and traceability (`qa-coverage-engine`);
- findings, remediation roadmaps, execution planning, and impact (`qa-findings-engine`, `qa-roadmap-engine`, `qa-execution-planner`, `qa-impact-analysis`);
- future-model simulation (`qa-model-simulation`, `qa-model-simulation-api`);
- staged canonical change verification (`qa-model-change`);
- generic analysis SPI and orchestration (`qaip-core`, `qaip-analysis`).

Maturity is **mixed**. The deterministic cores are well beyond a sketch: they use immutable records/sealed outcomes, explicit error codes, stable sorting, framework-independent unit tests, smoke/integration tests, and a released `qa-model-change` capability. Operationally, the platform is pre-production/early product: version is `0.1.0-SNAPSHOT`; registered models live only in an in-memory repository; APIs are spread across separately launchable Boot modules without an assembled deployment; error formats are inconsistent; no security, durable storage, migrations, telemetry, or domain-event integration is implemented. The honest assessment is a mature deterministic domain kernel surrounded by prototype-grade runtime operations.

## 2. Architecture overview

### 2.1 Layers and boundaries

| Layer | Concrete implementation | Responsibility |
|---|---|---|
| Transport | `*Controller`, `*ExceptionHandler`, API response records in Boot modules | HTTP mapping, request binding, status/error translation, transport DTO mapping |
| Application orchestration | `QaModelRegistrationService`, `RegisteredModel*Service`, `ExtractAndValidateService`, `SimulationFacade`, `AnalysisOrchestrator` | Sequence use cases and compose engines; no canonical rule ownership |
| Domain/analysis core | `QaModelValidationEngine`, `KnowledgeRule`, coverage analyzers, `FindingsService`, `RoadmapService`, `ExecutionPlanner`, `ImpactAnalyzer`, `ModelSimulationEngine`, change-verification stages | Deterministic decisions and immutable evidence/results |
| Ports/adapters | `AnalysisEngine`, `StoryAnalyzer`, `LlmClient`, `CoverageAnalyzer`, mappers | Replaceable execution or integration boundaries |
| Infrastructure | `InMemoryQaModelRepository`, Spring `@Configuration`, NetworkNT JSON Schema, Jackson | Storage, wiring, serialization/schema technology |
| Contract/resources | QA schema JSON, `qa-model`/validation result records and enums | Versioned external shape and shared vocabulary |

The strongest boundary is the Gradle module. Library modules deliberately avoid Spring; Boot modules wire them. The weaker boundary is the model representation: `JsonNode` crosses transport, repository, validation, coverage, trace, simulation, and change boundaries, so schema conformance—not Java type encapsulation—is the principal canonical-model boundary.

### 2.2 Module responsibilities and dependencies

| Module | Owns | Direct internal dependencies |
|---|---|---|
| `qa-model` | shared QA enums/validation DTOs and canonical schema resource | none |
| `qa-model-validation-core` | authoritative schema + semantic validation | none (external Jackson/NetworkNT only) |
| `qa-coverage-engine` | three coverage metrics and two traceability views; standalone REST app | validation core |
| `qa-findings-engine` | deterministic conversion of coverage problems to actionable findings | coverage, validation core |
| `qa-roadmap-engine` | finding-to-remediation-task mapping | findings |
| `qa-execution-planner` | topological execution waves and dependency errors | roadmap |
| `qa-impact-analysis` | expected structural effect of remediation tasks | execution planner, roadmap, findings, model |
| `qa-model-simulation` | base validation/fingerprinting, explicit candidate materialization, final validation | impact, validation core, model |
| `qa-model-simulation-api` | simulation HTTP adapter | simulation (findings/roadmap are runtime type providers) |
| `qa-model-change` | staged verification of declared canonical changes and retained evidence | model, validation core |
| `qa-model-validator` | validation/registration/trace/composed-analysis REST service and volatile repository | model, validation core, coverage, findings, roadmap, execution planner |
| `qa-model-extractor` | Story Input schema validation and deterministic canonical mapping; REST app | model, validation core |
| `qa-story-analyzer` | story-analysis HTTP use case, prompt construction, LLM port | extractor |
| `qaip-core` | generic analysis SPI, common assessment/finding/metric/metadata contracts | none |
| `qaip-analysis` | registry/executor/orchestrator plus validation and coverage adapters | core, coverage, validation core |

Build evidence is in each module's `build.gradle`; inclusion evidence is [`settings.gradle`](../../settings.gradle). The intended pipeline is materially visible in `RegisteredModelExecutionPlanService`: repository → coverage → findings → roadmap → execution plan.

```text
HTTP / LLM adapters
  qa-model-validator ───────────────┬─> qa-execution-planner -> qa-roadmap-engine
  qa-model-extractor -> qa-model    │                            -> qa-findings-engine
        └────────────> validation-core                           -> qa-coverage-engine
  qa-story-analyzer -> extractor    └────────────────────────────> validation-core
  qa-model-simulation-api -> qa-model-simulation -> qa-impact-analysis
                                                      -> execution-planner/roadmap/findings/model
  qaip-analysis -> qaip-core + coverage + validation-core
  qa-model-change -> qa-model + validation-core

All arrows point from consumers/orchestrators to providers. No provider imports a consumer.
```

There are **no Gradle module cycles** in the declared project dependencies. Runtime-only findings/roadmap dependencies in `qa-model-simulation-api` satisfy classes exposed transitively through simulation contracts but do not reverse an edge. Package-level imports follow the same direction in the inspected implementation. The execution planner detects cycles in *roadmap task data* (`CyclicTaskDependencyException`); that is a domain validation feature, not an architectural dependency cycle.

### 2.3 Deployable units

`QaModelValidatorApplication`, `QaModelExtractorApplication`, `QaCoverageEngineApplication`, `SimulationApiApplication`, and `QaStoryAnalyzerApplication` each define a Spring Boot entry point. There is no code-level gateway, service discovery, message bus, or inter-service client among them. They are separately launchable adapters over shared in-process libraries, not implemented distributed microservices.

## 3. Domain model recovery

### 3.1 Canonical QA graph

The effective aggregate submitted to validation and analysis is a JSON document with `schemaVersion`, `project`, `sources`, `nodes`, and `relationships`, defined by `qa-model-v0.1.schema.json` and consumed as `JsonNode`. It behaves as an aggregate because validation and analysis operate on a whole snapshot and enforce cross-element invariants. It is **not** implemented as a Java `QaModel` entity, and its nodes are not Java entity classes; claiming otherwise would exceed the code.

Canonical identity is value-based in JSON (`id`) and in the change subsystem (`CanonicalIdentity`). `NodeType` and `RelationshipType` are enums in both `qa-model` and validation core. Semantic rules reveal the graph vocabulary and invariants: duplicate node/relationship IDs, unknown endpoints/sources, illegal relationship pairs, self-reference, duplicate edges, test-step order, and missing operation/story/rule/scenario/test/check links.

### 3.2 Entities and aggregate roots

| Classification | Objects | Why |
|---|---|---|
| Snapshot aggregate | canonical QA JSON document | schema/version boundary; rules inspect identity and relationships across the entire document |
| Registered-model entity | private `RegisteredModel` plus `ModelDescriptor` in `InMemoryQaModelRepository` | generated stable `modelId`, creation time, stored snapshot, lifecycle through save/find/list |
| Declared-change aggregate | `DeclaredChangeSet` containing ordered `DeclaredChange` values | change-set-wide ambiguity/intrinsic/base/aggregate/materialization verification |
| Proposed-model aggregate | `ProposedArtifactModel`, reconstructed root and retained evidence types | atomic candidate state across nodes and relationships before complete validation |
| Roadmap aggregate/result | `RoadmapReport` with `RemediationTask` values | task identities and dependencies are checked as a set by `ExecutionPlanner` |
| Simulation aggregate/result | `SimulationResult` | binds base fingerprint, future fingerprint, candidate model, applied materializations, and validation evidence |

`RemediationTask` is entity-like inside a roadmap because `taskId` is used for identity and dependencies. `TraceNode`, `TraceRelationship`, findings, coverage problems, and materializations are evidence/result values rather than independently stored entities.

### 3.3 Value objects

The code strongly favors immutable records. Important value objects include:

- validation: `ValidationIssue`, `ValidationSummary`, `QaModelValidationResult`;
- extraction: `ExtractionIssue`, `ExtractionSummary`, `QaModelExtractionResponse`, `ExtractAndValidateResponse`;
- coverage/traceability: `CoverageMetric`, `CoverageProblem`, `CoverageSummary`, `CoverageReport`, `TracePath`, `TraceabilityChain`, `TraceabilityChainReport`, `TraceabilityCoverageReport`;
- planning: `Finding`, `FindingsSummary`, `FindingsReport`, `RoadmapSummary`, `RoadmapReport`, `ExecutionWave`, `ExecutionPlanSummary`, `ExecutionPlan`;
- impact/simulation: `StructuralGap`, `ExpectedStructuralChange`, `TaskImpact`, `ImpactSummary`, `ImpactReport`, `TaskMaterialization`, `TaskMaterializationSet`, `AppliedMaterialization`, `SimulationResult`;
- change: `CanonicalIdentity` and `CanonicalQaModelVersion` (constructor-enforced scalar values), `DeclaredChange`, diagnostic records, and sealed success/failure evidence;
- generic analysis: `AnalysisContext`, `AnalysisAssessment`, `AnalysisMetric`, `AnalysisFinding`, `AnalysisMetadata`, `QualityAssessment`.

These belong to the value category because equality is represented by their state, they are immutable, and no repository gives them an independent lifecycle. Some “Report” values carry timestamps; this makes repeatability depend on injected clocks or excludes time from semantic comparison, but does not make them entities.

### 3.4 Domain services

Rules and transformations live in stateless services rather than the JSON aggregate: `QaModelValidationEngine`, `SemanticValidationEngine`, all `KnowledgeRule` implementations, `TraceEngine`, `CoverageService`/`CoverageAnalyzer`, `FindingsService`, `RoadmapService`, `ExecutionPlanner`, `ImpactAnalyzer`, `ModelSimulationEngine`, and the validators/materializers/verifiers under `qa-model-change`. They are domain services because each owns a decision spanning several values and has no persistent identity.

`QaModelRegistrationService`, `RegisteredModel*Service`, `ExtractAndValidateService`, `SimulationFacade`, and `AnalysisOrchestrator` are application services: they coordinate domain services and repositories rather than define the low-level rules.

### 3.5 Enums

The principal enum families are:

- graph/validation: `NodeType`, `RelationshipType`, `ValidationLayer`, `ValidationSeverity`;
- coverage/findings: `CoverageMetricCode`, `CoverageProblemType`, `CoverageSeverity`, `FindingCode`, `FindingSeverity`;
- remediation/planning/impact: `RemediationTaskType`, `RemediationTaskStatus`, `ImpactChangeType`, `ResolutionExpectation`, `RelationEndpointRole`, `ImpactAnalysisErrorCode`;
- change verification: `ArtifactCategory`, `ChangeKind`, failure/diagnostic/stage/classification enums in `change.validation`, `base`, `materialization`, `aggregate`, `root`, `complete`, and `verification`;
- simulation and generic analysis: `SimulationErrorCode`, `AssessmentStatus`, `QualityStatus`, `FindingCategory`.

Sealed interfaces such as `ArtifactState`, `IntrinsicChangeResult`, `BaseVerificationResult`, `ProposedModelMaterializationResult`, `AggregateTransitionValidationResult`, and `FinalChangeSetVerificationResult` encode closed outcome algebra rather than enums, preserving outcome-specific evidence.

### 3.6 Domain events

No domain-event abstraction, event record, publisher, listener, outbox, or message broker is present. Reports and diagnostics are synchronous return values, not events. The architecture is request/response and in-process composition.

## 4. REST API recovery

The table lists every controller mapping found under `src/main/java`. Springdoc also exposes generated OpenAPI/Swagger infrastructure in Boot apps, but those framework endpoints are not authored business APIs and are excluded.

| Method and path | Purpose and request | Success response | Business responsibility and dependencies |
|---|---|---|---|
| `POST /api/v1/qa-model/validate` | Validate a canonical model; body is arbitrary JSON expected to conform to v0.1 | `200 QaModelValidationResult` (valid flag, schema version, summary, issues) | `QaModelValidationController` → `QaModelValidationService` → `QaModelValidationEngine` |
| `POST /api/v1/models` | Validate and register a canonical model snapshot; body is model JSON | `201 ModelRegistrationResponse`, `Location: /api/v1/models/{id}` | `QaModelRegistrationService`; rejects invalid models with `422`; saves through `InMemoryQaModelRepository` |
| `GET /api/v1/models/{modelId}` | Retrieve stored snapshot | `200` raw model JSON | registration service/repository; `404 MODEL_NOT_FOUND` |
| `GET /api/v1/models` | List registrations, newest first | `200 List<ModelDescriptor>` | repository descriptors; no paging/filtering |
| `GET /api/v1/models/{modelId}/info` | Retrieve metadata/counts | `200 ModelDescriptor` | repository; `404` if absent |
| `GET /api/v1/models/{modelId}/trace?from=&to=` | Find a directed graph path | `200 TraceResponse` with `found`, length, alternating node/relationship elements | `QaModelTraceService` → repository + `TraceEngine`; missing params `400`, unknown model/node `404` |
| `GET /api/v1/models/{modelId}/coverage` | Analyze stored model coverage | `200 RegisteredModelCoverageResponse` | repository → `CoverageService` → `CoverageResponseMapper`; invalid analysis `422` |
| `GET /api/v1/models/{modelId}/findings` | Derive actionable findings | `200 RegisteredModelFindingsResponse` | repository → `FindingsService` (which owns validation/coverage composition) → mapper |
| `GET /api/v1/models/{modelId}/assessment` | Combined health, validation, coverage, and findings | `200 RegisteredModelAssessmentResponse` | repository → coverage → findings; `AssessmentHealth.from(...)` derives health |
| `GET /api/v1/models/{modelId}/roadmap` | Build remediation tasks | `200 RegisteredModelRoadmapResponse` | repository → coverage → findings → `RoadmapService` → mapper |
| `GET /api/v1/models/{modelId}/execution-plan` | Group roadmap tasks into dependency-safe waves | `200 RegisteredModelExecutionPlanResponse` | repository → coverage → findings → roadmap → `ExecutionPlanner` → mapper |
| `POST /api/v1/qa-model/extract` | Convert Story Input Model v0.1 to canonical model without LLM; body is story-input JSON | `200 QaModelExtractionResponse` | `QaModelExtractionService` → input schema validator + `StoryInputToQaModelMapper` |
| `POST /api/v1/qa-model/extract-and-validate` | Extract then validate canonical output | `200 ExtractAndValidateResponse` | `ExtractAndValidateService` → extraction service + validation core |
| `POST /api/v1/coverage` | Validate and calculate rule/scenario/check coverage for body model | `200 CoverageReport` | `CoverageController` → `CoverageService` → validation core + configured analyzers |
| `POST /api/v1/coverage/rules` | Backward-compatible alias for full coverage analysis | same `CoverageReport` as `/coverage` | same service; despite path, it does not return only rule coverage |
| `POST /api/v1/coverage/traceability/chains` | Build only complete traceability chains | `200 TraceabilityChainReport` | `TraceabilityChainService` → `TraceabilityChainBuilder` |
| `POST /api/v1/coverage/traceability` | Analyze maximal traceability paths and break status | `200 TraceabilityCoverageReport` | `TraceabilityCoverageService` → `TraceabilityCoverageAnalyzer` |
| `POST /api/v1/simulation` | Materialize an explicit future model; body `SimulationRequest(currentModel, impactReport, taskMaterializationSet)` | `200 SimulationResult` | `SimulationFacade` → `ModelSimulationEngine`; preparation, base fingerprints, materialization, full candidate validation |
| `POST /api/v1/story/analyze` | Analyze story text through configured analyzer; body `StoryAnalysisRequest` with Jakarta validation | `200 StoryAnalysisResult` | `StoryAnalysisController` → `StoryAnalyzer` (`LlmStoryAnalyzer`) → prompt factory + `LlmClient` |

Malformed JSON is generally `400`. Validator/extractor/coverage use small `{error,message,...}` maps; simulation uses RFC 9457-style `ProblemDetail` with a domain code and optional task/node/validation evidence. `qa-story-analyzer` relies on Spring validation/default error handling. This inconsistency is an as-built API characteristic.

Library public APIs not exposed by an authored controller include `FinalChangeSetVerifier.verify`, the staged `qa-model-change` validators/materializers, `ImpactAnalyzer`, `ModelSimulationEngine.simulate`, `UnifiedAnalysisService`, and the `AnalysisEngine` SPI.

## 5. Validation pipeline

### 5.1 Canonical model validation

Execution order is explicit in `QaModelValidationEngine.validate`:

1. `JsonSchemaQaModelValidator` runs the bundled Draft 2020-12 `schema/qa-model-v0.1.schema.json` using NetworkNT.
2. Semantic validation runs **only when schema issues are empty**. This short-circuit protects semantic rules from malformed shape and makes schema authority precede graph meaning.
3. `SemanticValidationEngine` iterates an immutable ordered list returned by `SemanticValidationRules.defaults()`.
4. Issues are deterministically sorted by severity, layer, nullable object ID, then code.
5. Errors and warnings are counted into `ValidationSummary`; `valid` means zero errors (warnings do not invalidate).

Schema messages become `ValidationIssue.schemaError`, with a normalized code from the validator message type and JSON instance path. Semantic rules return issues containing stable codes, layer/severity, target identity and path. The default rule order is encoded, but final issue order is the engine's comparator rather than evaluation order.

The semantic rules are individually extensible through `KnowledgeRule.evaluate(JsonNode)`. A caller can construct `SemanticValidationEngine` with another list, and tests can isolate a rule. The default production set is closed in code (`SemanticValidationRules.defaults()`); there is no runtime plugin discovery or Spring collection injection. Adding a production rule requires code and rebuilding.

### 5.2 Validation in downstream flows

- Registration validates before persistence and throws `InvalidQaModelException` on any error.
- `CoverageService` validates before analyzer execution and carries validation evidence in `CoverageReport`.
- Extraction first validates Story Input against its own schema, maps deterministically, and `ExtractAndValidateService` then calls canonical validation.
- Simulation validates inputs/preconditions while preparing, fingerprints the base, materializes only supplied future nodes, then fully validates the candidate; invalid candidates produce `CANDIDATE_MODEL_VALIDATION_FAILED` and HTTP 422.
- Canonical change has a deeper evidence-preserving pipeline: intrinsic declaration validation → Base lookup/verification → proposed materialization → aggregate-transition validation → root reconstruction → complete schema validation → complete semantic validation → final evidence consistency. `FinalChangeSetVerifier` explicitly does not re-run validation; it accepts retained `CompleteProposedRootValidationResult` evidence.

This produces two related but distinct validation boundaries: validation core answers whether a complete JSON model is valid; change verification answers whether a declared transition is intrinsically coherent, bound to Base truth, atomically materializable, completely valid, and backed by consistent retained evidence.

## 6. Component responsibilities

| Package/module | Owns | Must never own (enforced or implied by current dependency direction) | Public boundary / internals |
|---|---|---|---|
| `qagraph.model` / `qa-model` | shared contract enums/records and schema | Spring controllers, repository, analysis policy | public records/enums; no service internals |
| `qagraph.validationcore` | complete model validity authority | persistence, HTTP, coverage/remediation decisions | `QaModelValidationEngine`, result model; schema adapter and semantic rules internal to module |
| `qaip.coverage` | coverage metrics/problems and traceability analysis | findings/remediation policy or registration | `CoverageService`, analyzers and report records; controller/config are adapter shell |
| `qaip.findings` | translate validated coverage evidence to findings | roadmap ordering/execution | `FindingsService`, report/value types |
| `qaip.roadmap` | remediation tasks and dependencies | execution-wave scheduling or model mutation | `RoadmapService`, roadmap records |
| `qaip.execution` | validate task dependency graph and build waves | finding generation or task invention | `ExecutionPlanner`, plan records, explicit exceptions |
| `qaip.impact` | expected structural changes/gaps from roadmap evidence | candidate mutation or projected coverage | `ImpactAnalyzer`, catalog, report records |
| `qaip.simulation` | explicit materialization and validated future snapshot | invent missing materializations or persist/apply them | `ModelSimulationEngine`, request-domain/result records; preparation/materializer/fingerprint helpers package-private |
| `qagraph.change` | trusted declared-change verification and evidence lineage | persistence, approval, execution, impact analysis | staged public validators and sealed results; backend/equality/support helpers internal |
| `qagraph.repository` in validator | volatile registered snapshot storage | validation or analytical decisions | concrete `InMemoryQaModelRepository`; private `RegisteredModel` |
| `qagraph.service` in validator | registered-model use cases | low-level schema rules or HTTP serialization | application services; API mappers/controllers remain in `qagraph.api` |
| `qagraph.extractor` | input-schema validation and deterministic mapping | LLM inference and downstream coverage policy | extraction services/response types; mapper and validators as implementation |
| `qagraph.storyanalyzer` | LLM-assisted story analysis | canonical validation authority | `StoryAnalyzer` and `LlmClient` ports; `LlmStoryAnalyzer`, prompt factory and controller adapters |
| `qaip.core` | engine-neutral analysis contracts and metadata | concrete coverage/validation implementations | `AnalysisEngine`, mapper/id-generator SPIs and immutable values |
| `qaip.analysis` | compose generic engines and adapt validation/coverage | redefine the engines' domain rules | factory/service/orchestrator; concrete adapters/mappers mostly internal |

“Must never own” here records the boundary already expressed by package placement and one-way dependencies; it is not a proposal for a new layer.

## 7. Design patterns present

- **Strategy:** `KnowledgeRule`, `CoverageAnalyzer`, `AnalysisEngine`, `StoryAnalyzer`, and `LlmClient` define interchangeable behavior. Multiple concrete semantic and coverage strategies are instantiated and iterated.
- **Pipeline:** canonical validation has schema then semantic stages; canonical change encodes multiple typed stages; simulation prepares, materializes, validates, then fingerprints/returns. Each stage consumes the prior stage's evidence.
- **Adapter:** `ValidationAnalysisEngine` and `CoverageAnalysisEngine` adapt concrete engines to `AnalysisEngine`; response mappers adapt domain reports to registered-model API DTOs; controllers adapt HTTP to services.
- **Registry:** `AnalysisEngineRegistry` stores engines by identifier and is consumed by executor/orchestration code.
- **Facade/application service:** `SimulationFacade`, `UnifiedAnalysisService`, and registered-model services expose cohesive use cases over several lower-level components.
- **Repository:** `InMemoryQaModelRepository` abstracts save/find/list semantics, although it is a concrete class rather than an interface.
- **Factory/composition root:** `UnifiedAnalysisFactory`, metadata factories, and Spring `@Configuration` classes construct configured object graphs.
- **Mapper:** `StoryInputToQaModelMapper` and API/assessment mappers isolate deterministic representation conversion.
- **Result algebra / railway-style outcomes:** sealed interfaces with success and failure implementations in `qa-model-change` force callers to handle evidence-bearing alternatives without null/sentinel results.
- **Immutable value object:** Java records and defensive `List.copyOf`/`JsonNode.deepCopy` are pervasive.
- **Topological sort:** `ExecutionPlanner` creates execution waves and explicitly rejects unknown, self, duplicate, and cyclic task dependencies. This is an algorithmic pattern actually present, not an architectural label.

No event sourcing, CQRS, hexagonal framework, actor model, or persistent graph-database pattern is implemented. Ports exist selectively, but the overall repository should not be labeled fully hexagonal because repositories and many services depend directly on concrete classes and `JsonNode`.

## 8. Architectural decisions already encoded

| Potential ADR title | Evidence and classes | Encoded reasoning visible in implementation |
|---|---|---|
| Use Canonical QA Model v0.1 JSON as the integration contract | bundled schema; `JsonNode` signatures across validation, coverage, trace, simulation | keeps modules interoperable through one normalized document contract |
| Make complete validation schema-first and fail-fast | `QaModelValidationEngine.validate` invokes semantic rules only with zero schema issues | semantic rules may assume valid structural shape |
| Centralize complete-model authority in validation core | coverage, extractor, simulation, change backend depend on `QaModelValidationEngine` | avoids separate modules redefining complete validity |
| Preserve deterministic output ordering | validation comparator, repository descriptor comparator, sorted findings/plans and deterministic tests | same logical input should yield stable evidence/order, subject to report metadata |
| Separate findings, roadmap, execution, impact, and simulation | module graph and `RegisteredModelExecutionPlanService` chain | each downstream stage consumes upstream evidence and does not recalculate its responsibility |
| Do not project coverage without explicit simulation | impact produces expected structural changes; simulation requires `TaskMaterializationSet` and validates materialized candidate | expected impact is not silently presented as measured future coverage |
| Require explicit future artifacts in simulation | `TaskMaterialization`, `CandidateModelMaterializer`, input validation | engine does not invent node content from remediation intent |
| Verify canonical changes as retained evidence stages | sealed stage results throughout `qa-model-change`; `FinalChangeSetVerifier` comment and consistency checks | final verification audits lineage instead of recomputing and losing provenance |
| Bind change evidence to exact Base artifacts | `BaseArtifactIndex`, `BaseChangeVerifier`, identity/reference checks | declared transitions are accepted only against the actual base snapshot |
| Keep deterministic cores framework independent | `java-library` plugins and absence of Spring in validation/change/planning/impact/simulation/core | permits in-process use and narrow transport adapters |
| Store registered models as immutable process-local snapshots | `ConcurrentHashMap`, UUID, `deepCopy` on save/read in `InMemoryQaModelRepository` | provides thread-safe demo/runtime registration without mutation aliasing |
| Model closed stage outcomes with sealed types | change result interfaces and permitted success/failure types | makes illegal mixed evidence harder to construct and handling explicit |
| Abstract LLM access behind a port | `LlmClient`, `StoryAnalyzer`, `LlmStoryAnalyzer`, test `MockLlmClient` | keeps prompting/orchestration testable without a real provider |
| Expose compatible coverage alias | `CoverageController.analyzeRules` delegates to the same full `analyze` method | retains the older `/rules` route while response semantics evolved |

Several of these already have repository ADRs; the table is the independently recovered ADR candidate set from code.

## 9. Technical debt and risk ranking

### High

1. **Volatile concrete repository.** `InMemoryQaModelRepository` loses all registrations on restart, has no repository interface, generates non-repeatable UUIDs, and provides no deletion/versioning/tenant boundary. This blocks durable production use and couples application services to storage implementation.
2. **Duplicated canonical contract types/resources.** `NodeType`, `RelationshipType`, validation records, and the schema exist in both `qa-model` and `qa-model-validation-core`; extractor also bundles schema copies. Drift could make compilation succeed while services disagree about v0.1 semantics.
3. **Repeated downstream orchestration and recalculation.** Registered coverage/findings/assessment/roadmap/execution services repeat repository lookup, coverage validation, and pipeline composition. It is cohesive enough now, but duplication makes caching, evidence identity, error behavior, and pipeline evolution easy to diverge.
4. **No implemented production security/operability boundary.** Authn/authz, request limits, audit, metrics/tracing, persistence recovery, and deployment topology are absent. Public model bodies and LLM story content can be sensitive.

### Medium

5. **`JsonNode` is a leaky ubiquitous representation.** It avoids duplicate object models but exposes paths/string conventions throughout rules, trace, coverage, repository, change, and simulation. Schema changes can create widespread runtime rather than compile-time failures.
6. **Inconsistent HTTP error contracts.** Three handlers use map bodies, simulation uses `ProblemDetail`, story analysis relies on defaults, and domain planner exceptions have no visibly uniform mapping. Clients must implement service-specific parsing.
7. **Dependency/version skew.** Jackson is pinned as 2.18.3 or 2.19.2 in different libraries while Boot dependency management also participates. Validation core declares Jackson without a local version and relies on consumers/platform resolution.
8. **Temporal nondeterminism in reports.** Several reports include `Instant generatedAt`; repository uses real UTC clock and UUID. Core calculations may be deterministic while serialized outputs are not byte-for-byte repeatable unless clocks/IDs are injected.
9. **Concrete composition instead of ports in validator.** `RegisteredModel*Service` depends directly on `CoverageService`, `FindingsService`, etc.; the repository is concrete. Tests can construct them, but substitution and boundary tests require concrete graphs.
10. **Coverage summary relies on metric list positions.** `RegisteredModelAssessmentService.summary` reads metrics at indexes 0, 1, and 2. Analyzer/order changes can silently change meaning or fail despite `CoverageMetricCode` existing.

### Low

11. **Compatibility endpoint naming is misleading.** `/coverage/rules` returns the full modern report, as its controller implementation states.
12. **Formatting and encoding quality varies.** Some source files are compressed to one line and displayed Russian OpenAPI/error strings show encoding artifacts in this checkout/tooling path, reducing maintainability and generated documentation quality.
13. **Underused generic analysis layer.** `qaip-core`/`qaip-analysis` provides a registry/executor architecture, but registered-model REST orchestration calls concrete pipelines directly. Two orchestration styles can evolve independently.
14. **LLM runtime adapter completeness is unclear from core port.** The test mock proves substitution, but no durable provenance, model/version capture, retry/error taxonomy, or deterministic guarantee is encoded alongside `LlmResponse`.

No evidence establishes that these are defects in current supported scope; the ranking reflects evolution and operational risk.

## 10. Missing documentation

Architectural concepts present in code but not reliably discoverable from a single current document are:

- the complete authored endpoint inventory and the fact that endpoints belong to five separate Boot applications;
- the distinction between complete-model validation and staged declared-change verification;
- schema-first short-circuit behavior and the exact deterministic issue ordering;
- the canonical model's actual `JsonNode` boundary versus typed result contracts;
- volatile registration semantics, deep-copy guarantees, sort order, and restart data loss;
- exact downstream evidence chain: coverage → findings → roadmap → execution → impact → explicit simulation;
- the compatibility semantics of `/api/v1/coverage/rules`;
- the absence of domain events and persistence/service-to-service integration;
- sealed result algebras and evidence retention in canonical change;
- which modules are libraries versus independently deployable Boot applications;
- API error-format differences and status mapping;
- the relationship and partial overlap between direct registered-model orchestration and `qaip-analysis` SPI orchestration;
- determinism boundaries: algorithms and ordering are deterministic, timestamps/UUIDs/LLM output are not.

## 11. Architecture consistency review

| Quality | Strengths | Weaknesses / evidence |
|---|---|---|
| Separation of concerns | strong downstream module split; transport mostly thin; change stages narrowly named | validator services duplicate orchestration; JSON representation leaks across boundaries; story analyzer depends on deployable extractor module rather than a smaller contract/library boundary |
| Coupling | acyclic module graph; framework-independent cores; selective ports | several `api project(...)` dependencies expose upstream implementation types; concrete repository/services; duplicated model contracts create semantic coupling |
| Cohesion | validation rules, analyzers, planning, impact, simulation, and change stages each cluster by capability | `qa-model-validator` is a broad composition service containing registration, tracing, assessment, roadmap, and planning endpoints |
| Extensibility | strategy interfaces and Spring configurations support analyzers/adapters; semantic engine accepts injected rule lists | production defaults are hard-coded; no runtime extension discovery; JSON path conventions make schema extension cross-cutting |
| Testability | substantial unit, smoke, integration, contract, and release-gate tests; immutable results; mock LLM port; some clock injection | public default constructors instantiate collaborators in places; real clock/UUID and concrete repository reduce deterministic integration tests |
| Maintainability | descriptive modules/types; records and sealed results communicate invariants; one-way dependencies | contract/schema duplication, version skew, orchestration duplication, compressed source formatting, and two analysis composition approaches |
| Determinism | stable rule set, explicit sorting, topological planning, fingerprints, no hidden generation in simulation | UUID/timestamps and LLM behavior are intentionally nondeterministic; ordering guarantees are not uniformly expressed as API contracts |

Overall consistency is good in the deterministic core and canonical-change subsystem. The principal inconsistency is not a wrong architecture but a maturity seam: rigorously modeled in-process evidence calculations are exposed through runtime shells whose persistence, error standardization, and operational controls remain minimal.

## 12. Incremental evolution path

The following path preserves existing module boundaries and behavior; it does not introduce a replacement architecture.

1. **Freeze and test the recovered contracts.** Add architecture tests for the current acyclic module direction, endpoint contract tests for all controllers, and golden tests for deterministic ordering. Document timestamp/UUID/LLM exceptions.
2. **Remove contract drift risk.** Select the already-authoritative validation-core schema as the build source and add checks that packaged schema copies and duplicate enums remain identical; migrate duplicates only when compatibility tests prove no public break.
3. **Stabilize registered-model operations.** Extract the behavior already supplied by `InMemoryQaModelRepository` behind a small repository interface, retain the in-memory adapter for tests/demo, then add a durable adapter only when operational requirements demand it. Preserve deep-copy and ordering semantics.
4. **Consolidate existing application-pipeline composition.** Reuse one internal composition function for repository → coverage → findings → roadmap → execution, while keeping every existing endpoint and response. This removes recalculation divergence without changing the architecture.
5. **Unify transport errors.** Adopt the already used `ProblemDetail` approach across Boot adapters, preserving current domain codes and status meanings; publish the error schemas in generated OpenAPI.
6. **Make determinism controls explicit.** Inject `Clock` and ID generation wherever reports/registrations expose them, as `InMemoryQaModelRepository` already partially supports for `Clock`; keep default production behavior unchanged.
7. **Harden deployable shells.** Add request-size limits, authentication/authorization appropriate to model sensitivity, structured audit/metrics, health/readiness, and documented configuration. These are operational increments around existing use cases.
8. **Clarify the two analysis entry paths.** Either document direct registered-model composition and `qaip-analysis` as intentionally separate APIs, or incrementally have one delegate to the other where results are already equivalent. Do not force a migration without equivalence tests.
9. **Strengthen LLM evidence.** Keep `StoryAnalyzer`/`LlmClient`; add provider/model/prompt-version provenance and explicit failure mapping to `StoryAnalysisResult` or adjacent metadata without moving canonical validation into the LLM subsystem.
10. **Advance versions deliberately.** For any schema beyond 0.1, introduce version-selected schema/rule sets behind the existing validation entry point and keep Canonical Change's explicit supported-version rejection. Do not silently reinterpret stored v0.1 snapshots.

## Evidence index

Key implementation anchors for board verification:

- build topology: [`settings.gradle`](../../settings.gradle), module `build.gradle` files;
- validation authority and order: [`QaModelValidationEngine.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/QaModelValidationEngine.java), [`SemanticValidationRules.java`](../../qa-model-validation-core/src/main/java/ru/kuznetsov/qagraph/validationcore/validation/semantic/SemanticValidationRules.java);
- API composition: [`QaModelController.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/api/QaModelController.java), other `*Controller.java` files enumerated in section 4;
- volatile storage: [`InMemoryQaModelRepository.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/repository/InMemoryQaModelRepository.java);
- downstream chain: [`RegisteredModelExecutionPlanService.java`](../../qa-model-validator/src/main/java/ru/kuznetsov/qagraph/service/RegisteredModelExecutionPlanService.java);
- simulation boundary: [`ModelSimulationEngine.java`](../../qa-model-simulation/src/main/java/ru/kuznetsov/qaip/simulation/ModelSimulationEngine.java), [`SimulationRequest.java`](../../qa-model-simulation-api/src/main/java/ru/kuznetsov/qaip/simulation/api/SimulationRequest.java);
- retained change evidence: [`FinalChangeSetVerifier.java`](../../qa-model-change/src/main/java/ru/kuznetsov/qagraph/change/verification/FinalChangeSetVerifier.java);
- analysis SPI: [`AnalysisEngine.java`](../../qaip-core/src/main/java/ru/kuznetsov/qaip/core/engine/AnalysisEngine.java), [`AnalysisOrchestrator.java`](../../qaip-analysis/src/main/java/ru/kuznetsov/qaip/analysis/orchestration/AnalysisOrchestrator.java);
- executable evidence: unit/smoke/integration tests under each module's `src/test` tree.

