# План: FSM с явными переходами + динамические subStage + предплан

## Граф переходов между этапами

```
PLANNING ──────→ EXECUTION
    ↑                │
    │                ↓
    └──── (новый     │
    ────── промт)    │
                     ↓
     EXECUTION ←─── DONE
     (reopen)
```

### Разрешённые переходы

| from → to | Инициатор | Условие |
|-----------|----------|---------|
| PLANNING → EXECUTION | Кнопка "✅ Приступить" | Пользователь подтвердил план |
| EXECUTION → DONE | Кнопка "✅ Завершить" | Все subStage выполнены |
| EXECUTION → PLANNING | Кнопка "🔄 Продолжить уточнение" | Отказ от завершения |
| DONE → EXECUTION | Кнопка "🔄 Вернуться" | Reopen |
| DONE → PLANNING | Новый промт пользователя | Новая задача (не через LLM!) |

### Запрещённые (LLM не может предложить)

| from → to | Почему |
|-----------|--------|
| PLANNING → DONE | Нельзя завершить без выполнения |
| DONE → PLANNING | LLM не может начать новую задачу |
| EXECUTION → PLANNING | Только пользователь решает "уточнить" |

---

## Динамические subStage

### Как работают

LLM в **первом сообщении каждого этапа** объявляет subStage:
```
[SUBSTAGES: 3]
[SUBSTAGE_LABEL: Узнаю тему]
[STAGE_COMPLETE]
```

→ Пользователь видит план + кнопки ✅/🔄

После подтверждения — subStage переключаются последовательно:
```
[SUBSTAGE: 2]
[SUBSTAGE_LABEL: Уточняю настроение]
```

### Маркеры

| Маркер | Описание | Действие приложения |
|--------|----------|---------------------|
| `[SUBSTAGES: N]` | LLM объявляет количество subStage | Записать `subStagesTotal = N` |
| `[SUBSTAGE: N]` | Переключение на subStage N | Проверить: N == subStage + 1. Если нет → reject. |
| `[SUBSTAGE_LABEL: text]` | Название текущего subStage | Сохранить `subStageLabel = text` |
| `[STAGE_COMPLETE]` | Этап завершён | Показать кнопки ✅/🔄 |
| `[STAGE: NAME]` | Целевой этап (после подтверждения) | Переключить stage в БД |
| `[STEP: N]` | Шаг внутри subStage | Обновить `step` (как сейчас) |
| `[STEPS: N]` | Всего шагов | Обновить `totalSteps` |

### Валидация subStage

- `[SUBSTAGE: N]` разрешён **только** если N == subStage + 1
- Нельзя: 1 → 3 (пропуск)
- Нельзя: 2 → 1 (назад)
- Если LLM нарушает → system message: "⚠️ Недопустимый переход subStage: {current}→{attempted}. Разрешён: {current+1}"

### Пропуск subStage пользователем

- Кнопка "⏭ Пропустить" под текущим сообщением
- При нажатии → `subStage += 1` в БД
- LLM получает system message: "User skipped subStage {N}: {label}. Move to subStage {N+1}."
- Если пропущен последний subStage → LLM может поставить `[STAGE_COMPLETE]`

---

## Поведение системы по этапам

### PLANNING

1. **Вход** (первый запрос пользователя или новый промт в DONE)
2. LLM генерирует план subStage + `[STAGE_COMPLETE]`
3. Пользователь видит план + кнопки
4. **✅ Приступить** → subStage начинаются
5. **🔄 Изменить** → LLM переспрашивает (subStage заново)
6. Внутри subStage: LLM задаёт вопросы
7. Когда последний subStage завершён → `[STAGE_COMPLETE]`
8. Кнопки: **✅ Приступить к выполнению** / **🔄 Продолжить уточнение**

### EXECUTION

