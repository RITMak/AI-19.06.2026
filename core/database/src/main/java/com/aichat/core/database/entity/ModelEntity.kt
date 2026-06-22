package com.aichat.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "provider") val provider: String,
    @ColumnInfo(name = "is_free") val isFree: Boolean,
    @ColumnInfo(name = "input_price") val inputPrice: Double,
    @ColumnInfo(name = "output_price") val outputPrice: Double,
    @ColumnInfo(name = "context_length") val contextLength: Int
)