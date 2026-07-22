# Canonical QA Model Change

`qa-model-change` is the framework-independent, in-process domain module for
verifying an atomic declared change set against Canonical QA Model **v0.1**.
It is an internal trusted module, not an external SDK or serialization boundary.

## Public pipeline

Use the production entry points in this order:

| Stage | Entry point | Outcome |
|---|---|---|
| Intrinsic declarations | `IntrinsicChangeValidator` | intrinsic candidates, failures, ambiguity |
| Base truth | `BaseChangeVerifier` | Base-verified candidates or failures |
| Atomic materialization | `ProposedModelMaterializer` | Proposed Artifact Model or failure |
| Aggregate transition | `AggregateTransitionValidator` | endpoint-consistent model or failure |
| Root reconstruction | `ProposedCanonicalRootReconstructor` | complete Proposed Root or failure |
| Complete authority | `CompleteProposedRootValidator` | schema and semantic evidence |
| Finalization | `FinalChangeSetVerifier` | `VerifiedChangeSet` or `RejectedChangeSet` |

`VerifiedChangeSet` means every required stage passed against the exact retained
evidence. It does not mean persisted, committed, published, approved, deployed,
simulated, impact-assessed, or warning-free. Only `FinalChangeSetVerifier` can
construct it.

`RejectedChangeSet` is narrowly the rejection produced while finalizing a
`CompleteProposedRootValidationResult`. Earlier failures remain owned by their
stage-specific result types. Reachable final rejection stages are schema,
semantic, validation infrastructure, unsupported version, and final evidence
consistency.

Semantic warnings remain successful evidence. Their authoritative severity,
code, path, message, identity, and deterministic order are preserved.

## Domain policies

- Only the exact textual version `0.1` is supported; no migration occurs.
- Empty `DeclaredChangeSet` values are prohibited.
- A `MODIFIED` declaration whose before and after states are semantically equal
  is rejected as `MODIFIED_STATE_UNCHANGED`.
- Artifact identity is category-sensitive: a Node and Relationship may share an
  ID without collision.
- Successful stages retain the exact previous evidence instance. Substituted or
  independently reconstructed evidence is stale for this in-process contract.
- Equality is provenance-sensitive. Re-finalizing the same immutable Phase 8
  evidence is equal; independent chains need not be Java-equal. Compare their
  artifacts, root, diagnostics, warnings, and version for semantic determinism.
- Phase 6 checks transition referential integrity. Validation Core remains the
  authority for complete JSON Schema and semantic rules.

See [Canonical Change Architecture](../docs/canonical-change-architecture.md)
and [ADR-001: Exact-instance evidence binding](../docs/adr/ADR-001-exact-instance-evidence-binding.md).

## Verification

```powershell
.\gradlew.bat clean :qa-model-change:test --no-daemon --console=plain
.\gradlew.bat clean test --no-daemon --console=plain
.\gradlew.bat :qa-model-change:check --no-daemon --console=plain
git diff --check
```
