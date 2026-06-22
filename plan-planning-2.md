# План: Переработка PlanBubble в обычные баблы + единый флоу этапов

## Сводка изменений

Убираем PlanBubble как отдельный UI-компонент.
Все сообщения LLM — обычные стриминг-баблы.
Кнопки подтверждения/повтора показываются **после каждого сообщения** с `[STAGE_COMPLETE]`.

## Единый флоу для всех этапов

### PLANNING

1. LLM входит в этап, план НЕ подтверждён
2. LLM пишет **сообщение с планом**: перечисляет sub-stages + `[SUBSTAGES: N]` + `[SUBSTAGE_LABEL: ...]` + `[STAGE_COMPLETE]`
3. Сообщение отображается как **обычный стриминг-бабл** (ассистент)
4. После завершения стриминга — кнопки:
   - **"📋 Принять план"** → `confirmPlan()`
   - **"🔄 Продолжить уточнение"** → `rejectStageTransition()`
5. "Принять план" → `planConfirmed=true`, auto-send с trigger **"План утверждён"**
6. LLM проходит sub-stages последовательно (автоматически, без подтверждений пользователя)
7. Все sub-stages сделаны → LLM пишет **итоговое сообщение** (что спланировано, список результатов) + `[STAGE_COMPLETE]`
8. Кнопки:
   - **"✅ Принять"** → `confirmStageTransition()` → PLANNING→EXECUTION
   - **"🔄 Повторить этап"** → `repeatStage()` → сброс на subStage=0, trigger "Повторить этап"

### EXECUTION

1. LLM входит в этап, план НЕ подтверждён
2. LLM пишет **план выполнения**: sub-stages + `[STAGE_COMPLETE]`
3. Стриминг-бабл + кнопки: **"📋 Принять план"** / **"🔄 Продолжить уточнение"**
4. "Принять план" → `planConfirmed=true`, auto-send "План утверждён"
5. LLM выполняет sub-stages автоматически
6. Все sub-stages сделаны → LLM пишет **итоговое сообщение** (что сделано, результаты) + `[STAGE_COMPLETE]`
7. Кнопки:
   - **"✅ Завершить задачу"** → `confirmStageTransition()` → EXECUTION→DONE
   - **"🔄 Повторить этап"** → `repeatStage()` → сброс на subStage=0

### DONE

1. LLM пишет **итоговое сообщение** (резюме всей задачи) + `[STAGE_COMPLETE]`
2. Кнопки:
   - **"✅ Принять"** → просто убрать кнопки (`showStageConfirm=null`)
   - **"🔄 Повторить"** → `confirmStageTransition()` → DONE→EXECUTION (reopen)
3. Если пользователь пишет новое сообщение → DONE→PLANNING (новая задача)

---

## Изменения в ChatScreen.kt

### Что удалить
- `isPlanBubbleActive` (строка 263-265)
- Проверку `message == uiState.messages.lastOrNull() && message.role == Role.ASSISTANT` (строка 270-272)
- `PlanBubble` composable вызов (строки 304-320)

### Что изменить
- **Стриминг** (строка 347): убрать `&& uiState.planConfirmed`
  ```
  if (uiState.streamingText != null) {  // без planConfirmed
  ```
- **Спиннер** (строка 358): убрать `&& uiState.planConfirmed`
  ```
  if (uiState.isLoading) {  // без planConfirmed
  ```

### Логика кнопок после сообщений

```kotlin
// После сообщений, если есть STAGE_COMPLETE и не грузится
if (uiState.hasStageCompleteMarker && !uiState.isLoading && uiState.streamingText == null) {
    item(key = "stage_confirm_buttons") {
        if (!uiState.planConfirmed) {
            // Первый план этапа — кнопки "Принять план" / "Продолжить"
            PlanConfirmButtons(
                onConfirm = { viewModel.confirmPlan() },
                onReject = { viewModel.rejectStageTransition() }
            )
        } else {
            // Итоговое сообщение — кнопки "Принять" / "Повторить этап"
            StageConfirmButtons(
                confirmInfo = uiState.showStageConfirm,
                onConfirm = { viewModel.confirmStageTransition() },
                onReject = { viewModel.repeatStage() }
            )
        }
    }
}
```

### Новый composable PlanConfirmButtons

```kotlin
@Composable
fun PlanConfirmButtons(
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onConfirm, modifier = Modifier.weight(1f)) {
            Text("📋 Принять план", fontSize = 13.sp)
        }
        TextButton(onClick = onReject, modifier = Modifier.weight(1f)) {
            Text("🔄 Продолжить уточнение", fontSize = 13.sp)
        }
    }
}
```

---

## Изменения в ChatViewModel.kt

### confirmPlan() — исправить trigger

