package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.ChatStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatStateDao {

    @Upsert
    suspend fun upsert(state: ChatStateEntity)

    @Query("SELECT * FROM chat_state WHERE chat_id = :chatId LIMIT 1")
    suspend fun get(chatId: String): ChatStateEntity?

    @Query("SELECT * FROM chat_state WHERE chat_id = :chatId LIMIT 1")
    fun observe(chatId: String): Flow<ChatStateEntity?>

    @Query("DELETE FROM chat_state WHERE chat_id = :chatId")
    suspend fun delete(chatId: String)
}
