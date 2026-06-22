package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.WorkingMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkingMemoryDao {

    @Upsert
    suspend fun upsert(memory: WorkingMemoryEntity)

    @Query("DELETE FROM working_memory WHERE id = :memoryId")
    suspend fun delete(memoryId: String)

    @Query("DELETE FROM working_memory WHERE profile_id = :profileId")
    suspend fun deleteAllForProfile(profileId: String)

    @Query("SELECT * FROM working_memory WHERE profile_id = :profileId ORDER BY title ASC")
    fun getByProfileId(profileId: String): Flow<List<WorkingMemoryEntity>>

    @Query("SELECT * FROM working_memory WHERE id = :memoryId LIMIT 1")
    suspend fun getById(memoryId: String): WorkingMemoryEntity?
}