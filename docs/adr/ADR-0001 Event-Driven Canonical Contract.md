# ADR-0001
# Event-Driven Canonical Contract

**Статус:** Approved

**Дата:** 2026-08-05

**Связанные релизы:**

- Explorer Developer Preview 0.2
- Explorer Developer Preview 0.3

---

# Контекст

Explorer Developer Preview 0.2 поддерживает классическую инженерную модель реализации:

REST Controller
→ Service
→ Repository

Эта модель успешно квалифицирована на проектах:

- rabbitmq-notification-service
- order-service-redis-sdet

Однако исследование проекта

order-events-kafka-tests

выявило новый архитектурный стиль реализации.

Фактический путь обработки операции:

REST Controller
→ Kafka Producer
→ Kafka Topic
→ Kafka Listener
→ Service
→ Repository

Explorer 0.2 корректно определяет этот путь как

INCOMPLETE_IMPLEMENTATION_PATH

поскольку текущая Canonical Engineering Ontology не содержит инженерных сущностей для событийной обработки.

Таким образом, проблема не является ошибкой Runtime или Explorer.

Она представляет собой ограничение текущей онтологии.

---

# Решение

Canonical Engineering Ontology расширяется поддержкой событийных архитектур.

Новые инженерные роли вводятся как роли существующего типа

TECHNICAL_IMPLEMENTATION

без создания новых top-level node.

Добавляются следующие роли:

- REST_CONTROLLER
- APPLICATION_SERVICE
- REPOSITORY
- MESSAGE_PRODUCER
- MESSAGE_DESTINATION
- MESSAGE_CONSUMER

Эти роли описывают инженерные обязанности компонентов.

Они не привязаны к конкретной технологии.

---

# Сообщения (Messaging)

Canonical Engineering Ontology моделирует инженерные роли.

Технологическая реализация хранится отдельно.

Например:

MESSAGE_PRODUCER

technology = Kafka

или

technology = RabbitMQ

Аналогично:

MESSAGE_DESTINATION

может представлять:

- Kafka Topic
- RabbitMQ Queue
- RabbitMQ Exchange
- NATS Subject
- Pulsar Topic
- JMS Destination

Таким образом Canonical Model остаётся независимой от конкретного брокера сообщений.

---

# Новые отношения

Добавляются новые типы отношений.

MESSAGE_PRODUCER

PUBLISHES_TO

MESSAGE_DESTINATION

MESSAGE_CONSUMER

CONSUMES_FROM

MESSAGE_DESTINATION

Существующее отношение

USES

остаётся без изменений.

Используется только там, где действительно отражает инженерный факт зависимости.

---

# Канонический путь реализации

Для событийной операции:

BUSINESS_OPERATION

↓

IMPLEMENTED_BY

↓

REST_CONTROLLER

↓

USES

↓

MESSAGE_PRODUCER

↓

PUBLISHES_TO

↓

MESSAGE_DESTINATION

↓

CONSUMES_FROM

↓

MESSAGE_CONSUMER

↓

USES

↓

APPLICATION_SERVICE

↓

USES

↓

REPOSITORY

Все этапы должны быть подтверждены непосредственными инженерными доказательствами.

Отсутствующие этапы не выводятся по соглашениям именования, пакетам или особенностям фреймворка.

---

# Совместимость

Explorer Developer Preview 0.2 остаётся полностью совместим.

Классическая модель

Controller
→ Service
→ Repository

не изменяется.

Все существующие проекты продолжают импортироваться без изменений.

Новая модель является расширением существующей.

---

# Последствия

После утверждения настоящего ADR становятся возможны следующие capability:

Explorer 0.3

Capability E3.1

Event-Driven Implementation Path

Capability E3.2

Dynamic Local HTTP Resolution

Capability E3.3

Event-Driven Test Qualification

Без настоящего ADR реализация перечисленных возможностей не допускается.

---

# Qualification Corpus

Настоящее решение подтверждено расследованием следующих квалификационных проектов:

rabbitmq-notification-service

подтверждает классическую модель.

order-events-kafka-tests

подтверждает необходимость поддержки событийной архитектуры.

order-service-redis-sdet

подтверждает корректность Canonical Identity и Runtime Import.

---

# Статус решения

Approved.

Все последующие изменения Event-Driven поддержки должны соответствовать настоящему ADR.