package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_settings",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContextSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "summary_every") val summaryEvery: Int = 20
)
