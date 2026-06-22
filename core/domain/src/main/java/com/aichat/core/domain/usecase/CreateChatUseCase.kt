package com.aichat.core.domain.usecase

import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.model.AiProvider
import javax.inject.Inject

class CreateChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(provider: AiProvider, modelId: String, profileId: String, workingMemoryId: String? = null): ChatDomain {
        return chatRepository.createChat(provider, modelId, profileId, workingMemoryId)
    }
}
