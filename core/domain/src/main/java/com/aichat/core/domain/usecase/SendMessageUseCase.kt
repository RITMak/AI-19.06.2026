package com.aichat.core.domain.usecase

import com.aichat.core.common.util.TextCleaner
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.ChatStateDomain
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.domain.model.StageSummary
import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.domain.repository.ChatStateRepository
import com.aichat.core.domain.repository.ContextSettingsRepository
import com.aichat.core.domain.repository.InvariantRepository
import com.aichat.core.domain.repository.MessageRepository
import com.aichat.core.domain.service.AiProviderService
import com.aichat.core.domain.service.CompressedContext
import com.aichat.core.domain.service.ContextCompressor
import com.aichat.core.domain.service.ContextSummaryData
import com.aichat.core.domain.service.SummaryResult
import com.aichat.core.model.AiProvider
import com.aichat.core.model.MessageType
import com.aichat.core.model.Role
import java.util.UUID
import javax.inject.Inject

data class SendMessageResult(
    val userMessage: MessageDomain,
    val assistantMessage: MessageDomain,
    val compressedContext: CompressedContext? = null,
    val summaryResult: SummaryResult? = null,
    val summaryMessage: MessageDomain? = null,
    // FSM markers parsed from assistant response
    val fsmStageComplete: Boolean = false,
    val fsmTargetStage: ChatStage? = null,
    // Stage transition validation
    val fsmLlmTransitionRejected: String? = null
)

