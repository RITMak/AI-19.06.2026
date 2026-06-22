package com.aichat.core.domain.model

class AuthException(message: String) : Exception(message)
class RateLimitException(val retryAfterMs: Long) : Exception("Rate limited")
class ApiCallException(val statusCode: Int, message: String) : Exception(message)
class NetworkException(cause: Throwable) : Exception("Network error", cause)
class NetworkMessageException(message: String) : Exception(message)
