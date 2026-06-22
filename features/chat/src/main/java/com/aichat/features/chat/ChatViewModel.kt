package com.aichat.features.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.data.datastore.SettingsDataStore
import com.aichat.core.domain.model.ApiCallException
import com.aichat.core.domain.model.AuthException
import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.ChatStateDomain
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.domain.model.NetworkException
import com.aichat.core.domain.model.RateLimitException
import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.domain.repository.ChatStateRepository
import com.aichat.core.domain.repository.ContextSettingsRepository
import com.aichat.core.domain.repository.LongTermMemoryRepository
import com.aichat.core.domain.repository.MessageRepository
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.domain.repository.StageSummaryRepository
import com.aichat.core.domain.repository.WorkingMemoryRepository
import com.aichat.core.domain.model.StageSummary
import com.aichat.core.domain.usecase.MemoryContext
import com.aichat.core.domain.usecase.SendMessageUseCase
import com.aichat.core.model.AiProvider
import com.aichat.core.model.MessageType
import com.aichat.core.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val modelRepository: ModelRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val contextSettingsRepository: ContextSettingsRepository,
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository,
    private val chatStateRepository: ChatStateRepository,
    private val stageSummaryRepository: StageSummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentChat: ChatDomain? = null
    private var userMessageText: String = ""
    private var observeMessagesJob: Job? = null
    private var currentChatId: String? = null
    private var currentTaskNumber: Int = 0

    fun loadChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            val chat = chatRepository.getChatById(chatId) ?: return@launch
            currentChat = chat

            val cachedModels = modelRepository.getModels(chat.provider)
            val modelForChat = cachedModels.find { it.id == chat.modelId }
            val modelName = modelForChat?.name ?: chat.modelId
            val contextLimit = modelForChat?.contextLength ?: 0

            _uiState.value = _uiState.value.copy(
                modelName = modelName,
                contextLimit = contextLimit
            )

            // Load summary settings
            val contextSettings = contextSettingsRepository.getSettings(chatId)
            if (contextSettings != null) {
                _uiState.value = _uiState.value.copy(
                    summaryEvery = contextSettings.summaryEvery
                )
            }

            // Load memory context
            val wmId = chat.workingMemoryId
            if (wmId != null) {
                val workingMemory = workingMemoryRepository.getById(wmId)
                if (workingMemory != null) {
                    _uiState.value = _uiState.value.copy(
                        taskName = workingMemory.title
                    )
                }
            }

            // Load long-term memory (user info) for current profile
            val longTermMemories = longTermMemoryRepository.getByProfileId(chat.profileId).first()
            _uiState.value = _uiState.value.copy(
                userInfo = longTermMemories.map { Pair(it.key, it.value) }
            )

            // Load FSM state
            loadChatState(chatId)

            // Load messages
            observeMessages(chatId)
        }
    }

    private suspend fun loadChatState(chatId: String) {
        var state = chatStateRepository.get(chatId)
        val isNew = state == null
        if (isNew) {
            // Create initial state
            state = ChatStateDomain.initial(chatId)
            chatStateRepository.upsert(state)

            // Insert initial stage change bubble
            val stageMsg = MessageDomain(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = Role.SYSTEM,
                messageType = MessageType.STAGE_CHANGE,
                text = "🧠 Этап: Планирование"
            )
            messageRepository.insertMessage(stageMsg)
        }
        _uiState.value = _uiState.value.copy(
            currentStage = state.stage,
            currentStep = state.step,
            totalSteps = state.totalSteps,
            subStage = state.subStage,
            subStagesTotal = state.subStagesTotal,
            subStageLabel = state.subStageLabel,
            planConfirmed = state.planConfirmed,
            planProposedCount = state.planProposedCount,
            hasStageCompleteMarker = state.hasStageCompleteMarker,
            firstUserQuery = state.firstUserQuery,
            planningResult = state.planningResult,
            executionResult = state.executionResult
        )

        currentTaskNumber = stageSummaryRepository.getMaxTaskNumber(chatId) + 1
    }

    private fun observeMessages(chatId: String) {
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            messageRepository.getMessagesByChatId(chatId).collect { messages ->
                val totalTokens = messages.sumOf { it.totalTokens }
                val totalCost = messages.sumOf { it.costUsd }

                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    totalTokens = totalTokens,
                    totalCost = totalCost
                )
            }
        }
    }

    fun updateMessageText(text: String) {
        userMessageText = text
    }

    fun sendMessage() {
        val text = userMessageText.trim()
        if (text.isEmpty() || currentChat == null) return

        val chat = currentChat!!
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            streamingText = null,
            showStageConfirm = null,
            hasStageCompleteMarker = false,
            llmTransitionRejectedMessage = null
        )
        userMessageText = ""

        // Save first user query for FSM stage prompts
        val chatId = chat.id
        if (_uiState.value.firstUserQuery.isEmpty() && _uiState.value.currentStage == ChatStage.PLANNING) {
            viewModelScope.launch {
                val currentState = chatStateRepository.get(chatId)
                if (currentState != null && currentState.firstUserQuery.isEmpty()) {
                    val updatedState = currentState.copy(firstUserQuery = text)
                    chatStateRepository.upsert(updatedState)
                    _uiState.value = _uiState.value.copy(firstUserQuery = text)
                }
            }
        }

        viewModelScope.launch {
            try {
                // Generate chat title from first message if still default
                val currentTitle = chatRepository.getChatById(chat.id)?.title
                if (currentTitle == "New Chat") {
                    val title = text.take(50).replace("\n", " ")
                    chatRepository.updateChatTitle(chat.id, title)
                }

                // Load messages history
                val history = messageRepository.getMessagesByChatId(chat.id).first()

                // Get custom API key from settings
                val customApiKey = when (chat.provider) {
                    AiProvider.OPENROUTER -> settingsDataStore.openRouterApiKey.first() ?: ""
                    AiProvider.DEEPSEEK -> settingsDataStore.deepSeekApiKey.first() ?: ""
                }

                // Build memory context from current state
                val memoryContext = MemoryContext(
                    taskName = _uiState.value.taskName,
                    userInfo = _uiState.value.userInfo
                )

                // Build current FSM state
                val cs = _uiState.value
                val chatState = ChatStateDomain(
                    chatId = chat.id,
                    stage = cs.currentStage,
                    subStage = cs.subStage,
                    subStagesTotal = cs.subStagesTotal,
                    subStageLabel = cs.subStageLabel,
                    step = cs.currentStep,
                    totalSteps = cs.totalSteps,
                    planConfirmed = cs.planConfirmed,
                    planProposedCount = cs.planProposedCount,
                    firstUserQuery = cs.firstUserQuery,
                    planningResult = cs.planningResult,
                    executionResult = cs.executionResult
                )

                // If current stage is DONE and user sends a new prompt → switch to PLANNING
                if (_uiState.value.currentStage == ChatStage.DONE) {
                    chatStateRepository.setStage(chat.id, ChatStage.PLANNING)
                    _uiState.value = _uiState.value.copy(
                        currentStage = ChatStage.PLANNING,
                        currentStep = 0,
                        subStage = 0,
                        subStagesTotal = 0,
                        subStageLabel = "",
                        planConfirmed = false,
                        showStageConfirm = null,
                        hasStageCompleteMarker = false
                    )
                    chatStateRepository.setStep(chat.id, 0)
                    currentTaskNumber = stageSummaryRepository.getMaxTaskNumber(chat.id) + 1
                }

                // Load stage summaries for context
                val stageSummaries = stageSummaryRepository.getByChatId(chat.id)

                // Send request — useCase inserts user message into DB, returns result
                val result = sendMessageUseCase(
                    chatId = chat.id,
                    text = text,
                    provider = chat.provider,
                    modelId = chat.modelId,
                    messagesHistory = history,
                    customApiKey = customApiKey,
                    memoryContext = memoryContext,
                    chatState = chatState,
                    stageSummaries = stageSummaries
                )

                // --- Process FSM markers ---
                processFsmMarkers(chat.id, result)

                // Insert rejected transition messages if any
                val rejectedLlm = result.fsmLlmTransitionRejected
                if (rejectedLlm != null) {
                    val rejectedMsg = MessageDomain(
                        id = UUID.randomUUID().toString(),
                        chatId = chat.id,
                        role = Role.SYSTEM,
                        messageType = MessageType.FSM_REJECT,
                        text = rejectedLlm
                    )
                    messageRepository.insertMessage(rejectedMsg)
                }

                // Update total tokens and cost (assistant message only)
                val assMsg = result.assistantMessage
                val s = _uiState.value
                _uiState.value = _uiState.value.copy(
                    totalTokens = s.totalTokens + assMsg.totalTokens,
                    totalCost = s.totalCost + assMsg.costUsd
                )

                // Streaming animation
                val fullText = assMsg.text
                _uiState.value = _uiState.value.copy(isLoading = false, streamingText = "")

                for (i in fullText.indices) {
                    val partial = fullText.substring(0, i + 1)
                    _uiState.value = _uiState.value.copy(streamingText = partial)
                    delay(25)
                }

                // Done — insert assistant message into DB, then clear streaming
                messageRepository.insertMessage(result.assistantMessage)
                _uiState.value = _uiState.value.copy(streamingText = null)

            } catch (e: AuthException) {
                Log.e("ChatViewModel", "sendMessage auth error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamingText = null,
                    error = "❌ Неверный API ключ"
                )
            } catch (e: RateLimitException) {
                Log.e("ChatViewModel", "sendMessage rate limited", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamingText = null,
                    error = "⏳ Превышен лимит запросов, подождите"
                )
            } catch (e: ApiCallException) {
                Log.e("ChatViewModel", "sendMessage api error: ${e.statusCode}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamingText = null,
                    error = "⚠️ Ошибка сервера: ${e.message}"
                )
            } catch (e: NetworkException) {
                Log.e("ChatViewModel", "sendMessage network error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamingText = null,
                    error = "🌐 Нет соединения с сервером"
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "sendMessage failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamingText = null,
                    error = "❓ Неизвестная ошибка: ${e.message}"
                )
            }
        }
    }

    private suspend fun generateStageSummary(chatId: String, stage: ChatStage) {
        // Placeholder — summary generation requires providerService
    }

    private suspend fun processFsmMarkers(chatId: String, result: com.aichat.core.domain.usecase.SendMessageResult) {
        var dbProposedCount = _uiState.value.planProposedCount

        // Process stage complete — show confirm dialog and increment planProposedCount
        if (result.fsmStageComplete) {
            val confirmInfo = buildStageConfirmInfo(_uiState.value.currentStage, result.fsmTargetStage)
            if (!_uiState.value.planConfirmed) {
                dbProposedCount = _uiState.value.planProposedCount + 1
            }
            _uiState.value = _uiState.value.copy(
                hasStageCompleteMarker = true,
                showStageConfirm = confirmInfo,
                planProposedCount = dbProposedCount,
                llmTransitionRejectedMessage = result.fsmLlmTransitionRejected
            )
        } else {
            _uiState.value = _uiState.value.copy(
                hasStageCompleteMarker = false,
                llmTransitionRejectedMessage = result.fsmLlmTransitionRejected
            )
        }

        // Save state to DB
        val currentState = chatStateRepository.get(chatId)
        if (currentState != null) {
            val updatedState = currentState.copy(
                hasStageCompleteMarker = result.fsmStageComplete,
                planProposedCount = dbProposedCount
            )
            chatStateRepository.upsert(updatedState)
        }
    }

    private fun buildStageConfirmInfo(
        currentStage: ChatStage,
        targetStage: ChatStage?
    ): StageConfirmInfo? {
        val target = targetStage ?: when (currentStage) {
            ChatStage.PLANNING -> ChatStage.EXECUTION
            ChatStage.EXECUTION -> ChatStage.DONE
            ChatStage.DONE -> ChatStage.EXECUTION // reopen
        }

        val (confirm, reject) = when (currentStage) {
            ChatStage.PLANNING -> "✅ Приступить к выполнению" to "🔄 Продолжить уточнение"
            ChatStage.EXECUTION -> "✅ Завершить задачу" to "🔄 Нужно ещё доработать"
            ChatStage.DONE -> "🔄 Вернуться к выполнению" to "❌ Нет, спасибо"
        }

        return StageConfirmInfo(
            currentStage = currentStage,
            targetStage = target,
            confirmLabel = confirm,
            rejectLabel = reject
        )
    }

    private fun buildStageChangeText(targetStage: ChatStage): String {
        return when (targetStage) {
            ChatStage.PLANNING -> "🧠 Этап: Планирование"
            ChatStage.EXECUTION -> "⚡ Этап: Выполнение"
            ChatStage.DONE -> "🎉 Этап: Завершено"
        }
    }

    fun confirmStageTransition() {
        val confirmInfo = _uiState.value.showStageConfirm ?: return
        val chatId = currentChatId ?: return

        viewModelScope.launch {
            // 0. Get last assistant message text to save as stage result
            val messages = messageRepository.getMessagesByChatId(chatId).first()
            val lastAssistantMessage = messages.lastOrNull { it.role == Role.ASSISTANT }
            val lastAssistantText = lastAssistantMessage?.text ?: ""

            // 1. Insert stage change bubble
            val stageText = buildStageChangeText(confirmInfo.targetStage)
            val stageMessage = MessageDomain(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = Role.SYSTEM,
                messageType = MessageType.STAGE_CHANGE,
                text = stageText,
                textClean = ""
            )
            messageRepository.insertMessage(stageMessage)

            // 2. Update stage in DB — reset subStage + planConfirmed for new stage
            // Also save stage result for prompt context
            val currentState = chatStateRepository.get(chatId) ?: return@launch

            val newState = currentState.copy(
                stage = confirmInfo.targetStage,
                step = 0,
                planConfirmed = false,
                hasStageCompleteMarker = false,
                planningResult = if (confirmInfo.targetStage == ChatStage.EXECUTION) lastAssistantText else currentState.planningResult,
                executionResult = if (confirmInfo.targetStage == ChatStage.DONE) lastAssistantText else currentState.executionResult
            )
            chatStateRepository.upsert(newState)

            // 3. Update UI
            _uiState.value = _uiState.value.copy(
                currentStage = confirmInfo.targetStage,
                currentStep = 0,
                subStage = 0,
                subStagesTotal = 0,
                subStageLabel = "",
                planConfirmed = false,
                showStageConfirm = null,
                hasStageCompleteMarker = false,
                planningResult = if (confirmInfo.targetStage == ChatStage.EXECUTION) lastAssistantText else _uiState.value.planningResult,
                executionResult = if (confirmInfo.targetStage == ChatStage.DONE) lastAssistantText else _uiState.value.executionResult
            )

            // 4. Auto-send to LLM for the next stage
            autoSendStage(confirmInfo.currentStage, confirmInfo.targetStage)
        }
    }

    /**
     * Called when user clicks "✅ Приступить" on the pre-plan stage.
     * Sets planConfirmed=true so LLM starts working through sub-stages.
     */
    fun confirmPlan() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            val currentState = chatStateRepository.get(chatId) ?: return@launch
            val newState = currentState.copy(
                planConfirmed = true,
                hasStageCompleteMarker = false
            )
            chatStateRepository.upsert(newState)

            _uiState.value = _uiState.value.copy(
                planConfirmed = true,
                showStageConfirm = null,
                hasStageCompleteMarker = false
            )

            // Отправляем "План утверждён" — LLM начинает sub-stage 0
            userMessageText = "План утверждён"
            sendMessage()
        }
    }

    fun repeatStage() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            val currentState = chatStateRepository.get(chatId) ?: return@launch
            val newState = currentState.copy(
                subStage = 0,
                planConfirmed = false,
                hasStageCompleteMarker = false
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
    fun rejectStageTransition() {
        val confirmInfo = _uiState.value.showStageConfirm ?: return

        // DONE → just hide buttons, wait for user message
        if (confirmInfo.currentStage == ChatStage.DONE) {
            _uiState.value = _uiState.value.copy(
                showStageConfirm = null,
                hasStageCompleteMarker = false
            )
            return
        }

        val chatId = currentChatId ?: return

        // Hide buttons — user will type clarification manually
        _uiState.value = _uiState.value.copy(
            showStageConfirm = null,
            hasStageCompleteMarker = false
        )

        // Auto-send clarification message
        viewModelScope.launch {
            autoSendStage(confirmInfo.currentStage, confirmInfo.currentStage)
        }
    }

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

    fun updateSummaryEvery(summaryEvery: Int) {
        val chatId = currentChat?.id ?: return
        viewModelScope.launch {
            val settings = com.aichat.core.domain.model.ContextSettingsDomain(
                chatId = chatId,
                summaryEvery = summaryEvery
            )
            contextSettingsRepository.upsertSettings(settings)
            _uiState.value = _uiState.value.copy(
                summaryEvery = summaryEvery
            )
        }
    }
}