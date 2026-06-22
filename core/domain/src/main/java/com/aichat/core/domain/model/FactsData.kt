package com.aichat.core.domain.model

data class FactsData(
    val chatId: String,
    val goals: String = "",
    val constraints: String = "",
    val preferences: String = "",
    val decisions: String = "",
    val agreements: String = ""
)