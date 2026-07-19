# Experiment Lifecycle

## Terms, states, and roles

The lifecycle is:

```text
Idea -> Draft hypothesis -> Protocol review -> Dataset freeze -> READY
     -> Execution -> Result review -> COMPLETED -> Replication
     -> SUPERSEDED or INVALIDATED when necessary
```

Repository status values are `DRAFT`, `READY`, `RUNNING`, `COMPLETED`, `INVALIDATED`, and `SUPERSEDED`. “Idea,” “Draft hypothesis,” “Protocol review,” “Dataset freeze,” “Execution,” “Result review,” and “Replication” are lifecycle phases, not additional status values. Outcomes are only `SUPPORTED`, `NOT_SUPPORTED`, and `INCONCLUSIVE`.

Roles may be filled by named people appropriate to the experiment: the **owner** authors and maintains the experiment; the **protocol reviewer** checks falsifiability, controls, and reproducibility; the **dataset custodian** freezes and governs data; the **operator** executes without changing frozen criteria; the **result reviewer** checks evidence and conclusion; and the **replication owner** performs an independently identified repetition. Role assignments must be recorded, and conflicts of interest disclosed.

## Transition contract

| Transition | Entry conditions | Required artifacts | Permitted changes | Exit conditions | Responsible role |
|---|---|---|---|---|---|
| Idea -> Draft hypothesis (`DRAFT`) | Research question and potential decision value identified | Initial experiment specification | Any documented revision | Falsifiable hypothesis, null hypothesis, scope, variables, and draft criteria exist | Owner |
| Draft hypothesis -> Protocol review | Draft specification is complete enough to challenge | `experiment.md`, draft `protocol.md`, threats and required evidence | Hypothesis and criteria may change with change history | Reviewer comments resolved or explicitly rejected with rationale | Owner + protocol reviewer |
| Protocol review -> Dataset freeze | Protocol is executable; inclusion/exclusion and ground truth are defined | Reviewed protocol, draft dataset card | Protocol clarification allowed; substantive changes require renewed review | Dataset version, manifest, checksums, access, and quality checks are fixed | Dataset custodian + protocol reviewer |
| Dataset freeze -> `READY` | Hypothesis/criteria preregistered; protocol and dataset frozen; all MUST reproducibility fields present | Versioned specification, protocol, dataset card, integrity manifest, review record | Editorial corrections only; substantive change returns to review and creates a new version when frozen content changes | Owner and reviewer approve `READY` | Owner + protocol reviewer |
| `READY` -> Execution (`RUNNING`) | Inputs pass integrity checks; environment and operator are identified | Execution ID, actual parameter/environment manifest | No hypothesis, criteria, metric, or dataset changes; only controlled deviations below | At least one run is attempted and every attempt is recorded | Operator |
| Execution -> Result review | Runs end by completion, timeout, failure, or abort rule | Raw outputs, logs, failed-run records, checksums, calculated metrics, draft result | Calculations may be corrected without changing frozen formulas; observations remain immutable | Draft outcome and evidence-bounded conclusion are ready for review | Operator + owner |
| Result review -> `COMPLETED` | Evidence and deviations are available | Reviewed result report and retained raw/generated artifacts | Corrections are append-only or versioned; hypothesis remains frozen | Reviewer accepts outcome as `SUPPORTED`, `NOT_SUPPORTED`, or `INCONCLUSIVE` | Result reviewer |
| `COMPLETED` -> Replication | Original result and protocol are accessible | New execution ID or linked experiment; declared replication type and deviations | Replication may vary preregistered factors; changes are explicit | Replication report links bidirectionally to the original | Replication owner + reviewer |
| Any retained state -> `SUPERSEDED` | A newer version/experiment replaces its intended use | Supersession rationale and link | Historical artifacts are immutable | Replacement is identified; old record remains accessible | Owner + reviewer |
| `READY`, `RUNNING`, or `COMPLETED` -> `INVALIDATED` | A flaw makes the planned or reported inference unreliable | Invalidation rationale, affected evidence, reviewer decision | Append invalidation notice; do not rewrite or delete history | Status is changed and any replacement/repair is linked | Owner + result reviewer |

## Freeze and deviations

Preregistration means the research question, hypothesis, null hypothesis, variables, evidence requirements, decision criteria, analysis formulas, protocol, and dataset version are frozen before the first outcome-bearing execution. A content-addressed repository commit or equivalent immutable review record establishes the freeze.

A protocol deviation is acceptable only to protect people, secrets, systems, or data integrity, or when a preregistered contingency explicitly permits it. The operator records the reason, timing, authorization, affected runs, and likely validity impact before continuing when feasible. Deviations never silently redefine criteria; material deviations normally produce `INCONCLUSIVE` or a new experiment version.

Every failed or aborted run receives an execution ID and retains its available command, parameters, environment, exit status, logs, partial observations, retry decision, and failure classification. Failed runs are not removed from denominators unless the frozen protocol explicitly defines that exclusion.

## Retention, replication, and correction

Negative (`NOT_SUPPORTED`) and `INCONCLUSIVE` results are retained with the same artifacts and review expectations as `SUPPORTED` results. Replications link the original experiment ID/version and execution IDs, state whether they are exact or conceptual, and link back from the original record when possible.

Invalidation records a discovered design, dataset, implementation, analysis, or integrity defect without deleting the experiment. The invalidation notice identifies who decided, when, why, which conclusions are affected, and any successor. `SUPERSEDED` means a newer record replaces practical use; it does not mean the old evidence was false.
