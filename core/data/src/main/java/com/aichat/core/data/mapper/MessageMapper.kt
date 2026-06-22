package com.aichat.core.data.mapper

import com.aichat.core.database.entity.MessageEntity
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.model.MessageType
import com.aichat.core.model.Role

fun MessageEntity.toDomain(): MessageDomain = MessageDomain(
    id = id,
    chatId = chatId,
    role = Role.valueOf(role),
    messageType = MessageType.valueOf(messageType),
    text = text,
    textClean = textClean,
    totalTokens = totalTokens,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    costUsd = costUsd,
    promptCost = promptCost,
    completionCost = completionCost
)

fun MessageDomain.toEntity(): MessageEntity = MessageEntity(
    id = id,
    chatId = chatId,
    role = role.name,
    messageType = messageType.name,
    text = text,
    textClean = textClean,
    totalTokens = totalTokens,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    costUsd = costUsd,
    promptCost = promptCost,
    completionCost = completionCost
)
