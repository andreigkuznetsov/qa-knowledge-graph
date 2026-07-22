# QA Knowledge Graph

QA Knowledge Graph is a multi-module Java 21 platform for deterministic
validation, analysis, and planning over the Canonical QA Model.

## Canonical Change v1.0.0

Canonical Change is the release-ready, framework-independent change-verification
capability in [`qa-model-change`](qa-model-change/README.md). Software release
**v1.0.0** supports the Canonical QA Model schema version **0.1**. These are
separate version domains: v1.0.0 is the software release; 0.1 is the model
contract accepted by this release.

The pipeline validates declared changes intrinsically, verifies Base truth,
materializes the proposed model atomically, checks aggregate references,
reconstructs the canonical root, delegates complete schema and semantic
authority to Validation Core, and emits final verified or rejected evidence.

## Release documentation

- [Canonical Change v1.0.0 release notes](docs/releases/canonical-change-v1.0.0.md)
- [Release manifest](docs/releases/canonical-change-v1.0.0-manifest.md)
- [Canonical Change architecture](docs/canonical-change-architecture.md)
- [Canonical Change ADR index](docs/adr/README.md)
- [Changelog](CHANGELOG.md)

## Repository modules

The release centers on `qa-model-change` and directly depends on `qa-model` and
`qa-model-validation-core`. Other modules provide the validator service and
downstream coverage, findings, roadmap, execution-planning, impact-analysis,
simulation, extraction, story-analysis, and QA intelligence capabilities.
See [module responsibilities](docs/architecture/module-responsibilities.md) and
the [system architecture](docs/architecture/system-architecture.md).

## Requirements

- JDK 21
- the checked-in Gradle Wrapper (Gradle 8.14.3)

## Build and verification

From the repository root on Windows:

```powershell
.\gradlew.bat clean :qa-model-change:test --no-daemon --console=plain
.\gradlew.bat clean test --no-daemon --console=plain
.\gradlew.bat :qa-model-change:check --no-daemon --console=plain
git diff --check
```

On Linux or macOS, replace `.\gradlew.bat` with `./gradlew`.

## Current boundaries

Canonical Change is a trusted, in-process verification library. It does not
persist, publish, approve, deploy, execute, simulate, migrate, repair, or
impact-assess a model. See the release notes for the complete limitations and
public API summary.
