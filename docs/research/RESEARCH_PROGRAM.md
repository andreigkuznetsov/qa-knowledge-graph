# QAIP 0.7 Research Program

## Purpose

QAIP 0.7 is a research-oriented release. Its implementation is an instrument for testing hypotheses, not evidence by its existence. The current Canonical QA Model (also represented in the repository as Normalized QA Model v0.1), semantic validation, coverage analysis, traceability analysis, findings, and metrics are research artifacts and engineering capabilities; they are not automatically validated scientific claims.

The broad research direction is whether explicit, evidence-backed relationships among QA artifacts improve engineering analysis and decision support. This is a research direction, not a proven central hypothesis. Each narrower claim must be expressed as a falsifiable hypothesis and evaluated by a versioned experiment with a reviewed protocol, frozen dataset, preserved observations, identified evidence, and bounded conclusion.

EP-0001, **Research Infrastructure**, defines documentation and infrastructure for that work. EP-0001 does not itself validate the main hypothesis, and this Commit-01 does not implement an experiment runner.

## Terms and evidence standard

- A **hypothesis** is a falsifiable prediction frozen before execution.
- An **experiment** evaluates it under a versioned protocol and dataset.
- A **functional test** checks implemented behavior against a specification; it does not by itself test a research claim.
- An **observation** is a recorded measurement or event.
- **Evidence** is the preserved set of relevant observations and artifacts used against preregistered criteria.
- A **conclusion** is an interpretation bounded by that evidence and the observed threats to validity.

Experiment outcomes are `SUPPORTED`, `NOT_SUPPORTED`, or `INCONCLUSIVE`. `SUPPORTED` is conditional and does not mean permanently proven.

## Candidate research areas

The following areas are candidates for future hypotheses and experiments. Listing them assigns no completed work and makes no claim of academic novelty:

- model adequacy;
- inter-rater agreement;
- traceability accuracy;
- coverage validity;
- engineering-task performance;
- explainability;
- change-impact accuracy;
- LLM grounding.

Research records follow the [experiment lifecycle](EXPERIMENT_LIFECYCLE.md), [reproducibility rules](REPRODUCIBILITY_RULES.md), and repository [experiment contract](../../experiments/README.md). Directional statements in architecture vision documents remain aspirations until supported by applicable experiments.
