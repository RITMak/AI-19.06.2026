# План: Профили пользователей

## 1. База данных (Room)

### 1.1 Новая entity `ProfileEntity`
- `id: String` (UUID, PK)
- `name: String`
- `isActive: Boolean` (default false) — метка активного профиля
- `createdAt: Long`

### 1.2 Изменение `ChatEntity`
- Добавить `profileId: String` (NOT NULL, foreign key → profiles.id, onDelete = CASCADE)

### 1.3 Новый DAO `ProfileDao`
```
@Dao interface ProfileDao
  - insert(profile: ProfileEntity)
  - update(profile: ProfileEntity)
  - delete(profile: ProfileEntity)
  - getAllProfiles(): Flow<List<ProfileEntity>>
  - getActiveProfile(): Flow<ProfileEntity?>
  - getProfileById(id: String): ProfileEntity?
  - setAllInactive() — сбросить isActive=false у всех
```

### 1.4 Обновление `AiChatDatabase`
- Добавить ProfileEntity в entities
- Добавить abstract fun profileDao(): ProfileDao
- version = 3 (чистая установка)

## 2. Domain слой

### 2.1 Новая модель `ProfileDomain`
```
data class ProfileDomain(
  val id: String,
  val name: String,
  val isActive: Boolean,
  val createdAt: Long
)
```

### 2.2 Изменение `ChatDomain`
- Добавить `val profileId: String`

### 2.3 Новый репозиторий `ProfileRepository`
```
interface ProfileRepository {
  suspend fun createProfile(name: String): ProfileDomain
  suspend fun deleteProfile(profileId: String)  // каскадное удаление чатов
  suspend fun setActiveProfile(profileId: String)
  suspend fun getActiveProfile(): Flow<ProfileDomain?>
  fun getAllProfiles(): Flow<List<ProfileDomain>>
}
```

## 3. Data слой

### 3.1 Мапперы
- `ProfileMapper`: ProfileEntity ↔ ProfileDomain
- Обновить `ChatMapper` (добавить profileId)

### 3.2 Имплементация `ProfileRepositoryImpl`
- ProfileDao → ProfileDomain через маппер
- При создании профиля: setAllInactive() → вставка с isActive=true
- При удалении: CASCADE удалит чаты (Room foreign key)

## 4. DI

### 4.1 `DatabaseModule.kt`
- Добавить provideProfileDao()

### 4.2 `RepositoryModule.kt`
- Добавить bind ProfileRepository → ProfileRepositoryImpl

## 5. Feature-модуль `features/profile`

### 5.1 UI-состояние `ProfileUiState`
```
data class ProfileUiState(
  val profiles: List<ProfileDomain> = emptyList(),
  val activeProfile: ProfileDomain? = null,
  val showCreateDialog: Boolean = false,
  val showSwitchDialog: Boolean = false,
  val showDeleteConfirmDialog: ProfileDomain? = null,
  val isLoading: Boolean = false
)
```

### 5.2 ViewModel `ProfileViewModel`
```
@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val profileRepository: ProfileRepository
) : ViewModel()
```

Методы:
- `loadProfiles()` — подписка на Flow профилей
- `createProfile(name: String)` — создаёт и ставит активным
- `switchProfile(profileId: String)` — переключает активный профиль
- `deleteProfile(profileId: String)` — показывает confirm диалог
- `confirmDeleteProfile()` — удаляет
- `cancelDeleteProfile()` — закрывает confirm
- `showCreateDialog() / hideCreateDialog()`
- `showSwitchDialog() / hideSwitchDialog()`

### 5.3 UI Components

#### a) `CreateProfileDialog.kt`
- AlertDialog с TextField "Введите имя профиля"
- Кнопка "Создать" (disabled если имя пустое)
- Кнопка "Отмена"

