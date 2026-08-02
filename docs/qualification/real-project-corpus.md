# QAIP Real Project Corpus 1.0

## Corpus Roles

- **Regression baseline** — protects mature supported behavior and release continuity.
- **Mandatory acceptance** — verifies supported capabilities across materially different real services.
- **Extended discovery** — exposes future engineering domains without claiming current support.
- **Preparation required** — identifies reproducibility work needed before execution-based qualification.

## Mandatory Qualification Set

### BookShop

- **Repository:** https://github.com/andreigkuznetsov/java-jabki-final-project-api
- **Pinned reviewed revision:** `760c0b55022db735e65c0a952c3cd18f3d807001`
- **Role:** Regression baseline; Mandatory acceptance
- **Stack and tests:** Spring Boot 2.7, Spring MVC, Javax Bean Validation, Spring Data JPA, REST Assured, JUnit 5, Hamcrest, parameterized tests, helper-mediated assertions, and deterministic enum endpoints.
- **Currently exercised QAIP capabilities:** REST operation extraction, request-model binding, standard validation evidence, direct implementation flow, REST Assured test correlation, helper assertions, and direct assertion evidence.
- **Future/discovery patterns:** custom validation, transactions, Spring Security/JWT/roles, controller advice, and service-to-service flows.
- **Environment and repeatability:** Java 21 and installed Gradle; PostgreSQL/application environment required for execution; no committed Gradle wrapper. Static qualification remains possible without runtime execution.

### order-service-redis-sdet

- **Repository:** https://github.com/andreigkuznetsov/order-service-redis-sdet
- **Pinned reviewed revision:** `9e13a6e3e79d96d6503b8a4377f89b5448d12cfa`
- **Role:** Mandatory acceptance; Future capability discovery
- **Stack and tests:** Spring Boot 3.3, Spring MVC, Jakarta validation, records, JPA/PostgreSQL, REST Assured, AssertJ, Hamcrest, and JUnit 5.
- **Currently exercised QAIP capabilities:** REST operation extraction, request-model binding, broader standard validation, supported direct implementation flows, REST Assured test evidence, and direct assertion evidence.
- **Future/discovery patterns:** Redis cache, TTL and invalidation, rate limiting, idempotency, distributed lock, Flyway, Awaitility, Testcontainers, concurrency tests, k6, and Actuator.
- **Environment and repeatability:** Java 21 and installed Gradle; Docker for PostgreSQL and Redis Testcontainers; no committed Gradle wrapper.

### order-events-kafka-tests

- **Repository:** https://github.com/andreigkuznetsov/order-events-kafka-tests
- **Pinned reviewed revision:** `213dad90662a75017659d633256d0b53778acaff`
- **Role:** Mandatory acceptance; Future capability discovery
- **Stack and tests:** Spring Boot 3, Spring MVC, Jakarta validation, Spring Data JPA, JUnit 5, AssertJ, and unit and integration tests.
- **Currently exercised QAIP capabilities:** REST operation extraction, request-model binding, standard validation, Spring Data implementation evidence, and direct assertion evidence.
- **Future/discovery patterns:** Kafka producers and consumers, topics and routing, retry and DLQ, TestRestTemplate, Awaitility, Testcontainers, Mockito, Actuator, Micrometer, Prometheus, Grafana, and GitHub Actions.
- **Environment and repeatability:** Java 21; committed Gradle wrapper; Docker for Kafka and PostgreSQL Testcontainers. This repository has the strongest execution reproducibility in the mandatory set.

### rabbitmq-notification-service

- **Repository:** https://github.com/andreigkuznetsov/rabbitmq-notification-service
- **Pinned reviewed revision:** `5232ae82bbb21c9fd9000102b2d99b5c0aedaf5f`
- **Role:** Mandatory acceptance; Future capability discovery
- **Stack and tests:** Spring Boot 3.5, Spring MVC, Jakarta validation, Spring Data JPA, JUnit 5, AssertJ, and integration and asynchronous tests.
- **Currently exercised QAIP capabilities:** REST operation extraction, request-model binding, standard validation, direct implementation-flow variants, and direct assertion evidence.
- **Future/discovery patterns:** RabbitMQ producer and consumer, exchange/queue/binding topology, retry queues and DLQ, Awaitility, Testcontainers, transactions, provider port/adapter and deterministic stub, and Actuator/Prometheus endpoint.
- **Environment and repeatability:** Installed Gradle; Docker for RabbitMQ and PostgreSQL Testcontainers; no committed Gradle wrapper.

