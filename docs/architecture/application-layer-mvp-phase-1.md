# Application Layer Design — MVP Phase 1

**Status:** Proposed  
**Scope:** Architecture and API design only  
**Primary executable workflow:** `qaip analyze project.json`

## 1. Executive Summary

MVP Phase 1 adds one thin, synchronous application boundary around the completed
domain capabilities. A caller supplies a project file and output preferences.
The application parses the file into an immutable, structurally bound, and
explicitly untrusted `ParsedProject`; delegates every validity, verification,
and impact decision to the existing domain contracts; assembles one immutable
`ProjectReport`; and passes that report to a selected exporter. Parsing success
does not imply JSON Schema validity, semantic validity, change verification, or
manifest validity.

The fixed trust path is:

```text
project bytes
  → JsonProjectImporter
  → ParsedProject
  → authoritative schema/domain validation
  → complete canonical-change pipeline
  → VerifiedChangeSet
  → ImpactEvidenceAnalyzer
```

The primary application entry point is:

```java
public final class AnalyzeProject {
    public AnalyzeProject(JsonProjectImporter importer,
                          ProjectValidator validator,
                          ImpactEvidenceAnalyzer impactAnalyzer);

    public AnalyzeProjectResponse execute(AnalyzeProjectRequest request);
}
```

`JsonProjectImporter` is the single concrete JSON parser/binder. It is not an
interface for hypothetical formats and performs no validation. `ProjectValidator`
denotes the existing domain facade or a thin application call sequence over
existing authoritative stages; it does not authorize new validation rules.
Construction is explicit in the composition root. There is no container,
registry, workflow engine, event bus, persistence boundary, generic service
abstraction, or versioned importer factory.

`AnalyzeProject` returns data; it does not select an exporter, write a file, or
decide an exit code. The CLI adapter performs those transport concerns after the
use case returns. Exporters consume only `ProjectReport` and cannot depend on
domain objects.

The application layer starts when a transport adapter has converted its own
arguments into `AnalyzeProjectRequest`. It ends when it returns
`AnalyzeProjectResponse`. Reading/parsing project content is reached through the
concrete JSON importer; rendering and writing are outside the application
boundary. Import itself starts with immutable project bytes and ends with either
an untrusted `ParsedProject` or parse/binding diagnostics. Domain verification
starts only after that import result.

## 2. Architectural Goals

### Required qualities

- Expose one complete import → validation → impact → report workflow.
- Preserve the exact decisions, diagnostics, ordering, and versions produced by
  the domain.
- Make equivalent project bytes and the same execution context produce an
  equivalent report payload.
- Keep reports independent of CLI, Markdown, HTML, JSON, and future HTTP types.
- Make every normal negative outcome explicit and machine-readable.
- Permit implementation without modifying a domain contract.

### Non-goals

- Generalize the pipeline or make stages dynamically configurable.
- Recalculate, merge, suppress, promote, or reinterpret domain diagnostics.
- Infer impact when the domain returns `UNKNOWN` or `Failed`.
- Introduce a reusable platform abstraction for hypothetical use cases.

### Governing invariants

1. The application may choose **when** an existing domain operation runs; it may
   not change **what** that operation means.
2. A validation gate is derived only from the existing validation result.
3. `ImpactEvidenceCompleted` and `ImpactEvidenceFailed` are projected faithfully.
4. Domain list order is retained unless the domain contract explicitly declares
   the list unordered. The application never invents a competing sort order.
5. Every collection in a request, response, context, and report is defensively
   copied and unmodifiable; nullable fields are not used.
6. Every successful canonical-change stage passes the exact returned evidence
   instance to the next stage. Rebuilding an equivalent object, serializing and
   deserializing between stages, reconstructing later-stage evidence, or
   skipping a stage is forbidden.

## 3. Application Layer Responsibilities

### Application boundary

The transport-neutral entry point is `AnalyzeProject.execute`. Its contract is:

