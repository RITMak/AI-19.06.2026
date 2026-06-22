package com.aichat.core.di

import com.aichat.core.data.repository.ChatRepositoryImpl
import com.aichat.core.data.repository.ChatStateRepositoryImpl
import com.aichat.core.data.repository.ContextSettingsRepositoryImpl
import com.aichat.core.data.repository.ContextSummaryStorageImpl
import com.aichat.core.data.repository.InvariantRepositoryImpl
import com.aichat.core.data.repository.LongTermMemoryRepositoryImpl
import com.aichat.core.data.repository.MessageRepositoryImpl
import com.aichat.core.data.repository.ModelRepositoryImpl
import com.aichat.core.data.repository.ProfileRepositoryImpl
import com.aichat.core.data.repository.StageSummaryRepositoryImpl
import com.aichat.core.data.repository.WorkingMemoryRepositoryImpl
import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.domain.repository.ChatStateRepository
import com.aichat.core.domain.repository.ContextSettingsRepository
import com.aichat.core.domain.repository.InvariantRepository
import com.aichat.core.domain.repository.LongTermMemoryRepository
import com.aichat.core.domain.repository.MessageRepository
import com.aichat.core.domain.repository.ModelRepository
import com.aichat.core.domain.repository.ProfileRepository
import com.aichat.core.domain.repository.StageSummaryRepository
import com.aichat.core.domain.repository.WorkingMemoryRepository
import com.aichat.core.domain.service.ContextSummaryStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindContextSettingsRepository(impl: ContextSettingsRepositoryImpl): ContextSettingsRepository

    @Binds
    @Singleton
    abstract fun bindContextSummaryStorage(impl: ContextSummaryStorageImpl): ContextSummaryStorage

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindWorkingMemoryRepository(impl: WorkingMemoryRepositoryImpl): WorkingMemoryRepository

    @Binds
    @Singleton
    abstract fun bindLongTermMemoryRepository(impl: LongTermMemoryRepositoryImpl): LongTermMemoryRepository

    @Binds
    @Singleton
    abstract fun bindChatStateRepository(impl: ChatStateRepositoryImpl): ChatStateRepository

    @Binds
    @Singleton
    abstract fun bindStageSummaryRepository(impl: StageSummaryRepositoryImpl): StageSummaryRepository

    @Binds
    @Singleton
    abstract fun bindInvariantRepository(impl: InvariantRepositoryImpl): InvariantRepository
}
