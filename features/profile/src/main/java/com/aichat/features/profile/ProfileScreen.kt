package com.aichat.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.core.domain.model.ProfileDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSelectProfile: () -> Unit,
    onShowCreateProfile: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ProfileDomain?>(null) }

    LaunchedEffect(uiState.activeProfileId) {
        if (uiState.activeProfileId != null) {
            selectedProfileId = uiState.activeProfileId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профили") },
                navigationIcon = {
                    if (uiState.profiles.isNotEmpty()) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.profiles, key = { it.id }) { profile ->
                ProfileListItem(
                    profile = profile,
                    isSelected = profile.id == selectedProfileId,
                    showDelete = uiState.profiles.size > 1,
                    onSelect = { selectedProfileId = profile.id },
                    onDelete = { showDeleteConfirm = profile }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowCreateProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать новый профиль")
                }
            }
        }
    }

    // Кнопка "Выбрать профиль" — плавающая
    if (selectedProfileId != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    viewModel.switchProfile(selectedProfileId!!)
                    onSelectProfile()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Выбрать профиль")
            }
        }
    }

    // Диалог подтверждения удаления
    showDeleteConfirm?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удалить профиль") },
            text = {
                Text("Вы уверены, что хотите удалить профиль «${profile.name}»? Чаты будут удалены вместе с профилем.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = null
                        viewModel.deleteProfile(profile.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ProfileListItem(
    profile: ProfileDomain,
    isSelected: Boolean,
    showDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = profile.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить профиль",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}