```java
public record AnalyzeProjectRequest(
        ProjectInput input,
        AnalysisExecutionContext context) {}

public sealed interface ProjectInput {
    record PathInput(Path path) implements ProjectInput {}
    record ContentInput(String sourceName, byte[] content) implements ProjectInput {}
}

public record AnalysisExecutionContext(
        String invocationId,
        SliceAnalysisContext domainContext) {}

public sealed interface AnalyzeProjectResponse {
    record Reported(ProjectReport report) implements AnalyzeProjectResponse {}
    record Terminated(ApplicationError error) implements AnalyzeProjectResponse {}
}
```

`PathInput` is convenient for the CLI; `ContentInput` prevents a future REST
adapter from fabricating temporary files. `invocationId` is caller-supplied
correlation data and has no semantic effect. It is omitted from the deterministic
report payload. `domainContext` is the existing explicit set of qualification,
influence, and algorithm versions; MVP uses `SliceAnalysisContext.supported()`.
The application does not add a clock, locale, environment map, or arbitrary
attributes to semantic execution.

The CLI edge converts `PathInput` to immutable bytes before invoking
`JsonProjectImporter`; `ContentInput` already supplies those bytes. This does
not alter the application contract or create an input abstraction.

### Owned responsibilities

- enforce non-null application request shape;
- invoke stages in the fixed order defined in this document;
- stop at the gates declared by existing result contracts;
- project existing results into the report without semantic transformation;
- translate thrown boundary failures into the application error categories;
- return one immutable response.

### Explicitly not owned

- JSON Schema, semantic, manifest, identity, relationship qualification,
  traversal, proof selection, or impact classification rules;
- creation of a `VerifiedChangeSet`; only `FinalChangeSetVerifier` creates it
  after the complete canonical-change pipeline;
- report presentation or output I/O;
- CLI argument parsing and process termination;
- retry, caching, concurrency, telemetry policy, or persistence.

### Lifecycle

1. A composition root constructs `JsonProjectImporter`, existing
   validators/analyzer,
   `AnalyzeProject`, and concrete exporters with ordinary constructors.
2. A transport creates one immutable request per invocation.
3. `execute` runs once, synchronously, without retained mutable state.
4. The returned response is complete and immutable.
5. The transport optionally exports a `Reported` report, maps its status to an
   exit code, and releases all invocation-local data.

The use-case object may be reused sequentially because it is stateless. MVP
makes no thread-safety promise beyond the guarantees of its existing
dependencies.

### Error boundary

The application catches only at boundaries where it can classify a failure:

- importer-declared parse/binding failures become report diagnostics;
- existing validator and analyzer result variants become report sections;
- input-read and output-write failures are infrastructure termination errors;
- an uncaught runtime failure becomes `UnexpectedApplicationError` at the
  outermost boundary, preserving its cause for logging but never its stack trace
  in a report.

It does not catch an exception merely to continue with absent domain data.

## 4. Execution Pipeline

| Stage | Input | Output | Owner | Failure handling | Determinism |
|---|---|---|---|---|---|
| Acquire | `ProjectInput` | immutable bytes plus source name | CLI/input adapter | missing, unreadable, or oversized-by-host-policy input terminates as infrastructure failure | bytes are passed unchanged; source path has no semantic role |
| Parse and bind | project bytes | immutable untrusted `ParsedProject` retaining the original parsed JSON, or ordered parse/binding diagnostics | `JsonProjectImporter` | parse/binding failure produces `PARSE_FAILED`; no domain stage runs | the importer preserves supplied values and order and performs no normalization, defaulting, repair, or validation |
| Authoritative validation | `ParsedProject.originalDocument` plus mechanically mapped candidates | existing JSON Schema and semantic results | existing domain validators | domain-invalid input produces `VALIDATION_FAILED`; domain codes remain domain-owned | the original parsed document, including unknown fields, is validated; DTO-to-JSON reconstruction is forbidden |
| Verify change | untrusted base model and change declarations | `VerifiedChangeSet` or an existing stage-specific failure | complete canonical-change domain pipeline ending in `FinalChangeSetVerifier` | any failed stage produces `VALIDATION_FAILED`; later stages do not run | stages run in documented order and exact returned evidence instances pass forward without round-trip or reconstruction |
| Prepare impact request | `VerifiedChangeSet`, immutable manifest candidate, subject candidate, context candidate | existing `ImpactEvidenceRequest` | application (mechanical assembly only) | impossible/missing structurally bound values indicate an application defect; domain incompatibility remains analyzer-owned | field-for-field construction; no normalization, sorting, resolution, qualification, or trust promotion |
| Analyze impact | `ImpactEvidenceRequest` | existing `ImpactEvidenceCompleted` or `ImpactEvidenceFailed` | `ImpactEvidenceAnalyzer` domain capability | completed conclusion produces `ANALYZED`; declared failure produces `ANALYSIS_FAILED`; thrown defect is unexpected termination | analyzer owns interpretation of identity evidence, qualification, BFS, proof precedence, classification, and ordering |
| Assemble report | stage results | immutable `ProjectReport` | application | constructor invariant failure is an unexpected defect | lossless projection with fixed report contract version |
| Export | `ProjectReport` and destination | bytes/file | exporter plus CLI adapter | render/write failure terminates as infrastructure failure; it never changes the report | JSON is canonical; Markdown/HTML are stable for a fixed exporter version |

