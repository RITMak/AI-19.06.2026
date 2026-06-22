package com.aichat.core.domain.model

import com.aichat.core.model.AiProvider

data class AiModelDomain(
    val id: String,
    val name: String,
    val provider: AiProvider,
    val isFree: Boolean = false,
    val inputPrice: Double = 0.0,
    val outputPrice: Double = 0.0,
    val contextLength: Int = 0
)