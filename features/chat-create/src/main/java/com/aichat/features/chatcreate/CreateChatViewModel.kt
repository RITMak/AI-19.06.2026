package com.aichat.features.chatcreate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.core.data.datastore.SettingsDataStore
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.domain.model.WorkingMemoryDomain
import com.aichat.core.domain.repository.LongTermMemoryRepository
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.domain.repository.ProfileRepository
import com.aichat.core.domain.repository.WorkingMemoryRepository
import com.aichat.core.domain.usecase.CreateChatUseCase
import com.aichat.core.domain.usecase.GetModelsUseCase
import com.aichat.core.model.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateChatViewModel @Inject constructor(
    private val getModelsUseCase: GetModelsUseCase,
    private val createChatUseCase: CreateChatUseCase,
    private val modelRepository: ModelRepository,
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository,
    private val workingMemoryRepository: WorkingMemoryRepository,
    private val longTermMemoryRepository: LongTermMemoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChatUiState())
    val uiState: StateFlow<CreateChatUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        observeActiveProfile()
    }

    private fun observeActiveProfile() {
        viewModelScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(activeProfileId = profile?.id)
                if (profile != null) {
                    loadWorkingMemories(profile.id)
                }
            }
        }
    }

    private fun loadWorkingMemories(profileId: String) {
        viewModelScope.launch {
            workingMemoryRepository.getByProfileId(profileId).collect { memories ->
                _uiState.value = _uiState.value.copy(workingMemories = memories)
            }
        }
    }

    fun selectProvider(provider: AiProvider) {
        Log.d("CreateChatVM", "selectProvider: $provider -> clearing models")
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            models = emptyList(),
            selectedModelForCreate = null,
            isModelDropdownExpanded = false,
            error = null
        )
        loadModels()
    }

    fun setFilter(filter: ModelFilter) {
        _uiState.value = _uiState.value.copy(
            filter = filter,
            selectedModelForCreate = null,
            isModelDropdownExpanded = false
        )
    }

    fun selectModelForCreate(model: AiModelDomain) {
        _uiState.value = _uiState.value.copy(
            selectedModelForCreate = model,
            isModelDropdownExpanded = false
        )
    }

    fun toggleModelDropdown() {
        _uiState.value = _uiState.value.copy(
            isModelDropdownExpanded = !_uiState.value.isModelDropdownExpanded
        )
    }

    fun dismissModelDropdown() {
        _uiState.value = _uiState.value.copy(isModelDropdownExpanded = false)
    }

    fun toggleTaskDropdown() {
        _uiState.value = _uiState.value.copy(
            isTaskDropdownExpanded = !_uiState.value.isTaskDropdownExpanded
        )
    }

    fun dismissTaskDropdown() {
        _uiState.value = _uiState.value.copy(isTaskDropdownExpanded = false)
    }

    fun selectWorkingMemory(memory: WorkingMemoryDomain?) {
        _uiState.value = _uiState.value.copy(selectedWorkingMemory = memory)
    }

    fun toggleWorkingDialog() {
        _uiState.value = _uiState.value.copy(showWorkingDialog = !_uiState.value.showWorkingDialog)
    }

    fun addWorkingMemory(title: String) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            val memory = WorkingMemoryDomain(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                title = title
            )
            workingMemoryRepository.upsert(memory)
            _uiState.value = _uiState.value.copy(
                showWorkingDialog = false,
                selectedWorkingMemory = memory
            )
        }
    }

    fun createChat(onChatCreated: (chatId: String) -> Unit) {
        val model = _uiState.value.selectedModelForCreate ?: return
        val profileId = _uiState.value.activeProfileId ?: return
        val workingMemory = _uiState.value.selectedWorkingMemory ?: return
        viewModelScope.launch {
            val chat = createChatUseCase(model.provider, model.id, profileId, workingMemory.id)
            onChatCreated(chat.id)
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value
                val apiKey = when (state.selectedProvider) {
                    AiProvider.OPENROUTER -> settingsDataStore.openRouterApiKey.first() ?: ""
                    AiProvider.DEEPSEEK -> settingsDataStore.deepSeekApiKey.first() ?: ""
                }
                Log.d("CreateChatVM", "loadModels provider=${state.selectedProvider} apiKey=${apiKey.take(8)}...")

                val models = modelRepository.fetchModelsFromNetwork(state.selectedProvider, apiKey)
                Log.d("CreateChatVM", "loadModels before save: models.size=${models.size}")
                _uiState.value = _uiState.value.copy(
                    models = models,
                    isLoading = false,
                    selectedModelForCreate = null,
                    isModelDropdownExpanded = false
                )
                val afterSaveCount = _uiState.value.models.size
                val free = models.count { it.isFree }
                val paid = models.count { !it.isFree }
                Log.d("CreateChatVM", "loadModels done: provider=${state.selectedProvider.name} total=${models.size} free=$free paid=$paid afterSaveInState=$afterSaveCount")
            } catch (e: Exception) {
                Log.e("CreateChatVM", "loadModels error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}