package com.aichat.features.chats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    dismissable: Boolean = true
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        title = { Text("Создать профиль") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Введите имя профиля") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            if (dismissable) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}