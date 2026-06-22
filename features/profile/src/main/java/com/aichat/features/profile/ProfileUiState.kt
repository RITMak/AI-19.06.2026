package com.aichat.features.profile

import com.aichat.core.domain.model.ProfileDomain

data class ProfileUiState(
    val profiles: List<ProfileDomain> = emptyList(),
    val activeProfileId: String? = null,
    val isInitialized: Boolean = false,
    val deletingProfileId: String? = null
)