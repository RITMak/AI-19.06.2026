package com.aichat.features.chatcreate

import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.domain.model.WorkingMemoryDomain
import com.aichat.core.model.AiProvider

enum class ModelFilter {
    FREE, PAID
}

data class CreateChatUiState(
    val selectedProvider: AiProvider = AiProvider.OPENROUTER,
    val models: List<AiModelDomain> = emptyList(),
    val selectedModelForCreate: AiModelDomain? = null,
    val isLoading: Boolean = false,
    val filter: ModelFilter = ModelFilter.FREE,
    val isModelDropdownExpanded: Boolean = false,
    val isTaskDropdownExpanded: Boolean = false,
    val error: String? = null,
    val activeProfileId: String? = null,
    val workingMemories: List<WorkingMemoryDomain> = emptyList(),
    val selectedWorkingMemory: WorkingMemoryDomain? = null,
    val showWorkingDialog: Boolean = false
)