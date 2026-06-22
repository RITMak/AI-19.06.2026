package com.aichat.core.domain.usecase

import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.model.AiProvider
import javax.inject.Inject

class GetModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(provider: AiProvider): List<AiModelDomain> {
        return modelRepository.getModels(provider)
    }

    suspend fun getModelById(provider: AiProvider, modelId: String): AiModelDomain? {
        return modelRepository.getModels(provider).find { it.id == modelId }
    }
}