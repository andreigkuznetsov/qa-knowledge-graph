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

## Smoke suite

The smoke suite checks:

1. valid model returns `valid=true`;
2. JSON Schema violation returns `valid=false` and `JSON_SCHEMA` issue;
3. unknown target node returns `UNKNOWN_TO_NODE`;
4. invalid relationship direction returns `RELATIONSHIP_NOT_ALLOWED`;
5. scenario without test returns warning while `valid=true`;
6. malformed JSON returns HTTP 400.

## Current boundary

The project validates a serialized QA graph but does not persist it yet. Neo4j and importers should be added only after the model and validation rules are stable.
