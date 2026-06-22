package com.aichat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stage_summaries")
data class StageSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val taskNumber: Int,
    val stageOrder: Int,
    val stage: String,
    val summaryText: String,
    val createdAt: Long = System.currentTimeMillis()
)