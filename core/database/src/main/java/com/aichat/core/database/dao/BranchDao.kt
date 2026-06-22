package com.aichat.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aichat.core.database.entity.BranchEntity

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE chatId = :chatId")
    suspend fun getBranches(chatId: String): List<BranchEntity>

    @Query("SELECT * FROM branches WHERE branchId = :branchId")
    suspend fun getBranch(branchId: String): BranchEntity?

    @Upsert
    suspend fun upsertBranch(branch: BranchEntity)

    @Query("DELETE FROM branches WHERE branchId = :branchId")
    suspend fun deleteBranch(branchId: String)
}