package com.aichat.core.domain.usecase

import com.aichat.core.domain.repository.ChatRepository
import com.aichat.core.domain.repository.MessageRepository
import javax.inject.Inject

class DeleteChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(chatId: String) {
        messageRepository.deleteMessagesByChatId(chatId)
        chatRepository.deleteChat(chatId)
    }
}