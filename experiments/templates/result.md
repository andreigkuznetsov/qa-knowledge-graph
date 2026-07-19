# Result report: EXP-NNNN v<version> / <execution ID>

> Report observations even when execution fails. Do not alter the frozen hypothesis after seeing results; revise it only in a new experiment version or a new experiment.

This report separates the **hypothesis** from the **experiment**, automated **functional test** results from research outcomes, raw **observations** from selected **evidence**, and the evidence from the bounded **conclusion**.

## Execution record

| Field | Value |
|---|---|
| Experiment ID/version | `EXP-NNNN` / `<version>` |
| Execution ID | `<unique immutable ID>` |
| Implementation commit | `<full Git SHA>` |
| Date/time | `<ISO 8601 start/end with timezone>` |
| Environment | `<OS, architecture, Java/JVM, hardware/container, locale, timezone>` |
| Dataset ID/version | `<ID>` / `<version>` |
| Protocol version | `<version and link>` |
| Reviewer | `<name/role>` |

## Actual execution parameters

- Exact command/script: `<value>`
- Parameters, seeds, repetition, timeouts, service/model versions: `<complete actual values>`
- Dependency versions/lock reference: `<value>`

## Raw artifacts

| Artifact | Locator | Checksum | Retention/access |
|---|---|---|---|
| `<stdout, stderr, manifest, observations, output>` | `<path/URI>` | `<checksum>` | `<policy>` |

## Observations and calculated metrics

| Metric | Per-run observations | Calculation | Result | Tolerance/uncertainty |
|---|---|---|---|---|
| `<name>` | `<values/locator>` | `<frozen formula>` | `<value/unit>` | `<value>` |

## Deviations, failures, and anomalies

- Protocol deviations: `<what, why, approver, affected runs and validity impact; or none>`
- Failed runs: `<execution IDs, exit status, retained artifacts, retry decision; or none>`
- Anomalies: `<unexpected observations and handling; or none>`

## Criteria comparison and outcome

| Frozen criterion | Evidence | Met? | Rationale |
|---|---|---|---|
| `<success/failure/inconclusive criterion>` | `<artifact/metric>` | `<yes/no>` | `<reason>` |

**Outcome:** `<SUPPORTED | NOT_SUPPORTED | INCONCLUSIVE>`

`SUPPORTED` is limited to this hypothesis, protocol, dataset, and recorded conditions; it is not permanent proof.

## Evidence-based conclusion

`<Conclusion supported directly by preserved evidence. Separate observations from interpretation.>`

## Interpretation limits

`<Populations, contexts, causal claims, and generalizations not justified by this execution.>`

## Threats observed during execution

| Threat | Evidence/observation | Effect on conclusion | Follow-up |
|---|---|---|---|
| `<threat>` | `<evidence>` | `<effect>` | `<action>` |

## Review and repeatability

- Reviewer decision/date: `<accepted/revision requested and ISO date>`
- Repeatability status: `<not attempted/repeated consistently/repeated inconsistently/blocked>`
- Replication links: `<experiment/execution IDs or none>`
- Report corrections after review: `<append-only correction record>`
