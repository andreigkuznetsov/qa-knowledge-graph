# QAIP Research Experiments

This directory records reproducible experiments used to evaluate QAIP research hypotheses. An experiment asks a falsifiable research question and produces observations, evidence, and a bounded conclusion. It is not a substitute for automated testing.

## Experiments and tests

- A **hypothesis** is a falsifiable prediction stated before execution.
- An **experiment** follows a frozen protocol on a versioned dataset to test a hypothesis.
- A **functional test** (unit, integration, smoke, or acceptance test) checks whether implemented behavior meets a specification. Passing tests establish conformance for the tested cases; they do not validate a research hypothesis.
- An **observation** is a recorded measurement or event from an execution.
- **Evidence** is the preserved set of observations and artifacts used to evaluate stated criteria.
- A **conclusion** is the evidence-bounded interpretation of an outcome.

## Identifiers, versions, and status

Experiment IDs use `EXP-NNNN`, for example `EXP-0001`. They are distinct from epic IDs such as `EP-0001`. Material changes to a frozen hypothesis, protocol, dataset, or criteria require a new experiment version; a different research question normally requires a new experiment ID.

Allowed experiment statuses are `DRAFT`, `READY`, `RUNNING`, `COMPLETED`, `INVALIDATED`, and `SUPERSEDED`. Allowed hypothesis outcomes are `SUPPORTED`, `NOT_SUPPORTED`, and `INCONCLUSIVE`. `SUPPORTED` means only that the specified evidence met the preregistered criteria under the recorded conditions; it does not mean permanently proven.

## Experiment layout

Create one directory per experiment:

```text
experiments/
  EXP-0001-short-name/
    experiment.md
    protocol.md
    dataset.md
    inputs/                 # small, redistributable frozen inputs
    expected/               # preregistered expected outputs or criteria fixtures
    results/
      EXEC-YYYYMMDD-NNNN/
        result.md
        raw/                # preserved raw observations, when committable
        reports/            # generated reports selected for preservation
```

Copy and complete the files in [`templates/`](templates/). Dataset files may live under `inputs/`, in an approved artifact store, or at a controlled external location; `dataset.md` must identify the location, version, access conditions, and integrity checks. Expected outputs belong under `expected/`. Actual outputs belong under the execution directory in `results/`, never in `expected/`.

## Reproduction

To reproduce an experiment, obtain the recorded repository commit and dataset version, verify input checksums, recreate the protocol environment, run the exact recorded command with the recorded parameters and repetition count, preserve raw output, calculate metrics as specified, and compare them with the preregistered criteria. Follow the mandatory [reproducibility rules](../docs/research/REPRODUCIBILITY_RULES.md) and lifecycle in [EXPERIMENT_LIFECYCLE.md](../docs/research/EXPERIMENT_LIFECYCLE.md).

Source-control specifications, protocols, dataset cards, expected fixtures, result reports, small redistributable inputs, checksums, and reviewable raw results. Do not commit secrets, confidential data, licensed data that cannot be redistributed, local caches, build directories, temporary files, or large generated artifacts better held in an artifact store. Every excluded artifact must still have a stable locator, retention policy, checksum, and access instructions in the result report. Repository ignore rules may be added only when actual generated paths exist; this contract does not claim an experiment runner or report generator is implemented.