data class MemoryContext(
    val taskName: String? = null,
    val userInfo: List<Pair<String, String>> = emptyList()
)

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val contextSettingsRepository: ContextSettingsRepository,
    private val contextCompressor: ContextCompressor,
    private val chatStateRepository: ChatStateRepository,
    private val chatRepository: ChatRepository,
    private val invariantRepository: InvariantRepository,
    private val aiProviders: Map<String, @JvmSuppressWildcards AiProviderService>
) {
    suspend operator fun invoke(
        chatId: String,
        text: String,
        provider: AiProvider,
        modelId: String,
        messagesHistory: List<MessageDomain>,
        customApiKey: String,
        memoryContext: MemoryContext? = null,
        // FSM state from outside
        chatState: ChatStateDomain? = null,
        // Stage summaries from previous completed stages
        stageSummaries: List<StageSummary> = emptyList()
    ): SendMessageResult {
        val cleanText = TextCleaner.clean(text)

        val userMessage = MessageDomain(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.USER,
            text = text,
            textClean = cleanText
        )
        messageRepository.insertMessage(userMessage)

        val providerService = aiProviders[provider.name]
            ?: throw IllegalArgumentException("No provider service for ${provider.name}")

        // Use textClean from history, filter out messages that become empty after cleaning
        val cleanHistory = messagesHistory
            .filter { TextCleaner.isEmptyAfterClean(it.text).not() }
            .map { it.copy(text = it.textClean.ifBlank { TextCleaner.clean(it.text) }) }

        val cleanUserMessage = userMessage.copy(text = cleanText)

        // --- Summary logic: generate summary BEFORE main request ---
        val settings = contextSettingsRepository.getSettings(chatId)
        var summaryResult: SummaryResult? = null
        var currentSummaryText: String? = null

        if (settings != null && settings.summaryEvery > 0) {
            val userMsgCount = (messagesHistory.size + 1) / 2

            if (userMsgCount > 0 && userMsgCount % settings.summaryEvery == 0) {
                val lastSummaryData = contextCompressor.getLastSummary(chatId)

                if (lastSummaryData == null || userMsgCount != lastSummaryData.lastSummarizedUserMsgCount) {
                    val messagesToSummarize: List<MessageDomain>
                    val existingSummaryForPrompt: String?

                    if (lastSummaryData != null) {
                        val lastNPairs = settings.summaryEvery * 2
                        messagesToSummarize = cleanHistory
                            .takeLast(lastNPairs.coerceAtMost(cleanHistory.size))
                        existingSummaryForPrompt = lastSummaryData.summaryText
                    } else {
                        val firstNPairs = settings.summaryEvery * 2
                        messagesToSummarize = cleanHistory
                            .take(firstNPairs.coerceAtMost(cleanHistory.size))
                        existingSummaryForPrompt = null
                    }

                    if (messagesToSummarize.isNotEmpty()) {
                        val result = contextCompressor.generateSummary(
                            providerService = providerService,
                            apiKey = customApiKey,
                            modelId = modelId,
                            newMessages = messagesToSummarize,
                            existingSummary = existingSummaryForPrompt
                        )

                        summaryResult = result
                        currentSummaryText = result.summaryText

                        val summaryMessage = MessageDomain(
                            id = UUID.randomUUID().toString(),
                            chatId = chatId,
                            role = Role.SYSTEM,
                            messageType = MessageType.SUMMARY,
                            text = "📝 summary | ↑${result.totalTokens} | \$${"%.6f".format(result.costUsd)}",
                            textClean = "",
                            totalTokens = result.totalTokens,
                            costUsd = result.costUsd
                        )
                        messageRepository.insertMessage(summaryMessage)

                        val data = ContextSummaryData(
                            chatId = chatId,
                            summaryText = result.summaryText,
                            messageCount = messagesToSummarize.size,
                            lastSummarizedUserMsgCount = userMsgCount
                        )
                        contextCompressor.saveSummary(data)
                    }
                } else {
                    currentSummaryText = lastSummaryData.summaryText
                }
            } else {
                currentSummaryText = contextCompressor.getLastSummary(chatId)?.summaryText
            }
        }
        // --- End summary logic ---

        // --- Build stage system prompt ---
        val stageSystemPrompt = chatState?.let { buildStagePrompt(it, stageSummaries) }

        // Build memory context prompt with @scope grouping
        val memorySystemPrompt = if (memoryContext != null) {
            val parts = mutableListOf<String>()
            if (memoryContext.taskName != null) {
                parts.add("[Task: ${memoryContext.taskName}]")
            }
            if (memoryContext.userInfo.isNotEmpty()) {
                // Group by @scope prefix
                val grouped = mutableMapOf<String, MutableList<Pair<String, String>>>()
                for ((k, v) in memoryContext.userInfo) {
                    val trimmedKey = k.trim()
                    if (trimmedKey.startsWith("@")) {
                        val spaceIdx = trimmedKey.indexOf(' ')
                        val scope = if (spaceIdx > 0) trimmedKey.substring(1, spaceIdx).trim() else trimmedKey.substring(1).trim()
                        val attr = if (spaceIdx > 0) trimmedKey.substring(spaceIdx + 1).trim() else ""
                        grouped.getOrPut(scope) { mutableListOf() }.add(attr to v)
                    } else {
                        grouped.getOrPut("Я") { mutableListOf() }.add(trimmedKey to v)
                    }
                }
                val lines = mutableListOf<String>()
                val myScope = grouped.remove("Я")
                if (myScope != null) {
                    val attrs = myScope.joinToString(", ") { (k, v) -> "${k}: ${v}" }
                    lines.add("Я: ${attrs}")
                }
                for (scope in grouped.keys.sorted()) {
                    val attrs = grouped[scope]!!.joinToString(", ") { (k, v) -> "${k}: ${v}" }
                    lines.add("${scope}: ${attrs}")
                }
                parts.add("[User Info]\n${lines.joinToString("\n")}")
            }
            if (parts.isNotEmpty()) parts.joinToString("\n") else null
        } else null

        // Compress context with memory prompt only (NOT stage prompt)
        val compressed = contextCompressor.compress(
            messages = cleanHistory + cleanUserMessage,
            settings = settings,
            chatId = chatId,
            currentSummaryText = currentSummaryText,
            memorySystemPrompt = memorySystemPrompt?.let { TextCleaner.clean(it) }
        )

        // --- Build final history: invariant -> stage -> compressed ---
        val finalHistory = mutableListOf<MessageDomain>()

        // 1. Invariant rules as system message
        run {
            val chat = chatRepository.getChatById(chatId)
            if (chat != null) {
                val invariants = invariantRepository.getByProfileOnce(chat.profileId)
                if (invariants.isNotEmpty()) {
                    val rules = invariants.withIndex().joinToString("\n") { (i, it) -> "${i + 1}. ${it.text}" }
                    val invariantPrompt = TextCleaner.clean(
                        "You are an AI assistant. You MUST follow these rules (invariants):\n$rules\n\n" +
                        "These invariants are NOT optional. If the user asks you to do something that contradicts any of these rules, politely refuse and explain which invariant is violated."
                    )
                    finalHistory.add(MessageDomain(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        role = Role.SYSTEM,
                        text = invariantPrompt,
                        textClean = invariantPrompt
                    ))
                }
            }
        }

        // 2. Stage system prompt as separate system message
        if (stageSystemPrompt != null) {
            val stageClean = TextCleaner.clean(stageSystemPrompt)
            finalHistory.add(MessageDomain(
                id = "stage_$chatId",
                chatId = chatId,
                role = Role.SYSTEM,
                text = stageClean,
                textClean = stageClean
            ))
        }

        // 3. Compressed history (includes memory prompt + summary + conversation)
        finalHistory.addAll(compressed.history)

        val result = providerService.chat(
            apiKey = customApiKey,
            modelId = modelId,
            messages = finalHistory
        )

        val fullText = result.fullText

        // --- Parse FSM markers from response ---
        val fsmStageComplete = fullText.contains("[STAGE_COMPLETE]")
        val fsmTargetStage = parseStageMarker(fullText)

        // --- Validate Stage transition (LLM) ---
        var fsmLlmTransitionRejected: String? = null
        if (fsmTargetStage != null && chatState != null) {
            val from = chatState.stage
            if (!from.canLlmTransitionTo(fsmTargetStage)) {
                fsmLlmTransitionRejected = buildStageRejectedMessage(from, fsmTargetStage)
            }
        }

        // If stage transition rejected, ignore stage_complete too
        val finalStageComplete = if (fsmLlmTransitionRejected != null) false else fsmStageComplete
        val finalTargetStage = if (fsmLlmTransitionRejected != null) null else fsmTargetStage

        // Clean markers from display text before saving
        val cleanFullText = cleanMarkers(fullText)

        val assistantMessage = MessageDomain(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            role = Role.ASSISTANT,
            text = cleanFullText,
            textClean = TextCleaner.clean(cleanFullText), // cleaned version for LLM history (no markers, no noise)
            totalTokens = result.totalTokens,
            promptTokens = result.promptTokens,
            completionTokens = result.completionTokens,
            costUsd = result.costUsd,
            promptCost = result.promptCost,
            completionCost = result.completionCost
        )

        return SendMessageResult(
            userMessage = userMessage,
            assistantMessage = assistantMessage,
            compressedContext = if (compressed.usedSummary) compressed else null,
            summaryResult = summaryResult,
            summaryMessage = null,
            fsmStageComplete = finalStageComplete,
            fsmTargetStage = finalTargetStage,
            fsmLlmTransitionRejected = fsmLlmTransitionRejected
        )
    }

    // --- Marker parsing ---

    private fun parseStepMarker(text: String): Int? {
        val regex = """\[STEP:\s*(\d+)]""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseTotalStepsMarker(text: String): Int? {
        val regex = """\[STEPS:\s*(\d+)]""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseStageMarker(text: String): ChatStage? {
        val regex = """\[STAGE:\s*(\w+)]""".toRegex()
        val value = regex.find(text)?.groupValues?.get(1) ?: return null
        return try {
            ChatStage.valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseSubStageMarker(text: String): Int? {
        val regex = """\[SUBSTAGE:\s*(\d+)]""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseSubStagesTotalMarker(text: String): Int? {
        val regex = """\[SUBSTAGES:\s*(\d+)]""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseSubStageLabelMarker(text: String): String? {
        val regex = """\[SUBSTAGE_LABEL:\s*(.*?)]""".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }

    private fun parseAllSubStageLabels(text: String): List<String> {
        val regex = """\[SUBSTAGE_LABEL:\s*(.*?)]""".toRegex()
        return regex.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun cleanMarkers(text: String): String {
        val stepRegex = """\[STEP:\s*\d+]""".toRegex()
        val stepsRegex = """\[STEPS:\s*\d+]""".toRegex()
        val stageCompleteRegex = """\[STAGE_COMPLETE]""".toRegex()
        val stageRegex = """\[STAGE:\s*\w+]""".toRegex()
        val subStageRegex = """\[SUBSTAGE:\s*\d+]""".toRegex()
        val subStagesRegex = """\[SUBSTAGES:\s*\d+]""".toRegex()
        val subStageLabelRegex = """\[SUBSTAGE_LABEL:\s*.*?]""".toRegex()
        return text
            .replace(stepRegex, "")
            .replace(stepsRegex, "")
            .replace(stageCompleteRegex, "")
            .replace(stageRegex, "")
            .replace(subStageRegex, "")
            .replace(subStagesRegex, "")
            .replace(subStageLabelRegex, "")
            .trim()
    }

    // --- Prompt building ---

    private fun buildStagePrompt(chatState: ChatStateDomain, stageSummaries: List<StageSummary> = emptyList()): String? {
        val userQuery = chatState.firstUserQuery
        val planningResult = chatState.planningResult
        val executionResult = chatState.executionResult

        val basePrompt = if (chatState.planConfirmed) {
            // === VARIANT 3: PLAN CONFIRMED — EXECUTE AND COMPLETE STAGE ===
            when (chatState.stage) {
                ChatStage.PLANNING -> TextCleaner.clean(
                    "You are in the PLANNING stage. Plan has been confirmed.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "Execute the plan and produce the result.\n" +
                    "When done, output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "  [STAGE: EXECUTION]\n" +
                    "Allowed LLM transitions: PLANNING -> EXECUTION.\n" +
                    "Prohibited LLM transitions: PLANNING -> DONE."
                )
                ChatStage.EXECUTION -> TextCleaner.clean(
                    "You are in the EXECUTION stage. Plan has been confirmed.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "On the PLANNING stage we received the following result:\n" +
                    "*** ${planningResult} ***\n" +
                    "Execute the plan and produce the result.\n" +
                    "When done, output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "  [STAGE: DONE]\n" +
                    "Allowed LLM transitions: EXECUTION -> DONE.\n" +
                    "Prohibited LLM transitions: EXECUTION -> PLANNING."
                )
                ChatStage.DONE -> TextCleaner.clean(
                    "You are in the DONE stage.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "On the EXECUTION stage we received the following result:\n" +
                    "*** ${executionResult} ***\n" +
                    "Task is completed. Summarize the result.\n" +
                    "Output markers:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "If user writes a new message, they may be starting a new task — that is their choice.\n" +
                    "To reopen existing task: output [STAGE: EXECUTION] [STAGE_COMPLETE]."
                )
            }
        } else if (chatState.planProposedCount == 0) {
            // === VARIANT 1: FIRST ENTRY — LLM MUST CREATE THE PLAN ===
            when (chatState.stage) {
                ChatStage.PLANNING -> TextCleaner.clean(
                    "You are in the PLANNING stage.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "You must provide a step-by-step plan for the user's request.\n" +
                    "The plan should look like: 1) ... 2) ... 3) ... etc.\n" +
                    "Write a readable plan as text — user will see this message directly.\n" +
                    "After the plan, output:\n" +
                    "  [STAGE_COMPLETE] — so user can confirm the plan.\n" +
                    "Do NOT ask questions. Just show the plan.\n" +
                    "Allowed LLM transitions: PLANNING -> EXECUTION.\n" +
                    "Prohibited LLM transitions: PLANNING -> DONE."
                )
                ChatStage.EXECUTION -> TextCleaner.clean(
                    "You are in the EXECUTION stage.\n" +
                    "On the PLANNING stage we received the following result:\n" +
                    "*** ${planningResult} ***\n" +
                    "You must provide an execution plan for the user's request.\n" +
                    "The plan should look like: 1) ... 2) ... 3) ... etc.\n" +
                    "Write a readable execution plan as text — user will see this message directly.\n" +
                    "After the plan, output:\n" +
                    "  [STAGE_COMPLETE] — so user can confirm the plan.\n" +
                    "Do NOT execute yet. Just show the plan.\n" +
                    "Allowed LLM transitions: EXECUTION -> DONE.\n" +
                    "Prohibited LLM transitions: EXECUTION -> PLANNING."
                )
                ChatStage.DONE -> TextCleaner.clean(
                    "You are in the DONE stage.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "On the EXECUTION stage we received the following result:\n" +
                    "*** ${executionResult} ***\n" +
                    "Task is completed. Summarize the result.\n" +
                    "Output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "If user writes a new message, they may be starting a new task — that is their choice.\n" +
                    "To reopen existing task: output [STAGE: EXECUTION] [STAGE_COMPLETE]."
                )
            }
        } else {
            // === VARIANT 2: USER WANTS TO CLARIFY THE PLAN ===
            when (chatState.stage) {
                ChatStage.PLANNING -> TextCleaner.clean(
                    "You are in the PLANNING stage.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "User wants to improve the previously proposed plan.\n" +
                    "Update the previous plan based on user feedback.\n" +
                    "Write an updated readable plan.\n" +
                    "After the plan, output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "Do NOT ask questions. Just improve the plan.\n" +
                    "Allowed LLM transitions: PLANNING -> EXECUTION.\n" +
                    "Prohibited LLM transitions: PLANNING -> DONE."
                )
                ChatStage.EXECUTION -> TextCleaner.clean(
                    "You are in the EXECUTION stage.\n" +
                    "On the PLANNING stage we received the following result:\n" +
                    "*** ${planningResult} ***\n" +
                    "User wants to improve the previously proposed execution plan.\n" +
                    "Update the previous execution plan based on user feedback.\n" +
                    "Write an updated readable execution plan.\n" +
                    "After the plan, output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "Do NOT execute yet. Just improve the plan.\n" +
                    "Allowed LLM transitions: EXECUTION -> DONE.\n" +
                    "Prohibited LLM transitions: EXECUTION -> PLANNING."
                )
                ChatStage.DONE -> TextCleaner.clean(
                    "You are in the DONE stage.\n" +
                    "User query: *** ${userQuery} ***\n" +
                    "On the EXECUTION stage we received the following result:\n" +
                    "*** ${executionResult} ***\n" +
                    "Task is completed. Summarize the result.\n" +
                    "Output:\n" +
                    "  [STAGE_COMPLETE]\n" +
                    "If user writes a new message, they may be starting a new task — that is their choice.\n" +
                    "To reopen existing task: output [STAGE: EXECUTION] [STAGE_COMPLETE]."
                )
            }
        }

        return if (stageSummaries.isNotEmpty()) {
            val block = stageSummaries.joinToString("; ") { summary ->
                "[Completed task ${summary.taskNumber}, Stage ${summary.stage.label}: ${summary.summaryText}]"
            }
            TextCleaner.clean("$basePrompt [Previous completed stages: $block]")
        } else {
            basePrompt
        }
    }

    // --- Rejected transition messages ---

    private fun buildStageRejectedMessage(from: ChatStage, to: ChatStage): String {
        val allowed = from.allowedLlmTransitions.joinToString(", ") { it.label }
        return TextCleaner.clean(
            "Недопустимый переход: ${from.label} → ${to.label}. " +
            "Разрешённые LLM переходы: [$allowed]"
        )
    }

    private fun buildSubStageRejectedMessage(current: Int, attempted: Int): String {
        val expected = current + 1
        return TextCleaner.clean(
            "Недопустимый переход subStage: ${current} → ${attempted}. " +
            "Разрешён: $expected"
        )
    }
}