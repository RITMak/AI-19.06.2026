package com.aichat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.aichat.core.database.entity.ChatEntity
import com.aichat.core.database.entity.ChatStateEntity
import com.aichat.core.database.entity.ContextSettingsEntity
import com.aichat.core.database.entity.ContextSummaryEntity
import com.aichat.core.database.entity.InvariantEntity
import com.aichat.core.database.entity.LongTermMemoryEntity
import com.aichat.core.database.entity.MessageEntity
import com.aichat.core.database.entity.ModelEntity
import com.aichat.core.database.entity.ProfileEntity
import com.aichat.core.database.entity.StageSummaryEntity
import com.aichat.core.database.entity.WorkingMemoryEntity

@Database(
    entities = [
        ChatEntity::class,
        ChatStateEntity::class,
        MessageEntity::class,
        ModelEntity::class,
        ContextSettingsEntity::class,
        ContextSummaryEntity::class,
        ProfileEntity::class,
        StageSummaryEntity::class,
        WorkingMemoryEntity::class,
        LongTermMemoryEntity::class,
        InvariantEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AiChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun chatStateDao(): ChatStateDao
    abstract fun messageDao(): MessageDao
    abstract fun modelDao(): ModelDao
    abstract fun contextSettingsDao(): ContextSettingsDao
    abstract fun contextSummaryDao(): ContextSummaryDao
    abstract fun profileDao(): ProfileDao
    abstract fun stageSummaryDao(): StageSummaryDao
    abstract fun workingMemoryDao(): WorkingMemoryDao
    abstract fun longTermMemoryDao(): LongTermMemoryDao
    abstract fun invariantDao(): InvariantDao

    companion object {
        // No migrations — fresh install
    }
}