package com.aichat.core.domain.model

data class BranchData(
    val branchId: String,
    val chatId: String,
    val parentBranchId: String? = null,
    val name: String,
    val forkMessageId: String? = null
)