### Validation gate

The validation stage does not mean that the application independently reruns
rules already enforced inside a domain facade. Its implementation calls the
smallest existing facade that produces the necessary typed result. If
`ImpactEvidenceAnalyzer` owns manifest validation, its `ImpactEvidenceFailed`
result is authoritative; the application must not add a second manifest
validator. The conceptual pipeline stage describes the handoff, not duplicated
execution.

`ParsedProject` retains the original parsed JSON document. The authoritative
JSON Schema validator receives that exact document, including unknown fields;
validation must never operate on a DTO-to-JSON reconstruction. Schema mismatch,
semantic mismatch, invalid change evidence, and unsupported domain/version
semantics remain domain outcomes, even though their input came from JSON.

The canonical-change path is complete and fixed:
`IntrinsicChangeValidator` → `BaseChangeVerifier` →
`ProposedModelMaterializer` → `AggregateTransitionValidator` →
`ProposedCanonicalRootReconstructor` → `CompleteProposedRootValidator` →
`FinalChangeSetVerifier`. Only the final verified result supplies
`VerifiedChangeSet`. Each arrow passes the exact success instance returned by
the prior stage; equivalent reconstruction is not permitted.

The manifest does not cross this gate as a verified value.
`FrozenEvidenceManifest` is an immutable candidate until manifest validation
inside `ImpactEvidenceAnalyzer`. Imported identity assertions are frozen
evidence claims, imported relationships remain unqualified, and the subject and
`SliceAnalysisContext` remain request candidates until the analyzer accepts
them.

The gate predicate is the domain's success/valid variant. The application must
not derive validity from issue counts, severities, text, or an application-owned
allowlist.

### Multiple subjects

The completed vertical slice accepts one `SubjectArtifactRef`. Therefore the
MVP project contract contains exactly one subject and performs exactly one
analyzer invocation. Batch subjects are not silently looped in Phase 1 because
that would require an aggregate partial-failure contract not present in the
domain.

## 5. Use Case Catalogue

### `AnalyzeProject` — required

The only complete MVP use case. It imports and validates one project, analyzes
one subject, and returns one unified report. It is the application contract
behind `qaip analyze project.json`.

**Precondition:** a non-null input and supported explicit domain context.  
**Success:** `Reported` with status `ANALYZED`. A completed `UNKNOWN` impact
classification is a successful analysis, not an error.  
**Normal non-success:** `Reported` with `PARSE_FAILED`, `VALIDATION_FAILED`, or
`ANALYSIS_FAILED`.  
**Termination:** infrastructure or unexpected error.

### `ValidateProject` — justified but optional for the first implementation

This is a strict prefix of `AnalyzeProject`: acquire, import, and validate, then
produce the same `ProjectReport` with status `VALIDATED`. It is justified for
authoring feedback and CI linting without requiring impact execution.

It must reuse the same internal straight-line function or call sequence as
`AnalyzeProject`; it must not form a second validation pipeline. A separate
public class is needed only when the `qaip validate` command is implemented.
Its absence does not block the first usable MVP.

### “Generate Report” — not an application use case

