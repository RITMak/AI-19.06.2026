package com.aichat.di

import com.aichat.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @Singleton
    @Named("openrouter_default_key")
    fun provideOpenRouterDefaultKey(): String = BuildConfig.OPENROUTER_API_KEY

    @Provides
    @Singleton
    @Named("deepseek_default_key")
    fun provideDeepSeekDefaultKey(): String = BuildConfig.DEEPSEEK_API_KEY
}