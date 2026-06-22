package com.aichat.features.chats

import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.domain.model.ProfileDomain

data class ChatsUiState(
    val chats: List<ChatDomain> = emptyList(),
    val activeProfile: ProfileDomain? = null,
    val profiles: List<ProfileDomain> = emptyList(),
    val isLoading: Boolean = false,
    val isSwitchingProfile: Boolean = false,
    val isInitialized: Boolean = false
)