Report generation is already part of `AnalyzeProject`; serialization is a pure
output adapter operation:

```java
public interface ProjectReportExporter {
    byte[] export(ProjectReport report);
}
```

There is no `GenerateReportService`. Making formatting a use case would expose
presentation choices inside orchestration and would tempt exporters to retrieve
domain data. The CLI selects one of three concrete exporters directly.

No additional MVP use cases are justified. Import-only, re-analysis, report
conversion, batch analysis, and project registration do not contribute to the
single required workflow.

## 6. Report Model

### Contract

`ProjectReport` is the sole exporter input and the future REST response source.
It is an application-owned snapshot, not a domain aggregate:

```java
public record ProjectReport(
        String reportVersion,
        ReportStatus status,
        ProjectDescriptor project,
        ImportReport importReport,
        ValidationReport validation,
        Optional<ImpactReportView> impact) {}

public enum ReportStatus {
    PARSE_FAILED,
    VALIDATION_FAILED,
    VALIDATED,
    ANALYSIS_FAILED,
    ANALYZED
}

public record ProjectDescriptor(
        String sourceName,
        Optional<String> projectContractVersion,
        Optional<String> semanticFingerprint) {}

public record ImportReport(
        ImportStatus status,
        List<ReportDiagnostic> diagnostics) {}

public record ValidationReport(
        ValidationStatus status,
        List<ValidationSection> sections) {}

public record ValidationSection(
        ValidationKind kind,
        String contractVersion,
        ValidationStatus status,
        List<ReportDiagnostic> diagnostics) {}

public enum ValidationKind {
    PROJECT_SCHEMA,
    SEMANTIC,
    CHANGE_SET
}

public record ImpactReportView(
        ImpactRunStatus status,
        Optional<ImpactConclusionView> conclusion,
        Optional<ImpactFailureView> failure) {}
```

The report uses specific view records for the existing domain result graph:

- `ImpactConclusionView`: subject local ID, resolved subject assertion,
  `AFFECTED` or `UNKNOWN`, optional proof, ordered unknown reasons, snapshot,
  exact `SliceAnalysisContext`, and ordered rejected-evidence references;
- `ImpactProofView`, a sealed value with `DirectChangeProofView` and
  `RelationshipPathProofView` variants containing every field exposed by the
  existing proofs, including declaration index, canonical identities, change
  kind, and ordered qualified path steps;
- `ImpactFailureView`: existing `FailureCode` name and all ordered analysis
  diagnostics;
- `ReportDiagnostic`: stable owner code, severity when supplied, message,
  JSON/document path or object ID when supplied, and validation kind.

`ImportReport` records only JSON parsing and structural binding. Its `SUCCEEDED`
status means that an immutable `ParsedProject` was created; it does not assert
schema validity, semantic validity, change verification, or manifest validity.
Schema, semantic, and change failures retain their domain ownership in
`ValidationReport`. Manifest mismatch, identity meaning, relationship
qualification, and impact failures remain in `ImpactReportView`, because the
authoritative decisions occur inside `ImpactEvidenceAnalyzer`.

The concrete implementation must enumerate and map every public field of
`ImpactConclusion`, `DirectChangeProof`, `RelationshipPathProof`, path steps,
subject assertions, snapshot references, rejected evidence, and diagnostics.
It must not store opaque domain objects, call `toString()`, or omit lineage.

### Invariants by status

| Status | Import | Validation | Impact |
|---|---|---|---|
| `PARSE_FAILED` | failed, non-empty diagnostics | `NOT_RUN`, no sections | absent |
| `VALIDATION_FAILED` | succeeded | invalid, at least one domain section | absent |
| `VALIDATED` | succeeded | valid | absent |
| `ANALYSIS_FAILED` | succeeded | valid | present with failure only |
| `ANALYZED` | succeeded | valid | present with conclusion only |

`ImpactClassification.UNKNOWN` is contained in an `ANALYZED` report. It must
never be converted to `ANALYSIS_FAILED`, “not affected,” or a validation issue.

### Exporter sufficiency

- Markdown and HTML can render headings, summaries, diagnostics, proof paths,
  provenance, and rejected evidence from this model alone.
