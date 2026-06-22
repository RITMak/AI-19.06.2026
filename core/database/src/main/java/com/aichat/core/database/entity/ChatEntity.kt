package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkingMemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["working_memory_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("profile_id"), Index("working_memory_id")]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "provider") val provider: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "profile_id") val profileId: String,
    @ColumnInfo(name = "working_memory_id") val workingMemoryId: String? = null
)
