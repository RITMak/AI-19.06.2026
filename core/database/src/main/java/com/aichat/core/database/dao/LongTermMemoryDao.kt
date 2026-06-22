package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.LongTermMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LongTermMemoryDao {

    @Upsert
    suspend fun upsert(memory: LongTermMemoryEntity)

    @Query("DELETE FROM long_term_memory WHERE id = :memoryId")
    suspend fun delete(memoryId: String)

    @Query("DELETE FROM long_term_memory WHERE profile_id = :profileId")
    suspend fun deleteAllForProfile(profileId: String)

    @Query("SELECT * FROM long_term_memory WHERE profile_id = :profileId ORDER BY key ASC")
    fun getByProfileId(profileId: String): Flow<List<LongTermMemoryEntity>>

    @Query("SELECT * FROM long_term_memory WHERE id = :memoryId LIMIT 1")
    suspend fun getById(memoryId: String): LongTermMemoryEntity?
}