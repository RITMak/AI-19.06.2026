package com.aichat.features.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.model.Role

@Composable
fun MessageBubbleMenu(
    message: MessageDomain,
    onDismiss: () -> Unit,
    onEditAndResend: (() -> Unit)? = null,
    onCopyText: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        if (message.role == Role.USER) {
            DropdownMenuItem(
                text = { Text("✏️ Edit & Re-send") },
                onClick = {
                    onEditAndResend?.invoke()
                    onDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("📋 Copy") },
            onClick = {
                onCopyText()
                onDismiss()
            }
        )
    }
}