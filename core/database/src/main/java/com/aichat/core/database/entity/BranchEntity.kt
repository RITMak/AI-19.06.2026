package com.aichat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val branchId: String,
    val chatId: String,
    val parentBranchId: String? = null,
    val name: String,
    val forkMessageId: String? = null
)