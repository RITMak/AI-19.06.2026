package com.aichat.core.domain.model

enum class ChatStage(val label: String) {
    PLANNING("🧠 Планирование"),
    EXECUTION("⚡ Выполнение"),
    DONE("🎉 Завершено");

    val allowedTransitions: Set<ChatStage>
        get() = when (this) {
            PLANNING -> setOf(EXECUTION)
            EXECUTION -> setOf(DONE, PLANNING)
            DONE -> setOf(EXECUTION, PLANNING) // PLANNING — только новый промт пользователя
        }

    val allowedLlmTransitions: Set<ChatStage>
        get() = when (this) {
            PLANNING -> setOf(EXECUTION)
            EXECUTION -> setOf(DONE)
            DONE -> setOf(EXECUTION) // reopen, PLANNING — только пользователь
        }

    fun canTransitionTo(target: ChatStage): Boolean = target in allowedTransitions

    fun canLlmTransitionTo(target: ChatStage): Boolean = target in allowedLlmTransitions
}