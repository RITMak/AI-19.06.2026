package com.aichat.core.data.repository

import com.aichat.core.database.dao.ChatStateDao
import com.aichat.core.data.mapper.ChatStateMapper
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.ChatStateDomain
import com.aichat.core.domain.repository.ChatStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatStateRepositoryImpl @Inject constructor(
    private val dao: ChatStateDao,
    private val mapper: ChatStateMapper
) : ChatStateRepository {

    override suspend fun get(chatId: String): ChatStateDomain? {
        val entity = dao.get(chatId) ?: return null
        return mapper.entityToDomain(entity)
    }

    override fun observe(chatId: String): Flow<ChatStateDomain?> {
        return dao.observe(chatId).map { entity ->
            entity?.let { mapper.entityToDomain(it) }
        }
    }

    override suspend fun setStage(chatId: String, stage: ChatStage) {
        val entity = dao.get(chatId) ?: run {
            upsert(ChatStateDomain.initial(chatId).copy(stage = stage))
            return
        }
        dao.upsert(entity.copy(stage = stage.name))
    }

    override suspend fun setStep(chatId: String, step: Int) {
        val entity = dao.get(chatId) ?: return
        dao.upsert(entity.copy(step = step))
    }

    override suspend fun setTotalSteps(chatId: String, totalSteps: Int) {
        val entity = dao.get(chatId) ?: return
        dao.upsert(entity.copy(totalSteps = totalSteps))
    }

    override suspend fun upsert(state: ChatStateDomain) {
        dao.upsert(mapper.domainToEntity(state))
    }

    override suspend fun delete(chatId: String) {
        dao.delete(chatId)
    }
}