#### b) `SwitchProfileDialog.kt`
- AlertDialog со списком профилей (RadioButton — какой выбран)
- Каждый профиль с кнопкой удаления (иконка корзины)
- Кнопка "Создать новый профиль" внизу
- Кнопка "Отмена"

#### c) `DeleteProfileConfirmDialog.kt`
- AlertDialog: "Вы уверены, что хотите удалить профиль? Чаты будут удалены вместе с профилем."
- Кнопка "Удалить" (красная) + "Отмена"

## 6. Изменение `ChatsScreen` (бургер-меню)

### 6.1 В `ChatsScreen` добавить ProfileViewModel + ProfileUiState
### 6.2 Обновить drawerContent:
```
// Верх бургера:
Text("Профиль: ${activeProfile?.name ?: "Не выбран"}")
IconButton(onClick = openSwitchDialog) // иконка смены профиля
Divider()

// Текущие пункты:
NavigationDrawerItem("Чаты", selected=true)
NavigationDrawerItem("Настройки")
```

### 6.3 Диалоги:
- CreateProfileDialog (showCreateDialog)
- SwitchProfileDialog (showSwitchDialog)
- DeleteProfileConfirmDialog (showDeleteConfirmDialog)

## 7. Навигация

### 7.1 `MainActivity.kt`
При старте приложения:
```
val activeProfile by profileViewModel.activeProfile.collectAsState()
if (activeProfile == null && !isLoading) {
    // Показываем диалог создания профиля (первый запуск)
}
```

### 7.2 Gate-логика
Пока профиль не создан — диалог создания профиля поверх всего. После создания → ChatsScreen.

### 7.3 При смене профиля в SwitchProfileDialog
- Закрыть диалог
- ChatsScreen перезагрузит список чатов (ChatsViewModel подписан на Flow чатов по profileId)

## 8. Список файлов для изменения/создания

### Создать:
1. `core/database/.../entity/ProfileEntity.kt`
2. `core/database/.../dao/ProfileDao.kt`
3. `core/domain/.../model/ProfileDomain.kt`
4. `core/domain/.../repository/ProfileRepository.kt`
5. `core/data/.../mapper/ProfileMapper.kt`
6. `core/data/.../repository/ProfileRepositoryImpl.kt`
7. `features/profile/.../ProfileViewModel.kt`
8. `features/profile/.../CreateProfileDialog.kt`
9. `features/profile/.../SwitchProfileDialog.kt`
10. `features/profile/.../DeleteProfileConfirmDialog.kt`

### Изменить:
1. `core/database/.../entity/ChatEntity.kt` — +profileId
2. `core/database/.../AiChatDatabase.kt` — +ProfileEntity, +ProfileDao, version=3
3. `core/domain/.../model/ChatDomain.kt` — +profileId
4. `core/data/.../mapper/ChatMapper.kt` — +profileId mapping
5. `core/data/.../repository/ChatRepositoryImpl.kt` — фильтр по profileId
6. `core/di/.../DatabaseModule.kt` — +provideProfileDao
7. `core/di/.../RepositoryModule.kt` — +bind ProfileRepository
8. `features/chats/.../ChatsScreen.kt` — профиль в бургере + диалоги
9. `features/chats/.../ChatsViewModel.kt` — фильтр чатов по profileId
10. `features/chats/.../ChatsUiState.kt` — +activeProfile поле
11. `app/.../MainActivity.kt` — gate-логика при старте

## 9. Порядок реализации

1. Room: ProfileEntity + ProfileDao + ChatEntity.profileId → AiChatDatabase (version 3)
2. Domain: ProfileDomain + ProfileRepository
3. Data: ProfileMapper + ProfileRepositoryImpl + ChatMapper update + ChatRepositoryImpl update
4. DI: DatabaseModule + RepositoryModule
5. Feature profile: ViewModel + Dialog-компоненты
6. ChatsScreen: интеграция профиля в бургер
7. MainActivity: gate-логика при старте
8. Build & проверка