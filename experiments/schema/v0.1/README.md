# QAIP Experiment JSON Schemas v0.1

This directory contains Draft 2020-12 machine-readable contracts for the Commit-01 research terminology:

- [`experiment-definition.schema.json`](experiment-definition.schema.json) describes one version of an experiment specification, its frozen hypothesis, criteria, protocol/dataset references, and reproducibility plan.
- [`experiment-result.schema.json`](experiment-result.schema.json) describes one immutable execution result referencing a frozen definition. It deliberately has no field for replacing or revising the hypothesis.

Files use the repository convention `<subject>.schema.json`. Schema version `0.1` identifies this JSON contract; `version`, `experimentVersion`, protocol version, dataset version, and result version identify independently versioned research artifacts. Status (`DRAFT`, `READY`, `RUNNING`, `COMPLETED`, `INVALIDATED`, `SUPERSEDED`) describes repository lifecycle. Outcome (`SUPPORTED`, `NOT_SUPPORTED`, `INCONCLUSIVE`) describes the bounded evaluation of a hypothesis.

Definition and result are separate so execution observations cannot mutate the preregistered contract. `READY`, `RUNNING`, and `COMPLETED` definitions require a frozen hypothesis, complete protocol/dataset integrity references, non-empty criteria, evidence requirements, and reproducibility information. `INVALIDATED` and `SUPERSEDED` require history-preserving decision metadata. Results require raw artifacts, observations, criterion evaluations, exact execution identity, and a bounded conclusion. LLM metadata is conditional on service type `LLM`; secret values are never represented.

## Validation boundary

JSON Schema can enforce object shape, required fields, strict enums, ID/checksum/timestamp patterns, selected state conditions, and the presence of structured comparisons or review procedures. It cannot determine whether a hypothesis is genuinely falsifiable, a dataset is representative, a protocol is unbiased, observations are authentic, metrics are statistically appropriate, evidence is sufficient, or a conclusion is scientifically justified.

Structural validity is not scientific validity. Schema-valid criteria can still be poorly designed, and schema-valid evidence can still be insufficient. Protocol review, execution discipline, statistical or domain analysis, and reviewer judgment remain necessary.

## Examples and validation

The [`examples/valid/`](examples/valid/) records are illustrative schema-calibration data. They do not claim that the described experiment was executed. Each file in [`examples/invalid/`](examples/invalid/) is derived from a valid example and changes one focused rule:

| Invalid example | Expected failure |
|---|---|
| `invalid-experiment-id.json` | `experimentId` does not match `EXP-NNNN` |
| `invalid-status.json` | status is outside the exact repository enum |
| `ready-without-frozen-contract.json` | `READY` requires `hypothesis.frozen: true` and freeze metadata |
| `ready-without-protocol-checksum.json` | `READY` protocol requires a checksum |
| `ready-without-dataset-integrity-reference.json` | `READY` dataset requires an integrity reference |
| `automated-criterion-without-comparison.json` | automated criterion requires a comparison |
| `result-with-invalid-outcome.json` | outcome is outside the exact outcome enum |
| `result-without-evidence.json` | at least one raw artifact is required |
| `result-with-malformed-criterion-id.json` | criterion reference does not match `CRIT-NNN`/`CRIT-NNNN` |
| `invalid-git-commit-sha.json` | commit is not a full 40-hex SHA |
| `invalid-timestamp-without-timezone.json` | timestamp lacks `Z` or a numeric offset |
| `invalid-additional-property.json` | undeclared root property is rejected |

Run schema-focused validation from the repository root:

```powershell
.\gradlew.bat :qa-model-validation-core:test --tests "*ExperimentSchemaContractTest"
```

For a future compatible v0.1 clarification, retain validation meaning and append documentation. Any breaking required-field, enum, pattern, or semantic-shape change receives a new schema directory/version and migration notes; existing experiment/result records retain their original `schemaVersion` and schema reference.
