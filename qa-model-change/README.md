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

Phase 6 validates aggregate cross-artifact consistency of the materialized
Proposed Artifact Model. Every Relationship `from` and `to` endpoint must
resolve exactly to a Node in the final model; intermediate application order is
irrelevant and no cascading occurs. Aggregate-valid still does not mean complete
Canonical QA Model validity. Validation Core and final verification remain
deferred.

Phase 7 extracts a complete immutable Base root context together with its
artifact index, carries that exact evidence through the successful pipeline,
and deterministically reconstructs a full Proposed root after aggregate
validation. Root metadata is preserved exactly, while `schemaVersion`, `nodes`,
and `relationships` are reconstructed from version and Proposed artifact
evidence. Reconstruction is not complete Canonical QA Model validation;
Validation Core integration and final verification remain deferred.

Phase 8 validates the complete reconstructed Proposed root against the
authoritative JSON Schema before executing the authoritative Semantic
Validation Core. Schema and semantic evidence remain distinct, retained unknown
properties are diagnosed instead of discarded, and no normalization, repair,
or version migration is performed. Complete-valid does not mean persisted or
published, and final verification remains deferred.

Phase 9 composes the complete immutable Canonical Change evidence chain.
`VerifiedChangeSet` means that every required intrinsic, ambiguity, Base,
materialization, aggregate, reconstruction, schema, and semantic stage passed.
Semantic warnings remain visible and do not automatically invalidate success.
Verified does not mean persisted, published, committed, approved, or deployed.
Rejected results retain the actual deepest failing stage and its authoritative
evidence. Final verification is deterministic and introduces no transaction,
identifier, timestamp, Simulation, or Impact Analysis state.
