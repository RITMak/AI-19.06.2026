package com.aichat.core.domain.model

data class StageSummary(
    val chatId: String,
    val taskNumber: Int,
    val stageOrder: Int,
    val stage: ChatStage,
    val summaryText: String
)