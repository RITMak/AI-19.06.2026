package com.aichat.features.models.provider

interface AiProviderService

data class ChatResult(
    val fullText: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val costUsd: Double
)