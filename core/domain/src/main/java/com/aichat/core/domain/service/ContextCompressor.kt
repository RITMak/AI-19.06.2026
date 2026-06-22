package com.aichat.core.domain.service

import com.aichat.core.common.util.TextCleaner
import com.aichat.core.domain.model.ContextSettingsDomain
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.model.Role
import javax.inject.Inject
import javax.inject.Singleton

data class CompressedContext(
    val history: List<MessageDomain>,
    val usedSummary: Boolean,
    val summaryText: String?,
    val originalCount: Int,
    val compressedCount: Int
)

data class ContextSummaryData(
    val chatId: String,
    val summaryText: String,
    val messageCount: Int,
    val lastSummarizedUserMsgCount: Int
)

data class SummaryResult(
    val summaryText: String,
    val totalTokens: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val costUsd: Double,
    val promptCost: Double,
    val completionCost: Double
)

interface ContextSummaryStorage {
    suspend fun getSummary(chatId: String): ContextSummaryData?
    suspend fun upsertSummary(data: ContextSummaryData)
}

@Singleton
class ContextCompressor @Inject constructor(
    private val summaryStorage: ContextSummaryStorage
) {
    suspend fun compress(
        messages: List<MessageDomain>,
        settings: ContextSettingsDomain?,
        chatId: String,
        currentSummaryText: String? = null,
        memorySystemPrompt: String? = null
    ): CompressedContext {
        val originalCount = messages.size
        if (settings == null || settings.summaryEvery <= 0) {
            return noCompression(messages, originalCount, memorySystemPrompt)
        }
        return compressSummary(messages, settings, chatId, originalCount, currentSummaryText, memorySystemPrompt)
    }

    suspend fun getLastMessageCount(chatId: String): Int? {
        return summaryStorage.getSummary(chatId)?.messageCount
    }

    suspend fun getLastSummary(chatId: String): ContextSummaryData? {
        return summaryStorage.getSummary(chatId)
    }

    suspend fun saveSummary(data: ContextSummaryData) {
        summaryStorage.upsertSummary(data)
    }

    suspend fun generateSummary(
        providerService: AiProviderService,
        apiKey: String,
        modelId: String,
        newMessages: List<MessageDomain>,
        existingSummary: String?
    ): SummaryResult {
        val systemPrompt = buildString {
            append("You are a memory compression system. ")
            append("Task: Update the existing memory summary using new conversation data. ")
            append("Rules: merge new information with existing summary; ")
            append("do not repeat old information; ")
            append("keep structure: goals, preferences, facts, decisions, tasks; ")
            append("remove noise and duplicates; ")
            append("preserve important updates or contradictions; ")
            append("output updated full summary (not incremental notes only).")
            if (existingSummary != null) {
                append(" Existing memory: $existingSummary")
            }
        }
        val cleanSystemPrompt = TextCleaner.clean(systemPrompt)

        val messages = mutableListOf<MessageDomain>()
        messages.add(
            MessageDomain(
                id = "summary_system_prompt",
                chatId = "summary",
                role = Role.SYSTEM,
                text = cleanSystemPrompt,
                textClean = cleanSystemPrompt
            )
        )

        // Add conversation history as user message, cleaned
        val conversationText = newMessages.joinToString("; ") { msg ->
            val role = if (msg.role.name == "USER") "User" else "Assistant"
            val clean = TextCleaner.clean(msg.textClean.ifBlank { msg.text })
            "$role: $clean"
        }
        val cleanConversationText = TextCleaner.clean(conversationText)
        messages.add(
            MessageDomain(
                id = "summary_conversation",
                chatId = "summary",
                role = Role.USER,
                text = cleanConversationText,
                textClean = cleanConversationText
            )
        )

        val result = providerService.chat(
            apiKey = apiKey,
            modelId = modelId,
            messages = messages
        )

        return SummaryResult(
            summaryText = TextCleaner.clean(result.fullText),
            totalTokens = result.totalTokens,
            promptTokens = result.promptTokens,
            completionTokens = result.completionTokens,
            costUsd = result.costUsd,
            promptCost = result.promptCost,
            completionCost = result.completionCost
        )
    }

    private fun noCompression(messages: List<MessageDomain>, originalCount: Int, memorySystemPrompt: String? = null): CompressedContext {
        val history = if (memorySystemPrompt != null) {
            val sysMsg = createMemorySystemMessage(messages.firstOrNull()?.chatId ?: "", memorySystemPrompt)
            mutableListOf(sysMsg).apply { addAll(messages) }
        } else {
            messages
        }
        return CompressedContext(
            history = history,
            usedSummary = false,
            summaryText = null,
            originalCount = originalCount,
            compressedCount = history.size
        )
    }

    private suspend fun compressSummary(
        messages: List<MessageDomain>,
        settings: ContextSettingsDomain,
        chatId: String,
        originalCount: Int,
        currentSummaryText: String?,
        memorySystemPrompt: String? = null
    ): CompressedContext {
        val summaryToUse = currentSummaryText ?: summaryStorage.getSummary(chatId)?.summaryText

        return if (summaryToUse != null) {
            val keepLast = 20 // keep last 20 messages when summary exists
            val lastMessages = messages.takeLast(keepLast)
            val summaryBlock = buildSummaryBlock(summaryToUse)
            val result = mutableListOf<MessageDomain>()
            if (memorySystemPrompt != null) {
                result.add(createMemorySystemMessage(chatId, memorySystemPrompt))
            }
            result.add(createSummaryMessage(chatId, summaryBlock))
            result.addAll(lastMessages)
            CompressedContext(
                history = result,
                usedSummary = true,
                summaryText = summaryToUse,
                originalCount = originalCount,
                compressedCount = result.size
            )
        } else {
            val history = if (memorySystemPrompt != null) {
                val sysMsg = createMemorySystemMessage(messages.firstOrNull()?.chatId ?: "", memorySystemPrompt)
                mutableListOf(sysMsg).apply { addAll(messages) }
            } else {
                messages
            }
            CompressedContext(
                history = history,
                usedSummary = false,
                summaryText = null,
                originalCount = originalCount,
                compressedCount = history.size
            )
        }
    }

    private fun buildSummaryBlock(summary: String): String {
        val cleanSummary = TextCleaner.clean(summary)
        return "[Previous conversation summary: $cleanSummary]"
    }

    private fun createSummaryMessage(chatId: String, text: String): MessageDomain {
        return MessageDomain(
            id = "summary_$chatId",
            chatId = chatId,
            role = Role.SYSTEM,
            text = text,
            textClean = text
        )
    }

    private fun createMemorySystemMessage(chatId: String, prompt: String): MessageDomain {
        return MessageDomain(
            id = "memory_$chatId",
            chatId = chatId,
            role = Role.SYSTEM,
            text = prompt,
            textClean = prompt
        )
    }
}
