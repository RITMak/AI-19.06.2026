package com.aichat.core.data.repository

import android.util.Log
import com.aichat.core.database.dao.ModelDao
import com.aichat.core.data.mapper.toDomain
import com.aichat.core.data.mapper.toEntity
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.model.AiProvider
import com.aichat.core.network.ModelApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    private val modelDao: ModelDao,
    private val modelApiServices: Map<String, @JvmSuppressWildcards ModelApiService>
) : ModelRepository {

    override suspend fun getModels(provider: AiProvider): List<AiModelDomain> {
        val cached = modelDao.getModelsByProvider(provider.name)
        if (cached.isNotEmpty()) return cached.map { it.toDomain() }
        return emptyList()
    }

    override suspend fun fetchModelsFromNetwork(provider: AiProvider, apiKey: String): List<AiModelDomain> {
        Log.d("ModelRepo", "fetchModelsFromNetwork provider=$provider apiKey=${apiKey.take(8)}...")
        val service = modelApiServices[provider.name]
            ?: throw IllegalArgumentException("No ModelApiService for provider: $provider")
        val models = service.fetchModels(apiKey)
        // cache to DB
        val entities = models.map { it.toEntity() }
        modelDao.deleteModelsByProvider(provider.name)
        modelDao.insertModels(entities)
        Log.d("ModelRepo", "fetchModelsFromNetwork done, count=${models.size}")
        return models
    }

    override suspend fun clearModels() {
        modelDao.deleteAllModels()
    }
}