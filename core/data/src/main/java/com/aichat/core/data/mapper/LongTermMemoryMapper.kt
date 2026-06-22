package com.aichat.core.data.mapper

import com.aichat.core.database.entity.LongTermMemoryEntity
import com.aichat.core.domain.model.LongTermMemoryDomain

fun LongTermMemoryEntity.toDomain(): LongTermMemoryDomain = LongTermMemoryDomain(
    id = id,
    profileId = profileId,
    key = key,
    value = value
)

fun LongTermMemoryDomain.toEntity(): LongTermMemoryEntity = LongTermMemoryEntity(
    id = id,
    profileId = profileId,
    key = key,
    value = value
)