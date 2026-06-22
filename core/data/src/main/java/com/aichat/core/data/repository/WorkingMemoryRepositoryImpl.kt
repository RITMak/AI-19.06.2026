package com.aichat.core.data.repository

import com.aichat.core.database.dao.WorkingMemoryDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.WorkingMemoryDomain
import com.aichat.core.domain.repository.WorkingMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkingMemoryRepositoryImpl @Inject constructor(
    private val workingMemoryDao: WorkingMemoryDao
) : WorkingMemoryRepository {

    override suspend fun upsert(memory: WorkingMemoryDomain) {
        workingMemoryDao.upsert(memory.toEntity())
    }

    override suspend fun delete(memoryId: String) {
        workingMemoryDao.delete(memoryId)
    }

    override fun getByProfileId(profileId: String): Flow<List<WorkingMemoryDomain>> {
        return workingMemoryDao.getByProfileId(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(memoryId: String): WorkingMemoryDomain? {
        return workingMemoryDao.getById(memoryId)?.toDomain()
    }
}