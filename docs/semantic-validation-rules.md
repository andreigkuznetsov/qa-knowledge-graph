# Правила семантической валидации v0.1

## ERROR
- Дублирование node.id или relationship.id.
- Отсутствующий узел в from/to.
- Отсутствующий sourceId.
- Недопустимое направление связи.
- Дублирующая связь from + type + to.
- Запрещённая самоссылка.

## WARNING
- Business Operation без правила, сценария или технической реализации.
- Scenario без Test Implementation.
- Business Rule без покрытия Scenario --COVERS--> Business Rule.
- Test Implementation без Check.
- Confirmed-узел без SourceReference.
- Check без владельца.
