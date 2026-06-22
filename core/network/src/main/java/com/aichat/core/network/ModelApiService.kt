package com.aichat.core.network

import com.aichat.core.domain.model.AiModelDomain

interface ModelApiService {
    suspend fun fetchModels(apiKey: String): List<AiModelDomain>
}