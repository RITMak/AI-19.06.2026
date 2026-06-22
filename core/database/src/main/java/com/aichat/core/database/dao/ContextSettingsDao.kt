package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.core.database.entity.ContextSettingsEntity

@Dao
interface ContextSettingsDao {
    @Query("SELECT * FROM context_settings WHERE chat_id = :chatId")
    suspend fun getSettings(chatId: String): ContextSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ContextSettingsEntity)

    @Query("DELETE FROM context_settings WHERE chat_id = :chatId")
    suspend fun deleteSettings(chatId: String)
}