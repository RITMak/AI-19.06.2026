package com.aichat.features.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ContextSettingsDialog(
    currentSummaryEvery: Int,
    onDismiss: () -> Unit,
    onApply: (summaryEvery: Int) -> Unit
) {
    var summaryEveryText by remember { mutableStateOf(currentSummaryEvery.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Summary Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Summarize every N pairs of messages. " +
                           "When this threshold is reached, old messages are compressed into a summary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = summaryEveryText,
                    onValueChange = { summaryEveryText = it.filter { c -> c.isDigit() } },
                    label = { Text("Summarize every N messages") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val summaryEvery = summaryEveryText.toIntOrNull() ?: 20
                    onApply(summaryEvery.coerceIn(1, 200))
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}