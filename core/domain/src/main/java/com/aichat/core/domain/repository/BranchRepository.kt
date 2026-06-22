package com.aichat.core.domain.repository

import com.aichat.core.domain.model.BranchData

interface BranchRepository {
    suspend fun getBranches(chatId: String): List<BranchData>
    suspend fun getBranch(branchId: String): BranchData?
    suspend fun createBranch(data: BranchData)
    suspend fun deleteBranch(branchId: String)
    suspend fun renameBranch(branchId: String, newName: String)
}