- JSON serializes the complete model with enum names as stable symbolic values;
  optional values are absent/null according to one documented JSON convention,
  never inferred by an exporter.
- A future REST adapter maps the report to an HTTP DTO without invoking domain
  behavior.

The report deliberately excludes wall-clock timestamps, absolute input paths,
hostnames, stack traces, and exporter configuration. This keeps the semantic
payload reproducible. If transport metadata is later needed, it belongs in an
outer transport envelope.

## 7. Error Model

### Application error taxonomy

```java
public sealed interface ApplicationError {
    String code();
    String message();
}

public record InfrastructureError(
        String code, String message, Optional<String> sourceOrDestination)
        implements ApplicationError {}

public record UnexpectedApplicationError(
        String code, String message, String correlationId)
        implements ApplicationError {}
```

Parsing, validation, and impact-analysis failures are not members of
`ApplicationError`; they are expected, exportable report states. This prevents
one failure from being represented both as a report and as a terminal error.

| Category | Examples | Representation | Continue? |
|---|---|---|---|
| Infrastructure | cannot read input, cannot create directory, cannot write output | terminating `InfrastructureError` | no |
| Parsing/import | malformed JSON, invalid encoding, or JSON shape cannot be losslessly bound | `PARSE_FAILED` report with importer-owned diagnostics | report assembly/export only |
| Domain validation and change verification | JSON Schema mismatch, semantic mismatch, invalid change set, or unsupported domain/version semantics returned by the owning stage | `VALIDATION_FAILED` report with unchanged domain diagnostics | report assembly/export only |
| Impact analysis | manifest mismatch, identity meaning, relationship qualification, or analyzer failures such as `INVALID_REQUEST`, `INVALID_MANIFEST`, `UNSUPPORTED_VERSION`, `INTEGRITY_MISMATCH`, `INCOMPATIBLE_CHANGE_DOMAIN`, and `CHANGE_MANIFEST_MISMATCH` | completed analyzer evidence or `ANALYSIS_FAILED` with exact domain failure code and diagnostics | report assembly/export only |
| Unexpected | violated supposedly stable contract, mapper exhaustiveness defect, uncaught runtime exception | terminating `UnexpectedApplicationError`; cause logged at adapter boundary | no |

A well-formed completed conclusion of `UNKNOWN`, unresolved identity, or
rejected relationship evidence is domain output, not an application error.
Likewise, the application never catches a domain `Failed` value and converts it
to an exception.

The source location of an error does not determine its owner. The importer must
not relabel schema, semantic, change, version, manifest, identity,
qualification, or impact decisions as parsing/binding failures.

For terminal errors, stdout contains no report. The CLI writes one concise
message to stderr. Stack traces are disabled by default and may be exposed only
by a future explicit diagnostic flag outside the report contract.

## 8. CLI Contract

### Commands

```text
qaip analyze <project-file> [--format markdown|html|json] [--output <path>]
qaip validate <project-file> [--format markdown|html|json] [--output <path>]
qaip version
qaip help [command]
```

`analyze` is required for MVP. `validate` is included in the contract but may
follow immediately after the first implementation. No implicit command is
supported.

### Arguments

- `<project-file>` is one readable regular file. `-`/stdin is out of scope for
  Phase 1 so that source naming and read errors stay unambiguous.
- `--format` defaults to `markdown`.
- `--output` names a file. If omitted, output is written to stdout.
- Repeated options, unknown options, missing values, unsupported formats, and
  extra positional arguments are usage errors.
- Output parent directories must already exist. The CLI does not create a
  directory tree or overwrite multiple files.

### Output locations and streams

| Situation | stdout | stderr | File |
|---|---|---|---|
| no `--output`, report produced | exactly the exported report, ending with one newline for text formats | empty | none |
| `--output` supplied, report produced | empty | empty | exactly the exported report at the requested path |
| usage or terminal failure | empty | one concise diagnostic plus usage hint when applicable | no report |

Machine-readable JSON is never mixed with progress text. MVP emits no progress,
banners, warnings, or logs to stdout. Existing destination files are replaced
only after the full export has been rendered successfully, using the safest
atomic replacement supported by the local adapter; a replacement failure is an
infrastructure error.

