# Canonical Change v1.0.0 Release Notes

## 1. Release identity

- Software: Canonical Change
- Release: v1.0.0
- Release date: 2026-07-22
- Module: `qa-model-change`
- Supported Canonical QA Model: exact version `0.1`

The software release and model schema use independent version domains.

## 2. Executive summary

Canonical Change v1.0.0 provides a deterministic, immutable, in-process domain
pipeline for deciding whether an atomic declared change set is valid against a
known Canonical QA Model 0.1 Base and for retaining the evidence behind that
decision.

## 3. Release verdict

**CANONICAL CHANGE v1.0.0 RELEASE READY**

This verdict is conditional only on completing the documented local commit and
annotated-tag procedure.

## 4. Scope

The release covers declaration modeling, intrinsic validation, Base evidence
extraction and verification, atomic materialization, aggregate transition
validation, canonical-root reconstruction, authoritative complete validation,
and final verification.

## 5. Non-scope

The release does not persist, commit, publish, approve, deploy, execute,
simulate, impact-assess, migrate, repair, or generate models. It provides no
REST API, Spring component, database integration, or LLM integration.

## 6. Supported model contract

Only the exact textual Canonical QA Model version `0.1` is accepted throughout
the evidence chain. The release performs no coercion or version migration.

## 7. Pipeline

The production order is:

1. `IntrinsicChangeValidator`
2. `BaseChangeVerifier`
3. `ProposedModelMaterializer`
4. `AggregateTransitionValidator`
5. `ProposedCanonicalRootReconstructor`
6. `CompleteProposedRootValidator`
7. `FinalChangeSetVerifier`

Base evidence extraction precedes this flow. See the detailed
[architecture diagram](../canonical-change-architecture.md#5-pipeline-diagram).

## 8. Public API summary

| Concern | Primary public API | Public outcome |
|---|---|---|
| Declarations | `DeclaredChange`, `DeclaredChangeSet` | immutable requested transition |
| Base evidence | `CanonicalBaseEvidenceExtractor` | extracted evidence or extraction failure |
| Intrinsic rules | `IntrinsicChangeValidator` | `IntrinsicChangeSetResult` |
| Base truth | `BaseChangeVerifier` | `BaseChangeSetResult` |
| Materialization | `ProposedModelMaterializer` | `ProposedModelMaterializationResult` |
| Aggregate integrity | `AggregateTransitionValidator` | `AggregateTransitionValidationResult` |
| Root reconstruction | `ProposedCanonicalRootReconstructor` | `ProposedRootReconstructionResult` |
| Complete authority | `CompleteProposedRootValidator` | `CompleteProposedRootValidationResult` |
| Finalization | `FinalChangeSetVerifier` | `VerifiedChangeSet` or `RejectedChangeSet` |

Public result interfaces are read contracts. Successful provenance-bearing
implementations are created by their owning production services, not by
consumers or transport deserialization.

## 9. Change semantics

`ADDED` requires target absence and an after state. `MODIFIED` requires an exact
matching Base/before state and a materially different after state. `REMOVED`
requires a matching Base/before state and removes only that target. Empty change
sets and semantic no-op modifications are prohibited.

## 10. Identity semantics

Logical identity is `(ArtifactCategory, CanonicalIdentity)`. IDs are exact,
case-sensitive, and unnormalized. Node and Relationship namespaces are distinct,
so the same text ID may safely occur in both categories.

## 11. Atomicity

Materialization applies the complete declared set or produces failure evidence;
it never exposes a partially applied proposed model.

## 12. Validation authority

Aggregate validation owns transition referential integrity. The
`qa-model-validation-core` module remains the sole authority for complete JSON
Schema and semantic validation. Schema validation runs before semantic
validation, and schema failure prevents semantic execution.

## 13. Evidence and provenance

Every successful stage retains mandatory evidence from the prior stage. Later
stages require the exact in-process instance, preventing stale or substituted
evidence from advancing. See [ADR-001](../adr/ADR-001-exact-instance-evidence-binding.md).

## 14. Determinism and immutability

Inputs containing mutable JSON are defensively copied, result collections are
immutable, ordering is explicit and stable, and the pipeline generates no time,
UUID, or revision values. Java equality remains provenance-sensitive; semantic
comparison should use artifacts, reconstructed JSON, diagnostics, warnings, and
version.

## 15. Warnings and diagnostics

Semantic warnings preserve authoritative severity, code, path, message,
identity, and deterministic order. Warnings do not turn otherwise successful
complete validation into failure. Each earlier pipeline stage owns its own
failure type; final rejection is intentionally limited to finalization of the
complete-validation result.

## 16. Security and trust boundary

The module is a trusted in-process library rather than an external SDK or wire
protocol. Protected construction of successful evidence, mandatory provenance,
exact version checks, defensive copies, and final consistency checks reduce the
risk of forged, stale, or mutated evidence inside the supported boundary.

## 17. Compatibility and dependencies

The release requires Java 21 and Gradle 8.14.3 for the documented build. The
module depends directly on `qa-model`, `qa-model-validation-core`, and Jackson
Databind. It has no Spring Boot or database dependency.

## 18. Verification evidence

The release gate exercises intrinsic and Base truth, stale evidence,
materialization atomicity, complete schema and semantic failure, warning success,
mutation safety, deterministic ordering, same-ID category safety, and final
provenance. The complete repository build also verifies downstream integration.
Exact commands and counts are recorded in the
[release manifest](canonical-change-v1.0.0-manifest.md).

## 19. Known limitations

- Pipeline orchestration is caller-owned; there is no single façade operation.
- Evidence is process-local and cannot be serialized and resumed.
- Only Canonical QA Model 0.1 is supported.
- No migration or compatibility adapter is provided.
- No persistence, transaction manager, transport contract, or distributed
  provenance mechanism is provided.
- Verification does not imply business approval, persistence, publication,
  deployment, execution success, impact, or absence of warnings.

## 20. Upgrade and adoption guidance

Consumers should invoke the public services in the documented order, retain the
exact returned evidence instances, handle every sealed/result alternative, and
treat `VerifiedChangeSet` only as verification evidence. Consumers upgrading a
model contract other than 0.1 must wait for an explicitly compatible release or
perform migration outside this module.

## 21. Release procedure

After all verification commands pass and the working tree contains only the
reviewed release candidate:

1. Commit the reviewed release package, for example with message
   `Release Canonical Change v1.0.0`.
2. Create an annotated tag: `git tag -a canonical-change-v1.0.0 -m "Canonical Change v1.0.0"`.
3. Verify the tag points to the intended commit and that its tree is clean.
4. Push the branch and tag only after normal project review and authorization.

This document does not authorize a remote push, publication, deployment, or
external release creation.
