package com.aichat.features.chat

import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.MessageDomain

data class StageConfirmInfo(
    val currentStage: ChatStage,
    val targetStage: ChatStage,
    val confirmLabel: String,
    val rejectLabel: String
)

data class ChatUiState(
    val modelName: String = "",
    val messages: List<MessageDomain> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val streamingText: String? = null,
    val totalTokens: Int = 0,
    val totalCost: Double = 0.0,
    val contextLimit: Int = 0,
    val summaryEvery: Int = 0,
    val taskName: String? = null,
    val userInfo: List<Pair<String, String>> = emptyList(),
    // FSM state
    val currentStage: ChatStage = ChatStage.PLANNING,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val subStage: Int = 0,
    val subStagesTotal: Int = 0,
    val subStageLabel: String = "",
    val planConfirmed: Boolean = false,
    val planProposedCount: Int = 0,
    val showStageConfirm: StageConfirmInfo? = null,
    val hasStageCompleteMarker: Boolean = false,
    val llmTransitionRejectedMessage: String? = null,
    val firstUserQuery: String = "",
    val planningResult: String = "",
    val executionResult: String = ""
)
