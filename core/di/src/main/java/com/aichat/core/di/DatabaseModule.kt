package com.aichat.core.di

import android.content.Context
import androidx.room.Room
import com.aichat.core.database.AiChatDatabase
import com.aichat.core.database.dao.ChatDao
import com.aichat.core.database.dao.ChatStateDao
import com.aichat.core.database.dao.ContextSettingsDao
import com.aichat.core.database.dao.ContextSummaryDao
import com.aichat.core.database.dao.InvariantDao
import com.aichat.core.database.dao.LongTermMemoryDao
import com.aichat.core.database.dao.MessageDao
import com.aichat.core.database.dao.ModelDao
import com.aichat.core.database.dao.ProfileDao
import com.aichat.core.database.dao.StageSummaryDao
import com.aichat.core.database.dao.WorkingMemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AiChatDatabase {
        return Room.databaseBuilder(
            context,
            AiChatDatabase::class.java,
            "aichat.db"
        ).build()
    }

    @Provides
    fun provideChatDao(database: AiChatDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideMessageDao(database: AiChatDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideModelDao(database: AiChatDatabase): ModelDao = database.modelDao()

    @Provides
    fun provideContextSettingsDao(database: AiChatDatabase): ContextSettingsDao = database.contextSettingsDao()

    @Provides
    fun provideContextSummaryDao(database: AiChatDatabase): ContextSummaryDao = database.contextSummaryDao()

    @Provides
    fun provideProfileDao(database: AiChatDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideWorkingMemoryDao(database: AiChatDatabase): WorkingMemoryDao = database.workingMemoryDao()

    @Provides
    fun provideLongTermMemoryDao(database: AiChatDatabase): LongTermMemoryDao = database.longTermMemoryDao()

    @Provides
    fun provideChatStateDao(database: AiChatDatabase): ChatStateDao = database.chatStateDao()

    @Provides
    fun provideStageSummaryDao(database: AiChatDatabase): StageSummaryDao = database.stageSummaryDao()

    @Provides
    fun provideInvariantDao(database: AiChatDatabase): InvariantDao = database.invariantDao()
}
