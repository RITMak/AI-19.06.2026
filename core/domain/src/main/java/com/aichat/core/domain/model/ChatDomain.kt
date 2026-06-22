package com.aichat.core.domain.model

import com.aichat.core.model.AiProvider

data class ChatDomain(
    val id: String,
    val title: String,
    val provider: AiProvider,
    val modelId: String,
    val profileId: String,
    val workingMemoryId: String? = null
)
