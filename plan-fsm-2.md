# План: System prompt для этапов FSM + очистка маркеров (v3)

## Проблемы

1. LLM получает промпт PLANNING с жёстким "after 3 QA exchanges force STAGE_COMPLETE". Завершает этап недо-собрав инфу.
2. Промпт содержит подсказки ответов ("I'm still in the planning phase. Let me gather details first.").
3. Маркеры `[STEP:N]`, `[STAGE_COMPLETE]`, `[STAGE:X]` видны пользователю в UI. Должны быть скрыты, но кнопки под сообщением — показываться.

## Решение

### 1. Новый промпт PLANNING (чистые правила, без подсказок)

```kotlin
"[STAGE: PLANNING] You are in the PLANNING phase. " +
"Ask questions only. No suggestions, no solutions, no code, no recommendations. " +
"If the user answers a question, that topic is closed. Do not ask about it again even in a different form. " +
"If the user rejects a question or says it is irrelevant, accept it and move to the next topic. " +
"Keep asking questions until you have gathered all the information needed to execute the task. " +
"If the user explicitly says \"enough\", \"let's proceed\" or similar, force [STAGE_COMPLETE] even if some questions remain unanswered. " +
"Before outputting [STAGE_COMPLETE] write a brief summary of what was learned. " +
"The number of question answer exchanges is not fixed and depends on the task complexity."
```

- Нет фиксированного числа обменов (было "after 3 QA")
- Нет примеров ответов и подсказок
- Только правила: что делать, а что нет

### 2. Очистка маркеров из display text

Новая функция `cleanMarkers(text)`:

```kotlin
private fun cleanMarkers(text: String): String {
    val stepRegex = """\[STEP:\s*\d+]""".toRegex()
    val stepsRegex = """\[STEPS:\s*\d+]""".toRegex()
    val stageCompleteRegex = """\[STAGE_COMPLETE]""".toRegex()
    val stageRegex = """\[STAGE:\s*\w+]""".toRegex()
    return text
        .replace(stepRegex, "")
        .replace(stepsRegex, "")
        .replace(stageCompleteRegex, "")
        .replace(stageRegex, "")
        .trim()
}
```

Вызывается ДО сохранения `MessageDomain.text` в БД. Маркеры парсятся ДО очистки (на оригинальном `fullText`).

### 3. Визуал в UI

```
Ассистент:
Собрана информация:
• Бюджет: до 5000₽
• Хобби: чтение, кулинария

[✅ Приступить к выполнению]  [🔄 Продолжить уточнение]
```

Без `[STAGE_COMPLETE]` в тексте. Маркер вырезан, кнопки — под сообщением.

## Изменения: 1 файл

`core/domain/src/main/java/com/aichat/core/domain/usecase/SendMessageUseCase.kt`

### Что изменилось

| Изменение | Было | Стало |
|-----------|------|-------|
| Промпт PLANNING | "After 3 QA exchanges force STAGE_COMPLETE. Reject solution requests: I'm still in the planning phase." | Чистые правила без фикс числа QA, без подсказок |
| Промпт EXECUTION | `When step done → if more steps remain` | `When step done, if more steps remain` (→ убрана) |
| Промпт DONE | `To reopen → output` | `To reopen output` (→ убрана) |
| Очистка маркеров | Нет. `val cleanFullText = fullText` | `val cleanFullText = cleanMarkers(fullText)` |
| Новая функция | Нет | `private fun cleanMarkers(text)` — удаляет [STEP], [STEPS], [STAGE_COMPLETE], [STAGE] |

### Финальные промпты v3 (после TextCleaner — одна строка)

| Этап | Что получает LLM |
|------|-----------------|
| PLANNING | `[STAGE: PLANNING] You are in the PLANNING phase. Ask questions only. No suggestions, no solutions, no code, no recommendations. If the user answers a question, that topic is closed. Do not ask about it again even in a different form. If the user rejects a question or says it is irrelevant, accept it and move to the next topic. Keep asking questions until you have gathered all the information needed to execute the task. If the user explicitly says "enough", "let's proceed" or similar, force [STAGE_COMPLETE] even if some questions remain unanswered. Before outputting [STAGE_COMPLETE] write a brief summary of what was learned. The number of question answer exchanges is not fixed and depends on the task complexity.` |
| EXECUTION | `[STAGE: EXECUTION] Step 1/3. Execute the plan. Provide solutions, code, recommendations. When step done, if more steps remain output [STEP: N]. When all steps done append [STAGE_COMPLETE].` |
| DONE | `[STAGE: DONE] Task completed. Summarize result. Ask if user needs anything else. To reopen output [STAGE: EXECUTION] [STAGE_COMPLETE].` |

## Файлы НЕ требующие изменений

- ChatViewModel.kt — `processFsmMarkers()` работает с `result.fsmStageComplete`, который парсится ДО очистки. Без изменений.
- ChatUiState.kt — поля `showStageConfirm`, `hasStageCompleteMarker` уже есть. Без изменений.
- ChatScreen.kt — кнопки `StageConfirmButtons` уже отображаются. Без изменений.