## Extended Discovery Corpus

### graphQL-API-project

- **Repository:** https://github.com/andreigkuznetsov/graphQL-API-project
- **Pinned reviewed revision:** `158e5b79bffe9439dc9e19fd7d662403618740d2`
- **Discovery purpose:** GraphQL query, mutation, and subscription contracts; GraphQL HTTP transport; GraphQL WebSocket lifecycle; multi-layer test architecture; timeout and asynchronous patterns.
- **Preparation required:** Reproducible GraphQL, WebSocket, and mail-service environment.
- **Why not mandatory:** GraphQL semantics and multi-level correlation are not current M6 supported capabilities.

### grpc-account-tests

- **Repository:** https://github.com/andreigkuznetsov/grpc-account-tests
- **Pinned reviewed revision:** `6d6607ba0d6a58d2c8363fc67348130c7ce53dc9`
- **Discovery purpose:** Protobuf contracts, unary RPC, server/client/bidirectional streaming, gRPC status assertions, and async observer testing.
- **Preparation required:** Replace the default external host with a reproducible local or containerized service.
- **Why not mandatory:** gRPC/protobuf extraction is not supported by the current REST-oriented pipeline.

### llm-qa-demo

- **Repository:** https://github.com/andreigkuznetsov/llm-qa-demo
- **Pinned reviewed revision:** `a4a6c453e795df3345e0b9dba05a763010999fbb`
- **Discovery purpose:** Evaluation datasets, parameterized QA checks, hallucination/grounding/security evaluation, LLM-as-judge, retrieval metrics, tagged suites, CI, and artifacts.
- **Preparation required:** Pin Ollama models and separate deterministic contract checks from probabilistic evaluations.
- **Why not mandatory:** AI evaluation evidence and probabilistic semantics are not current M6 capabilities.

### wiregate_tests

- **Repository:** https://github.com/andreigkuznetsov/wiregate_tests
- **Pinned reviewed revision:** `543162304ee2cbd331edcf06f4f0a6af10ec5db7`
- **Discovery purpose:** Cross-layer API/UI/database validation, REST Assured, Selenide, and direct JDBC verification and cleanup.
- **Preparation required:** Reproducible frontend, backend, and PostgreSQL environment; removal of hard-coded local assumptions.
- **Why not mandatory:** UI and direct JDBC evidence are not current supported end-to-end domains.

### only.digital.webtest

- **Repository:** https://github.com/andreigkuznetsov/only.digital.webtest
- **Pinned reviewed revision:** `fde6eac384915b5ca7c201c647d96da5591527ac`
- **Discovery purpose:** Selenide selectors, browser actions, visibility/text/collection conditions, and runtime conditional helper behavior.
- **Preparation required:** Add or repair the documented Gradle wrapper, define headless browser execution, and address the live-site dependency.
- **Why not mandatory:** The project is small, execution depends on a live site, and UI extraction is not currently supported.

## Corpus Coverage Policy

- The corpus is evaluated collectively; no single repository must contain every supported pattern.
- Focused fixtures remain authoritative for exact variants absent from maintained real projects.
- Repositories are qualified against pinned revisions; revision changes require corpus review before qualification claims are updated.
- Static source qualification and execution-based qualification must be distinguished.
- Presence of a Planned pattern does not mean QAIP currently supports it.
- BookShop is a regression baseline, not the source of product requirements.

## Current Coverage Gaps

Focused fixtures remain required for supported variants not naturally represented in the corpus, including:

- multiple-method `@RequestMapping`;
- `@ModelAttribute` and implicit or generic request binding;
- boolean, temporal, and `Digits` constraints;
- production `@Autowired` field injection;
- MockMvc patterns;
- same-class and external-static helper boundaries;
- ambiguous service or repository targets;
- dynamic paths that must remain omitted;
- unsupported custom assertions and matchers;
- determinism, duplicate identities, and unknown-endpoint integrity.

These fixture responsibilities define exact deterministic boundaries; they are not corpus defects.
