# Canonical QA Model Change

`qa-model-change` is a framework-independent Canonical Change capability.

Phase 2 provides mutation-safe Node and Relationship Artifact State snapshots
and explicit Canonical QA Model v0.1 semantic equality. Semantic equality is
separate from Java value equality, and collection meaning remains owned by the
[Canonical QA Model Collection Semantics Specification v0.1](../docs/architecture/canonical-qa-model-collection-semantics-v0.1.md).

Artifact State construction establishes only a recognizable, defensively held
snapshot. It does not imply complete-model validation or successful Canonical
Change verification. Validation Core integration and the change-verification
lifecycle are intentionally not implemented yet.

Phase 3 adds immutable, untrusted Declared Change and Declared Change Set
values. Intrinsic validation returns expected declaration failures as explicit,
deterministically ordered outcomes and does not consult a Base Model. Passing
intrinsic validation does not produce a verified change; Base Model verification
and Proposed Model construction remain deferred.

Phase 4 verifies intrinsically valid declarations against an immutable Base
artifact index. `ADDED` requires target absence; `MODIFIED` and `REMOVED`
require target existence and a semantically matching before state. A
Base-verified candidate is not globally verified, and no Proposed Model is
built. Validation Core integration and aggregate validation remain deferred.

Phase 5 deterministically materializes an all-or-nothing candidate Proposed
Artifact Model while leaving the Base Model unchanged. `ADDED` inserts the
declared after state, `MODIFIED` replaces the target with its after state, and
`REMOVED` deletes only the exact target without cascading. Materialized does not
mean valid or verified; Validation Core and aggregate validation remain
deferred.
