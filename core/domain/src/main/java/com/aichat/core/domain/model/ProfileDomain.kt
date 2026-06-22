package com.aichat.core.domain.model

data class ProfileDomain(
    val id: String,
    val name: String,
    val isActive: Boolean = false
)
