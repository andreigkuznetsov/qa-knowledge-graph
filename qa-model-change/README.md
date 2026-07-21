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
