# QA Model

`qa-model` owns the Canonical QA Model vocabulary and its JSON Schema resource.

## Semantic specifications

JSON Schema defines the structural constraints of Canonical QA Model v0.1.
Collection equality and ordering meaning are defined by the normative
[Canonical QA Model Collection Semantics Specification v0.1](../docs/architecture/canonical-qa-model-collection-semantics-v0.1.md).

The `uniqueItems` JSON Schema keyword constrains duplicate values; by itself it
does not define whether a collection is ordered or order-insensitive.
