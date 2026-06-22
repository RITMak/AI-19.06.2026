package com.aichat.core.domain.repository

import com.aichat.core.domain.model.WorkingMemoryDomain
import kotlinx.coroutines.flow.Flow

interface WorkingMemoryRepository {

    suspend fun upsert(memory: WorkingMemoryDomain)

    suspend fun delete(memoryId: String)

    fun getByProfileId(profileId: String): Flow<List<WorkingMemoryDomain>>

    suspend fun getById(memoryId: String): WorkingMemoryDomain?
}