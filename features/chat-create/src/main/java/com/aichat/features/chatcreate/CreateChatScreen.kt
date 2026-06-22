package com.aichat.features.chatcreate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.core.domain.model.AiModelDomain
import com.aichat.core.domain.model.WorkingMemoryDomain
import com.aichat.core.model.AiProvider
import com.aichat.features.chats.WorkingMemoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatScreen(
    onBack: () -> Unit,
    onModelSelected: (chatId: String) -> Unit = {},
    viewModel: CreateChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredModels by remember {
        derivedStateOf {
            when (uiState.filter) {
                ModelFilter.FREE -> uiState.models.filter { it.isFree }
                ModelFilter.PAID -> uiState.models.filter { !it.isFree }
            }
        }
    }

    // Working memory dialog
    if (uiState.showWorkingDialog) {
        WorkingMemoryDialog(
            onConfirm = { title ->
                viewModel.addWorkingMemory(title)
            },
            onDismiss = { viewModel.toggleWorkingDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создать чат") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Error message
            if (uiState.error != null) {
                Text(
                    text = "Ошибка: ${uiState.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Provider selector
            Text("Провайдер", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiProvider.entries.forEach { provider ->
                    FilterChip(
                        selected = uiState.selectedProvider == provider,
                        onClick = { viewModel.selectProvider(provider) },
                        label = { Text(provider.displayName) },
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Free/Paid filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    ModelFilter.FREE -> "Бесплатные"
                                    ModelFilter.PAID -> "Платные"
                                }
                            )
                        },
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Model dropdown
            Text("Модель", style = MaterialTheme.typography.labelLarge)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val selectedModel = uiState.selectedModelForCreate

                ExposedDropdownMenuBox(
                    expanded = uiState.isModelDropdownExpanded,
                    onExpandedChange = {
                        if (!uiState.isLoading) {
                            viewModel.toggleModelDropdown()
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = selectedModel?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Выберите модель") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (uiState.isModelDropdownExpanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle dropdown"
                            )
                        },
                        textStyle = if (selectedModel != null)
                            MaterialTheme.typography.bodyLarge
                        else
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = uiState.isModelDropdownExpanded,
                        onDismissRequest = { viewModel.dismissModelDropdown() }
                    ) {
                        if (filteredModels.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (uiState.filter) {
                                            ModelFilter.FREE -> "Нет бесплатных моделей"
                                            ModelFilter.PAID -> "Нет платных моделей"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {}
                            )
                        } else {
                            filteredModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = model.name,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "Context: ${model.contextLength} tokens",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (model == uiState.selectedModelForCreate) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = { viewModel.selectModelForCreate(model) }
                                )
                            }
                        }
                    }
                }
            }

            // Task selection
            Text("Задача", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selectedTask = uiState.selectedWorkingMemory

            if (uiState.workingMemories.isNotEmpty()) {
                ExposedDropdownMenuBox(
                        expanded = uiState.isTaskDropdownExpanded,
                        onExpandedChange = {
                            if (!uiState.isLoading) {
                                viewModel.toggleTaskDropdown()
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedTask?.title ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .menuAnchor(),
                            placeholder = { Text("Выберите задачу") },
                            textStyle = if (selectedTask != null)
                                MaterialTheme.typography.bodyLarge
                            else
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                            trailingIcon = {
                                Icon(
                                    if (uiState.isTaskDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select task"
                                )
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.isTaskDropdownExpanded,
                            onDismissRequest = { viewModel.dismissTaskDropdown() }
                        ) {
                            uiState.workingMemories.forEach { memory ->
                                DropdownMenuItem(
                                    text = {
                                        Text(memory.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    },
                                    onClick = {
                                        viewModel.selectWorkingMemory(memory)
                                        viewModel.dismissTaskDropdown()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Создайте задачу") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(onClick = { viewModel.toggleWorkingDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Новая задача", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create chat button
            Button(
                onClick = {
                    viewModel.createChat { chatId -> onModelSelected(chatId) }
                },
                enabled = uiState.selectedModelForCreate != null && uiState.selectedWorkingMemory != null && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Создать чат")
            }
        }
    }
}