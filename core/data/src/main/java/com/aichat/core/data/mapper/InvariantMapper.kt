package com.aichat.core.data.mapper

import com.aichat.core.database.entity.InvariantEntity
import com.aichat.core.domain.model.Invariant

fun InvariantEntity.toDomain(): Invariant = Invariant(
    id = id,
    profileId = profileId,
    text = text,
    createdAt = createdAt
)

fun Invariant.toEntity(): InvariantEntity = InvariantEntity(
    id = id,
    profileId = profileId,
    text = text,
    createdAt = createdAt
)