### Exit codes

| Code | Meaning |
|---:|---|
| `0` | `ANALYZED` or `VALIDATED`; includes an analyzed `UNKNOWN` conclusion |
| `2` | CLI usage error |
| `3` | `PARSE_FAILED` |
| `4` | `VALIDATION_FAILED` |
| `5` | `ANALYSIS_FAILED` |
| `6` | infrastructure read/render/write failure |
| `70` | unexpected internal failure |

The rendered report status is authoritative for codes 0, 3, 4, and 5. Export
failure supersedes report status because the requested workflow did not deliver
its output.

Default output examples:

```text
qaip analyze project.json
qaip analyze project.json --format json --output report.json
qaip analyze project.json --format html --output report.html
```

## 9. Package Structure

The minimum layout separates the pure Java `qaip-application` library from the
small serialization-only `qaip-import` library. `qaip-cli` is a tiny executable
adapter and may be a separate module only when packaging requires it:

```text
qaip-application/
  src/main/java/ru/kuznetsov/qaip/application/
    AnalyzeProject.java
    AnalyzeProjectRequest.java
    AnalyzeProjectResponse.java
    AnalysisExecutionContext.java
    ProjectInput.java
    ValidateProject.java                 # add only with validate command
    report/
      ProjectReport.java
      ReportStatus.java
      ProjectDescriptor.java
      ImportReport.java
      ValidationReport.java
      ValidationSection.java
      ImpactReportView.java
      ImpactConclusionView.java
      ImpactProofView.java
      DirectChangeProofView.java
      RelationshipPathProofView.java
      ImpactFailureView.java
      ReportDiagnostic.java
    error/
      ApplicationError.java
      InfrastructureError.java
      UnexpectedApplicationError.java

qaip-import/
  src/main/java/ru/kuznetsov/qaip/importing/
    JsonProjectImporter.java
    ProjectSource.java
    ProjectImportResult.java
    ProjectParsed.java
    ProjectParseFailed.java
    ParsedProject.java
    ProjectParseFailure.java

qaip-cli/                                # only executable/packaging concern
  src/main/java/ru/kuznetsov/qaip/cli/
    QaipCli.java                         # parsing, composition, exit mapping
    MarkdownProjectReportExporter.java
    HtmlProjectReportExporter.java
    JsonProjectReportExporter.java
```

If all three exporters already exist in an exporter module, the CLI package
contains only `QaipCli` and depends on that module. The concrete JSON importer
and immutable DTO/result records live in `qaip-import`, not under application.
`ParsedProject` retains a defensive copy of the original parsed JSON and only
lossless parsed fields. No import interface, format registry, generic mapper, or
versioned importer factory is introduced.

There are deliberately no `core`, `common`, `manager`, `service`, `util`,
`factory`, `port`, `adapter`, or `config` packages. The use-case class name is
the API. Report mapping may be one package-private method in `AnalyzeProject`;
a separate mapper is introduced only if both concrete use cases demonstrably
share it. A composition method in `QaipCli` is enough; no factory is required.

## 10. Dependency Rules

### Allowed compile-time direction

```text
qaip-cli
  -> qaip-application
  -> exporter module(s)
  -> qaip-import (only to compose concrete input handling)

qaip-application
  -> qaip-import public contracts
  -> existing domain modules, including qa-impact-evidence-core

exporter module(s)
  -> qaip-application report package only

qaip-import
  -> JSON parser/tree library only

domain modules
  -> never qaip-application, qaip-cli, or exporters
```

The application may depend on domain, import, and exporter **contracts**, but
the recommended implementation keeps exporters outside its compile path:
application returns a report and the CLI invokes an exporter. This narrower
direction prevents accidental rendering inside the use case while satisfying
the allowed dependency set.

### Forbidden dependencies and calls

- Domain → application, CLI, exporter, filesystem, or transport.
- Exporter → domain, importer, analyzer, validator, repository, or CLI.
- Importer → domain verification, manifest validation, identity resolution,
  relationship qualification, or impact analysis.
- Application → Spring, HTTP, database, DI framework, service locator, event
  bus, plugin API, workflow engine, or another application endpoint.
