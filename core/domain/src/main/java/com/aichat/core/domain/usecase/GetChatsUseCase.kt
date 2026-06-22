package com.aichat.core.domain.usecase

import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatDomain>> = chatRepository.getChats()
    operator fun invoke(profileId: String): Flow<List<ChatDomain>> = chatRepository.getChatsByProfileId(profileId)
}
