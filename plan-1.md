# План рефакторинга чата

**Цель:** Очистить окно чата от лишней логики, добавить обработку ошибок серверов.
**База данных:** Миграции не нужны — приложение ставится с 0, будет добавлен `fallbackToDestructiveMigration()`.

**Контекст:** Всегда работает в режиме Summary (сжатие каждые N сообщений). FULL и выбор режима удалены.

---

## 1. Удалить "Факты" (Facts)

### Удалить файлы целиком:
- `core/domain/src/main/java/.../model/FactsData.kt`
- `core/domain/src/main/java/.../service/FactsStorage.kt` — интерфейс
- `core/data/src/main/java/.../repository/FactsStorageImpl.kt` — реализация
- `features/chat/src/main/java/.../FactsBottomSheet.kt` — UI

### БД:
- Удалить `FactsEntity.kt` + `FactsDao.kt`
- Убрать их из `AiChatDatabase.kt` (entities и abstract fun)
- Убрать из `DatabaseModule.kt` провайдер FactsDao

### Изменения в ChatUiState.kt:
- Удалить поля: `facts`, `factsKeepLastCount`
- Удалить импорт `FactsData`

### Изменения в ChatViewModel.kt:
- Удалить `factsStorage` из конструктора DI
- Удалить методы: `loadFacts()`, `refreshFacts()`
- Удалить вызов `refreshFacts()` из `sendMessage()`
- Удалить `factsKeepLastCount` из `updateContextSettings()`
- Удалить импорт `FactsData`, `FactsStorage`

### Изменения в ChatScreen.kt:
- Удалить `showFactsSheet`, `FactsBottomSheet()` вызов
- Удалить пункт "📋 Facts" из overflow menu

### Изменения в SendMessageUseCase.kt:
- Удалить `factsStorage` из конструктора
- Удалить весь блок Facts (строки ~143-204): логика извлечения фактов, вызов `factsStorage.upsertFacts()`
- Удалить импорты FactsData, FactsStorage

### Изменения в ContextCompressor:
- Удалить метод `extractFacts()`

---

## 2. Контекст — только SUMMARY (всегда включён)

ContextMode enum удаляется целиком — режим контекста больше не выбирается, всегда суммаризация.

### Удалить файлы целиком:
- `core/domain/.../ContextMode` (enum в ContextSettingsDomain.kt) — удалить весь enum, companion object

### Изменения в ContextSettingsDomain.kt:
- Удалить поле `contextMode`
- Удалить поле `keepLastCount`, `truncateChars`, `factsKeepLastCount`
- Оставить только: `chatId`, `summaryEvery` (дефолт **20**)

```kotlin
data class ContextSettingsDomain(
    val chatId: String,
    val summaryEvery: Int = 20
)
```

### БД — ContextSettingsEntity.kt:
- Удалить колонки: `contextMode`, `keepLastCount`, `truncateChars`
- Оставить только: `chatId`, `summaryEvery`

### Изменения в ChatUiState.kt:
- Удалить: `contextMode`, `keepLastCount`, `summaryEvery`, `truncateChars`, `isContextCompressed`, `originalTokens`, `compressedTokens`
- Добавить: `summaryEvery: Int = 20` (для UI)
- Удалить импорт `ContextMode`

### Изменения в ChatViewModel.kt:
- `updateContextSettings()` → упростить: принимает только `summaryEvery`
- `loadChat()` — убрать загрузку `contextSettings` из репозитория
- `sendMessage()` — убрать блок `compressed` (строки ~207-214)

### ContextSettingsDialog.kt — переписать:
- Убрать радио-кнопки выбора режима
- Оставить только одно поле: "Summarize every N messages" (дефолт 20)
- Упростить `onApply`: только `summaryEvery`

### Изменения в ChatScreen.kt:
- BottomBar: убрать `modeText` блок (строки 184-197)
- BottomBar: убрать `ctxText` блок (строки 199-211), оставить только `totalCost`

### Изменения в SendMessageUseCase.kt:
- Убрать проверку `settings?.contextMode == ContextMode.SUMMARY` — суммаризация всегда активна
- Убрать ветки для KEEP_LAST, TRUNCATE, FACTS
- Логика: если `settings.summaryEvery > 0` → выполняем суммаризацию

---

## 3. Удалить ветки (Branches)

### Удалить файлы целиком:
- `core/domain/src/main/java/.../model/BranchData.kt`
- `core/domain/src/main/java/.../repository/BranchRepository.kt` — интерфейс
- `core/data/src/main/java/.../repository/BranchRepositoryImpl.kt` — реализация

### БД:
- Удалить `BranchEntity.kt` + `BranchDao.kt`
- Убрать их из `AiChatDatabase.kt`
- Убрать из `DatabaseModule.kt` провайдер BranchDao
- **MessageEntity.kt** — удалить поле `branchId`

### Изменения в ChatUiState.kt:
- Удалить: `branches`, `currentBranchId`, `currentBranchName`, `forkMessageIndex`, `forkMessageId`
- Удалить импорт `BranchData`

### Изменения в ChatViewModel.kt:
- Удалить `branchRepository` из конструктора DI
- Удалить методы: `loadBranches()`, `branchFromMessage()`, `switchBranch()`, `switchToParentBranch()`, `editAndResend()`
- В `loadChat()`: убрать вызовы `loadBranches()`, создание main branch
- В `observeMessages()`: убрать вычисление forkIndex, forkMessageId
- В `sendMessage()`: убрать параметр `branchId`
- Удалить импорт `BranchData`, `BranchRepository`

