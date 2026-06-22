package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContextSummaryEntity(
    @PrimaryKey @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "summary_text") val summaryText: String,
    @ColumnInfo(name = "message_count") val messageCount: Int,
    @ColumnInfo(name = "last_summarized_user_msg_count") val lastSummarizedUserMsgCount: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)