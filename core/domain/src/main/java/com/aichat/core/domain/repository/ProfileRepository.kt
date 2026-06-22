package com.aichat.core.domain.repository

import com.aichat.core.domain.model.ProfileDomain
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun createProfile(name: String): ProfileDomain
    suspend fun deleteProfile(profileId: String)
    suspend fun setActiveProfile(profileId: String)
    fun getActiveProfile(): Flow<ProfileDomain?>
    fun getAllProfiles(): Flow<List<ProfileDomain>>
}