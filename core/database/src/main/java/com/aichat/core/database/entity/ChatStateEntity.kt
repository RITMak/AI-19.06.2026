package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_state",
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
data class ChatStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "stage") val stage: String,
    @ColumnInfo(name = "step") val step: Int = 0,
    @ColumnInfo(name = "total_steps") val totalSteps: Int = 0,
    @ColumnInfo(name = "sub_stage") val subStage: Int = 0,
    @ColumnInfo(name = "sub_stages_total") val subStagesTotal: Int = 0,
    @ColumnInfo(name = "sub_stage_label") val subStageLabel: String = "",
    @ColumnInfo(name = "plan_confirmed") val planConfirmed: Boolean = false,
    @ColumnInfo(name = "plan_proposed_count") val planProposedCount: Int = 0,
    @ColumnInfo(name = "has_stage_complete_marker") val hasStageCompleteMarker: Boolean = false,
    @ColumnInfo(name = "first_user_query") val firstUserQuery: String = "",
    @ColumnInfo(name = "planning_result") val planningResult: String = "",
    @ColumnInfo(name = "execution_result") val executionResult: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
