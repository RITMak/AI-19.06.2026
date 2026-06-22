package com.aichat.core.domain.repository

import com.aichat.core.domain.model.Invariant
import kotlinx.coroutines.flow.Flow

interface InvariantRepository {

    fun observeByProfile(profileId: String): Flow<List<Invariant>>

    suspend fun getByProfileOnce(profileId: String): List<Invariant>

    suspend fun add(profileId: String, text: String): Invariant

    suspend fun remove(id: String)
}