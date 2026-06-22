package com.aichat.core.domain.repository

import com.aichat.core.domain.model.MessageDomain
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagesByChatId(chatId: String): Flow<List<MessageDomain>>
    suspend fun insertMessage(message: MessageDomain)
    suspend fun deleteMessagesByChatId(chatId: String)
}
