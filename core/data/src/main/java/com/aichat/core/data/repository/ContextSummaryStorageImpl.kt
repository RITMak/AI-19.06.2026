package com.aichat.core.data.repository

import com.aichat.core.database.dao.ContextSummaryDao
import com.aichat.core.database.entity.ContextSummaryEntity
import com.aichat.core.domain.service.ContextSummaryData
import com.aichat.core.domain.service.ContextSummaryStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextSummaryStorageImpl @Inject constructor(
    private val contextSummaryDao: ContextSummaryDao
) : ContextSummaryStorage {

    override suspend fun getSummary(chatId: String): ContextSummaryData? {
        val entity = contextSummaryDao.getSummary(chatId) ?: return null
        return ContextSummaryData(
            chatId = entity.chatId,
            summaryText = entity.summaryText,
            messageCount = entity.messageCount,
            lastSummarizedUserMsgCount = entity.lastSummarizedUserMsgCount
        )
    }

    override suspend fun upsertSummary(data: ContextSummaryData) {
        val entity = ContextSummaryEntity(
            chatId = data.chatId,
            summaryText = data.summaryText,
            messageCount = data.messageCount,
            lastSummarizedUserMsgCount = data.lastSummarizedUserMsgCount,
            updatedAt = System.currentTimeMillis()
        )
        contextSummaryDao.upsertSummary(entity)
    }
}