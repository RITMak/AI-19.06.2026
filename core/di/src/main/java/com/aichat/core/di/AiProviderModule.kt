package com.aichat.core.di

import com.aichat.core.domain.service.AiProviderService
import com.aichat.features.models.provider.DeepSeekProvider
import com.aichat.features.models.provider.OpenRouterProvider
import com.aichat.core.model.AiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiProviderModule {

    @Provides
    @Singleton
    @IntoMap
    @StringKey("OPENROUTER")
    fun provideOpenRouterProvider(provider: OpenRouterProvider): AiProviderService = provider

    @Provides
    @Singleton
    @IntoMap
    @StringKey("DEEPSEEK")
    fun provideDeepSeekProvider(provider: DeepSeekProvider): AiProviderService = provider
}