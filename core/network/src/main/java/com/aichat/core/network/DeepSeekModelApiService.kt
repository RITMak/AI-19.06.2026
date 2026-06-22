package com.aichat.core.network

import android.util.Log
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.model.AiProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
data class DeepSeekPricing(
    val prompt: Double = 0.0,
    val completion: Double = 0.0
)

@Serializable
data class DeepSeekModelInfo(
    val id: String,
    val name: String? = null,
    val pricing: DeepSeekPricing? = null,
    @SerialName("context_length") val contextLength: Long? = null
)

@Serializable
data class DeepSeekModelsResponse(
    val data: List<DeepSeekModelInfo> = emptyList()
)

private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com"

@Singleton
class DeepSeekModelApiService @Inject constructor(
    private val apiClient: AiApiClient,
    private val json: Json,
    @Named("deepseek_default_key") private val defaultApiKey: String
) : ModelApiService {

    override suspend fun fetchModels(apiKey: String): List<AiModelDomain> {
        val effectiveApiKey = apiKey.ifBlank { defaultApiKey }
        Log.d("DeepSeekModelApi", ">>> fetchModels start, apiKey=${effectiveApiKey.take(8)}...")
        val url = "$DEEPSEEK_BASE_URL/models"
        try {
            val modelsText = apiClient.getModels(url, effectiveApiKey)
            Log.d("DeepSeekModelApi", "<<< fetchModels response=${modelsText.take(500)}")
            val modelsResponse = json.decodeFromString<DeepSeekModelsResponse>(modelsText)
            val result = modelsResponse.data.map { modelInfo ->
                AiModelDomain(
                    id = modelInfo.id,
                    name = modelInfo.name ?: modelInfo.id,
                    provider = AiProvider.DEEPSEEK,
                    isFree = modelInfo.pricing?.prompt == 0.0 && modelInfo.pricing?.completion == 0.0,
                    inputPrice = modelInfo.pricing?.prompt ?: 0.0,
                    outputPrice = modelInfo.pricing?.completion ?: 0.0,
                    contextLength = modelInfo.contextLength?.toInt() ?: 0
                )
            }
            Log.d("DeepSeekModelApi", "<<< fetchModels done, count=${result.size}")
            return result
        } catch (e: Exception) {
            Log.e("DeepSeekModelApi", "fetchModels error: ${e.message}", e)
            throw e
        }
    }
}