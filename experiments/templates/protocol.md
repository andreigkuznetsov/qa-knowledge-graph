# Protocol: EXP-NNNN v<version>

> Complete every item or mark it not applicable with a reason. This protocol must be reviewed and frozen before execution.

This protocol executes an **experiment** that tests a frozen **hypothesis**; it is not a **functional test** specification. It defines how **observations** are collected and preserved as **evidence** for a bounded **conclusion**.

## Identity and prerequisites

- Experiment/version: `EXP-NNNN` / `<version>`
- Protocol version: `<version>`
- Required access and permissions: `<details>`
- Required tools, services, credentials, disk, and memory: `<details>`
- Preconditions and verification commands: `<details>`

## Environment and exact software versions

| Component | Exact version/image/digest | Verification command |
|---|---|---|
| Repository commit | `<full SHA>` | `git rev-parse HEAD` |
| QAIP release/schema | `<release>` / `<schema>` | `<method>` |
| Operating system | `<name, version, architecture>` | `<command>` |
| Java/JVM | `<vendor and exact version>` | `java -version` |
| Gradle/dependencies | `<wrapper and resolved/locked versions>` | `<command/file>` |
| Other software/service | `<version/model/revision>` | `<method>` |

- Locale: `<value>`
- Timezone and clock assumptions: `<value/synchronization>`
- Hardware/runtime constraints: `<CPU, memory, accelerator, container>`
- External service state and availability assumptions: `<details>`

## Dataset

- Dataset ID/version: `<ID>` / `<version>`
- Dataset-card link: [`dataset.md`](dataset.md)
- Location and access procedure: `<details>`
- Input checksums and verification command: `<details>`

## Preparation

1. `<Exact ordered step, command, expected state, and validation.>`
2. `<Freeze/configure inputs without changing observations.>`

## Execution design

- Exact command or script: `<copy-pasteable command>`
- Working directory: `<path>`
- Configuration and parameters: `<complete values>`
- Deterministic ordering rule: `<rule>`
- Number of repetitions: `<integer and rationale>`
- Random seed(s): `<values and generation method, or N/A with reason>`
- Warm-up/cool-down: `<details>`
- Timeout per run/overall: `<duration and action>`

### Procedure

1. `<Ordered execution step with expected observable checkpoint.>`
2. `<Record execution ID and immutable parameter manifest.>`
3. `<Preserve stdout, stderr, exit code, timestamps, and outputs.>`

## Failure handling and deviations

- Failed runs remain recorded with execution ID, logs, partial artifacts, and classification.
- Retry conditions and maximum retries: `<preregistered rule>`
- Abort conditions: `<rule>`
- Permitted deviation for safety or data integrity: `<rule and approver>`
- Any deviation must be recorded in the result and may force `INCONCLUSIVE`; it must never be silently incorporated.

## Data collection and metrics

| Observation | Source | Collection timing/method | Raw format/location |
|---|---|---|---|
| `<name>` | `<source>` | `<method>` | `<path/locator>` |

| Metric | Exact formula | Aggregation | Units/precision | Accepted tolerance |
|---|---|---|---|---|
| `<name>` | `<formula>` | `<method>` | `<unit>` | `<tolerance>` |

## Result validation

1. `<Integrity, schema, completeness, and plausibility checks.>`
2. `<Comparison procedure against frozen success/failure/inconclusive criteria.>`
3. `<Independent review or calculation cross-check.>`

## Cleanup

1. `<Stop services and remove temporary material without deleting retained evidence.>`
2. `<Confirm artifact upload, checksum, access control, and retention.>`

## Reproducibility checklist

- [ ] Repository commit, release, schema, OS, Java, and dependencies are exact.
- [ ] Dataset version, location, access, and input checksums are recorded and verified.
- [ ] Locale, timezone, deterministic ordering, seeds, services, and secrets handling are specified.
- [ ] Exact command, parameters, repetition count, timeouts, retries, and failure rules are specified.
- [ ] Collection, metric formulas, tolerances, validation, artifact locations, and cleanup are specified.
- [ ] The protocol and hypothesis were reviewed and frozen before execution.
- [ ] All applicable rules in [`../../docs/research/REPRODUCIBILITY_RULES.md`](../../docs/research/REPRODUCIBILITY_RULES.md) are satisfied.
