package com.aichat.core.data.mapper

import com.aichat.core.database.entity.ChatStateEntity
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.ChatStateDomain
import javax.inject.Inject

class ChatStateMapper @Inject constructor() {

    fun entityToDomain(entity: ChatStateEntity): ChatStateDomain = ChatStateDomain(
        chatId = entity.chatId,
        stage = entity.stage.toChatStage(),
        step = entity.step,
        totalSteps = entity.totalSteps,
        subStage = entity.subStage,
        subStagesTotal = entity.subStagesTotal,
        subStageLabel = entity.subStageLabel,
        planConfirmed = entity.planConfirmed,
        planProposedCount = entity.planProposedCount,
        hasStageCompleteMarker = entity.hasStageCompleteMarker,
        firstUserQuery = entity.firstUserQuery,
        planningResult = entity.planningResult,
        executionResult = entity.executionResult
    )

    fun domainToEntity(domain: ChatStateDomain): ChatStateEntity = ChatStateEntity(
        chatId = domain.chatId,
        stage = domain.stage.name,
        step = domain.step,
        totalSteps = domain.totalSteps,
        subStage = domain.subStage,
        subStagesTotal = domain.subStagesTotal,
        subStageLabel = domain.subStageLabel,
        planConfirmed = domain.planConfirmed,
        planProposedCount = domain.planProposedCount,
        hasStageCompleteMarker = domain.hasStageCompleteMarker,
        firstUserQuery = domain.firstUserQuery,
        planningResult = domain.planningResult,
        executionResult = domain.executionResult
    )

    private fun String.toChatStage(): ChatStage = try {
        ChatStage.valueOf(this)
    } catch (_: IllegalArgumentException) {
        ChatStage.PLANNING
    }
}