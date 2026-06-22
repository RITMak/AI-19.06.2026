package com.aichat.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val statusCode: Int, val responseBody: String) : Exception("HTTP $statusCode: $responseBody")

@Singleton
class AiApiClient @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun getModels(
        url: String,
        apiKey: String
    ): String {
        Log.d("AiApiClient", "GET $url")
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $apiKey")
        }
        checkResponse(response)
        val body = response.bodyAsText()
        Log.d("AiApiClient", "GET $url — $body")
        return body
    }

    suspend fun postChatRequest(
        url: String,
        apiKey: String,
        body: Any,
        referer: String? = null
    ): String {
        Log.d("AiApiClient", "POST $url — $body")
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $apiKey")
            if (referer != null) header("HTTP-Referer", referer)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        checkResponse(response)
        val responseBody = response.bodyAsText()
        Log.d("AiApiClient", "POST $url — $responseBody")
        return responseBody
    }

    suspend fun postStreamOnly(
        url: String,
        apiKey: String,
        bodyJson: String,
        referer: String? = null,
        onChunk: (line: String) -> Unit
    ) {
        Log.d("AiApiClient", "POST $url — $bodyJson")
        val response = httpClient.post(url) {
            header("Authorization", "Bearer $apiKey")
            if (referer != null) header("HTTP-Referer", referer)
            contentType(ContentType.Application.Json)
            setBody(bodyJson)
        }
        checkResponse(response)

        // Read entire response as text — handles both SSE and non-streaming JSON
        val fullBody = response.bodyAsText()
        val lines = fullBody.lines()
        var gotDataLine = false

        for (line in lines) {
            if (line.startsWith("data: ")) {
                gotDataLine = true
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                onChunk(data)
            }
        }

        // No SSE at all — server returned a single JSON object
        if (!gotDataLine && fullBody.isNotBlank()) {
            Log.d("AiApiClient", "Non-streaming $url, len=${fullBody.length}")
            onChunk(fullBody.trim())
        }
        Log.d("AiApiClient", "POST $url DONE")
    }

    private suspend fun checkResponse(response: HttpResponse) {
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw ApiException(response.status.value, body)
        }
    }
}