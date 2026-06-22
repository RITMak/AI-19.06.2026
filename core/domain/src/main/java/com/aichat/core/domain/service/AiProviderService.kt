package com.aichat.core.domain.service

import com.aichat.core.model.AiProvider
import com.aichat.core.domain.model.MessageDomain

interface AiProviderService {
    val providerType: AiProvider

    suspend fun chat(
        apiKey: String,
        modelId: String,
        messages: List<MessageDomain>
    ): ChatResult
}
