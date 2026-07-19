# QA Knowledge Graph

Multi-module Gradle project for validating the Normalized QA Model before persistence in a graph database.

## Modules

### `qa-model`

Pure Java module containing:

- shared enums and validation response contracts;
- `qa-model-v0.1.schema.json`;
- no Spring Boot or database dependencies.

### `qa-model-validator`

Spring Boot REST service containing:

- `POST /api/v1/qa-model/validate`;
- registration, retrieval, listing, and tracing of in-memory QA models;
- `GET /api/v1/models/{modelId}/coverage` for registered-model coverage;
- `GET /api/v1/models/{modelId}/findings` for actionable structural gaps;
- `GET /api/v1/models/{modelId}/assessment` for a unified model assessment;
- JSON Schema Draft 2020-12 validation;
- semantic graph validation;
- integration and smoke tests.

## Required local files

Add Gradle Wrapper files from another Gradle project:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Use a Gradle 8.x wrapper compatible with Java 21 and Spring Boot 3.5.x.

## Commands

Windows:

```powershell
.\gradlew.bat clean test
.\gradlew.bat :qa-model-validator:smokeTest
.\gradlew.bat :qa-model-validator:bootRun
```

Linux/macOS:

```bash
./gradlew clean test
./gradlew :qa-model-validator:smokeTest
./gradlew :qa-model-validator:bootRun
```

## Endpoint

```http
POST /api/v1/qa-model/validate
Content-Type: application/json
```

Example request is located at:

```text
docs/qa-model-v0.1.example.json
```

Registered-model coverage is available after `POST /api/v1/models`:

```http
GET /api/v1/models/{modelId}/coverage
```

The response reports three structural metrics in stable order:
`RULE_SCENARIO_COVERAGE`, `SCENARIO_TEST_COVERAGE`, and
`TEST_CHECK_COVERAGE`. They describe explicit relationships in the model;
they do not represent test execution status or successful test results. The
current model does not prove that a particular `CHECK` validates a particular
`BUSINESS_RULE`. Empty node categories are represented by
`coveragePercent: 0.0`.

Registered-model findings use these stable codes and severities:

- `BUSINESS_RULE_WITHOUT_SCENARIO` — `HIGH`;
- `SCENARIO_WITHOUT_TEST` — `MEDIUM`;
- `TEST_WITHOUT_CHECK` — `MEDIUM`.

Validation determines whether a model is acceptable. Coverage measures
structural completeness. Findings identify exact nodes that require action.
Findings are structural: they do not represent test execution results, and the
model cannot prove that a particular `CHECK` validates a particular
`BUSINESS_RULE`.

The assessment endpoint is an orchestration API: it aggregates the existing
validation, coverage, and findings results and adds an overall `PASS`,
`WARNING`, or `FAIL` health value. It does not introduce another analysis or
include exploratory trace results.

## Smoke suite

The smoke suite checks:

1. valid model returns `valid=true`;
2. JSON Schema violation returns `valid=false` and `JSON_SCHEMA` issue;
3. unknown target node returns `UNKNOWN_TO_NODE`;
4. invalid relationship direction returns `RELATIONSHIP_NOT_ALLOWED`;
5. scenario without test returns warning while `valid=true`;
6. malformed JSON returns HTTP 400.

## Current boundary

The project keeps registered QA models in memory. Coverage is calculated on
demand with the existing coverage engine. No external database or inferred
graph relationships are used.
