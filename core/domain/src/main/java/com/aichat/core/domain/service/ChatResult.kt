package com.aichat.core.domain.service

data class ChatResult(
    val fullText: String,
    val totalTokens: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val costUsd: Double,
    val promptCost: Double = 0.0,
    val completionCost: Double = 0.0
)
