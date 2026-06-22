package com.aichat.core.model

data class Chat(
    val id: String,
    val title: String,
    val provider: AiProvider,
    val modelId: String
)