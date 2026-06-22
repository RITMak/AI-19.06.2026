package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.InvariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvariantDao {

    @Upsert
    suspend fun upsert(invariant: InvariantEntity)

    @Query("DELETE FROM invariants WHERE id = :invariantId")
    suspend fun delete(invariantId: String)

    @Query("DELETE FROM invariants WHERE profile_id = :profileId")
    suspend fun deleteAllForProfile(profileId: String)

    @Query("SELECT * FROM invariants WHERE profile_id = :profileId ORDER BY created_at ASC")
    fun getByProfileId(profileId: String): Flow<List<InvariantEntity>>

    @Query("SELECT * FROM invariants WHERE profile_id = :profileId ORDER BY created_at ASC")
    suspend fun getByProfileIdOnce(profileId: String): List<InvariantEntity>
}