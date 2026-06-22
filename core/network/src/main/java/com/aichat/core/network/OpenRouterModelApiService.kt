package com.aichat.core.network

import android.util.Log
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.model.AiProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OpenRouterPricing(
    val prompt: Double = 0.0,
    val completion: Double = 0.0,
    val image: Double = 0.0,
    val request: Double = 0.0
)

@Serializable
data class OpenRouterModelInfo(
    val id: String,
    val name: String? = null,
    val pricing: OpenRouterPricing? = null,
    @SerialName("context_length") val contextLength: Long? = null
)

@Serializable
data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelInfo> = emptyList()
)

private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

@Singleton
class OpenRouterModelApiService @Inject constructor(
    private val apiClient: AiApiClient,
    private val json: Json
) : ModelApiService {

    override suspend fun fetchModels(apiKey: String): List<AiModelDomain> {
        Log.d("OpenRouterModelApi", ">>> fetchModels start, apiKey=${apiKey.take(8)}...")
        val url = "$OPENROUTER_BASE_URL/models"
        try {
            val modelsText = apiClient.getModels(url, apiKey)
            Log.d("OpenRouterModelApi", "<<< fetchModels response=${modelsText.take(500)}")
            val modelsResponse = json.decodeFromString<OpenRouterModelsResponse>(modelsText)
            val result = modelsResponse.data.map { modelInfo ->
                AiModelDomain(
                    id = modelInfo.id,
                    name = modelInfo.name ?: modelInfo.id,
                    provider = AiProvider.OPENROUTER,
                    isFree = modelInfo.pricing?.prompt == 0.0 && modelInfo.pricing?.completion == 0.0,
                    inputPrice = modelInfo.pricing?.prompt ?: 0.0,
                    outputPrice = modelInfo.pricing?.completion ?: 0.0,
                    contextLength = modelInfo.contextLength?.toInt() ?: 0
                )
            }
            Log.d("OpenRouterModelApi", "<<< fetchModels done, count=${result.size}")
            return result
        } catch (e: Exception) {
            Log.e("OpenRouterModelApi", "fetchModels error: ${e.message}", e)
            throw e
        }
    }
}