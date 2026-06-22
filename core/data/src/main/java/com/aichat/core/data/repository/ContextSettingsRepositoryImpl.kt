package com.aichat.core.data.repository

import com.aichat.core.database.dao.ContextSettingsDao
import com.aichat.core.database.entity.ContextSettingsEntity
import com.aichat.core.domain.model.ContextSettingsDomain
import com.aichat.core.domain.repository.ContextSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextSettingsRepositoryImpl @Inject constructor(
    private val contextSettingsDao: ContextSettingsDao
) : ContextSettingsRepository {

    override suspend fun getSettings(chatId: String): ContextSettingsDomain? {
        val entity = contextSettingsDao.getSettings(chatId) ?: return null
        return entity.toDomain()
    }

    override suspend fun upsertSettings(settings: ContextSettingsDomain) {
        contextSettingsDao.upsertSettings(settings.toEntity())
    }

    override suspend fun deleteSettings(chatId: String) {
        contextSettingsDao.deleteSettings(chatId)
    }

    private fun ContextSettingsEntity.toDomain() = ContextSettingsDomain(
        chatId = chatId,
        summaryEvery = summaryEvery
    )

    private fun ContextSettingsDomain.toEntity() = ContextSettingsEntity(
        chatId = chatId,
        summaryEvery = summaryEvery
    )
}