# План: Инварианты профиля

## Описание
Добавить систему инвариантов — правил поведения AI-ассистента, привязанных к профилю. Инварианты подмешиваются в system prompt при отправке каждого сообщения в чате этого профиля.

---

## 1. База данных (Room)

### 1.1 Новая entity `InvariantEntity`

```
InvariantEntity(
    id: String,              // UUID, PK
    profileId: String,       // FK→profiles.id, onDelete=CASCADE
    text: String,            // текст правила
    createdAt: Long
)
```

### 1.2 Новый DAO `InvariantDao`

```
@Dao interface InvariantDao
    fun getByProfileId(profileId: String): Flow<List<InvariantEntity>>
    suspend fun getByProfileIdOnce(profileId: String): List<InvariantEntity>
    suspend fun insert(entity: InvariantEntity)
    suspend fun delete(id: String)
    suspend fun deleteByProfileId(profileId: String)
```

### 1.3 Обновление `AiChatDatabase`

- Добавить `InvariantEntity` в entities
- Добавить `abstract fun invariantDao(): InvariantDao`
- version = 1 (чистая установка через fallbackToDestructiveMigration)

---

## 2. Domain слой

### 2.1 Новая модель `Invariant`

```
data class Invariant(
    val id: String,
    val profileId: String,
    val text: String,
    val createdAt: Long
)
```

### 2.2 Новый репозиторий `InvariantRepository`

```
interface InvariantRepository {
    fun observeByProfile(profileId: String): Flow<List<Invariant>>
    suspend fun getByProfileOnce(profileId: String): List<Invariant>
    suspend fun add(profileId: String, text: String): Invariant
    suspend fun remove(id: String)
}
```

---

## 3. Data слой

### 3.1 Маппер `InvariantMapper`

- `InvariantEntity ↔ Invariant`

### 3.2 Имплементация `InvariantRepositoryImpl`

- `InvariantDao` → `Invariant` через маппер
- `add()` — генерирует UUID, создаёт entity, вставляет, возвращает domain-модель

---

## 4. DI

### 4.1 `DatabaseModule.kt`

- Добавить `provideInvariantDao()`

### 4.2 `RepositoryModule.kt`

- Добавить `bind InvariantRepository → InvariantRepositoryImpl`

---

## 5. Внедрение в `SendMessageUseCase`

```kotlin
// В начале invoke():
val invariants = invariantRepository.getByProfileOnce(chat.profileId)
if (invariants.isNotEmpty()) {
    val rules = invariants.withIndex().joinToString("\n") { (i, it) -> "${i + 1}. ${it.text}" }
    val prompt = TextCleaner.clean(
        "You are an AI assistant. You MUST follow these rules (invariants):\n$rules\n\n" +
        "These invariants are NOT optional. If the user asks you to do something that contradicts any of these rules, politely refuse and explain which invariant is violated."
    )
    // system message → добавляется в начало списка сообщений
}
```

---

## 6. UI — `InvariantsScreen` (как WorkingMemory)

Доступ: бургер (ChatsScreen) → "📜 Инварианты" (под "🧠 Память")

UI полностью идентичен WorkingMemory:
- Scaffold + TopAppBar "📜 Инварианты" + кнопка назад
- Список: каждый элемент = текст правила + иконка удалить
- FAB "+" → AlertDialog с TextField "Новое правило"
- Пустой список: "Правил пока нет. Добавьте правило для AI-ассистента."

### 6.1 `InvariantsViewModel`

```
@HiltViewModel
class InvariantsViewModel @Inject constructor(
    private val invariantRepository: InvariantRepository,
    private val profileRepository: ProfileRepository
) : ViewModel()
```

Методы:
- загрузить инварианты активного профиля
- добавить правило
- удалить правило

---

## 7. Навигация

### 7.1 `ChatsViewModel`

- Добавить `fun onNavigateToInvariants()` — открывает InvariantsScreen

### 7.2 `ChatsScreen` (бургер)

- Добавить пункт "📜 Инварианты" после "🧠 Память"
- `onClick = onNavigateToInvariants`

---

## 8. Список файлов

### Создать (8):
| # | Файл | Путь |
|---|------|------|
| 1 | `InvariantEntity.kt` | `core/database/.../entity/` |
| 2 | `InvariantDao.kt` | `core/database/.../dao/` |
| 3 | `Invariant.kt` | `core/domain/.../model/` |
| 4 | `InvariantRepository.kt` | `core/domain/.../repository/` |
| 5 | `InvariantMapper.kt` | `core/data/.../mapper/` |
| 6 | `InvariantRepositoryImpl.kt` | `core/data/.../repository/` |
| 7 | `InvariantsViewModel.kt` | `features/settings/.../` |
| 8 | `InvariantsScreen.kt` | `features/settings/.../` |

### Изменить (6):
| # | Файл | Действие |
|---|------|----------|
| 9 | `AiChatDatabase.kt` | +InvariantEntity +invariantDao() |
| 10 | `DatabaseModule.kt` | +provideInvariantDao |
| 11 | `RepositoryModule.kt` | +bind InvariantRepository |
| 12 | `SendMessageUseCase.kt` | +invariantRepository, system prompt |
| 13 | `ChatsScreen.kt` | +пункт "📜 Инварианты" в бургер |
| 14 | `ChatsViewModel.kt` | +navigateToInvariants |

---

## 9. Порядок реализации

1. Room: InvariantEntity + InvariantDao → AiChatDatabase
2. Domain: Invariant + InvariantRepository
3. Data: InvariantMapper + InvariantRepositoryImpl
4. DI: DatabaseModule + RepositoryModule
5. SendMessageUseCase: inject InvariantRepository, system prompt
6. Feature: InvariantsViewModel + InvariantsScreen
7. Nav: ChatsViewModel + ChatsScreen (бургер)
8. Build & проверка