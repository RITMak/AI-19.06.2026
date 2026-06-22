package com.aichat.core.data.repository

import com.aichat.core.database.dao.MessageDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getMessagesByChatId(chatId: String): Flow<List<MessageDomain>> =
        messageDao.getMessagesByChatId(chatId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertMessage(message: MessageDomain) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun deleteMessagesByChatId(chatId: String) {
        messageDao.deleteMessagesByChatId(chatId)
    }
}
