package com.aichat.core.data.repository

import com.aichat.core.database.dao.BranchDao
import com.aichat.core.database.entity.BranchEntity
import com.aichat.core.domain.model.BranchData
import com.aichat.core.domain.repository.BranchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchRepositoryImpl @Inject constructor(
    private val branchDao: BranchDao
) : BranchRepository {

    override suspend fun getBranches(chatId: String): List<BranchData> {
        return branchDao.getBranches(chatId).map { it.toDomain() }
    }

    override suspend fun getBranch(branchId: String): BranchData? {
        return branchDao.getBranch(branchId)?.toDomain()
    }

    override suspend fun createBranch(data: BranchData) {
        branchDao.upsertBranch(data.toEntity())
    }

    override suspend fun deleteBranch(branchId: String) {
        branchDao.deleteBranch(branchId)
    }

    override suspend fun renameBranch(branchId: String, newName: String) {
        val entity = branchDao.getBranch(branchId) ?: return
        branchDao.upsertBranch(entity.copy(name = newName))
    }

    private fun BranchEntity.toDomain() = BranchData(
        branchId = branchId,
        chatId = chatId,
        parentBranchId = parentBranchId,
        name = name,
        forkMessageId = forkMessageId
    )

    private fun BranchData.toEntity() = BranchEntity(
        branchId = branchId,
        chatId = chatId,
        parentBranchId = parentBranchId,
        name = name,
        forkMessageId = forkMessageId
    )
}