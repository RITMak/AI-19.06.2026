package com.aichat.core.domain.model

data class ContextSettingsDomain(
    val chatId: String,
    val summaryEvery: Int = 20
)