1. **Вход** (после PLANNING → кнопка "Приступить к выполнению")
2. LLM генерирует план subStage выполнения + `[STAGE_COMPLETE]`
3. Пользователь видит план + кнопки
4. **✅ Приступить** → subStage начинаются
5. **🔄 Изменить** → LLM переспрашивает
6. Внутри subStage: LLM выполняет (код, текст, рекомендации)
7. Когда последний subStage завершён → `[STAGE_COMPLETE]`
8. Кнопки: **✅ Завершить задачу** / **🔄 Нужно ещё доработать**

### DONE

1. **Вход** (после EXECUTION → кнопка "Завершить")
2. LLM подводит итог
3. Пользователь может:
   - Написать новый промт → PLANNING (новая задача)
   - Нажать **🔄 Вернуться к выполнению** → EXECUTION (reopen)
   - Нажать **❌ Нет, спасибо** → ничего

---

## System prompt для LLM

### buildStagePrompt — новая версия (с разделением на предплан / работа по плану)

```kotlin
private fun buildStagePrompt(chatState: ChatStateDomain, stageSummaries: List<StageSummary>): String? {
    val basePrompt = if (!chatState.planConfirmed) {
        // === ПЕРВЫЙ ВХОД В ЭТАП: LLM ДОЛЖНА ПОКАЗАТЬ ПЛАН ===
        when (chatState.stage) {
            ChatStage.PLANNING -> TextCleaner.clean(
                "[STAGE: PLANNING] This is your FIRST message in this stage.\n" +
                "Plan the work: output [SUBSTAGES: N] and [SUBSTAGE_LABEL: text] for each sub-stage.\n" +
                "Then output [STAGE_COMPLETE] so user can confirm the plan.\n" +
                "Do NOT ask questions yet. Just show the plan."
            )
            ChatStage.EXECUTION -> TextCleaner.clean(
                "[STAGE: EXECUTION] This is your FIRST message in this stage.\n" +
                "Plan the execution steps: output [SUBSTAGES: N] with [SUBSTAGE_LABEL: text] for each.\n" +
                "Then output [STAGE_COMPLETE] so user can confirm.\n" +
                "Do NOT execute yet. Just show the plan."
            )
            ChatStage.DONE -> TextCleaner.clean(
                "[STAGE: DONE] Task completed. Summarize result.\n" +
                "If user writes a new message, they may be starting a new task — that's their choice.\n" +
                "To reopen existing task: output [STAGE: EXECUTION] [STAGE_COMPLETE]."
            )
        }
    } else {
        // === ПЛАН ПОДТВЕРЖДЁН: LLM РАБОТАЕТ ПО ШАГАМ ===
        when (chatState.stage) {
            ChatStage.PLANNING -> TextCleaner.clean(
                "[STAGE: PLANNING, SubStage: ${chatState.subStage + 1}/${chatState.subStagesTotal}]\n" +
                "Plan confirmed. Proceed sub-stage by sub-stage.\n" +
                "Allowed LLM transitions: PLANNING→EXECUTION.\n" +
                "Prohibited LLM transitions: PLANNING→DONE, PLANNING→NOTHING.\n" +
                "Sub-stage rules: sequential only. Output [SUBSTAGE: N+1] when current sub-stage is done.\n" +
                "Ask questions. No solutions, no code.\n" +
                "When all sub-stages done, output [STAGE_COMPLETE] [STAGE: EXECUTION]."
            )
            ChatStage.EXECUTION -> TextCleaner.clean(
                "[STAGE: EXECUTION, SubStage: ${chatState.subStage + 1}/${chatState.subStagesTotal}]\n" +
                "Plan confirmed. Execute.\n" +
                "Allowed LLM transitions: EXECUTION→DONE.\n" +
                "Prohibited LLM transitions: EXECUTION→PLANNING.\n" +
                "Sub-stage rules: sequential only. Output [SUBSTAGE: N+1] when current sub-stage is done.\n" +
                "When all sub-stages done, output [STAGE_COMPLETE] [STAGE: DONE]."
            )
            ChatStage.DONE -> TextCleaner.clean(
                "[STAGE: DONE] Task completed. Summarize result.\n" +
                "If user writes a new message, they may be starting a new task — that's their choice.\n" +
                "To reopen existing task: output [STAGE: EXECUTION] [STAGE_COMPLETE]."
            )
        }
    }

    return if (stageSummaries.isNotEmpty()) {
        val block = stageSummaries.joinToString("; ") { summary ->
            "[Completed task ${summary.taskNumber}, Stage ${summary.stage.label}: ${summary.summaryText}]"
        }
        TextCleaner.clean("$basePrompt [Previous completed stages: $block]")
    } else {
        basePrompt
    }
}
```


