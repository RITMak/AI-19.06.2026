package com.aichat.core.data.repository

import com.aichat.core.database.dao.InvariantDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.Invariant
import com.aichat.core.domain.repository.InvariantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvariantRepositoryImpl @Inject constructor(
    private val invariantDao: InvariantDao
) : InvariantRepository {

    override fun observeByProfile(profileId: String): Flow<List<Invariant>> {
        return invariantDao.getByProfileId(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getByProfileOnce(profileId: String): List<Invariant> {
        return invariantDao.getByProfileIdOnce(profileId).map { it.toDomain() }
    }

    override suspend fun add(profileId: String, text: String): Invariant {
        val now = System.currentTimeMillis()
        val entity = com.aichat.core.database.entity.InvariantEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            text = text,
            createdAt = now
        )
        invariantDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun remove(id: String) {
        invariantDao.delete(id)
    }
}