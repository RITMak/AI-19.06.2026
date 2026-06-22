package com.aichat.core.data.mapper

import com.aichat.core.database.entity.ChatEntity
import com.aichat.core.domain.model.ChatDomain
import com.aichat.core.model.AiProvider

fun ChatEntity.toDomain(): ChatDomain = ChatDomain(
    id = id,
    title = title,
    provider = AiProvider.valueOf(provider),
    modelId = modelId,
    profileId = profileId,
    workingMemoryId = workingMemoryId
)

fun ChatDomain.toEntity(): ChatEntity = ChatEntity(
    id = id,
    title = title,
    provider = provider.name,
    modelId = modelId,
    profileId = profileId,
    workingMemoryId = workingMemoryId
)
