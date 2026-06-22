package com.aichat.core.domain.repository

import com.aichat.core.domain.model.ContextSettingsDomain

interface ContextSettingsRepository {
    suspend fun getSettings(chatId: String): ContextSettingsDomain?
    suspend fun upsertSettings(settings: ContextSettingsDomain)
    suspend fun deleteSettings(chatId: String)
}