package com.aichat.core.domain.model

sealed interface AiError {
    data class ApiError(val code: Int, val message: String) : AiError
    data class AuthError(val message: String) : AiError
    data class RateLimit(val retryAfterMs: Long = 30000L) : AiError
    data class NetworkError(val cause: Throwable) : AiError
    data object UnknownError : AiError
}