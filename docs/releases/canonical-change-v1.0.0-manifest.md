# Canonical Change v1.0.0 Release Manifest

## Identity

| Field | Value |
|---|---|
| Software release | Canonical Change v1.0.0 |
| Supported model contract | Canonical QA Model 0.1 |
| Release date | 2026-07-22 |
| Source branch | `feature/semantic-validation-phase-02` |
| Preparation baseline commit | `c1f79abc5a61ec832d1b016f420fdfb66f6a286b` |
| Release Git reference | `canonical-change-v1.0.0` |

The preparation baseline identifies the source inspected before packaging. The
annotated `canonical-change-v1.0.0` tag resolves to the immutable final release
commit. The manifest intentionally does not embed its own commit hash, avoiding
a self-referential commit-hash cycle.

## Toolchain

| Component | Version |
|---|---|
| Java language/toolchain | 21 |
| Verification runtime | Eclipse Adoptium JDK 21.0.7 |
| Gradle Wrapper | 8.14.3 |
| Project version currently declared by Gradle | 0.1.0-SNAPSHOT |

The Gradle project version is existing repository-wide metadata and is not the
Canonical Change software release label. The release label is v1.0.0; the
supported model contract is 0.1.

## Included source modules

- `qa-model-change` — released capability
- `qa-model` — Canonical QA Model contracts and schema
- `qa-model-validation-core` — authoritative schema and semantic validation

All repository modules participate in the full integration build, but are not
independently re-versioned by this Canonical Change release package.

## Verification commands and results

| Command | Result | Tests |
|---|---|---|
| `.\gradlew.bat clean :qa-model-change:test --no-daemon --console=plain` | PASS | 179 tests; 22 suites; 0 failures, errors, or skips |
| `.\gradlew.bat clean test --no-daemon --console=plain` | PASS | 502 tests; 86 suites; 0 failures, errors, or skips |
| `.\gradlew.bat :qa-model-change:check --no-daemon --console=plain` | PASS | Module test task up-to-date from clean repository run |
| `git diff --check` | PASS | Not applicable |
| Markdown local-link validation | PASS | 40 Markdown files checked |

## Artifact inventory

- Root overview: [`README.md`](../../README.md)
- Module contract: [`qa-model-change/README.md`](../../qa-model-change/README.md)
- Architecture: [`docs/canonical-change-architecture.md`](../canonical-change-architecture.md)
- Release notes: [`canonical-change-v1.0.0.md`](canonical-change-v1.0.0.md)
- Changelog: [`CHANGELOG.md`](../../CHANGELOG.md)
- ADR index: [`docs/adr/README.md`](../adr/README.md)
- Decision record: [`ADR-001`](../adr/ADR-001-exact-instance-evidence-binding.md)

## Packaging constraints

This source package does not imply binary publication, remote push, deployment,
or external release creation. Those remain separate, explicitly authorized
operations.
