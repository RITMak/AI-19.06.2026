package com.aichat.core.domain.repository

import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.model.AiProvider
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    suspend fun getModels(provider: AiProvider): List<AiModelDomain>
    suspend fun fetchModelsFromNetwork(provider: AiProvider, apiKey: String): List<AiModelDomain>
    suspend fun clearModels()
}