```kotlin
fun confirmPlan() {
    val chatId = currentChatId ?: return
    viewModelScope.launch {
        val currentState = chatStateRepository.get(chatId) ?: return@launch
        val newState = currentState.copy(
            planConfirmed = true,
            planSteps = emptyList(),
            hasStageCompleteMarker = false
        )
        chatStateRepository.upsert(newState)

        _uiState.value = _uiState.value.copy(
            planConfirmed = true,
            showStageConfirm = null,
            hasStageCompleteMarker = false,
            planSteps = emptyList()
        )

        // Отправляем "План утверждён" — LLM начинает sub-stage 0
        userMessageText = "План утверждён"
        sendMessage()
    }
}
```

### repeatStage() — новый метод

```kotlin
fun repeatStage() {
    val chatId = currentChatId ?: return
    viewModelScope.launch {
        chatStateRepository.setStep(chatId, 0)
        val newState = ChatStateDomain(
            chatId = chatId,
            stage = _uiState.value.currentStage,
            step = 0,
            subStage = 0,
            planConfirmed = false
        )
        chatStateRepository.upsert(newState)

        _uiState.value = _uiState.value.copy(
            currentStep = 0,
            subStage = 0,
            planConfirmed = false,
            showStageConfirm = null,
            hasStageCompleteMarker = false
        )

        userMessageText = "Повторить этап"
        sendMessage()
    }
}
```

### rejectStageTransition() — без изменений, оставить как есть

```kotlin
fun rejectStageTransition() {
    val confirmInfo = _uiState.value.showStageConfirm ?: return
    if (confirmInfo.currentStage == ChatStage.DONE) {
        _uiState.value = _uiState.value.copy(
            showStageConfirm = null,
            hasStageCompleteMarker = false
        )
        return
    }
    val chatId = currentChatId ?: return
    _uiState.value = _uiState.value.copy(showStageConfirm = null)
    viewModelScope.launch {
        autoSendStage(confirmInfo.currentStage, confirmInfo.currentStage)
    }
}
```

### autoSendStage() — добавить новые triggerText

```kotlin
private fun autoSendStage(currentStage: ChatStage, targetStage: ChatStage) {
    val triggerText = when {
        currentStage == ChatStage.PLANNING && targetStage == ChatStage.EXECUTION -> "Начинаю выполнение задачи"
        currentStage == ChatStage.EXECUTION && targetStage == ChatStage.DONE -> "Задача выполнена"
        currentStage == ChatStage.DONE && targetStage == ChatStage.EXECUTION -> "Продолжаю выполнение"
        currentStage == ChatStage.PLANNING && targetStage == ChatStage.PLANNING -> "Продолжить уточнение"
        currentStage == ChatStage.EXECUTION && targetStage == ChatStage.EXECUTION -> "Нужна доработка"
        currentStage == ChatStage.DONE && targetStage == ChatStage.DONE -> ""  // просто убрать кнопки
        else -> return
    }
    if (triggerText.isEmpty()) return
    userMessageText = triggerText
    sendMessage()
}
```

### processFsmMarkers() — убрать сбор planSteps (не нужен без PlanBubble)

```kotlin
private suspend fun processFsmMarkers(chatId: String, result: SendMessageResult) {
    var updated = _uiState.value.copy(
        hasStageCompleteMarker = result.fsmStageComplete,
        llmTransitionRejectedMessage = result.fsmLlmTransitionRejected,
        subStageTransitionRejectedMessage = result.fsmSubStageTransitionRejected
        // planSteps не используем
    )
    // ... остальная логика без planSteps
}
```

---

## Изменения в ChatUiState.kt

Убрать `planSteps: List<String>` — больше не нужен.

```kotlin
data class ChatUiState(
    // ... все поля, кроме planSteps
    // val planSteps: List<String> = emptyList() — удалить
)
```

---

## Изменения в SendMessageUseCase.kt

### buildStagePrompt() — поправить "первый вход в этап"

Сейчас в `!chatState.planConfirmed` LLM пишет "Do NOT ask questions. Just show the plan."
Нужно указать, что план должен быть читаемым текстом, а не набором маркеров:

```
"Write a readable plan as text (not just markers). 
User will see this message directly. 
Then output [SUBSTAGES: N] + [SUBSTAGE_LABEL: ...] for each sub-stage.
Finally output [STAGE_COMPLETE] so user can confirm the plan."
```

---

## Файлы для изменения

| № | Файл | Изменения |
|---|------|-----------|
| 1 | `features/chat/.../ChatScreen.kt` | Удалить PlanBubble, починить условия стриминга/спиннера, новые кнопки |
| 2 | `features/chat/.../ChatViewModel.kt` | `confirmPlan()`, `repeatStage()`, `processFsmMarkers` без planSteps, `autoSendStage` с новыми trigger-ами |
| 3 | `features/chat/.../ChatUiState.kt` | Удалить `planSteps` |
| 4 | `core/domain/.../usecase/SendMessageUseCase.kt` | Поправить `buildStagePrompt` для читаемого плана |