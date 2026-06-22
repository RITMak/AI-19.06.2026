package com.aichat.core.data.mapper

import com.aichat.core.database.entity.ModelEntity
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.model.AiProvider

fun ModelEntity.toDomain(): AiModelDomain = AiModelDomain(
    id = id,
    name = name,
    provider = AiProvider.valueOf(provider),
    isFree = isFree,
    inputPrice = inputPrice,
    outputPrice = outputPrice,
    contextLength = contextLength
)

fun AiModelDomain.toEntity(): ModelEntity = ModelEntity(
    id = id,
    name = name,
    provider = provider.name,
    isFree = isFree,
    inputPrice = inputPrice,
    outputPrice = outputPrice,
    contextLength = contextLength
)