## Изменения в БД (без миграции — fresh install)

Новые поля добавляются в `ChatStateEntity`. Room создаст таблицу с нуля.

### Поле chat_state (дополнительные поля)

| Поле | Тип | Описание |
|------|-----|----------|
| `subStage` | Int | Текущий subStage (0-based) |
| `subStagesTotal` | Int | Всего subStage на этом этапе |
| `subStageLabel` | String | Название текущего subStage |
| `planConfirmed` | Boolean | Пользователь подтвердил план этапа |

### ChatStateDomain

```kotlin
data class ChatStateDomain(
    val chatId: String,
    val stage: ChatStage = ChatStage.PLANNING,
    val subStage: Int = 0,
    val subStagesTotal: Int = 0,
    val subStageLabel: String = "",
    val step: Int = 0,
    val totalSteps: Int = 0,
    val planConfirmed: Boolean = false
) {
    companion object {
        fun initial(chatId: String) = ChatStateDomain(chatId = chatId)
    }
}
```

---

## Изменения в коде

### Файл 1: `ChatStage.kt` — добавить валидацию

```kotlin
enum class ChatStage(val label: String) {
    PLANNING("🧠 Планирование"),
    EXECUTION("⚡ Выполнение"),
    DONE("🎉 Завершено");

    val allowedTransitions: Set<ChatStage>
        get() = when (this) {
            PLANNING -> setOf(EXECUTION)
            EXECUTION -> setOf(DONE, PLANNING)
            DONE -> setOf(EXECUTION, PLANNING) // PLANNING — только новый промт
        }

    val allowedLlmTransitions: Set<ChatStage>
        get() = when (this) {
            PLANNING -> setOf(EXECUTION)
            EXECUTION -> setOf(DONE)
            DONE -> setOf(EXECUTION) // reopen, PLANNING — только пользователь
        }

    fun canTransitionTo(target: ChatStage): Boolean = target in allowedTransitions

    fun canLlmTransitionTo(target: ChatStage): Boolean = target in allowedLlmTransitions
}
```

### Файл 2: `ChatStateDomain.kt` — новые поля

subStage, subStagesTotal, subStageLabel, planConfirmed

### Файл 3: `ChatStateEntity.kt` — новые поля в БД

Аналогично ChatStateDomain

### Файл 4: `ChatStateMapper.kt` — map новых полей

### Файл 5: `SendMessageUseCase.kt` — основная логика

Изменения:
- Парсинг `[SUBSTAGES:N]`, `[SUBSTAGE:N]`, `[SUBSTAGE_LABEL:text]`
- Валидация последовательности subStage
- Валидация stage перехода (llm → `canLlmTransitionTo`)
- Если reject → system message в результат + stage/subStage не меняются
- Новое поле в `SendMessageResult`: `fsmSubStage: Int?`, `fsmSubStagesTotal: Int?`, `fsmSubStageLabel: String?`, `fsmLlmTransitionRejected: String?`
- **Новое поле `planLabels: List<String>`** — парсит ВСЕ `[SUBSTAGE_LABEL: ...]` из ответа, не только последний
- `buildStagePrompt` — новая версия с инструкциями про subStage + запрещённые переходы

### Файл 6: `ChatViewModel.kt` — обработка новых маркеров

- `processFsmMarkers`:
  - Парсинг subStage (только N→N+1)
  - Парсинг subStagesTotal
  - Парсинг subStageLabel
  - Обработка `llmTransitionRejectedMessage`
