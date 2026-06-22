package com.aichat.core.data.repository

import com.aichat.core.database.dao.ChatDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {

    override fun getChats(): Flow<List<ChatDomain>> {
        return chatDao.getAllChats().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getChatsByProfileId(profileId: String): Flow<List<ChatDomain>> {
        return chatDao.getChatsByProfileId(profileId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getChatById(id: String): ChatDomain? {
        return chatDao.getChatById(id)?.toDomain()
    }

    override suspend fun createChat(provider: AiProvider, modelId: String, profileId: String, workingMemoryId: String?): ChatDomain {
        val chat = ChatDomain(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            provider = provider,
            modelId = modelId,
            profileId = profileId,
            workingMemoryId = workingMemoryId
        )
        chatDao.insertChat(chat.toEntity())
        return chat
    }

    override suspend fun updateChatTitle(id: String, title: String) {
        chatDao.updateChatTitle(id, title)
    }

    override suspend fun deleteChat(id: String) {
        chatDao.deleteChat(id)
    }
}