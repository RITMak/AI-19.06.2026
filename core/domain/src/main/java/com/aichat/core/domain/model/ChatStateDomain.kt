package com.aichat.core.domain.model

data class ChatStateDomain(
    val chatId: String,
    val stage: ChatStage = ChatStage.PLANNING,
    val subStage: Int = 0,
    val subStagesTotal: Int = 0,
    val subStageLabel: String = "",
    val step: Int = 0,
    val totalSteps: Int = 0,
    val planConfirmed: Boolean = false,
    val planProposedCount: Int = 0,
    val hasStageCompleteMarker: Boolean = false,
    val firstUserQuery: String = "",
    val planningResult: String = "",
    val executionResult: String = ""
) {
    companion object {
        fun initial(chatId: String) = ChatStateDomain(
            chatId = chatId
        )
    }
}