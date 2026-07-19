# Reproducibility Rules

These rules apply to QAIP research experiments. An experiment cannot receive `READY` status while any applicable MUST information is missing. “Not applicable” is acceptable only with a reviewed reason.

A reproducible **experiment** evaluates a frozen **hypothesis**; a reproducible **functional test** only demonstrates specified behavior for its cases. Recorded measurements are **observations**, the relevant preserved observations and artifacts are **evidence**, and the resulting **conclusion** must remain bounded by that evidence.

## MUST requirements

Before execution, the experiment specification, protocol, dataset card, and integrity manifest MUST record:

- full repository commit SHA, QAIP release, and Canonical/Normalized QA Model schema version;
- operating-system name, exact version and architecture, plus Java vendor and exact JVM version;
- Gradle Wrapper version and a dependency lock, resolved dependency report, container digest, or other exact dependency-version mechanism;
- input dataset ID/version, stable locator and access procedure;
- cryptographic checksums for every input or for a canonical manifest, including the checksum algorithm and verification command;
- deterministic input and output ordering rules, including tie-breakers;
- all clock, current-date, duration, and timezone dependencies, the timezone used, and clock synchronization assumptions;
- locale, character encoding, and any collation rules;
- every random seed and pseudo-random algorithm, or a reason randomness is absent;
- every external service, endpoint role, exact API/service/model version where available, and a strategy for unavailable or mutable services;
- for LLM use: provider, exact model identifier/version, system and user prompts or their checksums and stable locators, temperature, top-p, seed when supported, maximum output tokens, tool configuration, response format, retry policy, and provider-side settings known to affect output;
- secrets-handling procedure: secret names/references and required permissions, never secret values in source-controlled or raw artifacts;
- exact execution command or script revision, working directory, configuration, and all actualizable parameters;
- repeat count, warm-up if any, timeout, retry, abort, and failed-run accounting rules;
- raw observation format, preservation location, checksums, access control, and retention period;
- generated report and metric-output preservation location, checksums, generator/version, and retention period;
- exact metric formulas, aggregation, units, precision, and acceptable numeric tolerance;
- evidence validation procedure and preregistered `SUPPORTED`, `NOT_SUPPORTED`, and `INCONCLUSIVE` criteria.

At execution time, the operator MUST record actual values rather than relying only on planned values: execution ID, start/end timestamps with timezone, resolved environment and dependencies, verified input checksums, exact command and parameters, seeds, service/model identifiers, per-run status, deviations, raw artifacts, and calculated metrics. Raw evidence MUST not be overwritten by generated reports.

When an artifact cannot be committed because it is confidential, restricted, or too large, its metadata, stable controlled locator, checksum, access instructions, and retention policy MUST still be recorded. Secrets MUST be redacted before logs or prompts are preserved.

## SHOULD requirements

Experiments SHOULD:

- provide a container image, environment manifest, or automated environment check;
- work from a clean checkout and record working-tree state;
- run on more than one machine or have an independent reproduction when the claim warrants it;
- use machine-readable parameter, observation, and checksum manifests alongside readable reports;
- preserve stdout, stderr, exit codes, resource use, and intermediate calculations;
- pin mutable external inputs or cache permitted responses with provenance;
- separate dataset construction, hypothesis authoring, execution, and result review roles where practical;
- document hardware, concurrency, scheduling, network conditions, and warm-up when they could affect results;
- test metric-calculation code against a small manually verified fixture.

A SHOULD omission requires a brief rationale in the protocol or result when it could affect interpretation.

## Experiment-specific requirements

Each experiment MUST add controls required by its own threat model. Examples include annotator training and inter-rater procedure for human labels, project split and leakage prevention for datasets, stable graph traversal ordering for coverage/traceability, calibration for performance measurements, blinded task allocation for engineering-task studies, and prompt/context isolation for LLM grounding.

Experiment-specific requirements must be written before `READY`, state how compliance is observed, and define whether a violation causes a failed run, `INCONCLUSIVE`, or `INVALIDATED`. They may strengthen but never weaken the MUST requirements.

See the [experiment templates](../../experiments/templates/) and [lifecycle](EXPERIMENT_LIFECYCLE.md) for the required records and freeze points.
