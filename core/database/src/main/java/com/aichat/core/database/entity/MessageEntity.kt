package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aichat.core.model.MessageType

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chat_id")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "message_type") val messageType: String = "USER_MESSAGE",
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "text_clean") val textClean: String,
    @ColumnInfo(name = "total_tokens") val totalTokens: Int,
    @ColumnInfo(name = "prompt_tokens") val promptTokens: Int,
    @ColumnInfo(name = "completion_tokens") val completionTokens: Int,
    @ColumnInfo(name = "cost_usd") val costUsd: Double,
    @ColumnInfo(name = "prompt_cost") val promptCost: Double = 0.0,
    @ColumnInfo(name = "completion_cost") val completionCost: Double = 0.0
)
