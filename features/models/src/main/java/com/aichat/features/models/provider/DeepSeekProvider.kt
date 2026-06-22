package com.aichat.features.models.provider

import android.util.Log
import com.aichat.core.domain.model.AiError
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.domain.service.AiProviderService
import com.aichat.core.domain.service.ChatResult
import com.aichat.core.model.AiProvider
import com.aichat.core.model.MessageType
import com.aichat.core.model.Role
import com.aichat.core.domain.model.NetworkException
import com.aichat.core.network.ApiException
import com.aichat.core.network.AiApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
data class DeepSeekMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class DeepSeekChatRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = false
)

@Serializable
data class PromptTokensDetails(
    @SerialName("cached_tokens") val cachedTokens: Int = 0
)

@Serializable
data class CompletionTokensDetails(
    @SerialName("reasoning_tokens") val reasoningTokens: Int = 0
)

@Serializable
data class DeepSeekUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
    @SerialName("prompt_tokens_details") val promptTokensDetails: PromptTokensDetails? = null,
    @SerialName("completion_tokens_details") val completionTokensDetails: CompletionTokensDetails? = null
)

@Serializable
data class DeepSeekChoice(
    val index: Int = 0,
    val message: DeepSeekMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class DeepSeekErrorInfo(
    val message: String? = null,
    val type: String? = null
)

@Serializable
data class DeepSeekChatResponse(
    val id: String = "",
    val choices: List<DeepSeekChoice> = emptyList(),
    val usage: DeepSeekUsage? = null,
    val error: DeepSeekErrorInfo? = null
)

@Serializable
data class DeepSeekModelInfo(
    val id: String,
    val owned_by: String = ""
)

@Serializable
data class DeepSeekModelsResponse(
    val data: List<DeepSeekModelInfo> = emptyList()
)

private const val BASE_URL = "https://api.deepseek.com/v1"

// V4 pricing per 1M tokens
private data class V4Pricing(
    val inputMiss: Double,
    val cacheHit: Double,
    val output: Double
)

private val FLASH_PRICING = V4Pricing(
    inputMiss = 0.14 / 1_000_000,
    cacheHit = 0.0028 / 1_000_000,
    output = 0.28 / 1_000_000
)

private val PRO_PRICING = V4Pricing(
    inputMiss = 0.435 / 1_000_000,
    cacheHit = 0.003625 / 1_000_000,
    output = 0.87 / 1_000_000
)

@Singleton
class DeepSeekProvider @Inject constructor(
    private val apiClient: AiApiClient,
    private val json: Json,
    private val modelRepository: ModelRepository,
    @Named("deepseek_default_key") private val defaultApiKey: String
) : AiProviderService {

    override val providerType: AiProvider = AiProvider.DEEPSEEK

    override suspend fun chat(
        apiKey: String,
        modelId: String,
        messages: List<MessageDomain>
    ): ChatResult {
        val effectiveApiKey = apiKey.ifBlank { defaultApiKey }
        val requestMessages = messages
            .filter { it.messageType != MessageType.SUMMARY && it.messageType != MessageType.STAGE_CHANGE }
            .map { msg ->
            DeepSeekMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.text
            )
        }

        val request = DeepSeekChatRequest(
            model = modelId,
            messages = requestMessages,
            stream = false
        )

        val response: String
        try {
            response = apiClient.postChatRequest(
                url = "$BASE_URL/chat/completions",
                apiKey = effectiveApiKey,
                body = request
            )
        } catch (e: ApiException) {
            throwDeepSeekError(e.statusCode, e.responseBody)
            throw e // fallback
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            throw NetworkException(e)
        }

        val parsed = json.decodeFromString<DeepSeekChatResponse>(response)

        // Check API error in response body
        if (parsed.error != null) {
            val errMsg = parsed.error.message ?: "Unknown DeepSeek API error"
            val errType = parsed.error.type ?: ""
            Log.e("DeepSeekProvider", "API error: $errMsg (type=$errType)")
            throw resolveDeepSeekError(errType, errMsg)
        }

        val fullText = parsed.choices.firstOrNull()?.message?.content ?: ""
        val usage = parsed.usage ?: DeepSeekUsage()

        // Determine pricing based on modelId
        val pricing = when {
            modelId.lowercase().contains("flash") -> FLASH_PRICING
            modelId.lowercase().contains("pro") -> PRO_PRICING
            else -> FLASH_PRICING // fallback
        }

        val cached = usage.promptTokensDetails?.cachedTokens ?: 0
        val reasoning = usage.completionTokensDetails?.reasoningTokens ?: 0

        val regularInputTokens = usage.promptTokens - cached
        val regularOutputTokens = usage.completionTokens - reasoning

        val inputCost = regularInputTokens * pricing.inputMiss
        val cacheCost = cached * pricing.cacheHit
        val reasoningCost = reasoning * pricing.inputMiss
        val outputCost = regularOutputTokens * pricing.output

        val costUsd = inputCost + cacheCost + reasoningCost + outputCost
        val promptCost = inputCost + cacheCost + reasoningCost
        val completionCost = outputCost

        return ChatResult(
            fullText = fullText,
            totalTokens = usage.totalTokens,
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            costUsd = costUsd,
            promptCost = promptCost,
            completionCost = completionCost
        )
    }

    private fun throwDeepSeekError(statusCode: Int, body: String) {
        val message = "DeepSeek HTTP $statusCode"
        when (statusCode) {
            401, 403 -> throw com.aichat.core.domain.model.AuthException(message)
            429 -> throw com.aichat.core.domain.model.RateLimitException(30000L)
            else -> throw com.aichat.core.domain.model.ApiCallException(statusCode, message)
        }
    }

    private fun resolveDeepSeekError(type: String, message: String): Exception {
        return when {
            type.contains("auth", ignoreCase = true) || type.contains("invalid", ignoreCase = true) ->
                com.aichat.core.domain.model.AuthException(message)
            type.contains("rate", ignoreCase = true) || type.contains("limit", ignoreCase = true) ->
                com.aichat.core.domain.model.RateLimitException(30000L)
            else -> com.aichat.core.domain.model.ApiCallException(0, message)
        }
    }
}