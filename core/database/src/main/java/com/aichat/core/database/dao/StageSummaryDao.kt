package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aichat.core.database.entity.StageSummaryEntity

@Dao
interface StageSummaryDao {
    @Query("SELECT * FROM stage_summaries WHERE chatId = :chatId ORDER BY taskNumber ASC, stageOrder ASC")
    suspend fun getByChatId(chatId: String): List<StageSummaryEntity>

    @Insert
    suspend fun insert(summary: StageSummaryEntity)

    @Query("SELECT COALESCE(MAX(taskNumber), 0) FROM stage_summaries WHERE chatId = :chatId")
    suspend fun getMaxTaskNumber(chatId: String): Int
}