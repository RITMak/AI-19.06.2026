package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aichat.core.database.entity.ModelEntity

@Dao
interface ModelDao {
    @Query("SELECT * FROM models WHERE provider = :provider ORDER BY name ASC")
    suspend fun getModelsByProvider(provider: String): List<ModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("DELETE FROM models WHERE provider = :provider")
    suspend fun deleteModelsByProvider(provider: String)

    @Query("DELETE FROM models")
    suspend fun deleteAllModels()
}