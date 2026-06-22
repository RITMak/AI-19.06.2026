package com.aichat.core.domain.model

data class LongTermMemoryDomain(
    val id: String,
    val profileId: String,
    val key: String,
    val value: String
)