package com.aichat.core.domain.model

import com.aichat.core.model.MessageType
import com.aichat.core.model.Role

data class MessageDomain(
    val id: String,
    val chatId: String,
    val role: Role,
    val messageType: MessageType = MessageType.USER_MESSAGE,
    val text: String,
    val textClean: String = "",
    val totalTokens: Int = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val costUsd: Double = 0.0,
    val promptCost: Double = 0.0,
    val completionCost: Double = 0.0
)
