package com.aichat.core.data.repository

import com.aichat.core.database.dao.LongTermMemoryDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.LongTermMemoryDomain
import com.aichat.core.domain.repository.LongTermMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LongTermMemoryRepositoryImpl @Inject constructor(
    private val longTermMemoryDao: LongTermMemoryDao
) : LongTermMemoryRepository {

    override suspend fun upsert(memory: LongTermMemoryDomain) {
        longTermMemoryDao.upsert(memory.toEntity())
    }

    override suspend fun delete(memoryId: String) {
        longTermMemoryDao.delete(memoryId)
    }

    override fun getByProfileId(profileId: String): Flow<List<LongTermMemoryDomain>> {
        return longTermMemoryDao.getByProfileId(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(memoryId: String): LongTermMemoryDomain? {
        return longTermMemoryDao.getById(memoryId)?.toDomain()
    }
}