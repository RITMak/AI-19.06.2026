package com.aichat.core.domain.repository

import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.ChatStateDomain
import kotlinx.coroutines.flow.Flow

interface ChatStateRepository {

    suspend fun get(chatId: String): ChatStateDomain?

    fun observe(chatId: String): Flow<ChatStateDomain?>

    suspend fun setStage(chatId: String, stage: ChatStage)

    suspend fun setStep(chatId: String, step: Int)

    suspend fun setTotalSteps(chatId: String, totalSteps: Int)

    suspend fun upsert(state: ChatStateDomain)

    suspend fun delete(chatId: String)
}