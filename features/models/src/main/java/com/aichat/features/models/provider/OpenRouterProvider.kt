package com.aichat.features.models.provider

import android.util.Log
import com.aichat.core.domain.model.ApiCallException
import com.aichat.core.domain.model.AuthException
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.domain.model.RateLimitException
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
data class OpenRouterMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val stream: Boolean = false
)

@Serializable
data class OpenRouterCostDetails(
    @SerialName("upstream_inference_prompt_cost") val promptCost: Double = 0.0,
    @SerialName("upstream_inference_completions_cost") val completionCost: Double = 0.0
)

@Serializable
data class OpenRouterUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
    val cost: Double = 0.0,
    @SerialName("cost_details") val costDetails: OpenRouterCostDetails? = null
)

@Serializable
data class OpenRouterChoice(
    val index: Int? = null,
    val message: OpenRouterMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenRouterErrorInfo(
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class OpenRouterChatResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice>? = null,
    val usage: OpenRouterUsage? = null,
    val error: OpenRouterErrorInfo? = null,
    val model: String? = null
)

private const val BASE_URL = "https://openrouter.ai/api/v1"

@Singleton
class OpenRouterProvider @Inject constructor(
    private val apiClient: AiApiClient,
    private val json: Json,
    @Named("openrouter_default_key") private val defaultApiKey: String
) : AiProviderService {

    override val providerType: AiProvider = AiProvider.OPENROUTER

    override suspend fun chat(
        apiKey: String,
        modelId: String,
        messages: List<MessageDomain>
    ): ChatResult {
        val effectiveApiKey = apiKey.ifBlank { defaultApiKey }
        val requestMessages = messages
            .filter { it.messageType != MessageType.SUMMARY && it.messageType != MessageType.STAGE_CHANGE }
            .map { msg ->
            OpenRouterMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                    Role.SYSTEM -> "system"
                },
                content = msg.text
            )
        }

        val request = OpenRouterChatRequest(
            model = modelId,
            messages = requestMessages,
            stream = false
        )

        val response: String
        try {
            response = apiClient.postChatRequest(
                url = "$BASE_URL/chat/completions",
                apiKey = effectiveApiKey,
                body = request,
                referer = "https://aichat.app"
            )
        } catch (e: ApiException) {
            throwOpenRouterError(e.statusCode, e.responseBody)
            throw e
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            throw NetworkException(e)
        }

        val parsed = json.decodeFromString<OpenRouterChatResponse>(response)

        // Check API error in response body
        if (parsed.error != null) {
            val errCode = parsed.error.code ?: 0
            val errMsg = parsed.error.message ?: "Unknown OpenRouter API error"
            Log.e("OpenRouterProvider", "API error: code=$errCode msg=$errMsg")
            throw resolveOpenRouterError(errCode, errMsg)
        }

        val fullText = parsed.choices?.firstOrNull()?.message?.content ?: ""
        val usage = parsed.usage ?: OpenRouterUsage()

        return ChatResult(
            fullText = fullText,
            totalTokens = usage.totalTokens,
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            costUsd = usage.cost,
            promptCost = usage.costDetails?.promptCost ?: 0.0,
            completionCost = usage.costDetails?.completionCost ?: 0.0
        )
    }

    private fun throwOpenRouterError(statusCode: Int, body: String) {
        when (statusCode) {
            401, 403 -> throw AuthException("OpenRouter HTTP $statusCode")
            429 -> throw RateLimitException(30000L)
            else -> throw ApiCallException(statusCode, "OpenRouter HTTP $statusCode")
        }
    }

    private fun resolveOpenRouterError(code: Int, message: String): Exception {
        return when (code) {
            401, 403 -> AuthException(message)
            429 -> RateLimitException(30000L)
            else -> ApiCallException(code, message)
        }
    }
}