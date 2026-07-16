# Матрица допустимых связей Normalized QA Model v0.1

| From | Relationship | To | Назначение |
|---|---|---|---|
| USER_STORY | DESCRIBES | BUSINESS_OPERATION | User Story описывает бизнес-операцию |
| BUSINESS_OPERATION | GOVERNED_BY | BUSINESS_RULE | Операция регулируется бизнес-правилом |
| BUSINESS_OPERATION | SPECIFIED_BY | SCENARIO | Операция конкретизируется BDD-сценарием |
| BUSINESS_OPERATION | IMPLEMENTED_BY | TECHNICAL_IMPLEMENTATION | Операция реализована техническим способом |
| TEST_IMPLEMENTATION | VALIDATES | SCENARIO | Тестовая реализация проверяет сценарий |
| TEST_IMPLEMENTATION | USES | TECHNICAL_IMPLEMENTATION | Тест использует технический канал |
| TEST_IMPLEMENTATION | HAS_CHECK | CHECK | Тест содержит конкретную проверку |
| SCENARIO | COVERS | BUSINESS_RULE | Сценарий покрывает правило |
| SCENARIO | REFINES | SCENARIO | Один сценарий уточняет другой |
| BUSINESS_RULE | DEPENDS_ON | BUSINESS_RULE | Одно правило зависит от другого |
| BUSINESS_RULE | SUPERSEDES | BUSINESS_RULE | Новая версия правила заменяет старую |
| USER_STORY | RELATED_TO | USER_STORY | Связанные User Story |
| BUSINESS_OPERATION | RELATED_TO | BUSINESS_OPERATION | Связанные бизнес-операции |