- CLI → domain analyzer directly. All analysis goes through `AnalyzeProject`.
- Any layer → its own REST endpoint to reuse behavior.
- Report mapper → domain computations or business-rule lookup tables.

MVP dependencies are passed through constructors. Exact concrete domain objects
may be accepted where stable; interfaces are not created solely to mock or swap
them.

Candidate mapping and the complete domain-verification call sequence belong to
the application orchestration after parsing. The import module cannot depend on
or construct `VerifiedChangeSet`. The application may construct only untrusted
candidate domain values and must pass exact domain success instances between
canonical-change stages.

## 11. Sequence Diagram

The diagram describes the designed contracts, not generated implementation:

```mermaid
sequenceDiagram
    actor User
    participant CLI as CLI adapter
    participant App as AnalyzeProject
    participant Import as JsonProjectImporter
    participant Validation as Schema/domain validation
    participant Change as Canonical-change pipeline
    participant Impact as ImpactEvidenceAnalyzer
    participant Export as Selected exporter

    User->>CLI: qaip analyze project.json
    CLI->>CLI: parse args and acquire bytes
    CLI->>App: execute(request)
    App->>Import: parse(project bytes)
    alt parse or binding failure
        Import-->>App: import diagnostics
        App-->>CLI: Reported(PARSE_FAILED)
    else parsed and structurally bound
        Import-->>App: ParsedProject (untrusted + original JSON)
        App->>Validation: validate(original parsed JSON and candidates)
        alt invalid
            Validation-->>App: existing invalid result + diagnostics
            App-->>CLI: Reported(VALIDATION_FAILED)
        else schema/domain candidates valid
            Validation-->>App: existing valid results
            App->>Change: run every stage with exact returned instances
            alt change verification fails
                Change-->>App: existing stage-specific failure
                App-->>CLI: Reported(VALIDATION_FAILED)
            else change verified
                Change-->>App: VerifiedChangeSet
                App->>Impact: analyze(VerifiedChangeSet + manifest/subject/context candidates)
                alt domain-declared analysis failure
                    Impact-->>App: ImpactEvidenceFailed
                    App-->>CLI: Reported(ANALYSIS_FAILED)
                else completed
                    Impact-->>App: ImpactEvidenceCompleted
                    App-->>CLI: Reported(ANALYZED)
                end
            end
        end
    end
    CLI->>Export: export(ProjectReport)
    Export-->>CLI: rendered bytes
    CLI-->>User: stdout or output file; mapped exit code
```

Infrastructure or unexpected failure exits the sequence immediately with
`Terminated`; no exporter is called because no valid report exists or delivery
cannot complete.

## 12. Extension Points

Only extensions already forced by the MVP boundary are defined:

1. **Exporter implementations.** Markdown, HTML, and JSON implement the same
   single-method output contract and consume `ProjectReport` only.
2. **Transport adapters.** A later REST adapter can construct
   `ContentInput`, invoke `AnalyzeProject`, and map the response. It receives no
   privileged domain access.
3. **Input source variant.** `ProjectInput` permits path and in-memory content;
   additional variants require a real transport need and must resolve to the
   same bytes/import contract.
4. **Report versioning.** `reportVersion` changes only for a breaking report
   contract change. Domain contract versions remain in their owning sections
   and are never assumed equal.
5. **Validate command.** The prefix use case may be exposed without changing
   `AnalyzeProject` or the report format.

These are closed, typed seams—not a plugin system. There is no registration,
dynamic discovery, arbitrary stage list, user-supplied rule, or fallback
implementation.

## 13. Out-of-Scope Items

