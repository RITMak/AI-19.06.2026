package com.aichat.core.di

import com.aichat.core.network.AiApiClient
import com.aichat.core.network.DeepSeekModelApiService
import com.aichat.core.network.ModelApiService
import com.aichat.core.network.OpenRouterModelApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    @Provides
    @Singleton
    fun provideAiApiClient(
        httpClient: HttpClient
    ): AiApiClient = AiApiClient(httpClient)

    @Provides
    @Singleton
    @IntoMap
    @StringKey("OPENROUTER")
    fun provideOpenRouterModelApiService(service: OpenRouterModelApiService): ModelApiService = service

    @Provides
    @Singleton
    @IntoMap
    @StringKey("DEEPSEEK")
    fun provideDeepSeekModelApiService(service: DeepSeekModelApiService): ModelApiService = service
}