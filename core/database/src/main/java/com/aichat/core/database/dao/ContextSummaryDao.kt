package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.core.database.entity.ContextSummaryEntity

@Dao
interface ContextSummaryDao {
    @Query("SELECT * FROM context_summaries WHERE chat_id = :chatId")
    suspend fun getSummary(chatId: String): ContextSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: ContextSummaryEntity)

    @Query("DELETE FROM context_summaries WHERE chat_id = :chatId")
    suspend fun deleteSummary(chatId: String)
}