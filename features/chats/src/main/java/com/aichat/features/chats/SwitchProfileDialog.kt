package com.aichat.features.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aichat.core.domain.model.ProfileDomain

@Composable
fun SwitchProfileDialog(
    profiles: List<ProfileDomain>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onDeleteProfile: (ProfileDomain) -> Unit,
    onShowCreateDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProfileId by remember { mutableStateOf(activeProfileId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите профиль") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfileId = profile.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = profile.id == selectedProfileId,
                            onClick = { selectedProfileId = profile.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profile.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (profiles.size > 1) {
                            IconButton(onClick = { onDeleteProfile(profile) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить профиль",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { onShowCreateDialog() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Создать новый профиль")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedProfileId?.let { onSelectProfile(it) }
                },
                enabled = selectedProfileId != null
            ) {
                Text("Выбрать профиль")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}