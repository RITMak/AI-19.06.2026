package com.aichat.core.data.repository

import com.aichat.core.database.dao.LongTermMemoryDao
import com.aichat.core.database.dao.ProfileDao
import com.aichat.core.database.entity.LongTermMemoryEntity
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.ProfileDomain
import com.aichat.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val longTermMemoryDao: LongTermMemoryDao
) : ProfileRepository {

    override suspend fun createProfile(name: String): ProfileDomain {
        profileDao.setAllInactive()
        val profile = ProfileDomain(
            id = UUID.randomUUID().toString(),
            name = name,
            isActive = true
        )
        profileDao.upsert(profile.toEntity())
        longTermMemoryDao.upsert(
            LongTermMemoryEntity(
                id = UUID.randomUUID().toString(),
                profileId = profile.id,
                key = "profile_name",
                value = name
            )
        )
        return profile
    }

    override suspend fun deleteProfile(profileId: String) {
        val profile = profileDao.getProfileById(profileId)
        val wasActive = profile?.isActive == true
        profileDao.delete(profileId)
        if (wasActive) {
            val remaining = profileDao.getAllProfilesOnce()
            if (remaining.isNotEmpty()) {
                setActiveProfile(remaining.first().id)
            }
        }
    }

    override suspend fun setActiveProfile(profileId: String) {
        profileDao.setAllInactive()
        val profile = profileDao.getProfileById(profileId) ?: return
        profileDao.upsert(profile.copy(isActive = true))
    }

    override fun getActiveProfile(): Flow<ProfileDomain?> {
        return profileDao.getActiveProfile().map { it?.toDomain() }
    }

    override fun getAllProfiles(): Flow<List<ProfileDomain>> {
        return profileDao.getAllProfiles().map { entities -> entities.map { it.toDomain() } }
    }
}