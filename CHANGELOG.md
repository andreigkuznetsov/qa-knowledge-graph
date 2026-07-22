# Changelog

All notable changes to this project are documented here.

## [1.0.0] - 2026-07-22

### Added

- Canonical Change's complete nine-stage verification pipeline for Canonical QA
  Model 0.1.
- Immutable, deterministic evidence and diagnostic contracts for intrinsic,
  Base, materialization, aggregate, root, complete, and final verification.
- Authoritative JSON Schema and semantic validation through
  `qa-model-validation-core`.
- Exact-instance provenance binding and final evidence-consistency checks.
- Integrated release-gate coverage, architecture documentation, release notes,
  release manifest, and ADR documentation.

### Security and correctness

- Successful provenance-bearing outcomes are constructed only by their owning
  production stages.
- Mutable JSON input is isolated by defensive copying.
- Empty change sets, semantic no-op modifications, stale evidence, unsupported
  versions, dangling references, and substituted final evidence are rejected.

### Compatibility

- Requires Java 21 and supports the exact Canonical QA Model version `0.1`.
- No persistence, transport, migration, or cross-process resume contract is
  included in this release.
