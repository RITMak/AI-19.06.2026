# План: Stage-Level Summary (v3)

## Идея

Сейчас суммари контекста (`ContextSummaryData`) работает в пределах **всего чата** — перезаписывается каждые N сообщений. Это не даёт памяти о завершённых задачах при новом запросе в DONE.

**Новое:** Stage-Level Summary — суммари для каждого завершённого этапа FSM. Накапливается. При новом запросе в DONE → LLM получает все суммари предыдущих задач как system prompt.

## Схема работы

```
[PLANNING]                          [EXECUTION]                       [DONE]
   │                                     │                              │
   ├─ вопросы/ответы                     ├─ получает:                    │
   │                                     │   - StageSummary(PLANNING)    │
   └─ → StageSummary:                    │   - выполняет шаги           │
      "Бюджет: 5000₽                     │                              │
       Хобби: чтение"                    └─ → StageSummary(EXECUTION):   │
                                          "Код: класс X создан"          ├─ получает:
                                                                        │   - StageSummary(PLANNING)
                                                                        │   - StageSummary(EXECUTION)
                                                                        └─ → StageSummary(DONE):
                                                                           "Задача решена: ..."
```

Новый запрос в DONE:
- stage = PLANNING
- LLM получает: `[STAGE: PLANNING] ... [Предыдущие задачи: 1. PLANNING: "...", EXECUTION: "...", DONE: "..."; 2. ...]`

## Новые файлы

### 1. `core/database/.../entity/StageSummaryEntity.kt`

```kotlin
@Entity(tableName = "stage_summaries")
data class StageSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val taskNumber: Int,       // 1, 2, 3... — номер завершённой задачи
    val stageOrder: Int,       // 1=PLANNING, 2=EXECUTION, 3=DONE
    val stage: String,         // "PLANNING" | "EXECUTION" | "DONE"
    val summaryText: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. `core/database/.../dao/StageSummaryDao.kt`

```kotlin
@Dao
interface StageSummaryDao {
    @Query("SELECT * FROM stage_summaries WHERE chatId = :chatId ORDER BY taskNumber ASC, stageOrder ASC")
    suspend fun getByChatId(chatId: String): List<StageSummaryEntity>

    @Insert
    suspend fun insert(summary: StageSummaryEntity)

    @Query("SELECT COALESCE(MAX(taskNumber), 0) FROM stage_summaries WHERE chatId = :chatId")
    suspend fun getMaxTaskNumber(chatId: String): Int
}
```

### 3. `core/domain/model/StageSummary.kt`

```kotlin
data class StageSummary(
    val chatId: String,
    val taskNumber: Int,
    val stageOrder: Int,
    val stage: ChatStage,
    val summaryText: String
)
```

### 4. `core/domain/repository/StageSummaryRepository.kt`

```kotlin
interface StageSummaryRepository {
    suspend fun getByChatId(chatId: String): List<StageSummary>
    suspend fun save(summary: StageSummary)
    suspend fun getMaxTaskNumber(chatId: String): Int
}
```

### 5. `core/data/repository/StageSummaryRepositoryImpl.kt`

Имплементация: маппинг `StageSummaryEntity` ↔ `StageSummary`, вызовы `StageSummaryDao`.

## Изменяемые файлы

### 6. `core/database/.../AiChatDatabase.kt`

- Добавить `StageSummaryEntity` в `entities = [...]`
- Добавить `abstract fun stageSummaryDao(): StageSummaryDao`
- `BranchDao.kt` уже есть в списке — добавить `StageSummaryDao`

### 7. DI модуль (DatabaseModule или аналогичный)

- Добавить `@Binds StageSummaryRepository` → `StageSummaryRepositoryImpl`
- Добавить провайдер `StageSummaryDao`

### 8. `core/domain/usecase/SendMessageUseCase.kt`

**Новый параметр `invoke()`:**
```kotlin
stageSummaries: List<StageSummary> = emptyList()
```

**Изменение `buildStagePrompt()`:**
После основного промпта этапа добавлять блок:
```kotlin
if (stageSummaries.isNotEmpty()) {
    val block = stageSummaries.joinToString("; ") {
        "[Task ${it.taskNumber}, Stage ${it.stage.label}: ${it.summaryText}]"
    }
    prompt += " [Previous completed stages: $block]"
}
```

### 9. `features/chat/.../ChatViewModel.kt`

**Новые поля:**
```kotlin
private val stageSummaryRepository: StageSummaryRepository
private var currentTaskNumber: Int = 0
```

**`loadChatState()` — инициализация taskNumber:**
```kotlin
currentTaskNumber = stageSummaryRepository.getMaxTaskNumber(chatId) + 1
```

**`confirmStageTransition()` — генерация StageSummary перед переходом:**
```kotlin
// Собрать контекст для генерации
val lastContextSummary = contextCompressor.getLastSummary(chatId)
val lastMessages = history.takeLast(4)
val summaryResult = contextCompressor.generateSummary(
    providerService = ...,
    apiKey = ...,
    modelId = ...,
    newMessages = lastMessages,
    existingSummary = lastContextSummary?.summaryText,
    customPrompt = "Summarize the completed ${currentStage.label} stage of task ${currentTaskNumber}. Focus on key decisions and results."
)

stageSummaryRepository.save(StageSummary(
    chatId = chatId,
    taskNumber = currentTaskNumber,
    stageOrder = currentStage.ordinal + 1,
    stage = currentStage,
    summaryText = summaryResult.summaryText
))
```

**`sendMessage()` — если DONE и новый промт:**
```kotlin
if (_uiState.value.currentStage == ChatStage.DONE) {
    chatStateRepository.setStage(chat.id, ChatStage.PLANNING)
    _uiState.value = _uiState.value.copy(currentStage = ChatStage.PLANNING, currentStep = 0)
    currentTaskNumber = stageSummaryRepository.getMaxTaskNumber(chat.id) + 1
}

// Передать stageSummaries
val stageSummaries = stageSummaryRepository.getByChatId(chat.id)
```

## Примечание

- Миграция БД не нужна — приложение ставится с нуля (version = 1)
- Существующее `ContextSummaryData` остаётся для сжатия контекста внутри этапа
- `StageSummary` и `ContextSummaryData` не пересекаются — разное назначение