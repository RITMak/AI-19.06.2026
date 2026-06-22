package com.aichat.core.domain.repository

import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.model.AiProvider
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<ChatDomain>>
    fun getChatsByProfileId(profileId: String): Flow<List<ChatDomain>>
    suspend fun getChatById(id: String): ChatDomain?
    suspend fun createChat(provider: AiProvider, modelId: String, profileId: String, workingMemoryId: String? = null): ChatDomain
    suspend fun updateChatTitle(id: String, title: String)
    suspend fun deleteChat(id: String)
}
