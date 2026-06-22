package com.aichat.core.domain.repository

import com.aichat.core.domain.model.StageSummary

interface StageSummaryRepository {
    suspend fun getByChatId(chatId: String): List<StageSummary>
    suspend fun save(summary: StageSummary)
    suspend fun getMaxTaskNumber(chatId: String): Int
}