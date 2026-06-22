package com.aichat.core.data.repository

import com.aichat.core.database.dao.StageSummaryDao
import com.aichat.core.database.entity.StageSummaryEntity
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.StageSummary
import com.aichat.core.domain.repository.StageSummaryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StageSummaryRepositoryImpl @Inject constructor(
    private val stageSummaryDao: StageSummaryDao
) : StageSummaryRepository {

    override suspend fun getByChatId(chatId: String): List<StageSummary> {
        return stageSummaryDao.getByChatId(chatId).map { it.toDomain() }
    }

    override suspend fun save(summary: StageSummary) {
        stageSummaryDao.insert(summary.toEntity())
    }

    override suspend fun getMaxTaskNumber(chatId: String): Int {
        return stageSummaryDao.getMaxTaskNumber(chatId)
    }

    private fun StageSummaryEntity.toDomain(): StageSummary {
        return StageSummary(
            chatId = chatId,
            taskNumber = taskNumber,
            stageOrder = stageOrder,
            stage = try { ChatStage.valueOf(stage) } catch (_: IllegalArgumentException) { ChatStage.PLANNING },
            summaryText = summaryText
        )
    }

    private fun StageSummary.toEntity(): StageSummaryEntity {
        return StageSummaryEntity(
            chatId = chatId,
            taskNumber = taskNumber,
            stageOrder = stageOrder,
            stage = stage.name,
            summaryText = summaryText
        )
    }
}