# Experiment specification: EXP-NNNN — <title>

> Replace every placeholder. Criteria must be observable, measurable, and falsifiable before status becomes `READY`.

## Metadata

| Field | Value |
|---|---|
| Experiment ID | `EXP-NNNN` |
| Title | `<title>` |
| Version | `<version>` |
| Status | `DRAFT` (`DRAFT`, `READY`, `RUNNING`, `COMPLETED`, `INVALIDATED`, or `SUPERSEDED`) |
| Owner | `<name/role>` |
| Date created | `<ISO 8601 date>` |
| Last updated | `<ISO 8601 date>` |
| Related epic/release | `<for example EP-0001 / QAIP 0.7>` |

## Research question

`<Question answerable from defined observations and evidence?>`

## Hypotheses

**Hypothesis:** If `<independent-variable change>`, then `<measurable dependent-variable effect>` under `<conditions>`, because `<testable rationale>`.

**Null hypothesis:** `<Explicit alternative/no-effect statement evaluated by the same metrics.>`

The hypothesis and null hypothesis are frozen before execution. A post-result revision requires a new experiment version or a new experiment.

## Motivation

`<Why this question matters; do not state the expected result as fact.>`

## Scope

`<Populations, systems, tasks, and conditions included.>`

## Out of scope

`<Explicit exclusions and conclusions this experiment cannot support.>`

## Variables

| Kind | Variable | Values/measurement | Control or rationale |
|---|---|---|---|
| Independent | `<name>` | `<manipulation>` | `<rationale>` |
| Dependent | `<name>` | `<unit and calculation>` | `<measurement>` |
| Controlled | `<name>` | `<fixed value>` | `<control method>` |

## Assumptions

- `<Assumption and how its violation will be detected.>`

## Required evidence

- `<Observation or artifact, source, collection method, and retention location.>`

## Preregistered decision criteria

Use numeric thresholds, named comparisons, sample sizes, and tolerances. “Works well” and similar judgments are invalid criteria.

- **Success / `SUPPORTED`:** `<All conditions that must be met.>`
- **Failure / `NOT_SUPPORTED`:** `<Conditions showing the hypothesis is not supported.>`
- **Inconclusive / `INCONCLUSIVE`:** `<Missing evidence, conflicting measures, insufficient sample, excessive failures, or validity threats.>`

`SUPPORTED` is conditional evidence, not permanent proof.

## Threats to validity

| Threat | Internal/external/construct/conclusion | Mitigation | Residual risk |
|---|---|---|---|
| `<threat>` | `<type>` | `<action>` | `<risk>` |

## Ethical, security, and confidentiality considerations

`<Human impact, consent if relevant, sensitive data, licenses, access controls, secrets, and disclosure constraints. State “none identified” with rationale if applicable.>`

## Related artifacts

- Protocol: [`protocol.md`](protocol.md)
- Dataset card: [`dataset.md`](dataset.md)
- Results: `<execution directories under results/ after execution>`
- `<Related experiment, issue, model, or analysis artifact>`

## Change history

| Date | Version | Author | Change | Freeze impact |
|---|---|---|---|---|
| `<date>` | `<version>` | `<author>` | `<change>` | `<none/new version/new experiment>` |