- `updatePlanConfirmed(chatId)` — после кнопки "Приступить"
- `skipSubStage()` — кнопка пропуска
- Сброс subStage при переходе между этапами

### Файл 7: `ChatUiState.kt` — новые поля

```kotlin
val subStage: Int = 0,
val subStagesTotal: Int = 0,
val subStageLabel: String = "",
val planConfirmed: Boolean = false,
val llmTransitionRejectedMessage: String? = null,
val subStageTransitionRejectedMessage: String? = null,
val planSteps: List<String> = emptyList()  // ← план из SUBSTAGE_LABEL для показа в PlanBubble
```

### Файл 8: `ChatScreen.kt` — отображение

- TopBar: `"🧠 Планирование | Шаг ${subStage+1}/${subStageTotal}"` (если subStagesTotal > 0)
- Кнопка "⏭ Пропустить" если subStage < subStagesTotal - 1
- Отображение `llmTransitionRejectedMessage` и `subStageTransitionRejectedMessage` как system-бабл (оранжевый)
- **Новый composable `PlanBubble`**:
  - Показывается когда: `hasStageCompleteMarker && !planConfirmed && planSteps.isNotEmpty()`
  - Заголовок: `"📋 План этапа "Название этапа""`
  - Нумерованный список: `1. ...\n2. ...\n3. ...`
  - Кнопки: `[✅ Приступить] [🔄 Продолжить уточнение]` (внутри карточки)
  - Чистый текст ответа LLM **не показывается** — только план
  - После "✅ Приступить" → `confirmPlan()` → план заменяется на нормальные сообщения LLM
  - После "🔄 Продолжить" → `rejectStageTransition()` → LLM получает сигнал уточнить план

### Файл 9 (новый): `ChatStageTest.kt`

Тесты:
```
- PLANNING → EXECUTION: ✅
- PLANNING → DONE: ✅ canTransitionTo=true, ⛔ canLlmTransitionTo=false
- EXECUTION → DONE: ✅
- EXECUTION → PLANNING: ✅ canTransitionTo=true, ⛔ canLlmTransitionTo=false
- DONE → EXECUTION: ✅
- DONE → PLANNING: ✅ canTransitionTo=true, ⛔ canLlmTransitionTo=false
- subStage: 0→1 ✅, 0→2 ⛔, 1→0 ⛔, 0→0 ⛔
```

---

## UI-сценарий: полный пример

### "напиши стих"

```
┌──────────────────────────────────────┐
│ 🧠 Планирование                      │
├──────────────────────────────────────┤
│                                      │
│ Пользователь:                        │
│ напиши стих                          │
│                                      │
│ 🧠 Этап: Планирование                │ ← StageChangeBubble
│                                      │
│ ┌──────────────────────────────┐     │
│ │ 📋 План этапа "Планирование" │     │ ← PlanBubble (вместо сырого текста)
│ │                              │     │
│ │ 1. Узнаю тему стиха          │     │
│ │ 2. Уточню настроение         │     │
│ │ 3. Выберу размер и рифму     │     │
│ │                              │     │
│ │ [✅ Приступить] [🔄 Уточнить]│     │ ← кнопки внутри карточки
│ └──────────────────────────────┘     │
│                                      │
│ (пользователь нажал "Приступить")     │
│ → planConfirmed=true                 │
│ → auto-send: "Продолжить уточнение"  │
│                                      │
│ Ассистент:                           │
│ Шаг 1/3. Давай начнём!               │ ← нормальный message bubble
│ О чём будет стих?                    │   (маркеры вырезаны)
│                                      │
│ Пользователь:                        │
│ о любви                              │
│                                      │
│ Ассистент:                           │
│ Шаг 2/3. Какое настроение?           │
│                                      │
│ Пользователь:                        │
│ романтичный                          │
│                                      │
│ Ассистент:                           │
│ Шаг 3/3. Какой размер предпочитаешь? │
│                                      │
│ Пользователь:                        │
│ четырёхстопный ямб                   │
│                                      │
│ ┌──────────────────────────────┐     │
│ │ 📋 План этапа "Выполнение"   │     │ ← PlanBubble для EXECUTION
│ │                              │     │
│ │ 1. Написать стих             │     │
│ │ 2. Проверить ритм            │     │
│ │                              │     │
│ │ [✅ Приступить] [🔄 Уточнить]│     │
│ └──────────────────────────────┘     │
└──────────────────────────────────────┘
```