### Изменения в ChatScreen.kt:
- Удалить: `showBranchSwitchDialog`, `BranchSwitchDialog()`, `hasBranchSwitch`
- TopBar: убрать индикатор ветки (🌿 + имя ветки)
- Overflow menu: убрать "🔀 Switch branch"
- В `MessageBubble`: убрать `isForkOrigin`, `forkMessageId`

### MessageBubbleMenu.kt — переписать:
- Убрать "🌿 Branch from here", `onBranchFromHere`
- Оставить только: "✏️ Edit & Re-send" (для user) + "📋 Copy"

### Изменения в SendMessageUseCase.kt:
- Убрать параметр `branchId` из `invoke()`

---

## 4. Добавить обработку ошибок серверов

### Создать новый файл: `core/domain/src/main/java/.../AiError.kt`
```kotlin
sealed interface AiError {
    data class ApiError(val code: Int, val message: String) : AiError
    data class AuthError(val message: String) : AiError
    data class RateLimit(val retryAfterMs: Long) : AiError
    data class NetworkError(val cause: Throwable) : AiError
    data object UnknownError : AiError
}
```

### Изменить AiApiClient.kt (core/network):
- Проверять HTTP статус ответа (200 vs 4xx/5xx)
- Кидать исключение с HTTP кодом при 4xx/5xx
- Ловить таймауты и сетевые ошибки

### DeepSeekProvider.kt:
- Вместо `throw RuntimeException("DeepSeek API error: ...")` — вернуть ошибку типизированно
- Парсить `error.type` из ответа для определения Auth / RateLimit
- Возвращать `AiResult<ChatResult>`

### OpenRouterProvider.kt:
- **Сейчас нет проверки `parsed.error`** — добавить!
- Парсить `error.code` для определения Auth / RateLimit
- Возвращать `AiResult<ChatResult>`

### SendMessageUseCase.kt:
- Оборачивать вызов `providerService.chat()` в try-catch
- Маппить исключения из AiApiClient и AiProviderService в `AiError`
- Пробрасывать `AiError` наверх через результат/исключение

### ChatViewModel.kt:
- В `sendMessage()` catch блок: обрабатывать `AiError` типизированно
- Показывать человеческие сообщения:
  - AuthError → "❌ Неверный API ключ"
  - NetworkError → "🌐 Нет соединения с сервером"
  - RateLimit → "⏳ Превышен лимит запросов, подождите"
  - ApiError → "⚠️ Ошибка сервера: {message}"
  - UnknownError → "❓ Неизвестная ошибка"

### ChatScreen.kt:
- Отображать ошибку с иконкой и типом (сейчас просто text в bottomBar)
- Использовать `Snackbar` или цветной баннер

---

## Сводка изменений по файлам

| Файл | Действие |
|------|----------|
| `ChatUiState.kt` | Сильные правки |
| `ChatViewModel.kt` | Сильные правки |
| `ChatScreen.kt` | Сильные правки |
| `FactsBottomSheet.kt` | Удалить |
| `ContextSettingsDialog.kt` | Переписать (только summaryEvery) |
| `MessageBubbleMenu.kt` | Переписать |
| `BranchSwitchDialog` (в ChatScreen) | Удалить |
| `SendMessageUseCase.kt` | Средние правки |
| `ContextCompressor.kt` | Удалить extractFacts |
| `ContextSettingsDomain.kt` | Сильные правки |
| `ContextMode (enum)` | Удалить целиком |
| `FactsData.kt` | Удалить |
| `FactsStorage.kt` | Удалить |
| `FactsStorageImpl.kt` | Удалить |
| `BranchData.kt` | Удалить |
| `BranchRepository.kt` | Удалить |
| `BranchRepositoryImpl.kt` | Удалить |
| `DeepSeekProvider.kt` | Добавить обработку ошибок |
| `OpenRouterProvider.kt` | Добавить обработку ошибок |
| `AiApiClient.kt` | Добавить проверку HTTP статусов |
| `AiError.kt` | **Создать** |
| `FactsEntity.kt` | Удалить |
| `FactsDao.kt` | Удалить |
| `BranchEntity.kt` | Удалить |
| `BranchDao.kt` | Удалить |
| `MessageEntity.kt` | Удалить `branchId` |
| `ContextSettingsEntity.kt` | Удалить `contextMode`, `keepLastCount`, `truncateChars` |
| `AiChatDatabase.kt` | Убрать entities, dao |
| `DatabaseModule.kt` | Убрать провайдеры |

---

## Порядок выполнения

1. **БД**: Entity + Dao для Facts и Branches → удалить. MessageEntity → удалить branchId. ContextSettingsEntity → удалить колонки. AiChatDatabase → обновить. DatabaseModule → обновить.
2. **Domain модели**: FactsData → удалить. BranchData → удалить. ContextMode → удалить целиком. ContextSettingsDomain → упростить.
3. **Services**: FactsStorage → удалить. ContextCompressor → удалить extractFacts.
4. **SendMessageUseCase**: убрать Facts, Branches, ContextMode, упростить до всегда-включённой суммаризации.
5. **ChatUiState**: очистить поля, оставить summaryEvery=20.
6. **ChatViewModel**: очистить методы и DI.
7. **ChatScreen + диалоги**: убрать Facts / Branches UI, переписать ContextSettingsDialog (только summaryEvery).
8. **Error handling**: AiError → AiApiClient → Providers → UseCase → VM → UI.
9. **MessageBubbleMenu**: переписать, убрать branch.