package com.aichat.core.data.mapper

import com.aichat.core.database.entity.ProfileEntity
import com.aichat.core.domain.model.ProfileDomain

fun ProfileEntity.toDomain(): ProfileDomain = ProfileDomain(
    id = id,
    name = name,
    isActive = isActive
)

fun ProfileDomain.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    name = name,
    isActive = isActive
)
