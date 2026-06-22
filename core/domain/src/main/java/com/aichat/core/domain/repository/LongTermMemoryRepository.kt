package com.aichat.core.domain.repository

import com.aichat.core.domain.model.LongTermMemoryDomain
import kotlinx.coroutines.flow.Flow

interface LongTermMemoryRepository {

    suspend fun upsert(memory: LongTermMemoryDomain)

    suspend fun delete(memoryId: String)

    fun getByProfileId(profileId: String): Flow<List<LongTermMemoryDomain>>

    suspend fun getById(memoryId: String): LongTermMemoryDomain?
}