---

## Обработка invalid transition (LLM пытается перепрыгнуть)

### Пример: LLM в PLANNING ставит `[STAGE: DONE][STAGE_COMPLETE]`

```
1. SendMessageUseCase парсит fsmTargetStage = DONE
2. Проверка: ChatStage.PLANNING.canLlmTransitionTo(DONE) → false
3. Результат:
   - fsmTargetStage игнорируется
   - fsmStageComplete игнорируется
   - fsmLlmTransitionRejected = "⚠️ Недопустимый переход: Планирование→Завершено. Разрешённые LLM переходы: [Выполнение]"
4. ChatViewModel вставляет system message в список сообщений
5. Пользователь видит оранжевый бабл с предупреждением
6. LLM получает это предупреждение в контексте при следующем запросе
```

### Что видит пользователь

```
┌──────────────────────────────────────┐
│                                      │
│ ⚠️ Недопустимый переход:             │
│    Планирование → Завершено          │
│    Разрешённые LLM: [Выполнение]     │
│                                      │
└──────────────────────────────────────┘
```

---

## Восстановление после паузы

При загрузке чата:
1. `loadChatState(chatId)` — загружает `ChatStateDomain` из БД
2. Все поля восстанавливаются: stage, subStage, subStagesTotal, planConfirmed
3. LLM получает актуальный system prompt: `[STAGE: PLANNING, SubStage: 2/3]`
4. Пользователь продолжает с того же места

Никакой дополнительной логики не нужно — всё уже в БД.

---

## Список всех файлов для изменения/создания

### Изменить (9 файлов)

| № | Файл | Изменения |
|---|------|-----------|
| 1 | `core/domain/.../model/ChatStage.kt` | allowedTransitions, allowedLlmTransitions, canTransitionTo, canLlmTransitionTo |
| 2 | `core/domain/.../model/ChatStateDomain.kt` | +subStage, subStagesTotal, subStageLabel, planConfirmed |
| 3 | `core/database/.../entity/ChatStateEntity.kt` | новые столбцы |
| 4 | `core/data/.../mapper/ChatStateMapper.kt` | map новых полей |
| 5 | `core/domain/.../usecase/SendMessageUseCase.kt` | парсинг subStage, валидация, buildStagePrompt, +planLabels |
| 6 | `features/chat/.../ChatViewModel.kt` | обработка subStage, invalid transition, planConfirmed, skipSubStage, сбор planSteps |
| 7 | `features/chat/.../ChatUiState.kt` | новые поля, +planSteps |
| 8 | `features/chat/.../ChatScreen.kt` | отображение subStage, кнопка пропуска, invalid transition бабл, +PlanBubble |
| 9 | `core/domain/.../model/ChatStageTest.kt` (новый) | тесты |

---

## Итог

| Требование | Статус |
|-----------|--------|
| Явные допустимые состояния (PLANNING, EXECUTION, DONE) | ✅ |
| Разрешённые переходы между этапами + валидация | ✅ |
| LLM не может "перепрыгнуть" этап | ✅ (canLlmTransitionTo) |
| Динамические subStage (LLM сама решает) | ✅ |
| Предплан + подтверждение пользователя | ✅ |
| Пропуск subStage пользователем | ✅ |
| Последовательная валидация subStage (N→N+1) | ✅ |
| Обработка invalid transition + feedback LLM | ✅ |
| Корректность после паузы | ✅ (всё в БД) |
| Тесты | ✅ (ChatStageTest) |