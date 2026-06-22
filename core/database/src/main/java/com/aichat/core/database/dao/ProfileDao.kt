package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun delete(profileId: String)

    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfilesOnce(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE is_active = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): ProfileEntity?

    @Query("UPDATE profiles SET is_active = 0")
    suspend fun setAllInactive()
}