| Item | MVP decision and reason |
|---|---|
| Spring Boot | Excluded. A synchronous constructor-wired Java use case and CLI need no application framework; Spring would add lifecycle and configuration surfaces without domain value. |
| REST API | Excluded. The first usable workflow is local CLI. The transport-neutral request/report keeps REST possible later without prebuilding it. |
| Database | Excluded. One project file is processed in one invocation; no query or shared state exists. |
| Persistence | Excluded. Reports are written only as requested output artifacts. Projects, runs, and domain objects are not registered or stored. |
| LLM | Excluded. Import, validation, qualification, traversal, and analysis are deterministic existing capabilities; probabilistic interpretation would violate replayability. |
| AI Agent | Excluded. The workflow has a fixed stage order and no autonomous planning or tool selection. |
| Web UI | Excluded. It does not contribute to `qaip analyze project.json`; HTML is a report format, not an interactive application. |
| Authentication | Excluded. MVP is a single local process operating with the invoking user's filesystem permissions and exposes no remote boundary. |
| Multi-user execution | Excluded. There is no server, shared mutable state, tenancy, or concurrent run coordination. |
| Asynchronous execution | Excluded. One bounded local analysis returns synchronously and domain contracts are synchronous. |
| Plugin architecture | Excluded. The three fixed exporters are selected by a CLI enum/switch; dynamic discovery would expand compatibility and security obligations. |
| Service locator / DI container | Excluded. There are three application collaborators and direct constructor wiring is explicit. |
| Generic workflow engine / event bus | Excluded. The pipeline is a fixed straight-line call sequence with result-variant branches, not a configurable process. |
| Factories for future flexibility | Excluded. Constructors and one CLI composition root suffice. Add a factory only if a concrete construction invariant appears. |
| Batch or multi-subject projects | Excluded. The stable analyzer accepts one subject; partial success and aggregate ordering are not yet domain contracts. |
| Incremental analysis, caching, retries | Excluded. They add state and failure semantics without being required for the first workflow. |
| Watch mode and stdin | Excluded. They introduce lifecycle/stream semantics unrelated to the first file-based invocation. |
| Domain redesign | Excluded. Existing validation, identity, qualification, traversal, proof, and impact contracts are treated as stable and authoritative. |

## 14. MVP Readiness Assessment

### Ready

- The domain already exposes the terminal invocation boundary
  `ImpactEvidenceAnalyzer.analyze(ImpactEvidenceRequest)`.
- Its request already contains a genuine `VerifiedChangeSet` plus an immutable
  manifest candidate, one subject candidate, and explicit versioned
  `SliceAnalysisContext` candidate needed for replay. The analyzer owns their
  manifest/compatibility acceptance.
- Its sealed result already distinguishes completed conclusions from declared
  analysis failures.
- Completed conclusions already preserve classification, proof/unknown reasons,
  snapshot/context, and rejected evidence required for an auditable report.
- The application can therefore remain a fixed sequence of calls plus a
  lossless report projection.

### Must be confirmed before implementation, without changing domain contracts

1. The project-file JSON contract and importer mapping identify exactly one
   untrusted base/change declaration set, frozen evidence manifest candidate,
   subject candidate, and analysis-context candidate. `ParsedProject` must
   retain the original parsed JSON, including unknown fields, for authoritative
   schema validation; DTO-to-JSON reconstruction is forbidden.
2. The existing validation facade(s) expose their ordered diagnostics without
   requiring the application to infer validity.
3. Every public nested field of the impact result has an explicit report-view
   mapping; enum switches are exhaustive and fail loudly on contract evolution.
4. A single report contract version and JSON optional-field convention are
   chosen before exporters are implemented.
5. The complete canonical-change pipeline is invoked in documented order, only
   `FinalChangeSetVerifier` creates `VerifiedChangeSet`, and every success result
   is passed as the exact instance to the next stage without reconstruction or
   serialization round-trip.
6. The manifest remains a candidate until `ImpactEvidenceAnalyzer`; no
   prevalidated manifest type or duplicate manifest validator is introduced.
7. The build includes the completed impact-evidence module as a normal project
   dependency rather than relying on compiled local artifacts.

### Readiness verdict

**Architecturally ready with the import-contract trust model aligned.** No
domain change is required. The complete
application behavior is one primary use-case class, immutable request/response
and report records, direct constructor composition, mechanical result mapping,
and fixed exporter selection at the CLI edge.

An implementation is acceptable only if `qaip analyze project.json` can be
traced field-for-field to existing domain calls and their returned values. Any
new rule, alternative sort, inferred classification, hidden retry, or exporter
domain call is outside this design and must be rejected in review.
