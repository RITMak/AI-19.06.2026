# План: Система памяти (3 уровня) + рефакторинг экрана создания чата

## Архитектура

### 1. Краткосрочная память = история сообщений чата ✅ (уже реализована)

### 2. Рабочая память (WorkingMemory)
- Задачи/цели, привязанные к профилю
- Чат может быть привязан к задаче (ChatEntity.workingMemoryId)
- Таблица: `working_memory`
  - `id` (PK, String)
  - `profile_id` (FK→profiles CASCADE)
  - `title` (String)
  - `description` (String)

### 3. Долговременная память (LongTermMemory)
- Информация о пользователе (ключ → значение)
- Таблица: `long_term_memory`
  - `id` (PK, String)
  - `profile_id` (FK→profiles CASCADE)
  - `key` (String)
  - `value` (String)

### 4. Изменения в существующих таблицах
- ProfileEntity: удалить `createdAt`
- ChatEntity: добавить `workingMemoryId: String?` (nullable FK→working_memory, onDelete=SET_NULL)
- AiChatDatabase: версия → 1, добавить новые entity + dao

---

## Пошаговый план реализации

### Шаг 1 — core/database/
1. CREATE `entity/WorkingMemoryEntity.kt`
2. CREATE `entity/LongTermMemoryEntity.kt`
3. CREATE `dao/WorkingMemoryDao.kt`
4. CREATE `dao/LongTermMemoryDao.kt`
5. EDIT `entity/ProfileEntity.kt` — удалить `createdAt`
6. EDIT `entity/ChatEntity.kt` — добавить `workingMemoryId`
7. EDIT `AiChatDatabase.kt` — версия 1, новые entity и dao

### Шаг 2 — core/domain/
8. CREATE `model/WorkingMemoryDomain.kt`
9. CREATE `model/LongTermMemoryDomain.kt`
10. CREATE `repository/WorkingMemoryRepository.kt`
11. CREATE `repository/LongTermMemoryRepository.kt`

### Шаг 3 — core/data/
12. CREATE `mapper/WorkingMemoryMapper.kt`
13. CREATE `mapper/LongTermMemoryMapper.kt`
14. CREATE `repository/WorkingMemoryRepositoryImpl.kt`
15. CREATE `repository/LongTermMemoryRepositoryImpl.kt`

### Шаг 4 — core/di/
16. EDIT `DatabaseModule.kt` — provide новых DAO
17. EDIT `RepositoryModule.kt` — provide новых repository

### Шаг 5 — core/domain/ usecases + поддержка workingMemoryId
18. EDIT `CreateChatUseCase.kt` — добавить `workingMemoryId: String?`
19. EDIT `ChatRepository.kt` — метод `createChat` с workingMemoryId
20. EDIT `ChatRepositoryImpl.kt` — реализация
21. EDIT `ChatDao.kt` — поддержка workingMemoryId
22. EDIT `ChatMapper.kt` — маппинг workingMemoryId
23. EDIT `ProfileMapper.kt` — убрать createdAt
24. EDIT `ProfileDomain.kt` — убрать createdAt

### Шаг 6 — Переименование ModelsScreen → CreateChatScreen
25. RENAME `features/models/` → `features/chat-create/` (папка)
26. RENAME `ModelsScreen.kt` → `CreateChatScreen.kt` (класс `ModelsScreen` → `CreateChatScreen`)
27. RENAME `ModelsViewModel.kt` → `CreateChatViewModel.kt` (класс → `CreateChatViewModel`)
28. RENAME `ModelsUiState.kt` → `CreateChatUiState.kt` (класс → `CreateChatUiState`)
29. EDIT `MainActivity.kt` — `onNavigateToModels` → `onNavigateToCreateChat`, импорт CreateChatScreen
30. EDIT `ChatsScreen.kt` — `onNavigateToModels` → `onNavigateToCreateChat`

### Шаг 7 — Добавить выбор задачи на экран CreateChat
31. EDIT `CreateChatUiState.kt` — добавить `workingMemories`, `selectedWorkingMemory`, `showWorkingDialog`
32. EDIT `CreateChatViewModel.kt` — inject `WorkingMemoryRepository`, загрузка задач, CRUD. При создании чата передавать `workingMemoryId`
33. EDIT `CreateChatScreen.kt` — добавить выпадающий список задач + кнопка "+" для новой задачи. enable кнопки только когда выбраны модель И задача

### Шаг 8 — Убрать MemoryBridge и SelectWorkingMemoryDialog
34. DELETE `MemoryBridge.kt`
35. DELETE `SelectWorkingMemoryDialog.kt`
36. EDIT `ChatsScreen.kt` — убрать showSelectMemoryDialog, FAB "+" → всегда `onNavigateToCreateChat()`
37. EDIT `ChatsViewModel.kt` — убрать `createChatWithMemory`

### Шаг 9 — Контекст памяти в промпт
38. EDIT `ChatUiState.kt` — добавить `taskName`, `userInfo`
39. EDIT `ChatViewModel.kt` — загружать `WorkingMemory` и `LongTermMemory` при `loadChat()`, передавать в `sendMessageUseCase`
40. EDIT `SendMessageUseCase.kt` — добавить `MemoryContext` с taskName + userInfo, передавать в `ContextCompressor.compress()`
41. EDIT `ContextCompressor.kt` — добавить `memorySystemPrompt` в `compress()`, вставлять system-сообщение с контекстом
42. EDIT `ChatScreen.kt` — кнопка "🧠 Память" в меню чата (просмотр taskName + userInfo)