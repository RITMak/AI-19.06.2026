package com.aichat.core.model

data class Message(
    val id: String,
    val chatId: String,
    val role: Role,
    val text: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val costUsd: Double = 0.0
)