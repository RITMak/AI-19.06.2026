package com.aichat.core.data.mapper

import com.aichat.core.database.entity.WorkingMemoryEntity
import com.aichat.core.domain.model.WorkingMemoryDomain

fun WorkingMemoryEntity.toDomain(): WorkingMemoryDomain = WorkingMemoryDomain(
    id = id,
    profileId = profileId,
    title = title
)

fun WorkingMemoryDomain.toEntity(): WorkingMemoryEntity = WorkingMemoryEntity(
    id = id,
    profileId = profileId,
    title = title
)